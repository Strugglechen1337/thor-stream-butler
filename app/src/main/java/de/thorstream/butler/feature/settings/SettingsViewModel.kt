package de.thorstream.butler.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.thorstream.butler.R
import de.thorstream.butler.core.common.AppResult
import de.thorstream.butler.core.common.StringProvider
import de.thorstream.butler.domain.model.AppSettings
import de.thorstream.butler.domain.repository.NetworkHistoryRepository
import de.thorstream.butler.domain.repository.DiagnosticLogRepository
import de.thorstream.butler.domain.repository.SettingsRepository
import de.thorstream.butler.domain.service.ConfigurationTransferService
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TransferUiState(
    val inProgress: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
    private val historyRepository: NetworkHistoryRepository,
    private val configurationTransferService: ConfigurationTransferService,
    private val strings: StringProvider,
    private val diagnosticLogRepository: DiagnosticLogRepository,
) : ViewModel() {
    val settings: StateFlow<AppSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())
    val transferState = MutableStateFlow(TransferUiState())
    val diagnosticLogCount = MutableStateFlow(0)

    init {
        refreshDiagnosticLogCount()
    }

    fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            try {
                repository.update(transform(settings.value))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                transferState.value = TransferUiState(message = strings.get(R.string.error_local_data_failed))
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            try {
                historyRepository.clear()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                transferState.value = TransferUiState(message = strings.get(R.string.error_local_data_failed))
            }
        }
    }

    fun refreshDiagnosticLogCount() {
        viewModelScope.launch {
            try {
                diagnosticLogCount.value = diagnosticLogRepository.read().size
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                diagnosticLogCount.value = 0
            }
        }
    }

    fun clearDiagnosticLog() {
        viewModelScope.launch {
            try {
                diagnosticLogRepository.clear()
                diagnosticLogCount.value = 0
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                transferState.value = TransferUiState(message = strings.get(R.string.error_local_data_failed))
            }
        }
    }

    fun reportLinkFailure() {
        transferState.value = TransferUiState(message = strings.get(R.string.settings_about_link_failed))
    }

    fun exportConfiguration(documentUri: String, includeHistory: Boolean) {
        if (transferState.value.inProgress) return
        viewModelScope.launch {
            transferState.value = TransferUiState(inProgress = true)
            try {
                transferState.value = when (val result = configurationTransferService.exportTo(documentUri, includeHistory)) {
                    is AppResult.Success -> TransferUiState(message = strings.get(
                        R.string.settings_transfer_export_success,
                        result.value.streamingEntries,
                        result.value.localHosts,
                        result.value.historyMeasurements,
                    ))
                    is AppResult.Failure -> TransferUiState(message = result.error.message)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                transferState.value = TransferUiState(message = strings.get(R.string.settings_transfer_export_failed))
            }
        }
    }

    fun importConfiguration(documentUri: String) {
        if (transferState.value.inProgress) return
        viewModelScope.launch {
            transferState.value = TransferUiState(inProgress = true)
            try {
                transferState.value = when (val result = configurationTransferService.importFrom(documentUri)) {
                    is AppResult.Success -> TransferUiState(message = strings.get(
                        R.string.settings_transfer_import_success,
                        result.value.streamingEntries,
                        result.value.localHosts,
                        result.value.historyMeasurements,
                    ))
                    is AppResult.Failure -> TransferUiState(message = result.error.message)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                transferState.value = TransferUiState(message = strings.get(R.string.settings_transfer_invalid))
            }
        }
    }

    fun consumeTransferMessage() {
        transferState.value = transferState.value.copy(message = null)
    }
}
