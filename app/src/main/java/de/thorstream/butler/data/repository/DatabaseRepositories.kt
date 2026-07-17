package de.thorstream.butler.data.repository

import androidx.room.withTransaction
import de.thorstream.butler.data.database.LocalHostDao
import de.thorstream.butler.data.database.LocalHostEntity
import de.thorstream.butler.data.database.NetworkMeasurementDao
import de.thorstream.butler.data.database.NetworkMeasurementEntity
import de.thorstream.butler.data.database.StreamingEntryDao
import de.thorstream.butler.data.database.StreamingEntryEntity
import de.thorstream.butler.data.database.ThorDatabase
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
import de.thorstream.butler.domain.repository.LocalHostRepository
import de.thorstream.butler.domain.repository.NetworkHistoryRepository
import de.thorstream.butler.domain.repository.StreamingEntryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomStreamingEntryRepository @Inject constructor(
    private val dao: StreamingEntryDao,
) : StreamingEntryRepository {
    override fun observeEntries(): Flow<List<StreamingEntry>> = dao.observeAll().map { values -> values.map { it.toDomain() } }
    override suspend fun getEntries(): List<StreamingEntry> = dao.getAll().map { it.toDomain() }
    override suspend fun save(entry: StreamingEntry): Long = dao.upsert(entry.toEntity())
    override suspend fun delete(entry: StreamingEntry) = dao.delete(entry.toEntity())
    override suspend fun markLaunched(id: Long, timestamp: Long, quality: String?) = dao.markLaunched(id, timestamp, quality)
    override suspend fun updateSortOrders(entries: List<StreamingEntry>) = dao.updateSortOrders(entries.map { it.toEntity() })
    override suspend fun replaceAll(entries: List<StreamingEntry>) = dao.replaceAll(entries.map { it.toEntity() })

    override suspend fun ensureDemoEntries() {
        if (dao.count() != 0) return
        listOf(
            StreamingEntry(displayName = "GeForce NOW", packageName = "com.nvidia.geforcenow", streamingType = StreamingType.GEFORCE_NOW, sortOrder = 0, isDemo = true),
            StreamingEntry(displayName = "Xbox Cloud Gaming", packageName = "com.gamepass", streamingType = StreamingType.XBOX_CLOUD, sortOrder = 1, isDemo = true),
            StreamingEntry(displayName = "Moonlight", packageName = "com.limelight", streamingType = StreamingType.MOONLIGHT, sortOrder = 2, isDemo = true),
        ).forEach { dao.insertIfAbsent(it.toEntity()) }
    }
}

@Singleton
class RoomLocalHostRepository @Inject constructor(
    private val dao: LocalHostDao,
    private val streamingEntryDao: StreamingEntryDao,
    private val database: ThorDatabase,
) : LocalHostRepository {
    override fun observeHosts(): Flow<List<LocalHost>> = dao.observeAll().map { values -> values.map { it.toDomain() } }
    override suspend fun getHosts(): List<LocalHost> = dao.getAll().map { it.toDomain() }
    override suspend fun save(host: LocalHost): Long = dao.upsert(host.toEntity())
    override suspend fun delete(host: LocalHost) {
        database.withTransaction {
            streamingEntryDao.clearHostAssignments(host.id)
            dao.delete(host.toEntity())
        }
    }
    override suspend fun replaceAll(hosts: List<LocalHost>) = dao.replaceAll(hosts.map { it.toEntity() })
    override suspend fun updateTestResult(id: Long, reachable: Boolean, testedAt: Long) = dao.updateTestResult(id, reachable, testedAt)
}

@Singleton
class RoomNetworkHistoryRepository @Inject constructor(private val dao: NetworkMeasurementDao) : NetworkHistoryRepository {
    override fun observeHistory(): Flow<List<NetworkMeasurement>> = dao.observeRecent().map { values -> values.map { it.toDomain() } }
    override suspend fun getHistory(): List<NetworkMeasurement> = dao.getRecent().map { it.toDomain() }
    override suspend fun getAllHistory(): List<NetworkMeasurement> = dao.getAll().map { it.toDomain() }
    override suspend fun save(measurement: NetworkMeasurement): Long = dao.insertAndTrim(measurement.toEntity(), MAX_HISTORY_ENTRIES)
    override suspend fun replaceAll(measurements: List<NetworkMeasurement>) = dao.replaceAll(measurements.map { it.toEntity() })
    override suspend fun clear() = dao.clear()

    private companion object {
        const val MAX_HISTORY_ENTRIES = 100
    }
}

private fun StreamingEntry.toEntity() = StreamingEntryEntity(
    id = id,
    displayName = displayName,
    packageName = packageName,
    iconReference = iconReference,
    streamingType = streamingType.name,
    customName = customName,
    hostId = hostId,
    profileResolution = profile.resolution.name,
    profileFramesPerSecond = profile.framesPerSecond,
    profileBitrateMbps = profile.bitrateMbps,
    sortOrder = sortOrder,
    lastUsedAt = lastUsedAt,
    lastNetworkQuality = lastNetworkQuality?.name,
    isDemo = isDemo,
)
private fun StreamingEntryEntity.toDomain() = StreamingEntry(
    id = id,
    displayName = displayName,
    packageName = packageName,
    iconReference = iconReference,
    streamingType = StreamingType.valueOf(streamingType),
    customName = customName,
    hostId = hostId,
    profile = StreamingProfile(
        resolution = runCatching { StreamingResolution.valueOf(profileResolution) }.getOrDefault(StreamingResolution.AUTO),
        framesPerSecond = profileFramesPerSecond,
        bitrateMbps = profileBitrateMbps,
    ),
    sortOrder = sortOrder,
    lastUsedAt = lastUsedAt,
    lastNetworkQuality = lastNetworkQuality?.let(NetworkQuality::valueOf),
    isDemo = isDemo,
)
private fun LocalHost.toEntity() = LocalHostEntity(id, name, address, macAddress, port, streamingType.name, wakeOnLanEnabled, broadcastAddress, lastReachable, lastSuccessfulTestAt)
private fun LocalHostEntity.toDomain() = LocalHost(id, name, address, macAddress, port, StreamingType.valueOf(streamingType), wakeOnLanEnabled, broadcastAddress, lastReachable, lastSuccessfulTestAt)

private fun NetworkMeasurement.toEntity() = NetworkMeasurementEntity(
    id, timestamp, snapshot.connectionType.name, snapshot.localIpAddress, snapshot.gateway, snapshot.ssid,
    snapshot.wifiFrequencyMhz, snapshot.linkSpeedMbps, snapshot.signalStrengthPercent, snapshot.internetValidated,
    snapshot.dnsReachable, snapshot.latencyMs, snapshot.jitterMs, snapshot.packetLossPercent, snapshot.downloadMbps,
    snapshot.hostReachable, snapshot.host, assessment.quality.name, assessment.summary, assessment.problems, assessment.recommendations,
)

private fun NetworkMeasurementEntity.toDomain() = NetworkMeasurement(
    id = id,
    timestamp = timestamp,
    snapshot = NetworkSnapshot(
        connectionType = ConnectionType.valueOf(connectionType), localIpAddress = localIpAddress, gateway = gateway,
        ssid = ssid, wifiFrequencyMhz = wifiFrequencyMhz, linkSpeedMbps = linkSpeedMbps,
        signalStrengthPercent = signalStrengthPercent, internetValidated = internetValidated, dnsReachable = dnsReachable,
        latencyMs = latencyMs, jitterMs = jitterMs, packetLossPercent = packetLossPercent, downloadMbps = downloadMbps,
        hostReachable = hostReachable, host = host,
    ),
    assessment = QualityAssessment(NetworkQuality.valueOf(quality), summary, problems, recommendations),
)
