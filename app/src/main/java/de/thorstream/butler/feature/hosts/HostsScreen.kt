package de.thorstream.butler.feature.hosts

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.WifiFind
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.thorstream.butler.R
import de.thorstream.butler.core.designsystem.ThorCyan
import de.thorstream.butler.core.designsystem.label
import de.thorstream.butler.core.designsystem.ThorGray
import de.thorstream.butler.core.designsystem.ThorGreen
import de.thorstream.butler.core.designsystem.ThorRed
import de.thorstream.butler.domain.model.LocalHost
import de.thorstream.butler.domain.model.StreamingType
import java.text.DateFormat
import java.util.Date

@Composable
fun HostsRoute(viewModel: HostsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var editedHost by remember { mutableStateOf<LocalHost?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val localNetworkPermission = "android.permission.ACCESS_LOCAL_NETWORK"
    var pendingLocalAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val localPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) pendingLocalAction?.invoke() else viewModel.reportLocalNetworkPermissionDenied()
        pendingLocalAction = null
    }
    val withLocalNetworkPermission: (() -> Unit) -> Unit = { action ->
        if (Build.VERSION.SDK_INT < 37 || ContextCompat.checkSelfPermission(context, localNetworkPermission) == PackageManager.PERMISSION_GRANTED) {
            action()
        } else {
            pendingLocalAction = action
            localPermissionLauncher.launch(localNetworkPermission)
        }
    }

    LaunchedEffect(state.message) {
        state.message?.let { snackbar.showSnackbar(it); viewModel.consumeMessage() }
    }

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SnackbarHost(snackbar)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.hosts_kicker), color = ThorCyan, style = MaterialTheme.typography.labelLarge)
                Text(stringResource(R.string.hosts_title), style = MaterialTheme.typography.headlineLarge)
            }
            Button(onClick = { editedHost = null; showEditor = true }) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Text(" " + stringResource(R.string.hosts_create))
            }
        }
        if (state.hosts.isEmpty()) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.Router, contentDescription = null, modifier = Modifier.size(64.dp), tint = ThorGray)
                Text(stringResource(R.string.hosts_empty_title), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.hosts_empty_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(state.hosts, key = { it.id }) { host ->
                    HostCard(
                        host = host,
                        testing = state.testingHostId == host.id,
                        onTest = { withLocalNetworkPermission { viewModel.test(host) } },
                        onWake = { withLocalNetworkPermission { viewModel.wake(host) } },
                        onEdit = { editedHost = host; showEditor = true },
                        onDelete = { viewModel.delete(host) },
                    )
                }
            }
        }
    }
    if (showEditor) {
        HostEditorDialog(
            host = editedHost,
            onDismiss = { showEditor = false },
            onSave = { host -> viewModel.save(host).also { if (it == null) showEditor = false } },
        )
    }
}

@Composable
private fun HostCard(host: LocalHost, testing: Boolean, onTest: () -> Unit, onWake: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Router, contentDescription = null, tint = statusColor(host.lastReachable), modifier = Modifier.size(38.dp))
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(host.name, style = MaterialTheme.typography.titleLarge)
                    Text("${host.address}${host.port?.let { ":$it" }.orEmpty()} · ${host.streamingType.label()}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        when (host.lastReachable) {
                            true -> stringResource(
                                R.string.hosts_last_reachable,
                                host.lastSuccessfulTestAt?.let { " · ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(it))}" }.orEmpty(),
                            )
                            false -> stringResource(R.string.hosts_last_unreachable)
                            null -> stringResource(R.string.hosts_never_tested)
                        },
                        color = statusColor(host.lastReachable),
                    )
                }
                IconButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.hosts_edit)) }
                IconButton(onClick = onDelete) { Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.hosts_delete)) }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = onTest, enabled = !testing) {
                    if (testing) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Icon(Icons.Rounded.WifiFind, contentDescription = null)
                    Text(" " + stringResource(R.string.hosts_test_connection))
                }
                if (host.wakeOnLanEnabled) {
                    OutlinedButton(onClick = onWake, enabled = host.macAddress != null) {
                        Icon(Icons.Rounded.Bolt, contentDescription = null)
                        Text(" " + stringResource(R.string.hosts_wake))
                    }
                }
            }
        }
    }
}

@Composable
private fun HostEditorDialog(host: LocalHost?, onDismiss: () -> Unit, onSave: (LocalHost) -> String?) {
    var name by remember(host) { mutableStateOf(host?.name.orEmpty()) }
    var address by remember(host) { mutableStateOf(host?.address.orEmpty()) }
    var mac by remember(host) { mutableStateOf(host?.macAddress.orEmpty()) }
    var port by remember(host) { mutableStateOf(host?.port?.toString().orEmpty()) }
    var broadcast by remember(host) { mutableStateOf(host?.broadcastAddress ?: "255.255.255.255") }
    var type by remember(host) { mutableStateOf(host?.streamingType ?: StreamingType.SUNSHINE_HOST) }
    var wolEnabled by remember(host) { mutableStateOf(host?.wakeOnLanEnabled ?: false) }
    var typeMenu by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (host == null) R.string.hosts_create else R.string.hosts_edit)) },
        text = {
            LazyColumn(Modifier.heightIn(max = 480.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.hosts_field_name)) }, singleLine = true) }
                item { OutlinedTextField(address, { address = it }, label = { Text(stringResource(R.string.hosts_field_address)) }, singleLine = true) }
                item { OutlinedTextField(port, { port = it.filter(Char::isDigit) }, label = { Text(stringResource(R.string.hosts_field_port)) }, singleLine = true) }
                item {
                    Column {
                        FilledTonalButton(onClick = { typeMenu = true }) { Text(type.label()) }
                        DropdownMenu(typeMenu, { typeMenu = false }) {
                            StreamingType.entries.forEach { option -> DropdownMenuItem({ Text(option.label()) }, { type = option; typeMenu = false }) }
                        }
                    }
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.hosts_wol_title), fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.hosts_wol_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(wolEnabled, { wolEnabled = it })
                    }
                }
                if (wolEnabled) {
                    item { OutlinedTextField(mac, { mac = it }, label = { Text(stringResource(R.string.hosts_field_mac)) }, singleLine = true) }
                    item { OutlinedTextField(broadcast, { broadcast = it }, label = { Text(stringResource(R.string.hosts_field_broadcast)) }, singleLine = true) }
                }
                error?.let { item { Text(it, color = ThorRed) } }
            }
        },
        confirmButton = {
            Button(onClick = {
                error = onSave(
                    LocalHost(
                        id = host?.id ?: 0,
                        name = name,
                        address = address,
                        macAddress = mac.takeIf { it.isNotBlank() },
                        port = port.toIntOrNull(),
                        streamingType = type,
                        wakeOnLanEnabled = wolEnabled,
                        broadcastAddress = broadcast,
                        lastReachable = host?.lastReachable,
                        lastSuccessfulTestAt = host?.lastSuccessfulTestAt,
                    ),
                )
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

private fun statusColor(reachable: Boolean?): Color = when (reachable) { true -> ThorGreen; false -> ThorRed; null -> ThorGray }
