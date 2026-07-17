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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.json.JSONObject

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
        val hosts = RoomLocalHostRepository(database.localHostDao(), database.streamingEntryDao(), database)
        val history = RoomNetworkHistoryRepository(database.networkMeasurementDao())
        val settings = InMemorySettingsRepository()
        val service = AndroidConfigurationTransferService(context, entries, hosts, history, settings, TestStrings(), TestDiagnosticLogRepository(), database)
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

    @Test
    fun duplicateIdsAreRejectedWithoutChangingLocalData() = runTest {
        val entries = RoomStreamingEntryRepository(database.streamingEntryDao())
        val hosts = RoomLocalHostRepository(database.localHostDao(), database.streamingEntryDao(), database)
        val history = RoomNetworkHistoryRepository(database.networkMeasurementDao())
        val settings = InMemorySettingsRepository()
        val service = AndroidConfigurationTransferService(context, entries, hosts, history, settings, TestStrings(), TestDiagnosticLogRepository(), database)
        hosts.save(LocalHost(name = "Living room", address = "living-room.local"))
        hosts.save(LocalHost(name = "Office", address = "office.local"))

        service.exportTo(Uri.fromFile(document).toString(), includeHistory = false)
        val root = JSONObject(document.readText(Charsets.UTF_8))
        val exportedHosts = root.getJSONArray("hosts")
        exportedHosts.getJSONObject(1).put("id", exportedHosts.getJSONObject(0).getLong("id"))
        exportedHosts.getJSONObject(0).put("name", "Tampered")
        document.writeText(root.toString(), Charsets.UTF_8)

        val result = service.importFrom(Uri.fromFile(document).toString())

        assertTrue(result is AppResult.Failure)
        assertEquals(listOf("Living room", "Office"), hosts.getHosts().map(LocalHost::name).sorted())
    }

    @Test
    fun settingsFailureRollsBackDatabaseReplacement() = runTest {
        val entries = RoomStreamingEntryRepository(database.streamingEntryDao())
        val hosts = RoomLocalHostRepository(database.localHostDao(), database.streamingEntryDao(), database)
        val history = RoomNetworkHistoryRepository(database.networkMeasurementDao())
        val exportSettings = InMemorySettingsRepository(AppSettings(defaultTestTarget = "source.example"))
        val exportService = AndroidConfigurationTransferService(context, entries, hosts, history, exportSettings, TestStrings(), TestDiagnosticLogRepository(), database)
        val sourceHostId = hosts.save(LocalHost(name = "Source host", address = "source.local"))
        entries.save(StreamingEntry(displayName = "Source app", packageName = "test.source", hostId = sourceHostId))
        exportService.exportTo(Uri.fromFile(document).toString(), includeHistory = false)

        hosts.replaceAll(listOf(LocalHost(id = 99, name = "Local host", address = "local.example")))
        entries.replaceAll(listOf(StreamingEntry(id = 99, displayName = "Local app", packageName = "test.local", hostId = 99)))
        val failingSettings = FailingSettingsRepository(
            initial = AppSettings(defaultTestTarget = "local.example"),
            rejectedTarget = "source.example",
        )
        val importService = AndroidConfigurationTransferService(context, entries, hosts, history, failingSettings, TestStrings(), TestDiagnosticLogRepository(), database)

        val result = importService.importFrom(Uri.fromFile(document).toString())

        assertTrue(result is AppResult.Failure)
        assertEquals("Local host", hosts.getHosts().single().name)
        assertEquals("Local app", entries.getEntries().single().displayName)
        assertEquals("local.example", failingSettings.settings.value.defaultTestTarget)
    }

    @Test
    fun cancellationDuringSettingsWriteRollsBackDatabaseReplacement() = runTest {
        val entries = RoomStreamingEntryRepository(database.streamingEntryDao())
        val hosts = RoomLocalHostRepository(database.localHostDao(), database.streamingEntryDao(), database)
        val history = RoomNetworkHistoryRepository(database.networkMeasurementDao())
        val exportSettings = InMemorySettingsRepository(AppSettings(defaultTestTarget = "source.example"))
        val exportService = AndroidConfigurationTransferService(context, entries, hosts, history, exportSettings, TestStrings(), TestDiagnosticLogRepository(), database)
        val sourceHostId = hosts.save(LocalHost(name = "Source host", address = "source.local"))
        entries.save(StreamingEntry(displayName = "Source app", packageName = "test.source", hostId = sourceHostId))
        exportService.exportTo(Uri.fromFile(document).toString(), includeHistory = false)

        hosts.replaceAll(listOf(LocalHost(id = 99, name = "Local host", address = "local.example")))
        entries.replaceAll(listOf(StreamingEntry(id = 99, displayName = "Local app", packageName = "test.local", hostId = 99)))
        val cancellingSettings = CancellingSettingsRepository(
            initial = AppSettings(defaultTestTarget = "local.example"),
            cancelledTarget = "source.example",
        )
        val importService = AndroidConfigurationTransferService(context, entries, hosts, history, cancellingSettings, TestStrings(), TestDiagnosticLogRepository(), database)

        var cancellationObserved = false
        try {
            importService.importFrom(Uri.fromFile(document).toString())
        } catch (_: CancellationException) {
            cancellationObserved = true
        }

        assertTrue(cancellationObserved)
        assertEquals("Local host", hosts.getHosts().single().name)
        assertEquals("Local app", entries.getEntries().single().displayName)
        assertEquals("local.example", cancellingSettings.settings.value.defaultTestTarget)
    }

    private class InMemorySettingsRepository(initial: AppSettings = AppSettings()) : SettingsRepository {
        override val settings = MutableStateFlow(initial)
        override suspend fun update(settings: AppSettings) { this.settings.value = settings }
    }

    private class FailingSettingsRepository(
        initial: AppSettings,
        private val rejectedTarget: String,
    ) : SettingsRepository {
        override val settings = MutableStateFlow(initial)
        override suspend fun update(settings: AppSettings) {
            if (settings.defaultTestTarget == rejectedTarget) error("Simulated DataStore failure")
            this.settings.value = settings
        }
    }

    private class CancellingSettingsRepository(
        initial: AppSettings,
        private val cancelledTarget: String,
    ) : SettingsRepository {
        override val settings = MutableStateFlow(initial)
        override suspend fun update(settings: AppSettings) {
            if (settings.defaultTestTarget == cancelledTarget) throw CancellationException("Simulated cancellation")
            this.settings.value = settings
        }
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
