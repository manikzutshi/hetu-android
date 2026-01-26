package com.aurafarmers.hetu.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aurafarmers.hetu.ui.screens.home.HomeScreen
import com.aurafarmers.hetu.ui.screens.insights.InsightsScreen
import com.aurafarmers.hetu.ui.screens.journal.JournalScreen
import com.aurafarmers.hetu.ui.screens.settings.SettingsScreen
import com.aurafarmers.hetu.ui.screens.timeline.TimelineScreen
import com.aurafarmers.hetu.ui.screens.track.TrackScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Journal : Screen("journal")
    object Track : Screen("track")
    object Timeline : Screen("timeline")
    object Insights : Screen("insights")
    object Settings : Screen("settings")
}

@Composable
fun HetuNavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToJournal = { navController.navigate(Screen.Journal.route) },
                onNavigateToTrack = { navController.navigate(Screen.Track.route) },
                onNavigateToTimeline = { navController.navigate(Screen.Timeline.route) },
                onNavigateToInsights = { navController.navigate(Screen.Insights.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }
        
        composable(Screen.Journal.route) {
            JournalScreen(
                navController = navController
            )
        }
        
        composable(Screen.Track.route) {
            TrackScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.Timeline.route) {
            TimelineScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.Insights.route) {
            InsightsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
