package de.thorstream.butler.domain.repository

import de.thorstream.butler.core.common.AppResult
import de.thorstream.butler.domain.model.AppSettings
import de.thorstream.butler.domain.model.InstalledApp
import de.thorstream.butler.domain.model.LocalHost
import de.thorstream.butler.domain.model.StreamingSession
import de.thorstream.butler.domain.model.NetworkMeasurement
import de.thorstream.butler.domain.model.StreamingEntry
import kotlinx.coroutines.flow.Flow

interface InstalledAppsRepository {
    suspend fun getLaunchableApps(): AppResult<List<InstalledApp>>
    fun canLaunch(packageName: String): Boolean
    fun launch(packageName: String): AppResult<Unit>
}

interface StreamingEntryRepository {
    fun observeEntries(): Flow<List<StreamingEntry>>
    suspend fun getEntries(): List<StreamingEntry>
    suspend fun save(entry: StreamingEntry): Long
    suspend fun delete(entry: StreamingEntry)
    suspend fun markLaunched(id: Long, timestamp: Long, quality: String?)
    suspend fun updateSortOrders(entries: List<StreamingEntry>)
    suspend fun replaceAll(entries: List<StreamingEntry>)
    suspend fun ensureDemoEntries()
}

interface NetworkHistoryRepository {
    fun observeHistory(): Flow<List<NetworkMeasurement>>
    suspend fun getHistory(): List<NetworkMeasurement>
    suspend fun getAllHistory(): List<NetworkMeasurement>
    suspend fun save(measurement: NetworkMeasurement): Long
    suspend fun replaceAll(measurements: List<NetworkMeasurement>)
    suspend fun clear()
}

interface LocalHostRepository {
    fun observeHosts(): Flow<List<LocalHost>>
    suspend fun getHosts(): List<LocalHost>
    suspend fun save(host: LocalHost): Long
    suspend fun delete(host: LocalHost)
    suspend fun replaceAll(hosts: List<LocalHost>)
    suspend fun updateTestResult(id: Long, reachable: Boolean, testedAt: Long)
}

interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun update(settings: AppSettings)
}

enum class DiagnosticEvent {
    TEST_STARTED,
    CONNECTION_READ,
    TEST_FAILED,
    DNS_CHECKED,
    LATENCY_MEASURED,
    HOST_CHECKED,
    DOWNLOAD_MEASURED,
    TEST_COMPLETED,
    APP_LAUNCH_REQUESTED,
    APP_LAUNCH_SUCCEEDED,
    WAKE_ON_LAN_SENT,
    HOST_DISCOVERY_STARTED,
    HOST_DISCOVERY_COMPLETED,
    CONFIGURATION_EXPORTED,
    CONFIGURATION_IMPORTED,
}

data class DiagnosticLogEntry(
    val timestamp: Long,
    val event: DiagnosticEvent,
)

interface DiagnosticLogRepository {
    suspend fun log(event: DiagnosticEvent)
    suspend fun read(): List<DiagnosticLogEntry>
    suspend fun clear()
}

interface StreamingSessionRepository {
    /** Latest completed session, or null when none was recorded yet. */
    val lastSession: Flow<StreamingSession?>

    /** Marks a streaming app as launched now; replaces any previous active marker. */
    suspend fun startSession(entryName: String, startedAt: Long)

    /**
     * Completes the active session if one exists and its duration is
     * plausible; otherwise the marker is discarded silently.
     */
    suspend fun completeActiveSession(endedAt: Long)
}
