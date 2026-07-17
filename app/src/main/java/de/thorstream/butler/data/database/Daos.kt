package de.thorstream.butler.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface StreamingEntryDao {
    @Query("SELECT * FROM streaming_entries ORDER BY sortOrder, displayName")
    fun observeAll(): Flow<List<StreamingEntryEntity>>

    @Upsert
    suspend fun upsert(entity: StreamingEntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(entity: StreamingEntryEntity): Long

    @Query("SELECT COUNT(*) FROM streaming_entries")
    suspend fun count(): Int

    @Query("UPDATE streaming_entries SET lastUsedAt = :timestamp, lastNetworkQuality = :quality WHERE id = :id")
    suspend fun markLaunched(id: Long, timestamp: Long, quality: String?)

    @Delete
    suspend fun delete(entity: StreamingEntryEntity)
}

@Dao
interface LocalHostDao {
    @Query("SELECT * FROM local_hosts ORDER BY name")
    fun observeAll(): Flow<List<LocalHostEntity>>

    @Upsert
    suspend fun upsert(entity: LocalHostEntity): Long

    @Delete
    suspend fun delete(entity: LocalHostEntity)

    @Query("UPDATE local_hosts SET lastReachable = :reachable, lastSuccessfulTestAt = CASE WHEN :reachable THEN :testedAt ELSE lastSuccessfulTestAt END WHERE id = :id")
    suspend fun updateTestResult(id: Long, reachable: Boolean, testedAt: Long)
}

@Dao
interface NetworkMeasurementDao {
    @Query("SELECT * FROM network_measurements ORDER BY timestamp DESC LIMIT 100")
    fun observeRecent(): Flow<List<NetworkMeasurementEntity>>

    @Insert
    suspend fun insert(entity: NetworkMeasurementEntity): Long

    @Query("DELETE FROM network_measurements")
    suspend fun clear()
}

