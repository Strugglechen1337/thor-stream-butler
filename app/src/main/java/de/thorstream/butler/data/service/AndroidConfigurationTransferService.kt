package de.thorstream.butler.data.service

import android.content.Context
import androidx.core.net.toUri
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
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
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
            diagnosticLogRepository.log(DiagnosticEvent.CONFIGURATION_EXPORTED)
            AppResult.Success(ConfigurationTransferSummary(entries.size, hosts.size, history.size))
        } catch (error: Throwable) {
            if (error is kotlinx.coroutines.CancellationException) throw error
            AppResult.Failure(AppError.Technical(strings.get(R.string.settings_transfer_export_failed)))
        }
    }

    override suspend fun importFrom(documentUri: String): AppResult<ConfigurationTransferSummary> = withContext(Dispatchers.IO) {
        try {
            val uri = documentUri.toUri()
            val content = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                ?: throw IOException("Unable to open input document")
            val root = JSONObject(content)
            require(root.getString("format") == FORMAT)
            require(root.getInt("schemaVersion") in 1..SCHEMA_VERSION)

            val hosts = root.getJSONArray("hosts").mapObjects { it.toLocalHost() }
            val validHostIds = hosts.map { it.id }.toSet()
            val entries = root.getJSONArray("entries").mapObjects { it.toStreamingEntry(validHostIds) }
            val history = root.optJSONArray("history")?.mapObjects { it.toMeasurement() }
            val settings = root.getJSONObject("settings").toAppSettings()

            hostsRepository.replaceAll(hosts)
            entriesRepository.replaceAll(entries)
            settingsRepository.update(settings)
            history?.let { historyRepository.replaceAll(it) }
            diagnosticLogRepository.log(DiagnosticEvent.CONFIGURATION_IMPORTED)
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
        val address = getString("address").trim()
        val mac = nullableString("macAddress")
        val broadcast = optString("broadcastAddress", "255.255.255.255").trim()
        require(getString("name").isNotBlank())
        require(NetworkValidators.isValidHostnameOrIpv4(address))
        require(mac == null || NetworkValidators.isValidMac(mac))
        require(NetworkValidators.isValidHostnameOrIpv4(broadcast))
        return LocalHost(
            id = getLong("id"),
            name = getString("name").trim(),
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
        require(packageName.isNotBlank() && getString("displayName").isNotBlank())
        return StreamingEntry(
            id = getLong("id"),
            displayName = getString("displayName").trim(),
            packageName = packageName,
            iconReference = optString("iconReference", "package://$packageName"),
            streamingType = enumValue(optString("streamingType"), StreamingType.CUSTOM),
            customName = nullableString("customName"),
            hostId = requestedHost?.takeIf(validHostIds::contains),
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
        defaultTestTarget = optString("defaultTestTarget", "1.1.1.1").takeIf(NetworkValidators::isValidHostnameOrIpv4) ?: "1.1.1.1",
        pingCount = optInt("pingCount", 5).coerceIn(1, 20),
        testDurationSeconds = optInt("testDurationSeconds", 5).coerceIn(1, 30),
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
        return NetworkMeasurement(
            id = getLong("id"),
            timestamp = getLong("timestamp"),
            snapshot = NetworkSnapshot(
                connectionType = enumValue(snapshot.optString("connectionType"), ConnectionType.NONE),
                localIpAddress = snapshot.nullableString("localIpAddress"),
                gateway = snapshot.nullableString("gateway"),
                ssid = snapshot.nullableString("ssid"),
                wifiFrequencyMhz = snapshot.nullableInt("wifiFrequencyMhz"),
                linkSpeedMbps = snapshot.nullableInt("linkSpeedMbps"),
                signalStrengthPercent = snapshot.nullableInt("signalStrengthPercent"),
                internetValidated = snapshot.nullableBoolean("internetValidated"),
                dnsReachable = snapshot.nullableBoolean("dnsReachable"),
                latencyMs = snapshot.nullableDouble("latencyMs"),
                jitterMs = snapshot.nullableDouble("jitterMs"),
                packetLossPercent = snapshot.nullableDouble("packetLossPercent"),
                downloadMbps = snapshot.nullableDouble("downloadMbps"),
                hostReachable = snapshot.nullableBoolean("hostReachable"),
                host = snapshot.nullableString("host"),
            ),
            assessment = QualityAssessment(
                quality = enumValue(assessment.optString("quality"), NetworkQuality.NOT_MEASURABLE),
                summary = assessment.optString("summary"),
                problems = assessment.optJSONArray("problems")?.toStringList().orEmpty(),
                recommendations = assessment.optJSONArray("recommendations")?.toStringList().orEmpty(),
            ),
        )
    }

    private fun JSONObject.putNullable(key: String, value: Any?): JSONObject = put(key, value ?: JSONObject.NULL)
    private fun JSONObject.nullableString(key: String): String? = if (has(key) && !isNull(key)) getString(key) else null
    private fun JSONObject.nullableInt(key: String): Int? = if (has(key) && !isNull(key)) getInt(key) else null
    private fun JSONObject.nullableLong(key: String): Long? = if (has(key) && !isNull(key)) getLong(key) else null
    private fun JSONObject.nullableDouble(key: String): Double? = if (has(key) && !isNull(key)) getDouble(key) else null
    private fun JSONObject.nullableBoolean(key: String): Boolean? = if (has(key) && !isNull(key)) getBoolean(key) else null
    private fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> = List(length()) { index -> transform(getJSONObject(index)) }
    private fun JSONArray.toStringList(): List<String> = List(length()) { index -> getString(index) }
    private inline fun <reified T : Enum<T>> enumValue(value: String, fallback: T): T = runCatching { enumValueOf<T>(value) }.getOrDefault(fallback)

    private companion object {
        const val FORMAT = "thor-stream-butler"
        const val SCHEMA_VERSION = 1
    }
}
