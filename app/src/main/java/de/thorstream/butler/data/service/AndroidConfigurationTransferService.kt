package de.thorstream.butler.data.service

import android.content.Context
import androidx.core.net.toUri
import androidx.room.withTransaction
import dagger.hilt.android.qualifiers.ApplicationContext
import de.thorstream.butler.R
import de.thorstream.butler.core.common.AppError
import de.thorstream.butler.core.common.AppResult
import de.thorstream.butler.core.common.StringProvider
import de.thorstream.butler.core.validation.NetworkValidators
import de.thorstream.butler.domain.model.AppSettings
import de.thorstream.butler.domain.model.ConnectionType
import de.thorstream.butler.domain.model.LocalHost
import de.thorstream.butler.domain.model.NetworkMeasurement
import de.thorstream.butler.domain.model.NetworkQuality
import de.thorstream.butler.domain.model.NetworkSnapshot
import de.thorstream.butler.domain.model.QualityAssessment
import de.thorstream.butler.domain.model.StreamingEntry
import de.thorstream.butler.domain.model.StreamingProfile
import de.thorstream.butler.domain.model.StreamingResolution
import de.thorstream.butler.domain.model.StreamingType
import de.thorstream.butler.domain.model.ThemePreference
import de.thorstream.butler.domain.repository.LocalHostRepository
import de.thorstream.butler.domain.repository.DiagnosticEvent
import de.thorstream.butler.domain.repository.DiagnosticLogRepository
import de.thorstream.butler.domain.repository.NetworkHistoryRepository
import de.thorstream.butler.domain.repository.SettingsRepository
import de.thorstream.butler.domain.repository.StreamingEntryRepository
import de.thorstream.butler.domain.service.ConfigurationTransferService
import de.thorstream.butler.domain.service.ConfigurationTransferSummary
import de.thorstream.butler.data.database.ThorDatabase
import java.io.ByteArrayOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

@Singleton
class AndroidConfigurationTransferService @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val entriesRepository: StreamingEntryRepository,
    private val hostsRepository: LocalHostRepository,
    private val historyRepository: NetworkHistoryRepository,
    private val settingsRepository: SettingsRepository,
    private val strings: StringProvider,
    private val diagnosticLogRepository: DiagnosticLogRepository,
    private val database: ThorDatabase,
) : ConfigurationTransferService {
    override suspend fun exportTo(documentUri: String, includeHistory: Boolean): AppResult<ConfigurationTransferSummary> = withContext(Dispatchers.IO) {
        try {
            val entries = entriesRepository.getEntries()
            val hosts = hostsRepository.getHosts()
            val history = if (includeHistory) historyRepository.getHistory() else emptyList()
            val root = JSONObject()
                .put("format", FORMAT)
                .put("schemaVersion", SCHEMA_VERSION)
                .put("exportedAt", System.currentTimeMillis())
                .put("settings", settingsRepository.settings.first().toJson())
                .put("hosts", JSONArray().apply { hosts.forEach { put(it.toJson()) } })
                .put("entries", JSONArray().apply { entries.forEach { put(it.toJson()) } })
            if (includeHistory) root.put("history", JSONArray().apply { history.forEach { put(it.toJson()) } })

            val uri = documentUri.toUri()
            context.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter(Charsets.UTF_8)?.use { writer ->
                writer.write(root.toString(2))
            } ?: throw IOException("Unable to open output document")
            logSafely(DiagnosticEvent.CONFIGURATION_EXPORTED)
            AppResult.Success(ConfigurationTransferSummary(entries.size, hosts.size, history.size))
        } catch (error: Throwable) {
            if (error is kotlinx.coroutines.CancellationException) throw error
            AppResult.Failure(AppError.Technical(strings.get(R.string.settings_transfer_export_failed)))
        }
    }

    override suspend fun importFrom(documentUri: String): AppResult<ConfigurationTransferSummary> = withContext(Dispatchers.IO) {
        try {
            val uri = documentUri.toUri()
            val content = readUtf8WithLimit(uri.toString())
            val root = JSONObject(content)
            require(root.getString("format") == FORMAT)
            require(root.getInt("schemaVersion") in 1..SCHEMA_VERSION)

            val hostArray = root.getJSONArray("hosts").requireMaxSize(MAX_HOSTS)
            val entryArray = root.getJSONArray("entries").requireMaxSize(MAX_ENTRIES)
            val historyArray = root.optJSONArray("history")?.requireMaxSize(MAX_HISTORY)
            val hosts = hostArray.mapObjects { it.toLocalHost() }
            requireUniquePositiveIds(hosts.map(LocalHost::id))
            val validHostIds = hosts.map { it.id }.toSet()
            val entries = entryArray.mapObjects { it.toStreamingEntry(validHostIds) }
                .mapIndexed { index, entry -> entry.copy(sortOrder = index) }
            requireUniquePositiveIds(entries.map(StreamingEntry::id))
            val history = historyArray?.mapObjects { it.toMeasurement() }
            history?.let { requireUniquePositiveIds(it.map(NetworkMeasurement::id)) }
            val settings = root.getJSONObject("settings").toAppSettings()

            replaceConfigurationSafely(entries, hosts, history, settings)
            logSafely(DiagnosticEvent.CONFIGURATION_IMPORTED)
            AppResult.Success(ConfigurationTransferSummary(entries.size, hosts.size, history?.size ?: 0))
        } catch (error: Throwable) {
            if (error is kotlinx.coroutines.CancellationException) throw error
            AppResult.Failure(AppError.InvalidInput(strings.get(R.string.settings_transfer_invalid)))
        }
    }

    private fun LocalHost.toJson() = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("address", address)
        .putNullable("macAddress", macAddress)
        .putNullable("port", port)
        .put("streamingType", streamingType.name)
        .put("wakeOnLanEnabled", wakeOnLanEnabled)
        .put("broadcastAddress", broadcastAddress)
        .putNullable("lastReachable", lastReachable)
        .putNullable("lastSuccessfulTestAt", lastSuccessfulTestAt)

    private fun JSONObject.toLocalHost(): LocalHost {
        val address = NetworkValidators.normalizeHost(getString("address"))
        val mac = nullableString("macAddress")
        val broadcast = NetworkValidators.normalizeHost(optString("broadcastAddress", "255.255.255.255"))
        val name = getString("name").trim()
        require(name.isNotBlank() && name.length <= MAX_NAME_LENGTH)
        require(address.length <= MAX_HOST_LENGTH)
        require(NetworkValidators.isValidHostnameOrIp(address))
        require(mac == null || NetworkValidators.isValidMac(mac))
        require(broadcast.length <= MAX_HOST_LENGTH)
        require(NetworkValidators.isValidHostnameOrIp(broadcast))
        return LocalHost(
            id = getLong("id"),
            name = name,
            address = address,
            macAddress = mac?.let(NetworkValidators::normalizeMac),
            port = nullableInt("port")?.also { require(it in 1..65_535) },
            streamingType = enumValue(optString("streamingType"), StreamingType.CUSTOM),
            wakeOnLanEnabled = optBoolean("wakeOnLanEnabled", false),
            broadcastAddress = broadcast,
            lastReachable = nullableBoolean("lastReachable"),
            lastSuccessfulTestAt = nullableLong("lastSuccessfulTestAt"),
        )
    }

    private fun StreamingEntry.toJson() = JSONObject()
        .put("id", id)
        .put("displayName", displayName)
        .put("packageName", packageName)
        .put("iconReference", iconReference)
        .put("streamingType", streamingType.name)
        .putNullable("customName", customName)
        .putNullable("hostId", hostId)
        .put("profile", JSONObject()
            .put("resolution", profile.resolution.name)
            .put("framesPerSecond", profile.framesPerSecond)
            .put("bitrateMbps", profile.bitrateMbps))
        .put("sortOrder", sortOrder)
        .putNullable("lastUsedAt", lastUsedAt)
        .putNullable("lastNetworkQuality", lastNetworkQuality?.name)
        .put("isDemo", isDemo)

    private fun JSONObject.toStreamingEntry(validHostIds: Set<Long>): StreamingEntry {
        val packageName = getString("packageName").trim()
        val requestedHost = nullableLong("hostId")
        val profile = optJSONObject("profile") ?: JSONObject()
        val displayName = getString("displayName").trim()
        val customName = nullableString("customName")?.trim()?.takeIf(String::isNotEmpty)
        require(packageName.length in 1..MAX_PACKAGE_LENGTH && PACKAGE_NAME.matches(packageName))
        require(displayName.isNotBlank() && displayName.length <= MAX_NAME_LENGTH)
        require(customName == null || customName.length <= MAX_NAME_LENGTH)
        require(requestedHost == null || requestedHost in validHostIds)
        return StreamingEntry(
            id = getLong("id"),
            displayName = displayName,
            packageName = packageName,
            iconReference = "package://$packageName",
            streamingType = enumValue(optString("streamingType"), StreamingType.CUSTOM),
            customName = customName,
            hostId = requestedHost,
            profile = StreamingProfile(
                resolution = enumValue(profile.optString("resolution"), StreamingResolution.AUTO),
                framesPerSecond = profile.optInt("framesPerSecond", 60).coerceIn(30, 120),
                bitrateMbps = profile.optInt("bitrateMbps", 20).coerceIn(1, 200),
            ),
            sortOrder = optInt("sortOrder", 0),
            lastUsedAt = nullableLong("lastUsedAt"),
            lastNetworkQuality = nullableString("lastNetworkQuality")?.let { enumValue(it, NetworkQuality.NOT_MEASURABLE) },
            isDemo = optBoolean("isDemo", false),
        )
    }

    private fun AppSettings.toJson() = JSONObject()
        .put("preLaunchCheckEnabled", preLaunchCheckEnabled)
        .put("autoLaunchOnGreen", autoLaunchOnGreen)
        .put("warnOnYellow", warnOnYellow)
        .put("confirmOnRed", confirmOnRed)
        .put("defaultTestTarget", defaultTestTarget)
        .put("pingCount", pingCount)
        .put("testDurationSeconds", testDurationSeconds)
        .put("downloadTestEnabled", downloadTestEnabled)
        .put("theme", theme.name)
        .put("focusAnimationsEnabled", focusAnimationsEnabled)
        .put("diagnosticLoggingEnabled", diagnosticLoggingEnabled)

    private fun JSONObject.toAppSettings() = AppSettings(
        preLaunchCheckEnabled = optBoolean("preLaunchCheckEnabled", true),
        autoLaunchOnGreen = optBoolean("autoLaunchOnGreen", true),
        warnOnYellow = optBoolean("warnOnYellow", true),
        confirmOnRed = optBoolean("confirmOnRed", true),
        defaultTestTarget = NetworkValidators.normalizeHost(optString("defaultTestTarget", "1.1.1.1"))
            .takeIf(NetworkValidators::isValidHostnameOrIp) ?: "1.1.1.1",
        pingCount = optInt("pingCount", 5).coerceIn(1, 20),
        testDurationSeconds = optInt("testDurationSeconds", 5).coerceIn(1, 15),
        downloadTestEnabled = optBoolean("downloadTestEnabled", false),
        theme = enumValue(optString("theme"), ThemePreference.DARK),
        focusAnimationsEnabled = optBoolean("focusAnimationsEnabled", true),
        diagnosticLoggingEnabled = optBoolean("diagnosticLoggingEnabled", false),
    )

    private fun NetworkMeasurement.toJson() = JSONObject()
        .put("id", id)
        .put("timestamp", timestamp)
        .put("snapshot", snapshot.toJson())
        .put("assessment", JSONObject()
            .put("quality", assessment.quality.name)
            .put("summary", assessment.summary)
            .put("problems", JSONArray(assessment.problems))
            .put("recommendations", JSONArray(assessment.recommendations)))

    private fun NetworkSnapshot.toJson() = JSONObject()
        .put("connectionType", connectionType.name)
        .putNullable("localIpAddress", localIpAddress)
        .putNullable("gateway", gateway)
        .putNullable("ssid", ssid)
        .putNullable("wifiFrequencyMhz", wifiFrequencyMhz)
        .putNullable("linkSpeedMbps", linkSpeedMbps)
        .putNullable("signalStrengthPercent", signalStrengthPercent)
        .putNullable("internetValidated", internetValidated)
        .putNullable("dnsReachable", dnsReachable)
        .putNullable("latencyMs", latencyMs)
        .putNullable("jitterMs", jitterMs)
        .putNullable("packetLossPercent", packetLossPercent)
        .putNullable("downloadMbps", downloadMbps)
        .putNullable("hostReachable", hostReachable)
        .putNullable("host", host)

    private fun JSONObject.toMeasurement(): NetworkMeasurement {
        val snapshot = getJSONObject("snapshot")
        val assessment = getJSONObject("assessment")
        val timestamp = getLong("timestamp")
        val localIpAddress = snapshot.nullableString("localIpAddress").validatedText(MAX_HOST_LENGTH)
        val gateway = snapshot.nullableString("gateway").validatedText(MAX_HOST_LENGTH)
        val ssid = snapshot.nullableString("ssid").validatedText(MAX_SSID_LENGTH)
        val host = snapshot.nullableString("host").validatedText(MAX_HOST_LENGTH)
        val wifiFrequencyMhz = snapshot.nullableInt("wifiFrequencyMhz").validatedRange(1, 100_000)
        val linkSpeedMbps = snapshot.nullableInt("linkSpeedMbps").validatedRange(0, 1_000_000)
        val signalStrengthPercent = snapshot.nullableInt("signalStrengthPercent").validatedRange(0, 100)
        val latencyMs = snapshot.nullableDouble("latencyMs").validatedRange(0.0, MAX_MEASUREMENT_VALUE)
        val jitterMs = snapshot.nullableDouble("jitterMs").validatedRange(0.0, MAX_MEASUREMENT_VALUE)
        val packetLossPercent = snapshot.nullableDouble("packetLossPercent").validatedRange(0.0, 100.0)
        val downloadMbps = snapshot.nullableDouble("downloadMbps").validatedRange(0.0, MAX_MEASUREMENT_VALUE)
        val summary = assessment.optString("summary").also { require(it.length <= MAX_SUMMARY_LENGTH) }
        val problems = assessment.optJSONArray("problems")?.validatedStringList().orEmpty()
        val recommendations = assessment.optJSONArray("recommendations")?.validatedStringList().orEmpty()
        require(timestamp > 0)
        return NetworkMeasurement(
            id = getLong("id"),
            timestamp = timestamp,
            snapshot = NetworkSnapshot(
                connectionType = enumValue(snapshot.optString("connectionType"), ConnectionType.NONE),
                localIpAddress = localIpAddress,
                gateway = gateway,
                ssid = ssid,
                wifiFrequencyMhz = wifiFrequencyMhz,
                linkSpeedMbps = linkSpeedMbps,
                signalStrengthPercent = signalStrengthPercent,
                internetValidated = snapshot.nullableBoolean("internetValidated"),
                dnsReachable = snapshot.nullableBoolean("dnsReachable"),
                latencyMs = latencyMs,
                jitterMs = jitterMs,
                packetLossPercent = packetLossPercent,
                downloadMbps = downloadMbps,
                hostReachable = snapshot.nullableBoolean("hostReachable"),
                host = host,
            ),
            assessment = QualityAssessment(
                quality = enumValue(assessment.optString("quality"), NetworkQuality.NOT_MEASURABLE),
                summary = summary,
                problems = problems,
                recommendations = recommendations,
            ),
        )
    }

    private suspend fun replaceConfigurationSafely(
        entries: List<StreamingEntry>,
        hosts: List<LocalHost>,
        history: List<NetworkMeasurement>?,
        settings: AppSettings,
    ) {
        val previousEntries = entriesRepository.getEntries()
        val previousHosts = hostsRepository.getHosts()
        val previousHistory = historyRepository.getAllHistory()
        val previousSettings = settingsRepository.settings.first()

        try {
            database.withTransaction {
                hostsRepository.replaceAll(hosts)
                entriesRepository.replaceAll(entries)
                history?.let { historyRepository.replaceAll(it) }
            }
            settingsRepository.update(settings)
        } catch (error: Throwable) {
            // Cancellation is honored during the import. Restoration itself must
            // finish so the local configuration can never remain half replaced.
            withContext(NonCancellable) {
                runCatching {
                    database.withTransaction {
                        hostsRepository.replaceAll(previousHosts)
                        entriesRepository.replaceAll(previousEntries)
                        historyRepository.replaceAll(previousHistory)
                    }
                    settingsRepository.update(previousSettings)
                }.exceptionOrNull()?.let(error::addSuppressed)
            }
            throw error
        }
    }

    private fun readUtf8WithLimit(documentUri: String): String {
        val uri = documentUri.toUri()
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Unable to open input document")
        return input.use { stream ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var total = 0
            while (true) {
                val read = stream.read(buffer)
                if (read < 0) break
                total += read
                require(total <= MAX_IMPORT_BYTES)
                output.write(buffer, 0, read)
            }
            output.toString(Charsets.UTF_8.name())
        }
    }

    private fun requireUniquePositiveIds(ids: List<Long>) {
        require(ids.all { it > 0 })
        require(ids.distinct().size == ids.size)
    }

    private suspend fun logSafely(event: DiagnosticEvent) {
        try {
            diagnosticLogRepository.log(event)
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // An optional local log must not change the import/export result.
        }
    }

    private fun JSONArray.requireMaxSize(maxSize: Int): JSONArray = apply { require(length() <= maxSize) }
    private fun JSONArray.validatedStringList(): List<String> {
        require(length() <= MAX_ASSESSMENT_ITEMS)
        return List(length()) { index -> getString(index).also { require(it.length <= MAX_ASSESSMENT_ITEM_LENGTH) } }
    }

    private fun String?.validatedText(maxLength: Int): String? = this?.also { require(it.length <= maxLength) }
    private fun Int?.validatedRange(min: Int, max: Int): Int? = this?.also { require(it in min..max) }
    private fun Double?.validatedRange(min: Double, max: Double): Double? = this?.also {
        require(it.isFinite() && it in min..max)
    }

    private fun JSONObject.putNullable(key: String, value: Any?): JSONObject = put(key, value ?: JSONObject.NULL)
    private fun JSONObject.nullableString(key: String): String? = if (has(key) && !isNull(key)) getString(key) else null
    private fun JSONObject.nullableInt(key: String): Int? = if (has(key) && !isNull(key)) getInt(key) else null
    private fun JSONObject.nullableLong(key: String): Long? = if (has(key) && !isNull(key)) getLong(key) else null
    private fun JSONObject.nullableDouble(key: String): Double? = if (has(key) && !isNull(key)) getDouble(key) else null
    private fun JSONObject.nullableBoolean(key: String): Boolean? = if (has(key) && !isNull(key)) getBoolean(key) else null
    private fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> = List(length()) { index -> transform(getJSONObject(index)) }
    private inline fun <reified T : Enum<T>> enumValue(value: String, fallback: T): T = runCatching { enumValueOf<T>(value) }.getOrDefault(fallback)

    private companion object {
        const val FORMAT = "thor-stream-butler"
        const val SCHEMA_VERSION = 1
        const val MAX_IMPORT_BYTES = 5 * 1024 * 1024
        const val MAX_ENTRIES = 500
        const val MAX_HOSTS = 500
        const val MAX_HISTORY = 100
        const val MAX_NAME_LENGTH = 120
        const val MAX_PACKAGE_LENGTH = 255
        const val MAX_HOST_LENGTH = 253
        const val MAX_SSID_LENGTH = 128
        const val MAX_SUMMARY_LENGTH = 1_000
        const val MAX_ASSESSMENT_ITEMS = 20
        const val MAX_ASSESSMENT_ITEM_LENGTH = 500
        const val MAX_MEASUREMENT_VALUE = 1_000_000.0
        val PACKAGE_NAME = Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)*")
    }
}
