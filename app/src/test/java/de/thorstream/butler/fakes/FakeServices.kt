package de.thorstream.butler.fakes

import de.thorstream.butler.core.common.AppError
import de.thorstream.butler.core.common.AppResult
import de.thorstream.butler.core.common.StringProvider
import de.thorstream.butler.domain.model.AppSettings
import de.thorstream.butler.domain.model.LocalHost
import de.thorstream.butler.domain.model.InstalledApp
import de.thorstream.butler.domain.model.NetworkMeasurement
import de.thorstream.butler.domain.model.NetworkSnapshot
import de.thorstream.butler.domain.repository.LocalHostRepository
import de.thorstream.butler.domain.repository.DiagnosticEvent
import de.thorstream.butler.domain.repository.DiagnosticLogEntry
import de.thorstream.butler.domain.repository.DiagnosticLogRepository
import de.thorstream.butler.domain.repository.NetworkHistoryRepository
import de.thorstream.butler.domain.repository.SettingsRepository
import de.thorstream.butler.domain.repository.InstalledAppsRepository
import de.thorstream.butler.domain.repository.StreamingEntryRepository
import de.thorstream.butler.domain.model.StreamingEntry
import de.thorstream.butler.domain.service.DiagnosticProgress
import de.thorstream.butler.domain.service.HostDiscoveryService
import de.thorstream.butler.domain.service.NetworkDiagnosticsService
import de.thorstream.butler.domain.service.PingResult
import de.thorstream.butler.domain.service.PingService
import de.thorstream.butler.domain.service.SpeedTestService
import de.thorstream.butler.domain.service.WakeOnLanService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf

/**
 * Deterministic [StringProvider] for unit tests: encodes the resource id and
 * arguments so assertions can match on stable identifiers instead of
 * localized text.
 */
class FakeStringProvider : StringProvider {
    override fun get(resId: Int, vararg args: Any): String =
        "res:$resId" + if (args.isEmpty()) "" else ":" + args.joinToString(",")
}

class FakeNetworkDiagnosticsService(
    var progress: Flow<DiagnosticProgress> = emptyFlow(),
    var snapshotResult: AppResult<NetworkSnapshot> = AppResult.Failure(AppError.NoNetwork("no network")),
) : NetworkDiagnosticsService {
    var lastHost: String? = null
    var lastPort: Int? = null
    override fun runDiagnostics(target: String, pingCount: Int, host: String?, port: Int?, includeDownloadTest: Boolean, testDurationSeconds: Int): Flow<DiagnosticProgress> {
        lastHost = host
        lastPort = port
        return progress
    }
    override suspend fun readConnectionSnapshot() = snapshotResult
}

class FakePingService(var result: AppResult<PingResult>) : PingService {
    override suspend fun ping(host: String, count: Int, timeoutMillis: Int) = result
}

class FakeSpeedTestService(var result: AppResult<Double>) : SpeedTestService {
    override suspend fun measureDownloadMbps(testDurationSeconds: Int) = result
}

class FakeHostDiscoveryService(var reachable: Boolean = true) : HostDiscoveryService {
    override suspend fun isReachable(host: String, port: Int?, timeoutMillis: Int) = AppResult.Success(reachable)
}

class FakeWakeOnLanService(var result: AppResult<Unit> = AppResult.Success(Unit)) : WakeOnLanService {
    override suspend fun sendMagicPacket(macAddress: String, broadcastAddress: String, port: Int) = result
}

class FakeSettingsRepository(initial: AppSettings = AppSettings()) : SettingsRepository {
    override val settings = MutableStateFlow(initial)
    override suspend fun update(settings: AppSettings) { this.settings.value = settings }
}

class FakeHistoryRepository : NetworkHistoryRepository {
    val values = MutableStateFlow<List<NetworkMeasurement>>(emptyList())
    override fun observeHistory() = values
    override suspend fun getHistory() = values.value
    override suspend fun save(measurement: NetworkMeasurement): Long {
        values.value = listOf(measurement.copy(id = (values.value.size + 1).toLong())) + values.value
        return values.value.first().id
    }
    override suspend fun replaceAll(measurements: List<NetworkMeasurement>) { values.value = measurements }
    override suspend fun clear() { values.value = emptyList() }
}

class FakeInstalledAppsRepository : InstalledAppsRepository {
    var appsResult: AppResult<List<InstalledApp>> = AppResult.Success(emptyList())
    var launchResult: AppResult<Unit> = AppResult.Success(Unit)
    val launchedPackages = mutableListOf<String>()
    override suspend fun getLaunchableApps() = appsResult
    override fun canLaunch(packageName: String) = true
    override fun launch(packageName: String): AppResult<Unit> {
        launchedPackages += packageName
        return launchResult
    }
}

class FakeStreamingEntryRepository : StreamingEntryRepository {
    val values = MutableStateFlow<List<StreamingEntry>>(emptyList())
    override fun observeEntries() = values
    override suspend fun getEntries() = values.value
    override suspend fun save(entry: StreamingEntry): Long {
        val id = entry.id.takeIf { it != 0L } ?: ((values.value.maxOfOrNull { it.id } ?: 0L) + 1L)
        values.value = (values.value.filterNot { it.id == id } + entry.copy(id = id)).sortedBy { it.sortOrder }
        return id
    }
    override suspend fun delete(entry: StreamingEntry) { values.value = values.value.filterNot { it.id == entry.id } }
    override suspend fun markLaunched(id: Long, timestamp: Long, quality: String?) {
        values.value = values.value.map { if (it.id == id) it.copy(lastUsedAt = timestamp) else it }
    }
    override suspend fun updateSortOrders(entries: List<StreamingEntry>) {
        values.value = entries.mapIndexed { index, entry -> entry.copy(sortOrder = index) }
    }
    override suspend fun replaceAll(entries: List<StreamingEntry>) { values.value = entries }
    override suspend fun ensureDemoEntries() = Unit
}

class FakeDiagnosticLogRepository : DiagnosticLogRepository {
    val values = mutableListOf<DiagnosticLogEntry>()
    override suspend fun log(event: DiagnosticEvent) { values += DiagnosticLogEntry(System.currentTimeMillis(), event) }
    override suspend fun read(): List<DiagnosticLogEntry> = values.toList()
    override suspend fun clear() { values.clear() }
}

class FakeLocalHostRepository : LocalHostRepository {
    val values = MutableStateFlow<List<LocalHost>>(emptyList())
    override fun observeHosts() = values
    override suspend fun getHosts() = values.value
    override suspend fun save(host: LocalHost): Long {
        val id = host.id.takeIf { it != 0L } ?: ((values.value.maxOfOrNull { it.id } ?: 0L) + 1L)
        values.value = values.value.filterNot { it.id == id } + host.copy(id = id)
        return id
    }
    override suspend fun delete(host: LocalHost) { values.value = values.value.filterNot { it.id == host.id } }
    override suspend fun replaceAll(hosts: List<LocalHost>) { values.value = hosts }
    override suspend fun updateTestResult(id: Long, reachable: Boolean, testedAt: Long) {
        values.value = values.value.map { value ->
            if (value.id == id) value.copy(
                lastReachable = reachable,
                lastSuccessfulTestAt = testedAt.takeIf { reachable } ?: value.lastSuccessfulTestAt,
            ) else value
        }
    }
}

