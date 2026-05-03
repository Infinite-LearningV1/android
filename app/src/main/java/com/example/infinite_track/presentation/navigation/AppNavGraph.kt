package com.example.infinite_track.presentation.navigation

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.example.infinite_track.presentation.navigation.model.Screen
import com.example.infinite_track.presentation.screen.attendance.search.LocationSearchScreen
import com.example.infinite_track.presentation.screen.auth.LoginScreen
import com.example.infinite_track.presentation.screen.attendance.booking.WfaBookingScreen
import com.example.infinite_track.presentation.screen.attendance.booking.WfaBookingViewModel
import com.example.infinite_track.presentation.screen.splash.SplashScreen

fun NavGraphBuilder.appNavGraph(
    navController: NavHostController
) {
    // Splash Screen - entry point of the app
    composable(Screen.Splash.route) {
        SplashScreen(navController = navController)
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

    // Location Search Screen - untuk pencarian lokasi attendance
    composable(Screen.LocationSearch.route) {
        LocationSearchScreen(
            navController = navController,
            onLocationSelected = {
                // Handle location selection - bisa dikembangkan untuk menyimpan lokasi terpilih
                // Untuk sementara, kembali ke AttendanceScreen
                navController.navigateUp()
            }
        )
    }

    // WFA Booking Screen
    composable(
        route = Screen.WfaBooking.route,
        arguments = listOf(
            navArgument("latitude") { type = NavType.FloatType }, // Changed to FloatType
            navArgument("longitude") { type = NavType.FloatType } // Changed to FloatType
            // address argument removed - WfaBookingViewModel will fetch it
        )
    ) { backStackEntry ->
        val viewModel: WfaBookingViewModel = hiltViewModel()
        WfaBookingScreen(
            viewModel = viewModel,
            navController = navController
        )
    }

    // Note: FaceScannerScreen moved to MainContentNavGraph.kt
}
