package com.example.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.model.ActivityType
import com.example.ui.MainViewModel
import com.example.ui.screens.ActivityDetailScreen
import com.example.ui.screens.ActivityFinishedScreen
import com.example.ui.screens.ActivitySetupScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LiveTrackingScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.StatisticsScreen

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector? = null,
    val unselectedIcon: ImageVector? = null
) {
    data object Home : Screen("home", "Home", Icons.Filled.Home, Icons.Outlined.Home)
    data object History : Screen("history", "History", Icons.Filled.History, Icons.Outlined.History)
    data object Statistics : Screen("statistics", "Stats", Icons.Filled.BarChart, Icons.Outlined.BarChart)
    data object Profile : Screen("profile", "Profile", Icons.Filled.Person, Icons.Outlined.Person)

    data object Setup : Screen("setup/{activityType}", "Setup") {
        fun createRoute(type: ActivityType) = "setup/${type.name}"
    }
    data object LiveTracking : Screen("live_tracking", "Live Tracking")
    data object Finished : Screen("finished/{activityId}", "Finished") {
        fun createRoute(id: Long) = "finished/$id"
    }
    data object Detail : Screen("detail/{activityId}", "Detail") {
        fun createRoute(id: Long) = "detail/$id"
    }
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.History,
    Screen.Statistics,
    Screen.Profile
)

@Composable
fun PulseTrackNavGraph(
    viewModel: MainViewModel,
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val trackingState by viewModel.trackingState.collectAsState()

    val showBottomBar = currentRoute in listOf(
        Screen.Home.route,
        Screen.History.route,
        Screen.Statistics.route,
        Screen.Profile.route
    )

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically { it },
                exit = slideOutVertically { it }
            ) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    bottomNavItems.forEach { screen ->
                        val isSelected = currentRoute == screen.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) screen.selectedIcon!! else screen.unselectedIcon!!,
                                    contentDescription = screen.title
                                )
                            },
                            label = { Text(screen.title) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("bottom_nav_${screen.title.lowercase()}")
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Home Screen
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToSetup = { type ->
                        navController.navigate(Screen.Setup.createRoute(type))
                    },
                    onNavigateToDetail = { id ->
                        navController.navigate(Screen.Detail.createRoute(id))
                    },
                    onNavigateToHistory = {
                        navController.navigate(Screen.History.route)
                    }
                )
            }

            // Setup Screen
            composable(
                route = Screen.Setup.route,
                arguments = listOf(navArgument("activityType") { type = NavType.StringType })
            ) { backStackEntry ->
                val typeName = backStackEntry.arguments?.getString("activityType") ?: ActivityType.RUNNING.name
                val activityType = ActivityType.fromString(typeName)
                ActivitySetupScreen(
                    initialType = activityType,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onStartTracking = {
                        navController.navigate(Screen.LiveTracking.route) {
                            popUpTo(Screen.Home.route)
                        }
                    }
                )
            }

            // Live Tracking Screen
            composable(Screen.LiveTracking.route) {
                LiveTrackingScreen(
                    viewModel = viewModel,
                    onFinishActivity = { id ->
                        navController.navigate(Screen.Finished.createRoute(id)) {
                            popUpTo(Screen.Home.route)
                        }
                    },
                    onDiscard = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }
                )
            }

            // Finished Screen
            composable(
                route = Screen.Finished.route,
                arguments = listOf(navArgument("activityId") { type = NavType.LongType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getLong("activityId") ?: 0L
                ActivityFinishedScreen(
                    activityId = id,
                    viewModel = viewModel,
                    onSavedOrDone = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }
                )
            }

            // History Screen
            composable(Screen.History.route) {
                HistoryScreen(
                    viewModel = viewModel,
                    onNavigateToDetail = { id ->
                        navController.navigate(Screen.Detail.createRoute(id))
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            // Detail Screen
            composable(
                route = Screen.Detail.route,
                arguments = listOf(navArgument("activityId") { type = NavType.LongType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getLong("activityId") ?: 0L
                ActivityDetailScreen(
                    activityId = id,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            // Statistics Screen
            composable(Screen.Statistics.route) {
                StatisticsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            // Profile Screen
            composable(Screen.Profile.route) {
                ProfileScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
