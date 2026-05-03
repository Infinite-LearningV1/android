package com.example.infinite_track.presentation.main

import androidx.camera.core.ExperimentalGetImage
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.infinite_track.presentation.components.button.customfab.CustomFAB
import com.example.infinite_track.presentation.components.navigation.MainBottomBar
import com.example.infinite_track.presentation.navigation.main.MainShellNavigationPolicy
import com.example.infinite_track.presentation.navigation.model.Screen
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
    val mainContentNavController = rememberNavController()
    val mainViewModel: MainViewModel = hiltViewModel()

    val navBackStackEntry by mainContentNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val userRole by mainViewModel.userRole.collectAsState()
    val fabConfig = MainShellNavigationPolicy.fabConfigForRole(userRole)
    val isBottomBarVisible = MainShellNavigationPolicy.shouldShowBottomBar(currentRoute)

    LaunchedEffect(navigateToAttendance, currentRoute) {
        if (navigateToAttendance && currentRoute == Screen.Home.route) {
            mainContentNavController.safeNavigate(Screen.Attendance.route)
            onAttendanceNavigationHandled()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        bottomBar = {
            if (isBottomBarVisible) {
                MainBottomBar(
                    navController = mainContentNavController,
                    userRole = userRole
                )
            }
        },
        floatingActionButton = {
            if (isBottomBarVisible && fabConfig != null) {
                CustomFAB(fabConfig = fabConfig) {
                    mainContentNavController.safeNavigate(fabConfig.destination.route)
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
            NavHost(
                navController = mainContentNavController,
                startDestination = Screen.Home.route,
            ) {
                mainContentNavGraph(mainContentNavController, rootNavController)
            }
        }
    }
}
