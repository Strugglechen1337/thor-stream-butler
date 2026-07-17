package de.thorstream.butler.domain.repository

import de.thorstream.butler.core.common.AppResult
import de.thorstream.butler.domain.model.AppSettings
import de.thorstream.butler.domain.model.InstalledApp
import de.thorstream.butler.domain.model.LocalHost
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
    suspend fun save(entry: StreamingEntry): Long
    suspend fun delete(entry: StreamingEntry)
    suspend fun markLaunched(id: Long, timestamp: Long, quality: String?)
    suspend fun ensureDemoEntries()
}

interface NetworkHistoryRepository {
    fun observeHistory(): Flow<List<NetworkMeasurement>>
    suspend fun save(measurement: NetworkMeasurement): Long
    suspend fun clear()
}

interface LocalHostRepository {
    fun observeHosts(): Flow<List<LocalHost>>
    suspend fun save(host: LocalHost): Long
    suspend fun delete(host: LocalHost)
    suspend fun updateTestResult(id: Long, reachable: Boolean, testedAt: Long)
}

interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun update(settings: AppSettings)
}

