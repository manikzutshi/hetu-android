package com.aurafarmers.hetu.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.aurafarmers.hetu.ui.navigation.Screen
import com.aurafarmers.hetu.ui.screens.insights.InsightsScreen
import com.aurafarmers.hetu.ui.screens.journal.JournalScreen
import com.aurafarmers.hetu.ui.screens.settings.SettingsScreen
import com.aurafarmers.hetu.ui.screens.timeline.TimelineScreen
import com.aurafarmers.hetu.ui.screens.track.TrackScreen

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Tabs where BottomBar should be visible
    val bottomNavRoutes = listOf(
        Screen.Home.route,
        Screen.Timeline.route,
        Screen.Insights.route,
        Screen.Feed.route
    )
    
    // Tabs where the Main FAB (Track) should be visible
    // Feed has its own FAB, so exclude it
    val fabRoutes = listOf(
        Screen.Home.route,
        Screen.Timeline.route,
        Screen.Insights.route
    )
    
    val showBottomBar = currentRoute in bottomNavRoutes
    val showFab = currentRoute in fabRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                HetuBottomBar(navController = navController)
            }
        },
        floatingActionButton = {
            if (showFab) {
                FloatingActionButton(
                    onClick = { navController.navigate(Screen.Track.route) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Filled.Add, "Track")
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                com.aurafarmers.hetu.ui.screens.home.HomeScreen(
                    onNavigateToJournal = { navController.navigate(Screen.Journal.route) },
                    onNavigateToTrack = { navController.navigate(Screen.Track.route) },
                    onNavigateToTimeline = { 
                        navController.navigate(Screen.Timeline.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToInsights = { 
                        navController.navigate(Screen.Insights.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }
            
            composable(Screen.Journal.route) {
                JournalScreen(navController = navController)
            }
            
            composable(Screen.Timeline.route) {
                TimelineScreen(onBack = { navController.navigateUp() })
            }
            
            composable(Screen.Insights.route) {
                InsightsScreen(onBack = { navController.navigateUp() })
            }
            
            composable(Screen.Feed.route) {
                com.aurafarmers.hetu.ui.screens.feed.FeedScreen(onBack = { navController.navigateUp() })
            }
            
            composable(Screen.Track.route) {
                TrackScreen(onBack = { navController.popBackStack() })
            }
            
            composable(Screen.Settings.route) {
                 SettingsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

@Composable
fun HetuBottomBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Feed,
        BottomNavItem.Timeline,
        BottomNavItem.Insights
    )
    
    NavigationBar {
        items.forEach { item ->
            val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
            
            NavigationBarItem(
                icon = { 
                    Icon(
                        if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label
                    ) 
                },
                label = { Text(item.label) },
                selected = selected,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

sealed class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Home : BottomNavItem(Screen.Home.route, "Home", Icons.Filled.Home, Icons.Outlined.Home)
    object Feed : BottomNavItem(Screen.Feed.route, "Feed", Icons.Filled.PhotoLibrary, Icons.Outlined.PhotoLibrary)
    object Timeline : BottomNavItem(Screen.Timeline.route, "Timeline", Icons.Filled.History, Icons.Outlined.History)
    object Insights : BottomNavItem(Screen.Insights.route, "Insights", Icons.Filled.Lightbulb, Icons.Outlined.Lightbulb)
}
