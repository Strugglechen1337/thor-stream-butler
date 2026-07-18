package de.thorstream.butler.feature.history

import de.thorstream.butler.R
import de.thorstream.butler.domain.model.ConnectionType
import de.thorstream.butler.domain.model.NetworkMeasurement
import de.thorstream.butler.domain.model.NetworkQuality
import de.thorstream.butler.domain.model.NetworkSnapshot
import de.thorstream.butler.domain.model.QualityAssessment
import de.thorstream.butler.fakes.FakeHistoryRepository
import de.thorstream.butler.fakes.FakeStringProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `clear failure is exposed without crashing`() = runTest(dispatcher) {
        val repository = FakeHistoryRepository().apply {
            clearFailure = IllegalStateException("database unavailable")
        }
        val viewModel = HistoryViewModel(repository, FakeStringProvider())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        viewModel.clear()
        advanceUntilIdle()

        assertEquals("res:${R.string.error_local_data_failed}", viewModel.uiState.value.message)
    }

    @Test
    fun `wifi comparison is derived from history and can be selected`() = runTest(dispatcher) {
        val repository = FakeHistoryRepository().apply {
            values.value = listOf(
                NetworkMeasurement(
                    id = 1,
                    timestamp = 100,
                    snapshot = NetworkSnapshot(
                        connectionType = ConnectionType.WIFI,
                        ssid = "Gaming",
                        latencyMs = 20.0,
                        jitterMs = 4.0,
                        packetLossPercent = 0.0,
                    ),
                    assessment = QualityAssessment(NetworkQuality.OPTIMAL, "test"),
                ),
            )
        }
        val viewModel = HistoryViewModel(repository, FakeStringProvider())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        assertEquals("Gaming", viewModel.uiState.value.wifiComparison.networks.single().ssid)

        viewModel.selectView(HistoryView.WIFI_COMPARISON)
        advanceUntilIdle()

        assertEquals(HistoryView.WIFI_COMPARISON, viewModel.uiState.value.view)
    }
}
