package de.thorstream.butler.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingFlat
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import de.thorstream.butler.domain.model.NetworkMeasurement
import de.thorstream.butler.domain.model.NetworkQuality
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
fun HistoryRoute(viewModel: HistoryViewModel = hiltViewModel()) {
    val history by viewModel.history.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.history_kicker), color = ThorCyan, style = MaterialTheme.typography.labelLarge)
                Text(stringResource(R.string.history_title), style = MaterialTheme.typography.headlineLarge)
            }
            OutlinedButton(onClick = viewModel::clear, enabled = history.isNotEmpty()) {
                Icon(Icons.Rounded.DeleteSweep, contentDescription = null)
                Text(" " + stringResource(R.string.history_clear))
            }
        }
        if (history.isEmpty()) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.history_empty_title), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.history_empty_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                itemsIndexed(history, key = { _, item -> item.id }) { index, item ->
                    HistoryCard(item, history.getOrNull(index + 1))
                }
            }
        }
    }
}

@Composable
private fun HistoryCard(item: NetworkMeasurement, older: NetworkMeasurement?) {
    val trend = latencyTrend(item, older)
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
                Text("${item.snapshot.connectionType.label()}${item.snapshot.ssid?.let { " · $it" }.orEmpty()}")
                Text(
                    stringResource(
                        R.string.history_metrics,
                        item.snapshot.latencyMs.value("ms"),
                        item.snapshot.jitterMs.value("ms"),
                        item.snapshot.packetLossPercent.value("%"),
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                trend.labelRes?.let { labelRes ->
                    Text(
                        trend.arg?.let { stringResource(labelRes, it) } ?: stringResource(labelRes),
                        color = if (trend.direction < 0) ThorGreen else if (trend.direction > 0) ThorRed else ThorGray,
                    )
                }
                item.assessment.problems.firstOrNull()?.let { Text(it, color = ThorYellow) }
            }
        }
    }
}

private data class Trend(val direction: Int, @param:StringRes val labelRes: Int?, val arg: String? = null)

private fun latencyTrend(newer: NetworkMeasurement, older: NetworkMeasurement?): Trend {
    val current = newer.snapshot.latencyMs ?: return Trend(0, null)
    val previous = older?.snapshot?.latencyMs ?: return Trend(0, R.string.history_trend_first)
    val difference = current - previous
    if (abs(difference) < 2.0) return Trend(0, R.string.history_trend_flat)
    return if (difference < 0) Trend(-1, R.string.history_trend_improved, abs(difference).format())
    else Trend(1, R.string.history_trend_worse, difference.format())
}

private fun Double?.value(unit: String) = this?.let { "${it.format()} $unit" } ?: "–"
private fun Double.format() = String.format(Locale.getDefault(), "%.1f", this)
private fun qualityColor(quality: NetworkQuality): Color = when (quality) {
    NetworkQuality.OPTIMAL -> ThorGreen
    NetworkQuality.USABLE -> ThorYellow
    NetworkQuality.PROBLEMATIC -> ThorRed
    NetworkQuality.NOT_MEASURABLE -> ThorGray
}
