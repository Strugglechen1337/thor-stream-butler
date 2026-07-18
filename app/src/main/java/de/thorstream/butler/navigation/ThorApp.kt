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
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import de.thorstream.butler.R
import de.thorstream.butler.core.designsystem.ThorTheme
import de.thorstream.butler.feature.controllertest.ControllerTestRoute
import de.thorstream.butler.feature.dashboard.DashboardRoute
import de.thorstream.butler.feature.networktest.NetworkTestRoute
import de.thorstream.butler.feature.history.HistoryRoute
import de.thorstream.butler.feature.hosts.HostsRoute
import de.thorstream.butler.feature.settings.SettingsRoute
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private enum class Destination(val route: String, @param:StringRes val titleRes: Int, val icon: ImageVector) {
    Dashboard("dashboard", R.string.nav_dashboard, Icons.Rounded.Home),
    Network("network", R.string.nav_network_test, Icons.Rounded.Speed),
    Hosts("hosts", R.string.nav_hosts, Icons.Rounded.Dns),
    History("history", R.string.nav_history, Icons.Rounded.History),
    Settings("settings", R.string.nav_settings, Icons.Rounded.Settings),
}

/** Route without a navigation-bar entry; reached from the settings screen. */
private const val CONTROLLER_TEST_ROUTE = "controllertest"

@Composable
fun ThorApp(
    runNetworkTest: Boolean = false,
    onNetworkTestConsumed: () -> Unit = {},
    viewModel: ThorAppViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    ThorTheme(themePreference = settings.theme) {
        val navController = rememberNavController()
        val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
        // Quick Settings tile entry: bring the network test to the front.
        LaunchedEffect(runNetworkTest) {
            if (runNetworkTest) navController.open(Destination.Network)
        }
        BoxWithConstraints(Modifier.fillMaxSize()) {
            // Keep the full content width on portrait handhelds and compact tablets.
            // The rail is reserved for the Material 3 expanded-width breakpoint.
            val useNavigationRail = maxWidth >= 840.dp
            if (useNavigationRail) {
                Row(Modifier.fillMaxSize()) {
                    NavigationRail {
                        Destination.entries.forEach { destination ->
                            NavigationRailItem(
                                selected = currentRoute == destination.route,
                                onClick = { navController.open(destination) },
                                icon = { Icon(destination.icon, contentDescription = null) },
                                label = { Text(stringResource(destination.titleRes)) },
                            )
                        }
                    }
                    ThorNavHost(navController, Modifier.weight(1f), runNetworkTest, onNetworkTestConsumed)
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
                                    label = { Text(stringResource(destination.titleRes)) },
                                )
                            }
                        }
                    },
                ) { padding -> ThorNavHost(navController, Modifier.padding(padding), runNetworkTest, onNetworkTestConsumed) }
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
private fun ThorNavHost(
    navController: NavHostController,
    modifier: Modifier,
    runNetworkTest: Boolean = false,
    onNetworkTestConsumed: () -> Unit = {},
) {
    NavHost(
        navController = navController,
        startDestination = Destination.Dashboard.route,
        modifier = modifier,
    ) {
        composable(Destination.Dashboard.route) { DashboardRoute() }
        composable(Destination.Network.route) {
            NetworkTestRoute(autoStart = runNetworkTest, onAutoStartConsumed = onNetworkTestConsumed)
        }
        composable(Destination.History.route) { HistoryRoute() }
        composable(Destination.Hosts.route) { HostsRoute() }
        composable(Destination.Settings.route) {
            SettingsRoute(onOpenControllerTest = { navController.navigate(CONTROLLER_TEST_ROUTE) { launchSingleTop = true } })
        }
        composable(CONTROLLER_TEST_ROUTE) { ControllerTestRoute(onExit = { navController.popBackStack() }) }
    }
}
