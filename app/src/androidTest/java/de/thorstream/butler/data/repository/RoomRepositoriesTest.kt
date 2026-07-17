package de.thorstream.butler.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.thorstream.butler.data.database.ThorDatabase
import de.thorstream.butler.domain.model.ConnectionType
import de.thorstream.butler.domain.model.LocalHost
import de.thorstream.butler.domain.model.NetworkMeasurement
import de.thorstream.butler.domain.model.NetworkQuality
import de.thorstream.butler.domain.model.NetworkSnapshot
import de.thorstream.butler.domain.model.QualityAssessment
import de.thorstream.butler.domain.model.StreamingEntry
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
        val repository = RoomLocalHostRepository(database.localHostDao(), database.streamingEntryDao())
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
}
