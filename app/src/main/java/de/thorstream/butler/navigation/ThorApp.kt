package de.thorstream.butler.navigation

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import de.thorstream.butler.core.designsystem.ThorTheme
import de.thorstream.butler.feature.dashboard.DashboardRoute
import de.thorstream.butler.feature.networktest.NetworkTestRoute
import de.thorstream.butler.feature.history.HistoryRoute
import de.thorstream.butler.feature.hosts.HostsRoute
import de.thorstream.butler.feature.settings.SettingsRoute
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private enum class Destination(val route: String, val title: String, val icon: ImageVector) {
    Dashboard("dashboard", "Dashboard", Icons.Rounded.Home),
    Network("network", "Netzwerktest", Icons.Rounded.Speed),
    Hosts("hosts", "Hosts", Icons.Rounded.Dns),
    History("history", "Historie", Icons.Rounded.History),
    Settings("settings", "Einstellungen", Icons.Rounded.Settings),
}

@Composable
fun ThorApp(viewModel: ThorAppViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    ThorTheme(themePreference = settings.theme) {
        val navController = rememberNavController()
        val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val useNavigationRail = maxWidth >= 700.dp
            if (useNavigationRail) {
                Row(Modifier.fillMaxSize()) {
                    NavigationRail {
                        Destination.entries.forEach { destination ->
                            NavigationRailItem(
                                selected = currentRoute == destination.route,
                                onClick = { navController.open(destination) },
                                icon = { Icon(destination.icon, contentDescription = null) },
                                label = { Text(destination.title) },
                            )
                        }
                    }
                    ThorNavHost(navController, Modifier.weight(1f))
                }
            } else {
                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            Destination.entries.forEach { destination ->
                                NavigationBarItem(
                                    selected = currentRoute == destination.route,
                                    onClick = { navController.open(destination) },
                                    icon = { Icon(destination.icon, contentDescription = null) },
                                    label = { Text(destination.title) },
                                )
                            }
                        }
                    },
                ) { padding -> ThorNavHost(navController, Modifier.padding(padding)) }
            }
        }
    }
}

private fun NavHostController.open(destination: Destination) {
    navigate(destination.route) {
        popUpTo(Destination.Dashboard.route) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun ThorNavHost(navController: NavHostController, modifier: Modifier) {
    NavHost(
        navController = navController,
        startDestination = Destination.Dashboard.route,
        modifier = modifier,
    ) {
        composable(Destination.Dashboard.route) { DashboardRoute() }
        composable(Destination.Network.route) { NetworkTestRoute() }
        composable(Destination.History.route) { HistoryRoute() }
        composable(Destination.Hosts.route) { HostsRoute() }
        composable(Destination.Settings.route) { SettingsRoute() }
    }
}
