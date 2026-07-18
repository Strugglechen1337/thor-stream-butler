package de.thorstream.butler.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.thorstream.butler.domain.model.AppSettings
import de.thorstream.butler.domain.repository.DiagnosticEvent
import de.thorstream.butler.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FileDiagnosticLogRepositoryTest {
    private lateinit var repository: FileDiagnosticLogRepository
    private lateinit var settings: InMemorySettingsRepository

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        settings = InMemorySettingsRepository()
        repository = FileDiagnosticLogRepository(context, settings)
        repository.clear()
    }

    @After
    fun tearDown() = runTest { repository.clear() }

    @Test
    fun disabledLoggingStoresNothing() = runTest {
        repository.log(DiagnosticEvent.TEST_STARTED)
        assertEquals(0, repository.read().size)
    }

    @Test
    fun enabledLoggingStoresOnlyTypedEvent() = runTest {
        settings.settings.value = AppSettings(diagnosticLoggingEnabled = true)
        repository.log(DiagnosticEvent.LATENCY_MEASURED)

        assertEquals(listOf(DiagnosticEvent.LATENCY_MEASURED), repository.read().map { it.event })
    }

    private class InMemorySettingsRepository : SettingsRepository {
        override val settings = MutableStateFlow(AppSettings())
        override suspend fun update(settings: AppSettings) { this.settings.value = settings }
    }
}
