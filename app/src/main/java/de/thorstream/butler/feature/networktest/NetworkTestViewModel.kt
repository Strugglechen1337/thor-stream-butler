package de.thorstream.butler.feature.networktest

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.thorstream.butler.R
import de.thorstream.butler.core.designsystem.labelRes
import de.thorstream.butler.core.network.QualityEvaluator
import de.thorstream.butler.domain.model.NetworkMeasurement
import de.thorstream.butler.domain.model.NetworkSnapshot
import de.thorstream.butler.domain.model.QualityAssessment
import de.thorstream.butler.domain.repository.NetworkHistoryRepository
import de.thorstream.butler.domain.repository.SettingsRepository
import de.thorstream.butler.domain.service.NetworkDiagnosticsService
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class NetworkTestUiState(
    val running: Boolean = false,
    @param:StringRes val stepRes: Int = R.string.nettest_step_idle,
    val progress: Float = 0f,
    val snapshot: NetworkSnapshot? = null,
    val assessment: QualityAssessment? = null,
    val errorMessage: String? = null,
)

@HiltViewModel
class NetworkTestViewModel @Inject constructor(
    private val diagnosticsService: NetworkDiagnosticsService,
    private val settingsRepository: SettingsRepository,
    private val historyRepository: NetworkHistoryRepository,
    private val qualityEvaluator: QualityEvaluator,
) : ViewModel() {
    private val _uiState = MutableStateFlow(NetworkTestUiState())
    val uiState: StateFlow<NetworkTestUiState> = _uiState.asStateFlow()
    private var testJob: Job? = null

    fun startTest() {
        testJob?.cancel()
        testJob = viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            _uiState.value = NetworkTestUiState(running = true, stepRes = R.string.nettest_step_preparing)
            diagnosticsService.runDiagnostics(
                target = settings.defaultTestTarget,
                pingCount = settings.pingCount,
                includeDownloadTest = settings.downloadTestEnabled,
                testDurationSeconds = settings.testDurationSeconds,
            ).collect { progress ->
                _uiState.value = _uiState.value.copy(
                    running = !progress.completed,
                    stepRes = progress.step.labelRes(),
                    progress = progress.progress,
                    snapshot = progress.snapshot ?: _uiState.value.snapshot,
                    errorMessage = progress.errorMessage,
                )
                if (progress.completed && progress.snapshot != null) {
                    val assessment = qualityEvaluator.evaluate(progress.snapshot)
                    _uiState.value = _uiState.value.copy(assessment = assessment)
                    historyRepository.save(
                        NetworkMeasurement(
                            timestamp = System.currentTimeMillis(),
                            snapshot = progress.snapshot,
                            assessment = assessment,
                        ),
                    )
                }
            }
        }
    }

    fun cancelTest() {
        testJob?.cancel()
        testJob = null
        _uiState.value = _uiState.value.copy(running = false, stepRes = R.string.nettest_step_cancelled)
    }
}

