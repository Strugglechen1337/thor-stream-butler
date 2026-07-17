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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.thorstream.butler.core.designsystem.ThorCyan
import de.thorstream.butler.core.designsystem.ThorGray
import de.thorstream.butler.core.designsystem.ThorGreen
import de.thorstream.butler.core.designsystem.ThorRed
import de.thorstream.butler.core.designsystem.ThorYellow
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
        Text("THOR // DIAGNOSE", color = ThorCyan, style = MaterialTheme.typography.labelLarge)
        Text("Netzwerktest", style = MaterialTheme.typography.headlineLarge)
        Text("SSID und WLAN-Details können je nach Android-Version und Berechtigung nicht verfügbar sein.", color = MaterialTheme.colorScheme.onSurfaceVariant)

        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(state.step, style = MaterialTheme.typography.titleLarge)
                LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = startWithPermission, enabled = !state.running) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                        Text(if (state.progress > 0f) " Erneut testen" else " Vollständigen Test starten")
                    }
                    if (state.running) {
                        OutlinedButton(onClick = viewModel::cancelTest) {
                            Icon(Icons.Rounded.Cancel, contentDescription = null)
                            Text(" Abbrechen")
                        }
                    }
                }
                state.errorMessage?.let { Text(it, color = ThorRed, fontWeight = FontWeight.Bold) }
            }
        }

        state.assessment?.let { assessment ->
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(assessment.quality.displayName, color = qualityColor(assessment.quality), style = MaterialTheme.typography.headlineSmall)
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
        MetricCard("Verbindung", snapshot.connectionType.displayName)
        MetricCard("Lokale IP", snapshot.localIpAddress ?: "Nicht verfügbar")
        MetricCard("Gateway", snapshot.gateway ?: "Nicht verfügbar")
        MetricCard("SSID", snapshot.ssid ?: "Nicht verfügbar")
        MetricCard("WLAN-Band", snapshot.wifiFrequencyMhz?.let(::frequencyLabel) ?: "Nicht verfügbar")
        MetricCard("Link-Speed", snapshot.linkSpeedMbps?.let { "$it Mbit/s" } ?: "Nicht verfügbar")
        MetricCard("Signal", snapshot.signalStrengthPercent?.let { "$it %" } ?: "Nicht verfügbar")
        MetricCard("Internet", snapshot.internetValidated.asAvailability())
        MetricCard("DNS", snapshot.dnsReachable.asAvailability())
        MetricCard("Latenz", snapshot.latencyMs.metric("ms"))
        MetricCard("Jitter", snapshot.jitterMs.metric("ms"))
        MetricCard("Paketverlust", snapshot.packetLossPercent.metric("%"))
        MetricCard("Download", snapshot.downloadMbps.metric("Mbit/s"))
        snapshot.host?.let { MetricCard("Host $it", snapshot.hostReachable.asAvailability()) }
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

private fun Boolean?.asAvailability() = when (this) { true -> "Erreichbar"; false -> "Nicht erreichbar"; null -> "Nicht messbar" }
private fun Double?.metric(unit: String) = this?.let { String.format(Locale.GERMANY, "%.1f %s", it, unit) } ?: "Nicht gemessen"
private fun frequencyLabel(frequency: Int) = when (frequency) { in 2400..2500 -> "2,4 GHz"; in 4900..5900 -> "5 GHz"; in 5925..7125 -> "6 GHz"; else -> "$frequency MHz" }
private fun qualityColor(quality: NetworkQuality): Color = when (quality) {
    NetworkQuality.OPTIMAL -> ThorGreen
    NetworkQuality.USABLE -> ThorYellow
    NetworkQuality.PROBLEMATIC -> ThorRed
    NetworkQuality.NOT_MEASURABLE -> ThorGray
}
