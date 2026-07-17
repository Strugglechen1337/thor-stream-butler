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

data class DiagnosticProgress(
    val step: String,
    val progress: Float,
    val snapshot: NetworkSnapshot? = null,
    val completed: Boolean = false,
    val errorMessage: String? = null,
)

interface NetworkDiagnosticsService {
    fun runDiagnostics(target: String, pingCount: Int, host: String? = null, port: Int? = null): Flow<DiagnosticProgress>
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

interface WakeOnLanService {
    suspend fun sendMagicPacket(macAddress: String, broadcastAddress: String, port: Int = 9): AppResult<Unit>
}

