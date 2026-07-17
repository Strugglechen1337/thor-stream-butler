package de.thorstream.butler.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface StreamingEntryDao {
    @Query("SELECT * FROM streaming_entries ORDER BY sortOrder, displayName")
    fun observeAll(): Flow<List<StreamingEntryEntity>>

    @Upsert
    suspend fun upsert(entity: StreamingEntryEntity): Long

    @Query("SELECT * FROM streaming_entries ORDER BY sortOrder, displayName")
    suspend fun getAll(): List<StreamingEntryEntity>

    @Upsert
    suspend fun upsertAll(entities: List<StreamingEntryEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(entity: StreamingEntryEntity): Long

    @Query("SELECT COUNT(*) FROM streaming_entries")
    suspend fun count(): Int

    @Query("UPDATE streaming_entries SET lastUsedAt = :timestamp, lastNetworkQuality = :quality WHERE id = :id")
    suspend fun markLaunched(id: Long, timestamp: Long, quality: String?)

    @Query("UPDATE streaming_entries SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: Long, sortOrder: Int)

    @Transaction
    suspend fun updateSortOrders(entities: List<StreamingEntryEntity>) {
        entities.forEachIndexed { index, entity -> updateSortOrder(entity.id, index) }
    }

    @Query("UPDATE streaming_entries SET hostId = NULL WHERE hostId = :hostId")
    suspend fun clearHostAssignments(hostId: Long)

    @Query("DELETE FROM streaming_entries")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(entities: List<StreamingEntryEntity>) {
        deleteAll()
        upsertAll(entities)
    }

    @Delete
    suspend fun delete(entity: StreamingEntryEntity)
}

@Dao
interface LocalHostDao {
    @Query("SELECT * FROM local_hosts ORDER BY name")
    fun observeAll(): Flow<List<LocalHostEntity>>

    @Upsert
    suspend fun upsert(entity: LocalHostEntity): Long

    @Query("SELECT * FROM local_hosts ORDER BY name")
    suspend fun getAll(): List<LocalHostEntity>

    @Upsert
    suspend fun upsertAll(entities: List<LocalHostEntity>)

    @Query("DELETE FROM local_hosts")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(entities: List<LocalHostEntity>) {
        deleteAll()
        upsertAll(entities)
    }

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

    @Query("DELETE FROM network_measurements WHERE id NOT IN (SELECT id FROM network_measurements ORDER BY timestamp DESC, id DESC LIMIT :limit)")
    suspend fun trimToLatest(limit: Int)

    @Transaction
    suspend fun insertAndTrim(entity: NetworkMeasurementEntity, limit: Int): Long {
        val id = insert(entity)
        trimToLatest(limit)
        return id
    }

    @Query("SELECT * FROM network_measurements ORDER BY timestamp DESC LIMIT 100")
    suspend fun getRecent(): List<NetworkMeasurementEntity>

    @Query("SELECT * FROM network_measurements ORDER BY timestamp DESC, id DESC")
    suspend fun getAll(): List<NetworkMeasurementEntity>

    @Insert
    suspend fun insertAll(entities: List<NetworkMeasurementEntity>)

    @Query("DELETE FROM network_measurements")
    suspend fun clear()

    @Transaction
    suspend fun replaceAll(entities: List<NetworkMeasurementEntity>) {
        clear()
        insertAll(entities)
    }
}
