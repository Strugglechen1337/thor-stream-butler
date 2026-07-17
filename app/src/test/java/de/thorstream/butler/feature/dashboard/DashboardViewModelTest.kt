package de.thorstream.butler.feature.dashboard

import de.thorstream.butler.core.network.QualityEvaluator
import de.thorstream.butler.core.network.StreamingRecommendationEngine
import de.thorstream.butler.domain.model.AppSettings
import de.thorstream.butler.domain.model.ConnectionType
import de.thorstream.butler.domain.model.LocalHost
import de.thorstream.butler.domain.model.NetworkSnapshot
import de.thorstream.butler.domain.model.StreamingEntry
import de.thorstream.butler.domain.repository.DiagnosticEvent
import de.thorstream.butler.domain.service.DiagnosticProgress
import de.thorstream.butler.domain.service.DiagnosticStep
import de.thorstream.butler.fakes.FakeDiagnosticLogRepository
import de.thorstream.butler.fakes.FakeHistoryRepository
import de.thorstream.butler.fakes.FakeInstalledAppsRepository
import de.thorstream.butler.fakes.FakeLocalHostRepository
import de.thorstream.butler.fakes.FakeNetworkDiagnosticsService
import de.thorstream.butler.fakes.FakeSettingsRepository
import de.thorstream.butler.fakes.FakeStreamingEntryRepository
import de.thorstream.butler.fakes.FakeStringProvider
import de.thorstream.butler.fakes.FakeWakeOnLanService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `prelaunch diagnostics receive linked host and persist result`() = runTest(dispatcher) {
        val entry = StreamingEntry(id = 1, displayName = "Moonlight", packageName = "com.limelight", hostId = 7)
        val entries = FakeStreamingEntryRepository().apply { values.value = listOf(entry) }
        val hosts = FakeLocalHostRepository().apply {
            values.value = listOf(LocalHost(id = 7, name = "Gaming PC", address = "gaming-pc.local", port = 47989))
        }
        val snapshot = NetworkSnapshot(ConnectionType.ETHERNET, latencyMs = 15.0, jitterMs = 2.0, packetLossPercent = 0.0, hostReachable = true)
        val diagnostics = FakeNetworkDiagnosticsService(
            progress = flowOf(DiagnosticProgress(DiagnosticStep.COMPLETED, 1f, snapshot, completed = true)),
        )
        val history = FakeHistoryRepository()
        val viewModel = createViewModel(
            entries = entries,
            hosts = hosts,
            diagnostics = diagnostics,
            settings = FakeSettingsRepository(AppSettings(autoLaunchOnGreen = false)),
            history = history,
        )

        viewModel.launch(entry)
        advanceUntilIdle()

        assertEquals("gaming-pc.local", diagnostics.lastHost)
        assertEquals(47989, diagnostics.lastPort)
        assertEquals(1, history.values.value.size)
    }

    @Test
    fun `launch without precheck records successful app launch`() = runTest(dispatcher) {
        val entry = StreamingEntry(id = 1, displayName = "Steam Link", packageName = "com.valvesoftware.steamlink")
        val installedApps = FakeInstalledAppsRepository()
        val log = FakeDiagnosticLogRepository()
        val viewModel = createViewModel(
            entries = FakeStreamingEntryRepository().apply { values.value = listOf(entry) },
            installedApps = installedApps,
            settings = FakeSettingsRepository(AppSettings(preLaunchCheckEnabled = false)),
            log = log,
        )

        viewModel.launch(entry)
        advanceUntilIdle()

        assertEquals(listOf(entry.packageName), installedApps.launchedPackages)
        assertTrue(log.values.any { it.event == DiagnosticEvent.APP_LAUNCH_SUCCEEDED })
    }

    private fun createViewModel(
        entries: FakeStreamingEntryRepository = FakeStreamingEntryRepository(),
        hosts: FakeLocalHostRepository = FakeLocalHostRepository(),
        installedApps: FakeInstalledAppsRepository = FakeInstalledAppsRepository(),
        diagnostics: FakeNetworkDiagnosticsService = FakeNetworkDiagnosticsService(),
        settings: FakeSettingsRepository = FakeSettingsRepository(),
        history: FakeHistoryRepository = FakeHistoryRepository(),
        log: FakeDiagnosticLogRepository = FakeDiagnosticLogRepository(),
    ) = DashboardViewModel(
        entriesRepository = entries,
        localHostRepository = hosts,
        installedAppsRepository = installedApps,
        diagnosticsService = diagnostics,
        settingsRepository = settings,
        historyRepository = history,
        qualityEvaluator = QualityEvaluator(FakeStringProvider()),
        recommendationEngine = StreamingRecommendationEngine(),
        wakeOnLanService = FakeWakeOnLanService(),
        diagnosticLogRepository = log,
        strings = FakeStringProvider(),
    )
}
