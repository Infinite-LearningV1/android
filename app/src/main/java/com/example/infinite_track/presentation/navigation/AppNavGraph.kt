package com.example.infinite_track.presentation.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.example.infinite_track.presentation.screen.auth.LoginScreen
import com.example.infinite_track.presentation.screen.splash.SplashScreen
import com.example.infinite_track.presentation.screen.splash.SplashViewModel

fun NavGraphBuilder.appNavGraph(
    navController: NavHostController,
    splashViewModel: SplashViewModel
) {
    // Splash Screen - entry point of the app
    composable(Screen.Splash.route) {
        SplashScreen(
            navController = navController,
            splashViewModel = splashViewModel
        )
    }

    // Auth graph - contains login screen
    navigation(
        startDestination = Screen.Login.route,
        route = "auth_graph"
    ) {
        // Login Screen
        composable(Screen.Login.route) {
            LoginScreen(
                navigateToHome = {
                    // Navigate to main_graph after successful login
                    navController.navigate(Screen.Home.route) {
                        // Clear backstack after successful login
                        popUpTo(navController.graph.startDestinationId) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            )
        }
    }

    // Note: Attendance-related action routes live in MainContentNavGraph.kt
    // so the root graph stays focused on app entry and auth concerns.
}
