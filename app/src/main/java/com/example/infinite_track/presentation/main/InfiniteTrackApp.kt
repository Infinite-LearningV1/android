package com.example.infinite_track.presentation.main

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.example.infinite_track.domain.manager.SessionManager
import com.example.infinite_track.presentation.components.base.BaseLayout
import com.example.infinite_track.presentation.navigation.AppNavigator
import com.example.infinite_track.presentation.navigation.NavigationEvent
import com.example.infinite_track.presentation.navigation.Screen
import com.example.infinite_track.presentation.navigation.appNavGraph
import com.example.infinite_track.utils.DialogHelper
import com.example.infinite_track.utils.LocalLocationPermissionHelper
import com.example.infinite_track.utils.LocationPermissionHelper
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.runtime.CompositionLocalProvider

@Composable
fun InfiniteTrackApp(
    modifier: Modifier = Modifier,
    appNavigator: AppNavigator? = null,
    sessionManager: SessionManager? = null,
    locationPermissionHelper: LocationPermissionHelper? = null
) {
    // Root level NavController - handles top-level navigation
    val navController = rememberNavController()
    val context = LocalContext.current

    // Observe session expiration state
    val sessionExpired by sessionManager?.sessionExpired?.collectAsState() ?: remember { androidx.compose.runtime.mutableStateOf(false) }

    // Handle session expiration
    LaunchedEffect(sessionExpired) {
        if (sessionExpired) {
            // Show session expired dialog
            DialogHelper.showDialogError(
                context = context,
                title = "Sesi Berakhir",
                textContent = "Sesi Anda telah berakhir. Silakan login kembali untuk melanjutkan.",
                onConfirm = {
                    // Reset session expired state
                    sessionManager?.resetSessionExpired()

                    // Navigate to login and clear back stack
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
    }

    // Handle navigation events from AppNavigator
    LaunchedEffect(appNavigator) {
        appNavigator?.navigationEvents?.collectLatest { event ->
            when (event) {
                is NavigationEvent.NavigateToAttendance -> {
                    // Navigate to attendance screen
                    navController.navigate(Screen.Attendance.route) {
                        // Optional: Clear back stack if needed
                        launchSingleTop = true
                    }
                }

                is NavigationEvent.NavigateToScreen -> {
                    navController.navigate(event.route) {
                        launchSingleTop = true
                    }
                }

                is NavigationEvent.NavigateToLoginAfterSessionExpired -> {
                    // This event is now handled by SessionManager state above
                    // Keeping for backward compatibility if needed elsewhere
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
        }
    }

    // State animation defined at the app level - this will persist across screen navigations
    val infiniteTransition = rememberInfiniteTransition(label = "infinite_rotation")
    val rotationAnimation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 30000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val sizeTransition = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        sizeTransition.animateTo(
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 10000
                    0f at 0
                    1f at 5000
                    0f at 10000
                },
                repeatMode = RepeatMode.Restart
            )
        )
    }

    // Main box that contains both the BaseLayout and the rest of the UI
    Box(modifier = modifier.fillMaxSize()) {
        // BaseLayout displayed at the back layer
        BaseLayout(
            rotation = rotationAnimation,
            size = sizeTransition.value,
            modifier = Modifier.zIndex(-2f)
        )

        // Semi-transparent white layer over BaseLayout
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(-1f)
                .background(Color.White.copy(alpha = 0.5f))
        )

        // Clean, minimal root container
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Transparent
        ) {
            // Provide LocationPermissionHelper throughout the app
            CompositionLocalProvider(LocalLocationPermissionHelper provides locationPermissionHelper) {
                // Root NavHost with only top-level navigation concerns
                NavHost(
                    navController = navController,
                    startDestination = Screen.Splash.route
                ) {
                    // Connect to the app navigation graph
                    appNavGraph(navController)
                }
            }
        }
    }
}
