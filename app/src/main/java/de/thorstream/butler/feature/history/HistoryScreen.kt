package de.thorstream.butler.feature.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingFlat
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.thorstream.butler.R
import de.thorstream.butler.core.designsystem.ThorCyan
import de.thorstream.butler.core.designsystem.label
import de.thorstream.butler.core.designsystem.ThorGray
import de.thorstream.butler.core.designsystem.ThorGreen
import de.thorstream.butler.core.designsystem.ThorRed
import de.thorstream.butler.core.designsystem.ThorYellow
import de.thorstream.butler.domain.model.NetworkQuality
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryRoute(viewModel: HistoryViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var confirmClear by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SnackbarHost(snackbar)
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val title: @Composable () -> Unit = {
                Column {
                    Text(stringResource(R.string.history_kicker), color = ThorCyan, style = MaterialTheme.typography.labelLarge)
                    Text(stringResource(R.string.history_title), style = MaterialTheme.typography.headlineLarge)
                }
            }
            val action: @Composable () -> Unit = {
                OutlinedButton(onClick = { confirmClear = true }, enabled = state.allMeasurements.isNotEmpty()) {
                    Icon(Icons.Rounded.DeleteSweep, contentDescription = null)
                    Text(" " + stringResource(R.string.history_clear))
                }
            }
            if (maxWidth < 520.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    title()
                    action()
                }
            } else {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) { title() }
                    action()
                }
            }
        }
        if (state.allMeasurements.isNotEmpty()) {
            HistoryFilters(selected = state.filter, onSelect = viewModel::selectFilter)
            HistorySummaryCard(state)
        }
        if (state.allMeasurements.isEmpty()) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.history_empty_title), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.history_empty_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else if (state.items.isEmpty()) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.history_filter_empty), style = MaterialTheme.typography.titleLarge)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(state.items, key = { it.measurement.id }) { item ->
                    HistoryCard(item)
                }
            }
        }
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.history_clear_confirm_title)) },
            text = { Text(stringResource(R.string.history_clear_confirm_text)) },
            confirmButton = {
                TextButton(onClick = { viewModel.clear(); confirmClear = false }) {
                    Text(stringResource(R.string.history_clear_confirm_action))
                }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun HistoryFilters(selected: HistoryFilter, onSelect: (HistoryFilter) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HistoryFilter.entries.forEach { filter ->
            FilterChip(
                selected = selected == filter,
                onClick = { onSelect(filter) },
                label = { Text(stringResource(filter.labelRes())) },
            )
        }
    }
}

@Composable
private fun HistorySummaryCard(state: HistoryUiState) {
    val summary = state.summary
    val latencies = state.items.asReversed().mapNotNull { it.measurement.snapshot.latencyMs }.takeLast(20)
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.history_summary_title, summary.measurementCount), style = MaterialTheme.typography.titleLarge, color = ThorCyan)
            Text(
                stringResource(
                    R.string.history_summary_metrics,
                    summary.averageLatencyMs.value("ms"),
                    summary.averageJitterMs.value("ms"),
                    summary.averagePacketLossPercent.value("%"),
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (latencies.size > 1) {
                Text(stringResource(R.string.history_latency_trend), style = MaterialTheme.typography.labelLarge)
                LatencySparkline(latencies)
            }
        }
    }
}

@Composable
private fun LatencySparkline(values: List<Double>) {
    Canvas(Modifier.fillMaxWidth().height(64.dp)) {
        val minimum = values.minOrNull() ?: return@Canvas
        val maximum = values.maxOrNull() ?: return@Canvas
        val range = (maximum - minimum).coerceAtLeast(1.0)
        val step = size.width / (values.size - 1).coerceAtLeast(1)
        val path = Path()
        values.forEachIndexed { index, value ->
            val x = index * step
            val y = size.height - ((value - minimum) / range * size.height).toFloat()
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path = path, color = ThorCyan, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f))
    }
}

@Composable
private fun HistoryCard(itemState: HistoryItem) {
    val item = itemState.measurement
    val trend = itemState.trend
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = when (trend.direction) { -1 -> Icons.AutoMirrored.Rounded.TrendingDown; 1 -> Icons.AutoMirrored.Rounded.TrendingUp; else -> Icons.AutoMirrored.Rounded.TrendingFlat },
                contentDescription = null,
                tint = when (trend.direction) { -1 -> ThorGreen; 1 -> ThorRed; else -> ThorGray },
            )
            Column(Modifier.weight(1f)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(item.assessment.quality.label(), color = qualityColor(item.assessment.quality), style = MaterialTheme.typography.titleLarge)
                    Text(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(item.timestamp)), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("${item.snapshot.connectionType.label()}${item.snapshot.ssid?.let { " · $it" }.orEmpty()}${item.snapshot.host?.let { " · ${stringResource(R.string.history_host_label)}" }.orEmpty()}")
                Text(
                    stringResource(
                        R.string.history_metrics,
                        item.snapshot.latencyMs.value("ms"),
                        item.snapshot.jitterMs.value("ms"),
                        item.snapshot.packetLossPercent.value("%"),
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    when {
                        trend.isFirstComparable -> stringResource(R.string.history_trend_first)
                        trend.direction < 0 -> stringResource(R.string.history_trend_improved, kotlin.math.abs(trend.differenceMs ?: 0.0).format())
                        trend.direction > 0 -> stringResource(R.string.history_trend_worse, kotlin.math.abs(trend.differenceMs ?: 0.0).format())
                        trend.differenceMs != null -> stringResource(R.string.history_trend_flat)
                        else -> stringResource(R.string.history_trend_unavailable)
                    },
                    color = if (trend.direction < 0) ThorGreen else if (trend.direction > 0) ThorRed else ThorGray,
                )
                item.assessment.problems.firstOrNull()?.let { Text(it, color = ThorYellow) }
            }
        }
    }
}

@StringRes
private fun HistoryFilter.labelRes(): Int = when (this) {
    HistoryFilter.ALL -> R.string.history_filter_all
    HistoryFilter.WIFI -> R.string.conn_wifi
    HistoryFilter.ETHERNET -> R.string.conn_ethernet
    HistoryFilter.CELLULAR -> R.string.conn_cellular
    HistoryFilter.VPN -> R.string.conn_vpn
    HistoryFilter.WITH_HOST -> R.string.history_filter_host
}

private fun Double?.value(unit: String) = this?.let { "${it.format()} $unit" } ?: "–"
private fun Double.format() = String.format(Locale.getDefault(), "%.1f", this)
private fun qualityColor(quality: NetworkQuality): Color = when (quality) {
    NetworkQuality.OPTIMAL -> ThorGreen
    NetworkQuality.USABLE -> ThorYellow
    NetworkQuality.PROBLEMATIC -> ThorRed
    NetworkQuality.NOT_MEASURABLE -> ThorGray
}
