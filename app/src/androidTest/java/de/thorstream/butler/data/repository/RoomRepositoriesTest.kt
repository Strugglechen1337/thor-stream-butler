package de.thorstream.butler.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.thorstream.butler.data.database.ThorDatabase
import de.thorstream.butler.data.database.LocalHostEntity
import de.thorstream.butler.data.database.NetworkMeasurementEntity
import de.thorstream.butler.data.database.StreamingEntryEntity
import de.thorstream.butler.domain.model.ConnectionType
import de.thorstream.butler.domain.model.LocalHost
import de.thorstream.butler.domain.model.NetworkMeasurement
import de.thorstream.butler.domain.model.NetworkQuality
import de.thorstream.butler.domain.model.NetworkSnapshot
import de.thorstream.butler.domain.model.QualityAssessment
import de.thorstream.butler.domain.model.StreamingEntry
import de.thorstream.butler.domain.model.StreamingType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomRepositoriesTest {
    private lateinit var database: ThorDatabase

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ThorDatabase::class.java).allowMainThreadQueries().build()
    }

    @After fun closeDatabase() = database.close()

    @Test
    fun streamingEntriesArePersistedAndObserved() = runTest {
        val repository = RoomStreamingEntryRepository(database.streamingEntryDao())
        repository.save(StreamingEntry(displayName = "Moonlight", packageName = "com.limelight"))
        assertEquals("Moonlight", repository.observeEntries().first().single().displayName)
    }

    @Test
    fun hostsCanBePersistedUpdatedAndDeleted() = runTest {
        val repository = RoomLocalHostRepository(database.localHostDao(), database.streamingEntryDao(), database)
        val id = repository.save(LocalHost(name = "Gaming-PC", address = "192.168.1.10", port = 47984))
        repository.updateTestResult(id, true, 1234L)
        val host = repository.observeHosts().first().single()
        assertTrue(host.lastReachable == true)
        repository.delete(host)
        assertTrue(repository.observeHosts().first().isEmpty())
    }

    @Test
    fun historyStoresProblemsAndCanBeCleared() = runTest {
        val repository = RoomNetworkHistoryRepository(database.networkMeasurementDao())
        repository.save(
            NetworkMeasurement(
                timestamp = 10L,
                snapshot = NetworkSnapshot(ConnectionType.WIFI, latencyMs = 80.0),
                assessment = QualityAssessment(NetworkQuality.PROBLEMATIC, "Zu langsam", listOf("Hohe Latenz")),
            ),
        )
        assertEquals(listOf("Hohe Latenz"), repository.observeHistory().first().single().assessment.problems)
        repository.clear()
        assertTrue(repository.observeHistory().first().isEmpty())
    }

    @Test
    fun historyRetentionKeepsOnlyLatestHundredMeasurements() = runTest {
        val repository = RoomNetworkHistoryRepository(database.networkMeasurementDao())
        repeat(105) { index ->
            repository.save(
                NetworkMeasurement(
                    timestamp = index.toLong() + 1,
                    snapshot = NetworkSnapshot(ConnectionType.ETHERNET, latencyMs = index.toDouble()),
                    assessment = QualityAssessment(NetworkQuality.OPTIMAL, "OK"),
                ),
            )
        }

        val stored = repository.getAllHistory()

        assertEquals(100, stored.size)
        assertEquals(105L, stored.first().timestamp)
        assertEquals(6L, stored.last().timestamp)
    }

    @Test
    fun unknownPersistedEnumsUseSafeFallbacks() = runTest {
        database.streamingEntryDao().upsert(
            StreamingEntryEntity(
                displayName = "Forward app",
                packageName = "test.forward",
                iconReference = "package://test.forward",
                streamingType = "FUTURE_STREAMING_TYPE",
                customName = null,
                hostId = null,
                profileResolution = "FUTURE_RESOLUTION",
                profileFramesPerSecond = 999,
                profileBitrateMbps = -1,
                sortOrder = 0,
                lastUsedAt = null,
                lastNetworkQuality = "FUTURE_QUALITY",
                isDemo = false,
            ),
        )
        database.localHostDao().upsert(
            LocalHostEntity(
                name = "Forward host",
                address = "forward.local",
                macAddress = null,
                port = null,
                streamingType = "FUTURE_STREAMING_TYPE",
                wakeOnLanEnabled = false,
                broadcastAddress = "255.255.255.255",
                lastReachable = null,
                lastSuccessfulTestAt = null,
            ),
        )
        database.networkMeasurementDao().insert(
            NetworkMeasurementEntity(
                timestamp = 1L,
                connectionType = "FUTURE_CONNECTION",
                localIpAddress = null,
                gateway = null,
                ssid = null,
                wifiFrequencyMhz = null,
                linkSpeedMbps = null,
                signalStrengthPercent = null,
                internetValidated = null,
                dnsReachable = null,
                latencyMs = null,
                jitterMs = null,
                packetLossPercent = null,
                downloadMbps = null,
                hostReachable = null,
                host = null,
                quality = "FUTURE_QUALITY",
                summary = "Unavailable",
                problems = emptyList(),
                recommendations = emptyList(),
            ),
        )

        val entry = RoomStreamingEntryRepository(database.streamingEntryDao()).getEntries().single()
        val host = RoomLocalHostRepository(database.localHostDao(), database.streamingEntryDao(), database).getHosts().single()
        val measurement = RoomNetworkHistoryRepository(database.networkMeasurementDao()).getHistory().single()

        assertEquals(StreamingType.CUSTOM, entry.streamingType)
        assertEquals(120, entry.profile.framesPerSecond)
        assertEquals(1, entry.profile.bitrateMbps)
        assertEquals(null, entry.lastNetworkQuality)
        assertEquals(StreamingType.CUSTOM, host.streamingType)
        assertEquals(ConnectionType.OTHER, measurement.snapshot.connectionType)
        assertEquals(NetworkQuality.NOT_MEASURABLE, measurement.assessment.quality)
    }
}
