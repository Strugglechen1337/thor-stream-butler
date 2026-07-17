package de.thorstream.butler.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.thorstream.butler.core.common.AppResult
import de.thorstream.butler.domain.model.InstalledApp
import de.thorstream.butler.domain.model.StreamingEntry
import de.thorstream.butler.domain.model.StreamingType
import de.thorstream.butler.domain.repository.InstalledAppsRepository
import de.thorstream.butler.domain.repository.StreamingEntryRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardItem(val entry: StreamingEntry, val isInstalled: Boolean)

data class DashboardUiState(
    val items: List<DashboardItem> = emptyList(),
    val installedApps: List<InstalledApp> = emptyList(),
    val isLoadingApps: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val entriesRepository: StreamingEntryRepository,
    private val installedAppsRepository: InstalledAppsRepository,
) : ViewModel() {
    private val localState = MutableStateFlow(DashboardUiState())

    val uiState: StateFlow<DashboardUiState> = combine(
        entriesRepository.observeEntries(),
        localState,
    ) { entries, local ->
        local.copy(items = entries.map { DashboardItem(it, installedAppsRepository.canLaunch(it.packageName)) })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    init {
        viewModelScope.launch { entriesRepository.ensureDemoEntries() }
    }

    fun loadInstalledApps() {
        if (localState.value.isLoadingApps) return
        viewModelScope.launch {
            localState.value = localState.value.copy(isLoadingApps = true, message = null)
            localState.value = when (val result = installedAppsRepository.getLaunchableApps()) {
                is AppResult.Success -> localState.value.copy(installedApps = result.value, isLoadingApps = false)
                is AppResult.Failure -> localState.value.copy(isLoadingApps = false, message = result.error.message)
            }
        }
    }

    fun addApp(app: InstalledApp, type: StreamingType, customName: String?) {
        viewModelScope.launch {
            val nextOrder = (uiState.value.items.maxOfOrNull { it.entry.sortOrder } ?: -1) + 1
            entriesRepository.save(
                StreamingEntry(
                    displayName = customName?.trim().takeUnless { it.isNullOrBlank() } ?: app.label,
                    packageName = app.packageName,
                    iconReference = app.iconReference,
                    streamingType = type,
                    customName = customName?.trim().takeUnless { it.isNullOrBlank() },
                    sortOrder = nextOrder,
                ),
            )
            localState.value = localState.value.copy(message = "${app.label} wurde zum Dashboard hinzugefügt.")
        }
    }

    fun launch(entry: StreamingEntry) {
        when (val result = installedAppsRepository.launch(entry.packageName)) {
            is AppResult.Success -> viewModelScope.launch {
                entriesRepository.markLaunched(entry.id, System.currentTimeMillis(), entry.lastNetworkQuality?.name)
            }
            is AppResult.Failure -> localState.value = localState.value.copy(message = result.error.message)
        }
    }

    fun delete(entry: StreamingEntry) {
        viewModelScope.launch { entriesRepository.delete(entry) }
    }

    fun consumeMessage() {
        localState.value = localState.value.copy(message = null)
    }
}

