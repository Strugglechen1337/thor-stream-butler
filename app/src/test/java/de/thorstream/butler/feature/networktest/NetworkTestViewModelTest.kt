package de.thorstream.butler.feature.networktest

import de.thorstream.butler.core.network.QualityEvaluator
import de.thorstream.butler.domain.model.ConnectionType
import de.thorstream.butler.domain.model.NetworkQuality
import de.thorstream.butler.domain.model.NetworkSnapshot
import de.thorstream.butler.domain.service.DiagnosticProgress
import de.thorstream.butler.domain.service.DiagnosticStep
import de.thorstream.butler.fakes.FakeHistoryRepository
import de.thorstream.butler.fakes.FakeNetworkDiagnosticsService
import de.thorstream.butler.fakes.FakeSettingsRepository
import de.thorstream.butler.fakes.FakeStringProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NetworkTestViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `successful completed test is assessed and stored`() = runTest(dispatcher) {
        val snapshot = NetworkSnapshot(ConnectionType.ETHERNET, latencyMs = 12.0, jitterMs = 2.0, packetLossPercent = 0.0)
        val diagnostics = FakeNetworkDiagnosticsService(
            progress = flowOf(
                DiagnosticProgress(DiagnosticStep.LATENCY_MEASURED, 0.5f, snapshot),
                DiagnosticProgress(DiagnosticStep.COMPLETED, 1f, snapshot, completed = true),
            ),
        )
        val history = FakeHistoryRepository()
        val viewModel = NetworkTestViewModel(diagnostics, FakeSettingsRepository(), history, QualityEvaluator(FakeStringProvider()), FakeStringProvider())

        viewModel.startTest()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.running)
        assertEquals(NetworkQuality.OPTIMAL, viewModel.uiState.value.assessment?.quality)
        assertEquals(1, history.values.value.size)
    }

    @Test
    fun `missing network exposes error and stores no invented measurement`() = runTest(dispatcher) {
        val diagnostics = FakeNetworkDiagnosticsService(
            progress = flowOf(DiagnosticProgress(DiagnosticStep.NETWORK_UNAVAILABLE, 1f, completed = true, errorMessage = "no active network")),
        )
        val history = FakeHistoryRepository()
        val viewModel = NetworkTestViewModel(diagnostics, FakeSettingsRepository(), history, QualityEvaluator(FakeStringProvider()), FakeStringProvider())

        viewModel.startTest()
        advanceUntilIdle()

        assertEquals("no active network", viewModel.uiState.value.errorMessage)
        assertNull(viewModel.uiState.value.snapshot)
        assertEquals(0, history.values.value.size)
    }

    @Test
    fun `unexpected diagnostics failure never leaves test running`() = runTest(dispatcher) {
        val diagnostics = FakeNetworkDiagnosticsService(progress = flow { error("simulated failure") })
        val viewModel = NetworkTestViewModel(
            diagnostics,
            FakeSettingsRepository(),
            FakeHistoryRepository(),
            QualityEvaluator(FakeStringProvider()),
            FakeStringProvider(),
        )

        viewModel.startTest()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.running)
        assertEquals("res:${de.thorstream.butler.R.string.error_diagnostics_failed}", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `partial completed diagnostic is stored as not measurable`() = runTest(dispatcher) {
        val snapshot = NetworkSnapshot(ConnectionType.ETHERNET, latencyMs = 8.0, jitterMs = 1.0, packetLossPercent = 0.0)
        val history = FakeHistoryRepository()
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
        val viewModel = NetworkTestViewModel(
            diagnostics,
            FakeSettingsRepository(),
            history,
            QualityEvaluator(FakeStringProvider()),
            FakeStringProvider(),
        )

        viewModel.startTest()
        advanceUntilIdle()

        assertEquals(NetworkQuality.NOT_MEASURABLE, viewModel.uiState.value.assessment?.quality)
        assertEquals(NetworkQuality.NOT_MEASURABLE, history.values.value.single().assessment.quality)
    }
}

