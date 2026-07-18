package de.thorstream.butler.feature.hosts

import de.thorstream.butler.core.common.AppError
import de.thorstream.butler.core.common.AppResult
import de.thorstream.butler.domain.model.LocalHost
import de.thorstream.butler.fakes.FakeHostDiscoveryService
import de.thorstream.butler.fakes.FakeLocalHostDiscoveryService
import de.thorstream.butler.fakes.FakeLocalHostRepository
import de.thorstream.butler.fakes.FakeStringProvider
import de.thorstream.butler.fakes.FakeWakeOnLanService
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HostsViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `failed host service keeps previous status and exposes original error`() = runTest(dispatcher) {
        val host = LocalHost(id = 1, name = "Gaming PC", address = "gaming-pc.local")
        val repository = FakeLocalHostRepository().apply { values.value = listOf(host) }
        val viewModel = HostsViewModel(
            repository = repository,
            discoveryService = FakeHostDiscoveryService(AppResult.Failure(AppError.MissingPermission("permission required"))),
            localHostDiscoveryService = FakeLocalHostDiscoveryService(),
            wakeOnLanService = FakeWakeOnLanService(),
            strings = FakeStringProvider(),
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        viewModel.test(host)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.testingHostId)
        assertEquals("permission required", viewModel.uiState.value.message)
        assertNull(repository.values.value.single().lastReachable)
    }

    @Test
    fun `successful host test is persisted and clears progress`() = runTest(dispatcher) {
        val host = LocalHost(id = 1, name = "Gaming PC", address = "gaming-pc.local")
        val repository = FakeLocalHostRepository().apply { values.value = listOf(host) }
        val viewModel = HostsViewModel(
            repository = repository,
            discoveryService = FakeHostDiscoveryService(true),
            localHostDiscoveryService = FakeLocalHostDiscoveryService(),
            wakeOnLanService = FakeWakeOnLanService(),
            strings = FakeStringProvider(),
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        viewModel.test(host)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.testingHostId)
        assertEquals(true, repository.values.value.single().lastReachable)
    }
}
