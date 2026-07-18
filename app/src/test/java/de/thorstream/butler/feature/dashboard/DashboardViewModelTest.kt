package de.thorstream.butler.feature.dashboard

import de.thorstream.butler.core.network.QualityEvaluator
import de.thorstream.butler.core.network.StreamingRecommendationEngine
import de.thorstream.butler.domain.model.AppSettings
import de.thorstream.butler.domain.model.ConnectionType
import de.thorstream.butler.domain.model.LocalHost
import de.thorstream.butler.domain.model.NetworkSnapshot
import de.thorstream.butler.domain.model.NetworkQuality
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
import de.thorstream.butler.fakes.FakeStreamingSessionRepository
import de.thorstream.butler.fakes.FakeStringProvider
import de.thorstream.butler.fakes.FakeWakeOnLanService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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

    @Test
    fun `unexpected diagnostic failure finishes with not measurable decision`() = runTest(dispatcher) {
        val entry = StreamingEntry(id = 1, displayName = "Moonlight", packageName = "com.limelight")
        val diagnostics = FakeNetworkDiagnosticsService(
            progress = flow { throw IllegalStateException("unexpected") },
        )
        val viewModel = createViewModel(
            entries = FakeStreamingEntryRepository().apply { values.value = listOf(entry) },
            diagnostics = diagnostics,
        )
        val completedState = async(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.first { it.preLaunch?.assessment != null }
        }

        viewModel.launch(entry)
        advanceUntilIdle()

        val preLaunch = completedState.await().preLaunch
        assertEquals(1f, preLaunch?.progress)
        assertEquals(NetworkQuality.NOT_MEASURABLE, preLaunch?.assessment?.quality)
    }

    @Test
    fun `completed partial diagnostic error never auto launches`() = runTest(dispatcher) {
        val entry = StreamingEntry(id = 1, displayName = "Moonlight", packageName = "com.limelight")
        val snapshot = NetworkSnapshot(ConnectionType.ETHERNET, latencyMs = 8.0, jitterMs = 1.0, packetLossPercent = 0.0)
        val installedApps = FakeInstalledAppsRepository()
        val diagnostics = FakeNetworkDiagnosticsService(
            progress = flowOf(
                DiagnosticProgress(
                    DiagnosticStep.COMPLETED,
                    1f,
                    snapshot,
                    completed = true,
                    errorMessage = "diagnostic interrupted",
                ),
            ),
        )
        val viewModel = createViewModel(
            entries = FakeStreamingEntryRepository().apply { values.value = listOf(entry) },
            installedApps = installedApps,
            diagnostics = diagnostics,
        )
        val completedState = async(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.first { it.preLaunch?.assessment != null }
        }

        viewModel.launch(entry)
        advanceUntilIdle()

        assertEquals(NetworkQuality.NOT_MEASURABLE, completedState.await().preLaunch?.assessment?.quality)
        assertTrue(installedApps.launchedPackages.isEmpty())
    }

    @Test
    fun `optional logging failure does not block target launch`() = runTest(dispatcher) {
        val entry = StreamingEntry(id = 1, displayName = "Steam Link", packageName = "com.valvesoftware.steamlink")
        val entries = FakeStreamingEntryRepository().apply { values.value = listOf(entry) }
        val installedApps = FakeInstalledAppsRepository()
        val log = FakeDiagnosticLogRepository().apply { logFailure = IllegalStateException("disk unavailable") }
        val viewModel = createViewModel(
            entries = entries,
            installedApps = installedApps,
            settings = FakeSettingsRepository(AppSettings(preLaunchCheckEnabled = false)),
            log = log,
        )

        viewModel.launch(entry)
        advanceUntilIdle()

        assertEquals(listOf(entry.packageName), installedApps.launchedPackages)
        assertTrue(entries.values.value.single().lastUsedAt != null)
    }

    @Test
    fun `successful launch starts a session and returning completes it`() = runTest(dispatcher) {
        val entry = StreamingEntry(id = 1, displayName = "Moonlight", packageName = "com.limelight")
        val entries = FakeStreamingEntryRepository().apply { values.value = listOf(entry) }
        val sessions = FakeStreamingSessionRepository()
        val viewModel = createViewModel(
            entries = entries,
            settings = FakeSettingsRepository(AppSettings(preLaunchCheckEnabled = false)),
            sessions = sessions,
        )

        viewModel.launch(entry)
        advanceUntilIdle()
        assertEquals("Moonlight", sessions.activeName)

        sessions.activeStart = sessions.activeStart!! - 600_000
        viewModel.completeSessionIfAny()
        advanceUntilIdle()

        assertEquals("Moonlight", sessions.last.value?.entryName)
        assertEquals(10L, sessions.last.value?.durationMinutes)
    }

    private fun createViewModel(
        entries: FakeStreamingEntryRepository = FakeStreamingEntryRepository(),
        hosts: FakeLocalHostRepository = FakeLocalHostRepository(),
        installedApps: FakeInstalledAppsRepository = FakeInstalledAppsRepository(),
        diagnostics: FakeNetworkDiagnosticsService = FakeNetworkDiagnosticsService(),
        settings: FakeSettingsRepository = FakeSettingsRepository(),
        history: FakeHistoryRepository = FakeHistoryRepository(),
        log: FakeDiagnosticLogRepository = FakeDiagnosticLogRepository(),
        sessions: FakeStreamingSessionRepository = FakeStreamingSessionRepository(),
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
        sessionRepository = sessions,
    )
}
