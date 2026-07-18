package de.thorstream.butler.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.thorstream.butler.R
import de.thorstream.butler.core.common.StringProvider
import de.thorstream.butler.domain.model.ConnectionType
import de.thorstream.butler.domain.model.NetworkMeasurement
import de.thorstream.butler.domain.repository.NetworkHistoryRepository
import javax.inject.Inject
import kotlin.math.abs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class HistoryFilter { ALL, WIFI, ETHERNET, CELLULAR, VPN, WITH_HOST }

enum class HistoryView { TIMELINE, WIFI_COMPARISON }

data class HistoryTrend(val direction: Int, val differenceMs: Double? = null, val isFirstComparable: Boolean = false)

data class HistoryItem(
    val measurement: NetworkMeasurement,
    val trend: HistoryTrend,
)

data class HistorySummary(
    val measurementCount: Int = 0,
    val averageLatencyMs: Double? = null,
    val averageJitterMs: Double? = null,
    val averagePacketLossPercent: Double? = null,
)

data class HistoryUiState(
    val view: HistoryView = HistoryView.TIMELINE,
    val filter: HistoryFilter = HistoryFilter.ALL,
    val allMeasurements: List<NetworkMeasurement> = emptyList(),
    val items: List<HistoryItem> = emptyList(),
    val summary: HistorySummary = HistorySummary(),
    val wifiComparison: WifiComparisonSummary = WifiComparisonSummary(),
    val message: String? = null,
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: NetworkHistoryRepository,
    private val strings: StringProvider,
) : ViewModel() {
    private val view = MutableStateFlow(HistoryView.TIMELINE)
    private val filter = MutableStateFlow(HistoryFilter.ALL)
    private val message = MutableStateFlow<String?>(null)

    private val history = repository.observeHistory().catch {
        message.value = strings.get(R.string.error_local_data_failed)
        emit(emptyList())
    }

    val uiState: StateFlow<HistoryUiState> = combine(history, view, filter, message) { history, selectedView, selectedFilter, currentMessage ->
        val filtered = history.filter { it.matches(selectedFilter) }
        HistoryUiState(
            view = selectedView,
            filter = selectedFilter,
            allMeasurements = history,
            items = buildHistoryItems(filtered),
            summary = filtered.toSummary(),
            wifiComparison = buildWifiComparison(history),
            message = currentMessage,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun selectView(value: HistoryView) {
        view.value = value
    }

    fun selectFilter(value: HistoryFilter) {
        filter.value = value
    }

    fun clear() {
        viewModelScope.launch {
            try {
                repository.clear()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                message.value = strings.get(R.string.error_local_data_failed)
            }
        }
    }

    fun consumeMessage() {
        message.value = null
    }
}

internal fun buildHistoryItems(measurements: List<NetworkMeasurement>): List<HistoryItem> = measurements.mapIndexed { index, newer ->
    val older = measurements.drop(index + 1).firstOrNull { candidate -> candidate.isComparableTo(newer) }
    HistoryItem(newer, latencyTrend(newer, older))
}

private fun NetworkMeasurement.isComparableTo(other: NetworkMeasurement): Boolean =
    snapshot.connectionType == other.snapshot.connectionType &&
        snapshot.ssid == other.snapshot.ssid &&
        snapshot.host == other.snapshot.host

private fun latencyTrend(newer: NetworkMeasurement, older: NetworkMeasurement?): HistoryTrend {
    val current = newer.snapshot.latencyMs ?: return HistoryTrend(0)
    val previous = older?.snapshot?.latencyMs ?: return HistoryTrend(0, isFirstComparable = true)
    val difference = current - previous
    if (abs(difference) < 2.0) return HistoryTrend(0, difference)
    return HistoryTrend(if (difference < 0) -1 else 1, difference)
}

private fun NetworkMeasurement.matches(filter: HistoryFilter): Boolean = when (filter) {
    HistoryFilter.ALL -> true
    HistoryFilter.WIFI -> snapshot.connectionType == ConnectionType.WIFI
    HistoryFilter.ETHERNET -> snapshot.connectionType == ConnectionType.ETHERNET
    HistoryFilter.CELLULAR -> snapshot.connectionType == ConnectionType.CELLULAR
    HistoryFilter.VPN -> snapshot.connectionType == ConnectionType.VPN
    HistoryFilter.WITH_HOST -> snapshot.host != null
}

private fun List<NetworkMeasurement>.toSummary() = HistorySummary(
    measurementCount = size,
    averageLatencyMs = mapNotNull { it.snapshot.latencyMs }.averageOrNull(),
    averageJitterMs = mapNotNull { it.snapshot.jitterMs }.averageOrNull(),
    averagePacketLossPercent = mapNotNull { it.snapshot.packetLossPercent }.averageOrNull(),
)

private fun List<Double>.averageOrNull(): Double? = takeIf { it.isNotEmpty() }?.average()
