package de.thorstream.butler.feature.settings

import de.thorstream.butler.R
import de.thorstream.butler.core.common.AppResult
import de.thorstream.butler.domain.service.ConfigurationTransferService
import de.thorstream.butler.domain.service.ConfigurationTransferSummary
import de.thorstream.butler.fakes.FakeDiagnosticLogRepository
import de.thorstream.butler.fakes.FakeHistoryRepository
import de.thorstream.butler.fakes.FakeSettingsRepository
import de.thorstream.butler.fakes.FakeStringProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `unexpected export failure clears progress and shows localized error`() = runTest(dispatcher) {
        val viewModel = SettingsViewModel(
            repository = FakeSettingsRepository(),
            historyRepository = FakeHistoryRepository(),
            configurationTransferService = ThrowingTransferService(),
            strings = FakeStringProvider(),
            diagnosticLogRepository = FakeDiagnosticLogRepository(),
        )

        viewModel.exportConfiguration("content://backup", includeHistory = true)
        advanceUntilIdle()

        assertFalse(viewModel.transferState.value.inProgress)
        assertEquals("res:${R.string.settings_transfer_export_failed}", viewModel.transferState.value.message)
    }

    private class ThrowingTransferService : ConfigurationTransferService {
        override suspend fun exportTo(documentUri: String, includeHistory: Boolean): AppResult<ConfigurationTransferSummary> =
            error("simulated provider failure")

        override suspend fun importFrom(documentUri: String): AppResult<ConfigurationTransferSummary> =
            error("simulated provider failure")
    }
}
