package de.thorstream.butler.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.thorstream.butler.R
import de.thorstream.butler.BuildConfig
import de.thorstream.butler.core.designsystem.ThorCyan
import de.thorstream.butler.core.validation.NetworkValidators
import de.thorstream.butler.domain.model.AppSettings
import de.thorstream.butler.domain.model.ThemePreference
import kotlin.math.roundToInt
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsRoute(viewModel: SettingsViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val transferState by viewModel.transferState.collectAsStateWithLifecycle()
    val diagnosticLogCount by viewModel.diagnosticLogCount.collectAsStateWithLifecycle()
    var confirmClear by remember { mutableStateOf(false) }
    var confirmImport by remember { mutableStateOf(false) }
    var includeHistory by remember { mutableStateOf(false) }
    var targetText by remember(settings.defaultTestTarget) { mutableStateOf(settings.defaultTestTarget) }
    var targetError by remember { mutableStateOf(false) }
    var themeMenu by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val uriHandler = LocalUriHandler.current
    val openExternalLink: (String) -> Unit = { url ->
        try {
            uriHandler.openUri(url)
        } catch (_: Exception) {
            viewModel.reportLinkFailure()
        }
    }
    val exportDocument = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { viewModel.exportConfiguration(it.toString(), includeHistory) }
    }
    val importDocument = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.importConfiguration(it.toString()) }
    }

    LaunchedEffect(transferState.message) {
        transferState.message?.let { snackbar.showSnackbar(it); viewModel.consumeTransferMessage() }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SnackbarHost(snackbar)
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
                    targetText = it.trim().take(253)
                    targetError = !NetworkValidators.isValidHostnameOrIp(targetText)
                    if (!targetError) viewModel.update { current ->
                        current.copy(defaultTestTarget = NetworkValidators.normalizeHost(targetText))
                    }
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
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.settings_logging_count, diagnosticLogCount),
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FilledTonalButton(onClick = viewModel::clearDiagnosticLog, enabled = diagnosticLogCount > 0) {
                    Icon(Icons.Rounded.DeleteSweep, contentDescription = null)
                    Text(" " + stringResource(R.string.settings_logging_clear))
                }
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
            Text(stringResource(R.string.settings_transfer_title), fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.settings_transfer_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
            SettingSwitch(
                stringResource(R.string.settings_transfer_history_title),
                stringResource(R.string.settings_transfer_history_hint),
                includeHistory,
                enabled = !transferState.inProgress,
            ) { includeHistory = it }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(
                    onClick = {
                        val date = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date())
                        exportDocument.launch("thor-stream-butler-$date.json")
                    },
                    enabled = !transferState.inProgress,
                ) {
                    Icon(Icons.Rounded.Upload, contentDescription = null)
                    Text(" " + stringResource(R.string.settings_transfer_export))
                }
                FilledTonalButton(onClick = { confirmImport = true }, enabled = !transferState.inProgress) {
                    Icon(Icons.Rounded.Download, contentDescription = null)
                    Text(" " + stringResource(R.string.settings_transfer_import))
                }
                if (transferState.inProgress) CircularProgressIndicator()
            }
            FilledTonalButton(onClick = { confirmClear = true }) {
                Icon(Icons.Rounded.DeleteSweep, contentDescription = null)
                Text(" " + stringResource(R.string.settings_clear_history))
            }
            Text(stringResource(R.string.settings_local_only), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        SettingsGroup(stringResource(R.string.settings_group_about)) {
            Text(stringResource(R.string.settings_about_version, BuildConfig.VERSION_NAME), fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.settings_about_privacy_summary), color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(onClick = { showPrivacy = true }) {
                    Icon(Icons.Rounded.Info, contentDescription = null)
                    Text(" " + stringResource(R.string.settings_about_privacy_action))
                }
                FilledTonalButton(onClick = { openExternalLink(SOURCE_URL) }) {
                    Icon(Icons.Rounded.Code, contentDescription = null)
                    Text(" " + stringResource(R.string.settings_about_source_action))
                }
                FilledTonalButton(onClick = { openExternalLink("$SOURCE_URL/blob/main/LICENSE") }) {
                    Text(stringResource(R.string.settings_about_license_action))
                }
            }
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
    if (confirmImport) {
        AlertDialog(
            onDismissRequest = { confirmImport = false },
            title = { Text(stringResource(R.string.settings_transfer_import_confirm_title)) },
            text = { Text(stringResource(R.string.settings_transfer_import_confirm_text)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmImport = false
                    importDocument.launch(arrayOf("application/json", "text/json", "text/plain"))
                }) { Text(stringResource(R.string.settings_transfer_import)) }
            },
            dismissButton = { TextButton(onClick = { confirmImport = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
    if (showPrivacy) {
        AlertDialog(
            onDismissRequest = { showPrivacy = false },
            title = { Text(stringResource(R.string.settings_about_privacy_title)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.settings_about_privacy_body))
                    TextButton(onClick = {
                        val path = if (Locale.getDefault().language == "de") "de/datenschutz/" else "privacy/"
                        openExternalLink("$PAGES_URL$path")
                    }) { Text(stringResource(R.string.settings_about_privacy_web)) }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacy = false }) { Text(stringResource(R.string.action_close)) }
            },
        )
    }
}

private const val SOURCE_URL = "https://github.com/Strugglechen1337/thor-stream-butler"
private const val PAGES_URL = "https://strugglechen1337.github.io/thor-stream-butler/"

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

