package com.example.infinite_track.presentation.main

import android.widget.Toast
import androidx.camera.core.ExperimentalGetImage
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.infinite_track.presentation.components.button.customfab.CustomFAB
import com.example.infinite_track.presentation.components.navigation.BottomBarInternship
import com.example.infinite_track.presentation.components.navigation.BottomBarStaff
import com.example.infinite_track.presentation.navigation.Screen
import com.example.infinite_track.presentation.navigation.mainContentNavGraph
import com.example.infinite_track.utils.safeNavigate

@ExperimentalGetImage
@Composable
fun MainScreen(
    rootNavController: NavHostController,
    navigateToAttendance: Boolean = false,
    onAttendanceNavigationHandled: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Create a NavController specific to the main content area
    val mainContentNavController = rememberNavController()
    val mainViewModel: MainViewModel = hiltViewModel()

    // Get the current route for visibility decisions
    val navBackStackEntry by mainContentNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ""

    // Collect user role from MainViewModel
    val userRole by mainViewModel.userRole.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(navigateToAttendance, currentRoute) {
        if (navigateToAttendance) {
            if (currentRoute != Screen.Attendance.route) {
                mainContentNavController.safeNavigate(Screen.Attendance.route)
            }
            onAttendanceNavigationHandled()
        }
    }

    // Screens that should not display the bottom bar
    val screensWithoutBottomBar = listOf(
        Screen.Attendance.route,
        Screen.EditProfile.route,
        Screen.DetailMyAttendance.route,
        Screen.DetailListTimeOff.route,
        Screen.TimeOffRequest.route,
        Screen.FAQ.route,
        Screen.FaceScanner.route, // Hide bottom bar for face scanner
        Screen.LocationSearch.route // Hide bottom bar for location search
    )

    val isBottomBarVisible = currentRoute !in screensWithoutBottomBar

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent, // Makes scaffold background transparent
        contentColor = MaterialTheme.colorScheme.onBackground, // Ensures text is visible

        // Bottom bar navigation based on user role
        bottomBar = {
            if (isBottomBarVisible) {
                when (userRole) {
                    "Internship" -> BottomBarInternship(navController = mainContentNavController)
                    "Admin", "Employee", "Management" -> BottomBarStaff(navController = mainContentNavController)
                    else -> {}
                }
            }
        },

        // Floating action button
        floatingActionButton = {
            if (isBottomBarVisible && (userRole == "Management" || userRole == "Internship")) {
                CustomFAB(userRole = userRole) {
                    when (userRole) {
                        "Internship" -> mainContentNavController.safeNavigate(Screen.Attendance.route)
                        "Management" -> mainContentNavController.safeNavigate(Screen.TimeOffReq.route)
                        else -> Toast.makeText(
                            context,
                            "Role not recognized",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // NavHost for main content area using the main content nav controller
            NavHost(
                navController = mainContentNavController,
                startDestination = Screen.Home.route,
            ) {
                // Use the main content navigation graph
                mainContentNavGraph(mainContentNavController, rootNavController)
            }
        }
    }
}
