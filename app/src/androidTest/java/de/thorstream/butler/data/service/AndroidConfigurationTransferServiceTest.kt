package de.thorstream.butler.data.service

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.thorstream.butler.core.common.AppResult
import de.thorstream.butler.core.common.StringProvider
import de.thorstream.butler.data.database.ThorDatabase
import de.thorstream.butler.data.repository.RoomLocalHostRepository
import de.thorstream.butler.data.repository.RoomNetworkHistoryRepository
import de.thorstream.butler.data.repository.RoomStreamingEntryRepository
import de.thorstream.butler.domain.model.AppSettings
import de.thorstream.butler.domain.model.LocalHost
import de.thorstream.butler.domain.model.StreamingEntry
import de.thorstream.butler.domain.repository.SettingsRepository
import de.thorstream.butler.domain.repository.DiagnosticEvent
import de.thorstream.butler.domain.repository.DiagnosticLogEntry
import de.thorstream.butler.domain.repository.DiagnosticLogRepository
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidConfigurationTransferServiceTest {
    private lateinit var context: Context
    private lateinit var database: ThorDatabase
    private lateinit var document: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, ThorDatabase::class.java).allowMainThreadQueries().build()
        document = File(context.cacheDir, "configuration-transfer-test.json")
    }

    @After
    fun tearDown() {
        database.close()
        document.delete()
    }

    @Test
    fun exportAndImportPreserveHostAssignment() = runTest {
        val entries = RoomStreamingEntryRepository(database.streamingEntryDao())
        val hosts = RoomLocalHostRepository(database.localHostDao(), database.streamingEntryDao())
        val history = RoomNetworkHistoryRepository(database.networkMeasurementDao())
        val settings = InMemorySettingsRepository()
        val service = AndroidConfigurationTransferService(context, entries, hosts, history, settings, TestStrings(), TestDiagnosticLogRepository())
        val hostId = hosts.save(LocalHost(name = "Gaming PC", address = "gaming-pc.local", port = 47989))
        entries.save(StreamingEntry(displayName = "Moonlight", packageName = "com.limelight", hostId = hostId))

        val exportResult = service.exportTo(Uri.fromFile(document).toString(), includeHistory = false)
        assertTrue(exportResult is AppResult.Success)
        hosts.replaceAll(emptyList())
        entries.replaceAll(emptyList())

        val importResult = service.importFrom(Uri.fromFile(document).toString())

        assertTrue(importResult is AppResult.Success)
        assertEquals("Gaming PC", hosts.getHosts().single().name)
        assertEquals(hostId, entries.getEntries().single().hostId)
    }

    private class InMemorySettingsRepository : SettingsRepository {
        override val settings = MutableStateFlow(AppSettings())
        override suspend fun update(settings: AppSettings) { this.settings.value = settings }
    }

    private class TestStrings : StringProvider {
        override fun get(resId: Int, vararg args: Any): String = "error:$resId"
    }

    private class TestDiagnosticLogRepository : DiagnosticLogRepository {
        override suspend fun log(event: DiagnosticEvent) = Unit
        override suspend fun read(): List<DiagnosticLogEntry> = emptyList()
        override suspend fun clear() = Unit
    }
}
