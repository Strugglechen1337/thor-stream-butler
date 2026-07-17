package de.thorstream.butler.feature.dashboard

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.thorstream.butler.R
import de.thorstream.butler.core.common.AppResult
import de.thorstream.butler.core.common.StringProvider
import de.thorstream.butler.core.designsystem.labelRes
import de.thorstream.butler.domain.model.InstalledApp
import de.thorstream.butler.domain.model.ConnectionType
import de.thorstream.butler.domain.model.NetworkMeasurement
import de.thorstream.butler.domain.model.NetworkQuality
import de.thorstream.butler.domain.model.NetworkSnapshot
import de.thorstream.butler.domain.model.QualityAssessment
import de.thorstream.butler.domain.model.StreamingEntry
import de.thorstream.butler.domain.model.StreamingType
import de.thorstream.butler.domain.repository.InstalledAppsRepository
import de.thorstream.butler.domain.repository.NetworkHistoryRepository
import de.thorstream.butler.domain.repository.SettingsRepository
import de.thorstream.butler.domain.repository.StreamingEntryRepository
import de.thorstream.butler.domain.service.NetworkDiagnosticsService
import de.thorstream.butler.core.network.QualityEvaluator
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardItem(val entry: StreamingEntry, val isInstalled: Boolean)

data class DashboardUiState(
    val items: List<DashboardItem> = emptyList(),
    val installedApps: List<InstalledApp> = emptyList(),
    val isLoadingApps: Boolean = false,
    val message: String? = null,
    val preLaunch: PreLaunchUiState? = null,
    val focusAnimationsEnabled: Boolean = true,
)

data class PreLaunchUiState(
    val entry: StreamingEntry,
    @param:StringRes val stepRes: Int,
    val progress: Float,
    val snapshot: NetworkSnapshot? = null,
    val assessment: QualityAssessment? = null,
    val autoLaunching: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val entriesRepository: StreamingEntryRepository,
    private val installedAppsRepository: InstalledAppsRepository,
    private val diagnosticsService: NetworkDiagnosticsService,
    private val settingsRepository: SettingsRepository,
    private val historyRepository: NetworkHistoryRepository,
    private val qualityEvaluator: QualityEvaluator,
    private val strings: StringProvider,
) : ViewModel() {
    private val localState = MutableStateFlow(DashboardUiState())
    private var preLaunchJob: Job? = null

    val uiState: StateFlow<DashboardUiState> = combine(
        entriesRepository.observeEntries(),
        localState,
        settingsRepository.settings,
    ) { entries, local, settings ->
        local.copy(
            items = entries.map { DashboardItem(it, installedAppsRepository.canLaunch(it.packageName)) },
            focusAnimationsEnabled = settings.focusAnimationsEnabled,
        )
        // canLaunch queries the PackageManager per entry; keep that off the main thread.
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

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
            localState.value = localState.value.copy(message = strings.get(R.string.dashboard_app_added, app.label))
        }
    }

    fun launch(entry: StreamingEntry) {
        preLaunchJob?.cancel()
        preLaunchJob = viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            if (!settings.preLaunchCheckEnabled) {
                launchTarget(entry, null)
                return@launch
            }
            localState.value = localState.value.copy(
                preLaunch = PreLaunchUiState(entry, R.string.dashboard_check_preparing, 0f),
            )
            diagnosticsService.runDiagnostics(
                target = settings.defaultTestTarget,
                pingCount = settings.pingCount.coerceAtMost(5),
                includeDownloadTest = false,
            ).collect { progress ->
                localState.value = localState.value.copy(
                    preLaunch = localState.value.preLaunch?.copy(
                        stepRes = progress.step.labelRes(),
                        progress = progress.progress,
                        snapshot = progress.snapshot ?: localState.value.preLaunch?.snapshot,
                        errorMessage = progress.errorMessage,
                    ),
                )
                if (progress.completed) {
                    val snapshot = progress.snapshot ?: NetworkSnapshot(ConnectionType.NONE)
                    val assessment = qualityEvaluator.evaluate(snapshot)
                    // Only persist real measurements; a failed check must not invent history entries.
                    if (progress.snapshot != null) {
                        historyRepository.save(NetworkMeasurement(timestamp = System.currentTimeMillis(), snapshot = snapshot, assessment = assessment))
                    }
                    val autoLaunch = when (assessment.quality) {
                        NetworkQuality.OPTIMAL -> settings.autoLaunchOnGreen
                        NetworkQuality.USABLE -> true
                        NetworkQuality.PROBLEMATIC -> !settings.confirmOnRed
                        NetworkQuality.NOT_MEASURABLE -> false
                    }
                    localState.value = localState.value.copy(
                        preLaunch = localState.value.preLaunch?.copy(
                            stepRes = if (autoLaunch) R.string.dashboard_check_launching else R.string.dashboard_check_finished,
                            progress = 1f,
                            snapshot = snapshot,
                            assessment = assessment,
                            autoLaunching = autoLaunch,
                        ),
                    )
                    if (autoLaunch) {
                        if (assessment.quality == NetworkQuality.USABLE && settings.warnOnYellow) {
                            localState.value = localState.value.copy(message = assessment.summary)
                        }
                        delay(1_800)
                        launchTarget(entry, assessment.quality)
                    }
                }
            }
        }
    }

    private fun launchTarget(entry: StreamingEntry, quality: NetworkQuality?) {
        when (val result = installedAppsRepository.launch(entry.packageName)) {
            is AppResult.Success -> viewModelScope.launch {
                entriesRepository.markLaunched(entry.id, System.currentTimeMillis(), quality?.name ?: entry.lastNetworkQuality?.name)
                localState.value = localState.value.copy(preLaunch = null)
            }
            is AppResult.Failure -> localState.value = localState.value.copy(message = result.error.message, preLaunch = null)
        }
    }

    fun launchAnyway() {
        val state = localState.value.preLaunch ?: return
        preLaunchJob?.cancel()
        launchTarget(state.entry, state.assessment?.quality)
    }

    fun retryPreLaunch() {
        val entry = localState.value.preLaunch?.entry ?: return
        launch(entry)
    }

    fun cancelPreLaunch() {
        preLaunchJob?.cancel()
        preLaunchJob = null
        localState.value = localState.value.copy(preLaunch = null)
    }

    fun delete(entry: StreamingEntry) {
        viewModelScope.launch { entriesRepository.delete(entry) }
    }

    fun consumeMessage() {
        localState.value = localState.value.copy(message = null)
    }
}
