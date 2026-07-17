package de.thorstream.butler.feature.dashboard

import android.graphics.drawable.Drawable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.thorstream.butler.core.designsystem.ThorCyan
import de.thorstream.butler.core.designsystem.ThorGray
import de.thorstream.butler.core.designsystem.ThorGreen
import de.thorstream.butler.core.designsystem.ThorRed
import de.thorstream.butler.core.designsystem.ThorYellow
import de.thorstream.butler.domain.model.InstalledApp
import de.thorstream.butler.domain.model.NetworkQuality
import de.thorstream.butler.domain.model.StreamingType
import java.text.DateFormat
import java.util.Date

@Composable
fun DashboardRoute(viewModel: DashboardViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showPicker by remember { mutableStateOf(false) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("THOR // STREAM BUTLER", color = ThorCyan, style = MaterialTheme.typography.labelLarge)
                    Text("Deine Streams", style = MaterialTheme.typography.headlineLarge)
                }
                Button(onClick = { viewModel.loadInstalledApps(); showPicker = true }) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("App hinzufügen")
                }
            }
            if (state.items.isEmpty()) {
                EmptyDashboard(onAdd = { viewModel.loadInstalledApps(); showPicker = true })
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 230.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(state.items, key = { it.entry.id }) { item ->
                        StreamingTile(
                            item = item,
                            onLaunch = { viewModel.launch(item.entry) },
                            onDelete = { viewModel.delete(item.entry) },
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
    state.preLaunch?.let { preLaunch ->
        PreLaunchDialog(
            state = preLaunch,
            onCancel = viewModel::cancelPreLaunch,
            onRetry = viewModel::retryPreLaunch,
            onLaunchAnyway = viewModel::launchAnyway,
        )
    }
}

@Composable
private fun PreLaunchDialog(
    state: PreLaunchUiState,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onLaunchAnyway: () -> Unit,
) {
    val assessment = state.assessment
    val requiresDecision = assessment?.quality == NetworkQuality.PROBLEMATIC || assessment?.quality == NetworkQuality.NOT_MEASURABLE
    AlertDialog(
        onDismissRequest = { if (!state.autoLaunching) onCancel() },
        title = { Text(if (assessment == null) "Netzwerkcheck" else assessment.quality.displayName) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(state.entry.displayName, color = ThorCyan, fontWeight = FontWeight.Bold)
                LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth())
                Text(state.step)
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
            }
        },
        confirmButton = {
            if (requiresDecision) Button(onClick = onLaunchAnyway) { Text("Trotzdem starten") }
        },
        dismissButton = {
            if (requiresDecision) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = onRetry) { Text("Erneut testen") }
                    TextButton(onClick = onCancel) { Text("Abbrechen") }
                }
            } else if (!state.autoLaunching) {
                TextButton(onClick = onCancel) { Text("Abbrechen") }
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
        Text("Noch keine Streaming-Apps eingerichtet", style = MaterialTheme.typography.titleLarge)
        TextButton(onClick = onAdd) { Text("Erste App auswählen") }
    }
}

@Composable
private fun StreamingTile(item: DashboardItem, onLaunch: () -> Unit, onDelete: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.035f else 1f, label = "tile-scale")
    val borderColor by animateColorAsState(if (focused) ThorCyan else Color.Transparent, label = "tile-border")
    val shape = RoundedCornerShape(20.dp)
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.45f)
            .scale(scale)
            .border(3.dp, borderColor, shape)
            .clip(shape)
            .onFocusChanged { focused = it.isFocused }
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
                    Text(item.entry.streamingType.displayName, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = onDelete) { Icon(Icons.Rounded.Delete, contentDescription = "Kachel entfernen") }
            }
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    QualityDot(item.entry.lastNetworkQuality)
                    Text(
                        item.entry.lastUsedAt?.let { "Zuletzt ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(it))}" }
                            ?: if (item.isInstalled) "Bereit" else "Nicht installiert",
                        color = if (item.isInstalled) MaterialTheme.colorScheme.onSurfaceVariant else ThorRed,
                        maxLines = 1,
                    )
                }
                Spacer(Modifier.height(10.dp))
                FilledTonalButton(onClick = onLaunch, enabled = item.isInstalled, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                    Text(if (item.isInstalled) " Starten" else " Nicht verfügbar")
                }
            }
        }
    }
}

@Composable
private fun QualityDot(quality: NetworkQuality?) {
    val color = when (quality) {
        NetworkQuality.OPTIMAL -> ThorGreen
        NetworkQuality.USABLE -> ThorYellow
        NetworkQuality.PROBLEMATIC -> ThorRed
        else -> ThorGray
    }
    Box(Modifier.size(10.dp).clip(RoundedCornerShape(50)).border(5.dp, color, RoundedCornerShape(50)))
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
        title = { Text(if (selectedApp == null) "Installierte App wählen" else "Kachel konfigurieren") },
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
                        label = { Text("Eigener Name (optional)") },
                        singleLine = true,
                    )
                    Box {
                        FilledTonalButton(onClick = { typeMenuOpen = true }) { Text(selectedType.displayName) }
                        DropdownMenu(expanded = typeMenuOpen, onDismissRequest = { typeMenuOpen = false }) {
                            StreamingType.entries.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type.displayName) },
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
                Button(onClick = { onAdd(selectedApp!!, selectedType, customName) }) { Text("Hinzufügen") }
            }
        },
        dismissButton = {
            TextButton(onClick = { if (selectedApp == null) onDismiss() else selectedApp = null }) {
                Text(if (selectedApp == null) "Abbrechen" else "Zurück")
            }
        },
    )
}
