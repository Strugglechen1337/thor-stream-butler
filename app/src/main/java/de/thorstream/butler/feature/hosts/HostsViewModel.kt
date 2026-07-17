package de.thorstream.butler.feature.hosts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.thorstream.butler.R
import de.thorstream.butler.core.common.AppResult
import de.thorstream.butler.core.common.StringProvider
import de.thorstream.butler.core.validation.NetworkValidators
import de.thorstream.butler.domain.model.LocalHost
import de.thorstream.butler.domain.repository.LocalHostRepository
import de.thorstream.butler.domain.service.HostDiscoveryService
import de.thorstream.butler.domain.service.WakeOnLanService
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HostsUiState(
    val hosts: List<LocalHost> = emptyList(),
    val testingHostId: Long? = null,
    val message: String? = null,
)

@HiltViewModel
class HostsViewModel @Inject constructor(
    private val repository: LocalHostRepository,
    private val discoveryService: HostDiscoveryService,
    private val wakeOnLanService: WakeOnLanService,
    private val strings: StringProvider,
) : ViewModel() {
    private val localState = MutableStateFlow(HostsUiState())
    val uiState: StateFlow<HostsUiState> = combine(repository.observeHosts(), localState) { hosts, local -> local.copy(hosts = hosts) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HostsUiState())

    fun save(host: LocalHost): String? {
        if (host.name.isBlank()) return strings.get(R.string.hosts_error_name)
        if (!NetworkValidators.isValidHostnameOrIpv4(host.address.trim())) return strings.get(R.string.hosts_error_address)
        if (host.port != null && host.port !in 1..65_535) return strings.get(R.string.hosts_error_port)
        if (!host.macAddress.isNullOrBlank() && !NetworkValidators.isValidMac(host.macAddress)) return strings.get(R.string.hosts_error_mac)
        if (!NetworkValidators.isValidHostnameOrIpv4(host.broadcastAddress.trim())) return strings.get(R.string.hosts_error_broadcast)
        val normalized = host.copy(
            name = host.name.trim(),
            address = host.address.trim(),
            macAddress = host.macAddress?.takeIf { it.isNotBlank() }?.let(NetworkValidators::normalizeMac),
            broadcastAddress = host.broadcastAddress.trim(),
        )
        viewModelScope.launch { repository.save(normalized) }
        return null
    }

    fun delete(host: LocalHost) {
        viewModelScope.launch { repository.delete(host) }
    }

    fun test(host: LocalHost) {
        viewModelScope.launch {
            localState.value = localState.value.copy(testingHostId = host.id, message = null)
            val reachable = when (val result = discoveryService.isReachable(host.address, host.port)) {
                is AppResult.Success -> result.value
                is AppResult.Failure -> {
                    localState.value = localState.value.copy(message = result.error.message)
                    false
                }
            }
            repository.updateTestResult(host.id, reachable, System.currentTimeMillis())
            localState.value = localState.value.copy(
                testingHostId = null,
                message = if (reachable) strings.get(R.string.hosts_msg_reachable, host.name) else strings.get(R.string.hosts_msg_unreachable, host.name),
            )
        }
    }

    fun wake(host: LocalHost) {
        val mac = host.macAddress ?: run {
            localState.value = localState.value.copy(message = strings.get(R.string.hosts_msg_no_mac, host.name))
            return
        }
        viewModelScope.launch {
            localState.value = when (val result = wakeOnLanService.sendMagicPacket(mac, host.broadcastAddress)) {
                is AppResult.Success -> localState.value.copy(message = strings.get(R.string.hosts_msg_wol_sent, host.name))
                is AppResult.Failure -> localState.value.copy(message = result.error.message)
            }
        }
    }

    fun consumeMessage() {
        localState.value = localState.value.copy(message = null)
    }

    fun reportLocalNetworkPermissionDenied() {
        localState.value = localState.value.copy(
            message = strings.get(R.string.hosts_msg_local_permission),
        )
    }
}
