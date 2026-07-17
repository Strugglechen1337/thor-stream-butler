package de.thorstream.butler.fakes

import de.thorstream.butler.core.common.AppError
import de.thorstream.butler.core.common.AppResult
import de.thorstream.butler.core.common.StringProvider
import de.thorstream.butler.domain.model.AppSettings
import de.thorstream.butler.domain.model.LocalHost
import de.thorstream.butler.domain.model.NetworkMeasurement
import de.thorstream.butler.domain.model.NetworkSnapshot
import de.thorstream.butler.domain.repository.LocalHostRepository
import de.thorstream.butler.domain.repository.NetworkHistoryRepository
import de.thorstream.butler.domain.repository.SettingsRepository
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
    override fun runDiagnostics(target: String, pingCount: Int, host: String?, port: Int?, includeDownloadTest: Boolean, testDurationSeconds: Int) = progress
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
    override suspend fun save(measurement: NetworkMeasurement): Long {
        values.value = listOf(measurement.copy(id = (values.value.size + 1).toLong())) + values.value
        return values.value.first().id
    }
    override suspend fun clear() { values.value = emptyList() }
}

class FakeLocalHostRepository : LocalHostRepository {
    val values = MutableStateFlow<List<LocalHost>>(emptyList())
    override fun observeHosts() = values
    override suspend fun save(host: LocalHost): Long = 1
    override suspend fun delete(host: LocalHost) = Unit
    override suspend fun updateTestResult(id: Long, reachable: Boolean, testedAt: Long) = Unit
}

