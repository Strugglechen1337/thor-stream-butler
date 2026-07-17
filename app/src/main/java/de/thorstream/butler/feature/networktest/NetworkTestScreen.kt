package de.thorstream.butler.feature.networktest

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.thorstream.butler.R
import de.thorstream.butler.core.designsystem.ThorCyan
import de.thorstream.butler.core.designsystem.ThorGray
import de.thorstream.butler.core.designsystem.ThorGreen
import de.thorstream.butler.core.designsystem.ThorRed
import de.thorstream.butler.core.designsystem.ThorYellow
import de.thorstream.butler.core.designsystem.label
import de.thorstream.butler.domain.model.NetworkQuality
import de.thorstream.butler.domain.model.NetworkSnapshot
import java.util.Locale

@Composable
fun NetworkTestRoute(viewModel: NetworkTestViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val wifiPermissions = if (Build.VERSION.SDK_INT >= 33) {
        arrayOf(Manifest.permission.NEARBY_WIFI_DEVICES)
    } else {
        arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { viewModel.startTest() }
    val startWithPermission = {
        if (wifiPermissions.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }) viewModel.startTest()
        else permissionLauncher.launch(wifiPermissions)
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.nettest_kicker), color = ThorCyan, style = MaterialTheme.typography.labelLarge)
        Text(stringResource(R.string.nettest_title), style = MaterialTheme.typography.headlineLarge)
        Text(stringResource(R.string.nettest_permission_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)

        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(state.stepRes), style = MaterialTheme.typography.titleLarge)
                LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = startWithPermission, enabled = !state.running) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                        Text(" " + stringResource(if (state.progress > 0f) R.string.action_retry_test else R.string.nettest_start_full))
                    }
                    if (state.running) {
                        OutlinedButton(onClick = viewModel::cancelTest) {
                            Icon(Icons.Rounded.Cancel, contentDescription = null)
                            Text(" " + stringResource(R.string.action_cancel))
                        }
                    }
                }
                state.errorMessage?.let { Text(it, color = ThorRed, fontWeight = FontWeight.Bold) }
            }
        }

        state.assessment?.let { assessment ->
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(assessment.quality.label(), color = qualityColor(assessment.quality), style = MaterialTheme.typography.headlineSmall)
                    Text(assessment.summary)
                    assessment.problems.forEach { Text("• $it", color = ThorYellow) }
                    assessment.recommendations.forEach { Text("→ $it", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }

        state.snapshot?.let { MetricsGrid(it) }
    }
}

@Composable
private fun MetricsGrid(snapshot: NetworkSnapshot) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        MetricCard(stringResource(R.string.nettest_metric_connection), snapshot.connectionType.label())
        MetricCard(stringResource(R.string.nettest_metric_local_ip), snapshot.localIpAddress ?: stringResource(R.string.value_not_available))
        MetricCard(stringResource(R.string.nettest_metric_gateway), snapshot.gateway ?: stringResource(R.string.value_not_available))
        MetricCard(stringResource(R.string.nettest_metric_ssid), snapshot.ssid ?: stringResource(R.string.value_not_available))
        MetricCard(stringResource(R.string.nettest_metric_wifi_band), snapshot.wifiFrequencyMhz?.let { frequencyLabel(it) } ?: stringResource(R.string.value_not_available))
        MetricCard(stringResource(R.string.nettest_metric_link_speed), snapshot.linkSpeedMbps?.let { stringResource(R.string.nettest_link_speed_value, it) } ?: stringResource(R.string.value_not_available))
        MetricCard(stringResource(R.string.nettest_metric_signal), snapshot.signalStrengthPercent?.let { "$it %" } ?: stringResource(R.string.value_not_available))
        MetricCard(stringResource(R.string.nettest_metric_internet), availabilityLabel(snapshot.internetValidated))
        MetricCard(stringResource(R.string.nettest_metric_dns), availabilityLabel(snapshot.dnsReachable))
        MetricCard(stringResource(R.string.nettest_metric_latency), metricValue(snapshot.latencyMs, "ms"))
        MetricCard(stringResource(R.string.nettest_metric_jitter), metricValue(snapshot.jitterMs, "ms"))
        MetricCard(stringResource(R.string.nettest_metric_packet_loss), metricValue(snapshot.packetLossPercent, "%"))
        MetricCard(stringResource(R.string.nettest_metric_download), metricValue(snapshot.downloadMbps, "Mbit/s"))
        snapshot.host?.let { MetricCard(stringResource(R.string.nettest_metric_host, it), availabilityLabel(snapshot.hostReachable)) }
    }
}

@Composable
private fun MetricCard(label: String, value: String) {
    ElevatedCard(modifier = Modifier.widthIn(min = 150.dp, max = 260.dp)) {
        Column(Modifier.padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun availabilityLabel(value: Boolean?): String = stringResource(
    when (value) {
        true -> R.string.value_reachable
        false -> R.string.value_not_reachable
        null -> R.string.value_not_measurable
    },
)

@Composable
private fun metricValue(value: Double?, unit: String): String =
    value?.let { String.format(Locale.getDefault(), "%.1f %s", it, unit) } ?: stringResource(R.string.value_not_measured)

@Composable
private fun frequencyLabel(frequency: Int): String = when (frequency) {
    in 2400..2500 -> stringResource(R.string.nettest_freq_24)
    in 4900..5900 -> stringResource(R.string.nettest_freq_5)
    in 5925..7125 -> stringResource(R.string.nettest_freq_6)
    else -> stringResource(R.string.nettest_freq_mhz, frequency)
}

private fun qualityColor(quality: NetworkQuality): Color = when (quality) {
    NetworkQuality.OPTIMAL -> ThorGreen
    NetworkQuality.USABLE -> ThorYellow
    NetworkQuality.PROBLEMATIC -> ThorRed
    NetworkQuality.NOT_MEASURABLE -> ThorGray
}
