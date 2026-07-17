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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.thorstream.butler.R
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
        Text(stringResource(R.string.settings_kicker), color = ThorCyan, style = MaterialTheme.typography.labelLarge)
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineLarge)

        SettingsGroup(stringResource(R.string.settings_group_launch)) {
            SettingSwitch(stringResource(R.string.settings_prelaunch_title), stringResource(R.string.settings_prelaunch_hint), settings.preLaunchCheckEnabled) {
                viewModel.update { current -> current.copy(preLaunchCheckEnabled = it) }
            }
            SettingSwitch(stringResource(R.string.settings_auto_green_title), stringResource(R.string.settings_auto_green_hint), settings.autoLaunchOnGreen, settings.preLaunchCheckEnabled) {
                viewModel.update { current -> current.copy(autoLaunchOnGreen = it) }
            }
            SettingSwitch(stringResource(R.string.settings_warn_yellow_title), stringResource(R.string.settings_warn_yellow_hint), settings.warnOnYellow, settings.preLaunchCheckEnabled) {
                viewModel.update { current -> current.copy(warnOnYellow = it) }
            }
            SettingSwitch(stringResource(R.string.settings_confirm_red_title), stringResource(R.string.settings_confirm_red_hint), settings.confirmOnRed, settings.preLaunchCheckEnabled) {
                viewModel.update { current -> current.copy(confirmOnRed = it) }
            }
        }

        SettingsGroup(stringResource(R.string.settings_group_diagnostics)) {
            OutlinedTextField(
                value = targetText,
                onValueChange = {
                    targetText = it.trim()
                    targetError = !NetworkValidators.isValidHostnameOrIpv4(targetText)
                    if (!targetError) viewModel.update { current -> current.copy(defaultTestTarget = targetText) }
                },
                label = { Text(stringResource(R.string.settings_test_target)) },
                supportingText = { Text(stringResource(if (targetError) R.string.settings_test_target_error else R.string.settings_test_target_hint)) },
                isError = targetError,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            SliderSetting(stringResource(R.string.settings_ping_count), settings.pingCount, 1..10, stringResource(R.string.settings_unit_measurements)) { value -> viewModel.update { it.copy(pingCount = value) } }
            SliderSetting(stringResource(R.string.settings_test_duration), settings.testDurationSeconds, 1..15, stringResource(R.string.settings_unit_seconds)) { value -> viewModel.update { it.copy(testDurationSeconds = value) } }
            SettingSwitch(stringResource(R.string.settings_download_title), stringResource(R.string.settings_download_hint), settings.downloadTestEnabled) {
                viewModel.update { current -> current.copy(downloadTestEnabled = it) }
            }
            SettingSwitch(stringResource(R.string.settings_logging_title), stringResource(R.string.settings_logging_hint), settings.diagnosticLoggingEnabled) {
                viewModel.update { current -> current.copy(diagnosticLoggingEnabled = it) }
            }
        }

        SettingsGroup(stringResource(R.string.settings_group_appearance)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_theme), fontWeight = FontWeight.Bold)
                    Text(stringResource(if (settings.theme == ThemePreference.DARK) R.string.settings_theme_dark else R.string.settings_theme_system), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column {
                    FilledTonalButton(onClick = { themeMenu = true }) { Text(stringResource(if (settings.theme == ThemePreference.DARK) R.string.settings_theme_dark_short else R.string.settings_theme_system)) }
                    DropdownMenu(themeMenu, { themeMenu = false }) {
                        DropdownMenuItem({ Text(stringResource(R.string.settings_theme_dark)) }, { viewModel.update { it.copy(theme = ThemePreference.DARK) }; themeMenu = false })
                        DropdownMenuItem({ Text(stringResource(R.string.settings_theme_system)) }, { viewModel.update { it.copy(theme = ThemePreference.SYSTEM) }; themeMenu = false })
                    }
                }
            }
            SettingSwitch(stringResource(R.string.settings_focus_title), stringResource(R.string.settings_focus_hint), settings.focusAnimationsEnabled) {
                viewModel.update { current -> current.copy(focusAnimationsEnabled = it) }
            }
        }

        SettingsGroup(stringResource(R.string.settings_group_data)) {
            FilledTonalButton(onClick = { confirmClear = true }) {
                Icon(Icons.Rounded.DeleteSweep, contentDescription = null)
                Text(" " + stringResource(R.string.settings_clear_history))
            }
            Text(stringResource(R.string.settings_local_only), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.settings_clear_confirm_title)) },
            text = { Text(stringResource(R.string.settings_clear_confirm_text)) },
            confirmButton = { TextButton(onClick = { viewModel.clearHistory(); confirmClear = false }) { Text(stringResource(R.string.settings_clear_confirm_action)) } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.action_cancel)) } },
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
private fun SliderSetting(title: String, value: Int, range: IntRange, unit: String, onValue: (Int) -> Unit) {
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

