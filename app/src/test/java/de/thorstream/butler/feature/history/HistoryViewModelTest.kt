package de.thorstream.butler.feature.history

import de.thorstream.butler.R
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
}
