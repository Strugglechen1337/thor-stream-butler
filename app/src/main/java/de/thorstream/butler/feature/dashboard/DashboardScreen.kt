package de.thorstream.butler.feature.dashboard

import android.graphics.drawable.Drawable
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Gamepad
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
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
import de.thorstream.butler.domain.model.InstalledApp
import de.thorstream.butler.domain.model.NetworkQuality
import de.thorstream.butler.domain.model.LocalHost
import de.thorstream.butler.domain.model.StreamingProfile
import de.thorstream.butler.domain.model.StreamingResolution
import de.thorstream.butler.domain.model.StreamingType
import java.text.DateFormat
import java.util.Date

@Composable
fun DashboardRoute(viewModel: DashboardViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showPicker by remember { mutableStateOf(false) }
    var editedItem by remember { mutableStateOf<DashboardItem?>(null) }
    var pendingDelete by remember { mutableStateOf<DashboardItem?>(null) }
    val firstTileFocusRequester = remember { FocusRequester() }
    var initialFocusRequested by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val localNetworkPermission = "android.permission.ACCESS_LOCAL_NETWORK"
    var pendingLocalAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val localPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) pendingLocalAction?.invoke() else viewModel.reportLocalNetworkPermissionDenied()
        pendingLocalAction = null
    }
    val withLocalNetworkPermission: (Boolean, () -> Unit) -> Unit = { required, action ->
        if (!required || Build.VERSION.SDK_INT < 37 || ContextCompat.checkSelfPermission(context, localNetworkPermission) == PackageManager.PERMISSION_GRANTED) {
            action()
        } else {
            pendingLocalAction = action
            localPermissionLauncher.launch(localNetworkPermission)
        }
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    LaunchedEffect(state.items.isNotEmpty()) {
        if (state.items.isNotEmpty() && !initialFocusRequested) {
            initialFocusRequested = true
            firstTileFocusRequester.requestFocus()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.dashboard_kicker), color = ThorCyan, style = MaterialTheme.typography.labelLarge)
                    Text(stringResource(R.string.dashboard_title), style = MaterialTheme.typography.headlineLarge)
                }
                Button(onClick = { viewModel.loadInstalledApps(); showPicker = true }) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.dashboard_add_app))
                }
            }
            if (state.items.isEmpty()) {
                EmptyDashboard(onAdd = { viewModel.loadInstalledApps(); showPicker = true })
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 230.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize().focusRestorer().focusGroup(),
                ) {
                    items(state.items, key = { it.entry.id }) { item ->
                        StreamingTile(
                            item = item,
                            focusAnimationsEnabled = state.focusAnimationsEnabled,
                            modifier = if (item.entry.id == state.items.first().entry.id) Modifier.focusRequester(firstTileFocusRequester) else Modifier,
                            onLaunch = { withLocalNetworkPermission(item.host != null) { viewModel.launch(item.entry) } },
                            onEdit = { editedItem = item },
                            onMoveBack = { viewModel.moveEntry(item.entry, -1) },
                            onMoveForward = { viewModel.moveEntry(item.entry, 1) },
                            onDelete = { pendingDelete = item },
                        )
                    }
                }
            }
        }
    }

    if (showPicker) {
        AppPickerDialog(
            apps = state.installedApps,
            loading = state.isLoadingApps,
            onDismiss = { showPicker = false },
            onAdd = { app, type, name -> viewModel.addApp(app, type, name); showPicker = false },
        )
    }
    editedItem?.let { item ->
        TileConfigurationDialog(
            item = item,
            hosts = state.hosts,
            onDismiss = { editedItem = null },
            onSave = { hostId, profile ->
                viewModel.saveConfiguration(item.entry, hostId, profile)
                editedItem = null
            },
        )
    }
    pendingDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.dashboard_remove_confirm_title)) },
            text = { Text(stringResource(R.string.dashboard_remove_confirm_text, item.entry.displayName)) },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(item.entry); pendingDelete = null }) {
                    Text(stringResource(R.string.dashboard_remove_confirm_action))
                }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
    state.preLaunch?.let { preLaunch ->
        PreLaunchDialog(
            state = preLaunch,
            onCancel = viewModel::cancelPreLaunch,
            onRetry = viewModel::retryPreLaunch,
            onLaunchAnyway = viewModel::launchAnyway,
            onWake = { withLocalNetworkPermission(true, viewModel::wakeLinkedHost) },
        )
    }
}

@Composable
private fun PreLaunchDialog(
    state: PreLaunchUiState,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onLaunchAnyway: () -> Unit,
    onWake: () -> Unit,
) {
    val assessment = state.assessment
    val requiresDecision = assessment?.quality == NetworkQuality.PROBLEMATIC || assessment?.quality == NetworkQuality.NOT_MEASURABLE
    val canManuallyLaunch = assessment != null && !state.autoLaunching
    AlertDialog(
        onDismissRequest = { if (!state.autoLaunching) onCancel() },
        title = { Text(if (assessment == null) stringResource(R.string.dashboard_check_title) else assessment.quality.label()) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(state.entry.displayName, color = ThorCyan, fontWeight = FontWeight.Bold)
                LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth())
                Text(stringResource(state.stepRes))
                state.errorMessage?.let { Text(it, color = ThorRed, fontWeight = FontWeight.Bold) }
                assessment?.let {
                    Text(it.summary, color = when (it.quality) {
                        NetworkQuality.OPTIMAL -> ThorGreen
                        NetworkQuality.USABLE -> ThorYellow
                        NetworkQuality.PROBLEMATIC -> ThorRed
                        NetworkQuality.NOT_MEASURABLE -> ThorGray
                    })
                    it.problems.take(3).forEach { problem -> Text("• $problem") }
                    it.recommendations.firstOrNull()?.let { recommendation -> Text("→ $recommendation", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                state.recommendation?.let { recommendation ->
                    Text(
                        stringResource(
                            R.string.dashboard_recommended_profile,
                            resolutionLabel(recommendation.resolution),
                            recommendation.framesPerSecond,
                            recommendation.bitrateMbps,
                        ),
                        color = ThorCyan,
                        fontWeight = FontWeight.Bold,
                    )
                }
                if (state.host?.wakeOnLanEnabled == true && state.host.macAddress != null && state.snapshot?.hostReachable == false) {
                    FilledTonalButton(onClick = onWake) {
                        Icon(Icons.Rounded.Bolt, contentDescription = null)
                        Text(" " + stringResource(R.string.dashboard_wake_host, state.host.name))
                    }
                }
            }
        },
        confirmButton = {
            if (canManuallyLaunch) {
                Button(onClick = onLaunchAnyway) { Text(stringResource(if (requiresDecision) R.string.dashboard_launch_anyway else R.string.dashboard_launch)) }
            }
        },
        dismissButton = {
            if (requiresDecision) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = onRetry) { Text(stringResource(R.string.action_retry_test)) }
                    TextButton(onClick = onCancel) { Text(stringResource(R.string.action_cancel)) }
                }
            } else if (!state.autoLaunching) {
                TextButton(onClick = onCancel) { Text(stringResource(R.string.action_cancel)) }
            }
        },
    )
}

@Composable
private fun EmptyDashboard(onAdd: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Rounded.Gamepad, contentDescription = null, modifier = Modifier.size(64.dp), tint = ThorGray)
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.dashboard_empty_title), style = MaterialTheme.typography.titleLarge)
        TextButton(onClick = onAdd) { Text(stringResource(R.string.dashboard_empty_action)) }
    }
}

@Composable
private fun StreamingTile(
    item: DashboardItem,
    focusAnimationsEnabled: Boolean,
    modifier: Modifier = Modifier,
    onLaunch: () -> Unit,
    onEdit: () -> Unit,
    onMoveBack: () -> Unit,
    onMoveForward: () -> Unit,
    onDelete: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused && focusAnimationsEnabled) 1.035f else 1f, label = "tile-scale")
    val borderColor by animateColorAsState(if (focused) ThorCyan else Color.Transparent, label = "tile-border")
    val shape = RoundedCornerShape(20.dp)
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.25f)
            .scale(scale)
            .border(3.dp, borderColor, shape)
            .clip(shape)
            .onFocusChanged { focused = it.hasFocus }
            .clickable(enabled = item.isInstalled, onClick = onLaunch)
            .focusable(),
        shape = shape,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.Top) {
                PackageIcon(item.entry.packageName, Modifier.size(52.dp))
                Spacer(Modifier.size(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.entry.displayName, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(item.entry.streamingType.label(), color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.dashboard_configure_tile)) }
                IconButton(onClick = onDelete) { Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.dashboard_remove_tile)) }
            }
            Column {
                item.host?.let { host ->
                    Text(stringResource(R.string.dashboard_linked_host, host.name), color = ThorCyan, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(
                    stringResource(
                        R.string.dashboard_profile_summary,
                        resolutionLabel(item.entry.profile.resolution),
                        item.entry.profile.framesPerSecond,
                        item.entry.profile.bitrateMbps,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    QualityDot(item.entry.lastNetworkQuality)
                    Text(
                        item.entry.lastUsedAt?.let { stringResource(R.string.dashboard_last_used, DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(it))) }
                            ?: stringResource(if (item.isInstalled) R.string.dashboard_ready else R.string.dashboard_not_installed),
                        color = if (item.isInstalled) MaterialTheme.colorScheme.onSurfaceVariant else ThorRed,
                        maxLines = 1,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onMoveBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.dashboard_move_back)) }
                    FilledTonalButton(onClick = onLaunch, enabled = item.isInstalled, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                        Text(" " + stringResource(if (item.isInstalled) R.string.dashboard_launch else R.string.dashboard_unavailable))
                    }
                    IconButton(onClick = onMoveForward) { Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = stringResource(R.string.dashboard_move_forward)) }
                }
            }
        }
    }
}

@Composable
private fun TileConfigurationDialog(
    item: DashboardItem,
    hosts: List<LocalHost>,
    onDismiss: () -> Unit,
    onSave: (Long?, StreamingProfile) -> Unit,
) {
    var hostId by remember(item.entry.id) { mutableStateOf(item.entry.hostId) }
    var resolution by remember(item.entry.id) { mutableStateOf(item.entry.profile.resolution) }
    var framesPerSecond by remember(item.entry.id) { mutableStateOf(item.entry.profile.framesPerSecond.toString()) }
    var bitrate by remember(item.entry.id) { mutableStateOf(item.entry.profile.bitrateMbps.toString()) }
    var hostMenu by remember { mutableStateOf(false) }
    var resolutionMenu by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dashboard_configure_title, item.entry.displayName)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.dashboard_host_assignment), fontWeight = FontWeight.Bold)
                Box {
                    FilledTonalButton(onClick = { hostMenu = true }) {
                        Text(hosts.firstOrNull { it.id == hostId }?.name ?: stringResource(R.string.dashboard_no_host))
                    }
                    DropdownMenu(expanded = hostMenu, onDismissRequest = { hostMenu = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.dashboard_no_host)) }, onClick = { hostId = null; hostMenu = false })
                        hosts.forEach { host ->
                            DropdownMenuItem(text = { Text(host.name) }, onClick = { hostId = host.id; hostMenu = false })
                        }
                    }
                }
                Text(stringResource(R.string.dashboard_streaming_profile), fontWeight = FontWeight.Bold)
                Box {
                    FilledTonalButton(onClick = { resolutionMenu = true }) { Text(resolutionLabel(resolution)) }
                    DropdownMenu(expanded = resolutionMenu, onDismissRequest = { resolutionMenu = false }) {
                        StreamingResolution.entries.forEach { option ->
                            DropdownMenuItem(text = { Text(resolutionLabel(option)) }, onClick = { resolution = option; resolutionMenu = false })
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = framesPerSecond,
                        onValueChange = { framesPerSecond = it.filter(Char::isDigit).take(3) },
                        label = { Text(stringResource(R.string.dashboard_profile_fps)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = bitrate,
                        onValueChange = { bitrate = it.filter(Char::isDigit).take(3) },
                        label = { Text(stringResource(R.string.dashboard_profile_bitrate)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        hostId,
                        StreamingProfile(
                            resolution = resolution,
                            framesPerSecond = framesPerSecond.toIntOrNull()?.coerceIn(30, 120) ?: 60,
                            bitrateMbps = bitrate.toIntOrNull()?.coerceIn(1, 200) ?: 20,
                        ),
                    )
                },
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
private fun resolutionLabel(resolution: StreamingResolution): String = stringResource(
    when (resolution) {
        StreamingResolution.AUTO -> R.string.resolution_auto
        StreamingResolution.HD_720P -> R.string.resolution_720p
        StreamingResolution.FULL_HD_1080P -> R.string.resolution_1080p
        StreamingResolution.QHD_1440P -> R.string.resolution_1440p
        StreamingResolution.UHD_4K -> R.string.resolution_4k
    },
)

@Composable
private fun QualityDot(quality: NetworkQuality?) {
    val color = when (quality) {
        NetworkQuality.OPTIMAL -> ThorGreen
        NetworkQuality.USABLE -> ThorYellow
        NetworkQuality.PROBLEMATIC -> ThorRed
        else -> ThorGray
    }
    Box(Modifier.size(10.dp).clip(CircleShape).background(color))
}

@Composable
private fun PackageIcon(packageName: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val drawable: Drawable? = remember(packageName) {
        runCatching { context.packageManager.getApplicationIcon(packageName) }.getOrNull()
    }
    if (drawable != null) {
        val bitmap = remember(drawable) { drawable.toBitmap(96, 96).asImageBitmap() }
        Image(bitmap = bitmap, contentDescription = null, modifier = modifier.clip(RoundedCornerShape(12.dp)))
    } else {
        Box(modifier.clip(RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Gamepad, contentDescription = null, tint = ThorCyan, modifier = Modifier.fillMaxSize().padding(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppPickerDialog(
    apps: List<InstalledApp>,
    loading: Boolean,
    onDismiss: () -> Unit,
    onAdd: (InstalledApp, StreamingType, String?) -> Unit,
) {
    var selectedApp by remember { mutableStateOf<InstalledApp?>(null) }
    var selectedType by remember { mutableStateOf(StreamingType.CUSTOM) }
    var customName by remember { mutableStateOf("") }
    var typeMenuOpen by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (selectedApp == null) R.string.dashboard_picker_choose else R.string.dashboard_picker_configure)) },
        text = {
            if (loading) {
                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else if (selectedApp == null) {
                LazyColumn(modifier = Modifier.height(360.dp)) {
                    items(apps, key = { it.packageName }) { app ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { selectedApp = app }.padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            PackageIcon(app.packageName, Modifier.size(42.dp))
                            Spacer(Modifier.size(12.dp))
                            Column {
                                Text(app.label, fontWeight = FontWeight.Bold)
                                Text(app.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        HorizontalDivider()
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(selectedApp!!.label, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        label = { Text(stringResource(R.string.dashboard_picker_custom_name)) },
                        singleLine = true,
                    )
                    Box {
                        FilledTonalButton(onClick = { typeMenuOpen = true }) { Text(selectedType.label()) }
                        DropdownMenu(expanded = typeMenuOpen, onDismissRequest = { typeMenuOpen = false }) {
                            StreamingType.entries.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type.label()) },
                                    onClick = { selectedType = type; typeMenuOpen = false },
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (selectedApp != null) {
                Button(onClick = { onAdd(selectedApp!!, selectedType, customName) }) { Text(stringResource(R.string.action_add)) }
            }
        },
        dismissButton = {
            TextButton(onClick = { if (selectedApp == null) onDismiss() else selectedApp = null }) {
                Text(stringResource(if (selectedApp == null) R.string.action_cancel else R.string.action_back))
            }
        },
    )
}
