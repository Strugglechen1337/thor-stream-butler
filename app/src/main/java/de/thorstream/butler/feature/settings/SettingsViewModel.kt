package de.thorstream.butler.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.thorstream.butler.R
import de.thorstream.butler.core.common.AppResult
import de.thorstream.butler.core.common.StringProvider
import de.thorstream.butler.domain.model.AppSettings
import de.thorstream.butler.domain.repository.NetworkHistoryRepository
import de.thorstream.butler.domain.repository.SettingsRepository
import de.thorstream.butler.domain.service.ConfigurationTransferService
import javax.inject.Inject
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
) : ViewModel() {
    val settings: StateFlow<AppSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())
    val transferState = MutableStateFlow(TransferUiState())

    fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { repository.update(transform(settings.value)) }
    }

    fun clearHistory() {
        viewModelScope.launch { historyRepository.clear() }
    }

    fun exportConfiguration(documentUri: String, includeHistory: Boolean) {
        if (transferState.value.inProgress) return
        viewModelScope.launch {
            transferState.value = TransferUiState(inProgress = true)
            transferState.value = when (val result = configurationTransferService.exportTo(documentUri, includeHistory)) {
                is AppResult.Success -> TransferUiState(message = strings.get(
                    R.string.settings_transfer_export_success,
                    result.value.streamingEntries,
                    result.value.localHosts,
                    result.value.historyMeasurements,
                ))
                is AppResult.Failure -> TransferUiState(message = result.error.message)
            }
        }
    }

    fun importConfiguration(documentUri: String) {
        if (transferState.value.inProgress) return
        viewModelScope.launch {
            transferState.value = TransferUiState(inProgress = true)
            transferState.value = when (val result = configurationTransferService.importFrom(documentUri)) {
                is AppResult.Success -> TransferUiState(message = strings.get(
                    R.string.settings_transfer_import_success,
                    result.value.streamingEntries,
                    result.value.localHosts,
                    result.value.historyMeasurements,
                ))
                is AppResult.Failure -> TransferUiState(message = result.error.message)
            }
        }
    }

    fun consumeTransferMessage() {
        transferState.value = transferState.value.copy(message = null)
    }
}
