package de.thorstream.butler.feature.hosts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.thorstream.butler.R
import de.thorstream.butler.core.common.AppResult
import de.thorstream.butler.core.common.StringProvider
import de.thorstream.butler.core.validation.NetworkValidators
import de.thorstream.butler.domain.model.LocalHost
import de.thorstream.butler.domain.service.DiscoveredHost
import de.thorstream.butler.domain.repository.LocalHostRepository
import de.thorstream.butler.domain.service.HostDiscoveryService
import de.thorstream.butler.domain.service.LocalHostDiscoveryService
import de.thorstream.butler.domain.service.WakeOnLanService
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HostsUiState(
    val hosts: List<LocalHost> = emptyList(),
    val testingHostId: Long? = null,
    val isDiscovering: Boolean = false,
    val discoveredHosts: List<DiscoveredHost> = emptyList(),
    val message: String? = null,
)

@HiltViewModel
class HostsViewModel @Inject constructor(
    private val repository: LocalHostRepository,
    private val discoveryService: HostDiscoveryService,
    private val localHostDiscoveryService: LocalHostDiscoveryService,
    private val wakeOnLanService: WakeOnLanService,
    private val strings: StringProvider,
) : ViewModel() {
    private val localState = MutableStateFlow(HostsUiState())
    val uiState: StateFlow<HostsUiState> = combine(repository.observeHosts(), localState) { hosts, local -> local.copy(hosts = hosts) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HostsUiState())

    fun save(host: LocalHost): String? {
        if (host.name.isBlank() || host.name.trim().length > MAX_NAME_LENGTH) return strings.get(R.string.hosts_error_name)
        if (host.address.trim().length > MAX_HOST_LENGTH || !NetworkValidators.isValidHostnameOrIp(host.address.trim())) return strings.get(R.string.hosts_error_address)
        if (host.port != null && host.port !in 1..65_535) return strings.get(R.string.hosts_error_port)
        if (!host.macAddress.isNullOrBlank() && !NetworkValidators.isValidMac(host.macAddress)) return strings.get(R.string.hosts_error_mac)
        if (host.broadcastAddress.trim().length > MAX_HOST_LENGTH || !NetworkValidators.isValidHostnameOrIp(host.broadcastAddress.trim())) return strings.get(R.string.hosts_error_broadcast)
        val normalized = host.copy(
            name = host.name.trim(),
            address = NetworkValidators.normalizeHost(host.address),
            macAddress = host.macAddress?.takeIf { it.isNotBlank() }?.let(NetworkValidators::normalizeMac),
            broadcastAddress = NetworkValidators.normalizeHost(host.broadcastAddress),
        )
        viewModelScope.launch {
            try {
                repository.save(normalized)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                localState.value = localState.value.copy(message = strings.get(R.string.error_local_data_failed))
            }
        }
        return null
    }

    fun delete(host: LocalHost) {
        viewModelScope.launch {
            try {
                repository.delete(host)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                localState.value = localState.value.copy(message = strings.get(R.string.error_local_data_failed))
            }
        }
    }

    fun test(host: LocalHost) {
        viewModelScope.launch {
            localState.value = localState.value.copy(testingHostId = host.id, message = null)
            try {
                when (val result = discoveryService.isReachable(host.address, host.port)) {
                    is AppResult.Success -> {
                        repository.updateTestResult(host.id, result.value, System.currentTimeMillis())
                        localState.value = localState.value.copy(
                            message = if (result.value) strings.get(R.string.hosts_msg_reachable, host.name)
                            else strings.get(R.string.hosts_msg_unreachable, host.name),
                        )
                    }
                    is AppResult.Failure -> localState.value = localState.value.copy(message = result.error.message)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                localState.value = localState.value.copy(message = strings.get(R.string.error_local_data_failed))
            } finally {
                localState.value = localState.value.copy(testingHostId = null)
            }
        }
    }

    fun wake(host: LocalHost) {
        val mac = host.macAddress ?: run {
            localState.value = localState.value.copy(message = strings.get(R.string.hosts_msg_no_mac, host.name))
            return
        }
        viewModelScope.launch {
            try {
                localState.value = when (val result = wakeOnLanService.sendMagicPacket(mac, host.broadcastAddress)) {
                    is AppResult.Success -> localState.value.copy(message = strings.get(R.string.hosts_msg_wol_sent, host.name))
                    is AppResult.Failure -> localState.value.copy(message = result.error.message)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                localState.value = localState.value.copy(message = strings.get(R.string.error_local_data_failed))
            }
        }
    }

    fun discoverLocalHosts() {
        if (localState.value.isDiscovering) return
        viewModelScope.launch {
            localState.value = localState.value.copy(isDiscovering = true, discoveredHosts = emptyList(), message = null)
            try {
                localState.value = when (val result = localHostDiscoveryService.discover()) {
                    is AppResult.Success -> localState.value.copy(
                        discoveredHosts = result.value,
                        message = if (result.value.isEmpty()) strings.get(R.string.hosts_discovery_empty) else strings.get(R.string.hosts_discovery_found, result.value.size),
                    )
                    is AppResult.Failure -> localState.value.copy(message = result.error.message)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                localState.value = localState.value.copy(message = strings.get(R.string.hosts_discovery_failed))
            } finally {
                localState.value = localState.value.copy(isDiscovering = false)
            }
        }
    }

    fun dismissDiscovery() {
        localState.value = localState.value.copy(discoveredHosts = emptyList())
    }

    fun consumeMessage() {
        localState.value = localState.value.copy(message = null)
    }

    fun reportLocalNetworkPermissionDenied() {
        localState.value = localState.value.copy(
            message = strings.get(R.string.hosts_msg_local_permission),
        )
    }

    private companion object {
        const val MAX_NAME_LENGTH = 120
        const val MAX_HOST_LENGTH = 253
    }
}
