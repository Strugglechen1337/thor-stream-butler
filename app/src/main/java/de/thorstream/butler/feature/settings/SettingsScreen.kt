package de.thorstream.butler.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.thorstream.butler.core.designsystem.ThorCyan
import de.thorstream.butler.core.validation.NetworkValidators
import de.thorstream.butler.domain.model.AppSettings
import de.thorstream.butler.domain.model.ThemePreference
import kotlin.math.roundToInt

@Composable
fun SettingsRoute(viewModel: SettingsViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var confirmClear by remember { mutableStateOf(false) }
    var targetText by remember(settings.defaultTestTarget) { mutableStateOf(settings.defaultTestTarget) }
    var targetError by remember { mutableStateOf(false) }
    var themeMenu by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("THOR // KONFIGURATION", color = ThorCyan, style = MaterialTheme.typography.labelLarge)
        Text("Einstellungen", style = MaterialTheme.typography.headlineLarge)

        SettingsGroup("Startablauf") {
            SettingSwitch("Netzwerkcheck vor App-Start", "Kurzen Test vor jeder Streaming-App ausführen", settings.preLaunchCheckEnabled) {
                viewModel.update { current -> current.copy(preLaunchCheckEnabled = it) }
            }
            SettingSwitch("Automatischer Start bei Grün", "Nach kurzer Ergebnisanzeige direkt starten", settings.autoLaunchOnGreen, settings.preLaunchCheckEnabled) {
                viewModel.update { current -> current.copy(autoLaunchOnGreen = it) }
            }
            SettingSwitch("Hinweis bei Gelb", "Auf mögliche Qualitätseinbußen hinweisen", settings.warnOnYellow, settings.preLaunchCheckEnabled) {
                viewModel.update { current -> current.copy(warnOnYellow = it) }
            }
            SettingSwitch("Rückfrage bei Rot", "Starten, erneut testen oder abbrechen lassen", settings.confirmOnRed, settings.preLaunchCheckEnabled) {
                viewModel.update { current -> current.copy(confirmOnRed = it) }
            }
        }

        SettingsGroup("Diagnose") {
            OutlinedTextField(
                value = targetText,
                onValueChange = {
                    targetText = it.trim()
                    targetError = !NetworkValidators.isValidHostnameOrIpv4(targetText)
                    if (!targetError) viewModel.update { current -> current.copy(defaultTestTarget = targetText) }
                },
                label = { Text("Standard-Testziel") },
                supportingText = { Text(if (targetError) "Bitte gültigen Host oder IPv4-Adresse eingeben" else "Nur für Ping-/Erreichbarkeitstests") },
                isError = targetError,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            SliderSetting("Ping-Messungen", settings.pingCount, 1..10) { value -> viewModel.update { it.copy(pingCount = value) } }
            SliderSetting("Testdauer", settings.testDurationSeconds, 1..15, "Sekunden") { value -> viewModel.update { it.copy(testDurationSeconds = value) } }
            SettingSwitch("Downloadtest", "Optionaler HTTPS-Test; überträgt Testdaten und ist standardmäßig aus", settings.downloadTestEnabled) {
                viewModel.update { current -> current.copy(downloadTestEnabled = it) }
            }
            SettingSwitch("Diagnoseprotokoll", "Nur technische Zustände, keine IPs, SSIDs oder MAC-Adressen", settings.diagnosticLoggingEnabled) {
                viewModel.update { current -> current.copy(diagnosticLoggingEnabled = it) }
            }
        }

        SettingsGroup("Darstellung und Bedienung") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Theme", fontWeight = FontWeight.Bold)
                    Text(if (settings.theme == ThemePreference.DARK) "Thor Dark" else "System", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column {
                    FilledTonalButton(onClick = { themeMenu = true }) { Text(if (settings.theme == ThemePreference.DARK) "Dunkel" else "System") }
                    DropdownMenu(themeMenu, { themeMenu = false }) {
                        DropdownMenuItem({ Text("Thor Dark") }, { viewModel.update { it.copy(theme = ThemePreference.DARK) }; themeMenu = false })
                        DropdownMenuItem({ Text("System") }, { viewModel.update { it.copy(theme = ThemePreference.SYSTEM) }; themeMenu = false })
                    }
                }
            }
            SettingSwitch("Controller-Fokusanimationen", "Kacheln beim Fokussieren leicht vergrößern", settings.focusAnimationsEnabled) {
                viewModel.update { current -> current.copy(focusAnimationsEnabled = it) }
            }
        }

        SettingsGroup("Lokale Daten") {
            FilledTonalButton(onClick = { confirmClear = true }) {
                Icon(Icons.Rounded.DeleteSweep, contentDescription = null)
                Text(" Messhistorie löschen")
            }
            Text("Alle Einstellungen, Hosts und Messdaten bleiben ausschließlich auf diesem Gerät.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Messhistorie löschen?") },
            text = { Text("Dieser Vorgang entfernt alle gespeicherten Netzwerkprüfungen dauerhaft.") },
            confirmButton = { TextButton(onClick = { viewModel.clearHistory(); confirmClear = false }) { Text("Löschen") } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Abbrechen") } },
        )
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = ThorCyan)
            HorizontalDivider()
            content()
        }
    }
}

@Composable
private fun SettingSwitch(title: String, description: String, checked: Boolean, enabled: Boolean = true, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        }
        Switch(checked = checked, onCheckedChange = onChecked, enabled = enabled)
    }
}

@Composable
private fun SliderSetting(title: String, value: Int, range: IntRange, unit: String = "Messungen", onValue: (Int) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, fontWeight = FontWeight.Bold)
            Text("$value $unit", color = ThorCyan)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValue(it.roundToInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = (range.last - range.first - 1).coerceAtLeast(0),
        )
    }
}

