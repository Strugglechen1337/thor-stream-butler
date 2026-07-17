package de.thorstream.butler.feature.networktest

import de.thorstream.butler.core.network.QualityEvaluator
import de.thorstream.butler.domain.model.ConnectionType
import de.thorstream.butler.domain.model.NetworkQuality
import de.thorstream.butler.domain.model.NetworkSnapshot
import de.thorstream.butler.domain.service.DiagnosticProgress
import de.thorstream.butler.fakes.FakeHistoryRepository
import de.thorstream.butler.fakes.FakeNetworkDiagnosticsService
import de.thorstream.butler.fakes.FakeSettingsRepository
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
                DiagnosticProgress("Ping", 0.5f, snapshot),
                DiagnosticProgress("Fertig", 1f, snapshot, completed = true),
            ),
        )
        val history = FakeHistoryRepository()
        val viewModel = NetworkTestViewModel(diagnostics, FakeSettingsRepository(), history, QualityEvaluator())

        viewModel.startTest()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.running)
        assertEquals(NetworkQuality.OPTIMAL, viewModel.uiState.value.assessment?.quality)
        assertEquals(1, history.values.value.size)
    }

    @Test
    fun `missing network exposes error and stores no invented measurement`() = runTest(dispatcher) {
        val diagnostics = FakeNetworkDiagnosticsService(
            progress = flowOf(DiagnosticProgress("Netzwerk nicht verfügbar", 1f, completed = true, errorMessage = "Keine aktive Netzwerkverbindung gefunden.")),
        )
        val history = FakeHistoryRepository()
        val viewModel = NetworkTestViewModel(diagnostics, FakeSettingsRepository(), history, QualityEvaluator())

        viewModel.startTest()
        advanceUntilIdle()

        assertEquals("Keine aktive Netzwerkverbindung gefunden.", viewModel.uiState.value.errorMessage)
        assertNull(viewModel.uiState.value.snapshot)
        assertEquals(0, history.values.value.size)
    }
}

