package com.example.infinite_track.presentation.screen.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.infinite_track.R
import com.example.infinite_track.presentation.core.headline1
import com.example.infinite_track.presentation.navigation.Screen
import com.example.infinite_track.presentation.theme.Blue_500

@Composable
fun SplashScreen(
    navController: NavHostController,
    splashViewModel: SplashViewModel = hiltViewModel()
) {
    // Get the composition for the Lottie animation
    val composition by rememberLottieComposition(spec = LottieCompositionSpec.RawRes(R.raw.profile_sync))
    val progress by animateLottieCompositionAsState(composition)

    // Collect the navigation state from the ViewModel
    val navigationState by splashViewModel.navigationState.collectAsState()

    // Handle navigation based on animation progress and navigation state
    LaunchedEffect(progress, navigationState) {
        if (progress == 1f) {
            when (navigationState) {
                is SplashNavigationState.NavigateToHome -> {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                    }
                }

                is SplashNavigationState.NavigateToLogin -> {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                    }
                }
                // If still loading, wait for animation to complete
                SplashNavigationState.Loading -> { /* Wait for sync to complete */
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Lottie Animation with size 150dp
            LottieAnimation(
                composition = composition,
                modifier = Modifier
                    .size(200.dp)
                    .background(Color.Transparent)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Sync Profile",
                style = headline1,
                color = Blue_500,
                textAlign = TextAlign.Center
            )
        }
    }

}