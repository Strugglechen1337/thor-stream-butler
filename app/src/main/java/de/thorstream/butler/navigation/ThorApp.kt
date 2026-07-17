package de.thorstream.butler.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import de.thorstream.butler.core.designsystem.ThorTheme
import de.thorstream.butler.feature.dashboard.DashboardRoute

private enum class Destination(val route: String, val title: String, val icon: ImageVector) {
    Dashboard("dashboard", "Dashboard", Icons.Rounded.Home),
    Network("network", "Netzwerktest", Icons.Rounded.Speed),
    Hosts("hosts", "Hosts", Icons.Rounded.Dns),
    History("history", "Historie", Icons.Rounded.History),
    Settings("settings", "Einstellungen", Icons.Rounded.Settings),
}

@Composable
fun ThorApp() = ThorTheme {
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    Scaffold(
        bottomBar = {
            NavigationBar {
                Destination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(Destination.Dashboard.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(destination.icon, contentDescription = null) },
                        label = { Text(destination.title) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Dashboard.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(Destination.Dashboard.route) { DashboardRoute() }
            Destination.entries.filterNot { it == Destination.Dashboard }.forEach { destination ->
                composable(destination.route) { InitialScreen(destination.title) }
            }
        }
    }
}

@Composable
private fun InitialScreen(title: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("THOR // STREAM BUTLER", style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
        Text(title, style = androidx.compose.material3.MaterialTheme.typography.headlineLarge)
        Text("Initiale Projektstruktur ist bereit.")
    }
}
