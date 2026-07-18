package de.thorstream.butler.domain.service

import de.thorstream.butler.core.common.AppResult
import de.thorstream.butler.domain.model.NetworkSnapshot
import kotlinx.coroutines.flow.Flow

data class PingResult(
    val successfulLatenciesMs: List<Double>,
    val sentCount: Int,
) {
    val receivedCount: Int get() = successfulLatenciesMs.size
}

/** Locale-independent identifier for the current phase of a diagnostics run. */
enum class DiagnosticStep {
    DETECTING_CONNECTION,
    NETWORK_UNAVAILABLE,
    CONNECTION_READ,
    DNS_CHECKED,
    LATENCY_MEASURED,
    HOST_CHECKED,
    DOWNLOAD_MEASURED,
    COMPLETED,
}

data class DiagnosticProgress(
    val step: DiagnosticStep,
    val progress: Float,
    val snapshot: NetworkSnapshot? = null,
    val completed: Boolean = false,
    val errorMessage: String? = null,
)

interface NetworkDiagnosticsService {
    fun runDiagnostics(
        target: String,
        pingCount: Int,
        host: String? = null,
        port: Int? = null,
        includeDownloadTest: Boolean = false,
        testDurationSeconds: Int = 5,
    ): Flow<DiagnosticProgress>
    suspend fun readConnectionSnapshot(): AppResult<NetworkSnapshot>
}

interface PingService {
    suspend fun ping(host: String, count: Int, timeoutMillis: Int = 1500): AppResult<PingResult>
}

interface SpeedTestService {
    suspend fun measureDownloadMbps(testDurationSeconds: Int): AppResult<Double>
}

interface HostDiscoveryService {
    suspend fun isReachable(host: String, port: Int?, timeoutMillis: Int = 2000): AppResult<Boolean>
}

data class DiscoveredHost(
    val name: String,
    val address: String,
    val port: Int,
    val serviceType: String,
)

interface LocalHostDiscoveryService {
    suspend fun discover(timeoutMillis: Long = 8_000): AppResult<List<DiscoveredHost>>
}

interface WakeOnLanService {
    suspend fun sendMagicPacket(macAddress: String, broadcastAddress: String, port: Int = 9): AppResult<Unit>
}

data class ConfigurationTransferSummary(
    val streamingEntries: Int,
    val localHosts: Int,
    val historyMeasurements: Int,
)

interface ConfigurationTransferService {
    suspend fun exportTo(documentUri: String, includeHistory: Boolean): AppResult<ConfigurationTransferSummary>
    suspend fun importFrom(documentUri: String): AppResult<ConfigurationTransferSummary>
}

/**
 * Snapshot of device health values that matter during long streaming
 * sessions on handhelds. Every value is optional because Android may not
 * report it on all devices.
 */
data class DeviceStatus(
    val batteryPercent: Int? = null,
    val charging: Boolean? = null,
    val batteryTemperatureCelsius: Double? = null,
)

interface DeviceStatusService {
    fun readStatus(): DeviceStatus
}
