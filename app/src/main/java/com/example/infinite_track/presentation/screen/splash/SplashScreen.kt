package com.example.infinite_track.presentation.screen.splash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.infinite_track.R
import com.example.infinite_track.presentation.core.headline1
import com.example.infinite_track.presentation.navigation.Screen
import com.example.infinite_track.presentation.theme.Blue_500

/**
 * Transparent branded splash presentation gate.
 *
 * Global BaseLayout ownership stays in InfiniteTrackApp.
 * This screen only renders the Lottie once and gates navigation until:
 *   isAnimationFinished AND destination resolved.
 *
 * Session truth remains in SplashViewModel — this is presentation only.
 */
@Composable
fun SplashScreen(
    navController: NavHostController,
    splashViewModel: SplashViewModel
) {
    val compositionResult = rememberLottieComposition(
        spec = LottieCompositionSpec.RawRes(R.raw.infinite_track_splash)
    )
    val composition by compositionResult
    // Play once; hold final frame after completion (progress stays at 1f).
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = 1,
        isPlaying = true,
        restartOnPlay = false
    )
    // Failure must not trap the user; treat load failure as finished so destination can navigate.
    val isAnimationFinished = compositionResult.isFailure || (composition != null && progress >= 1f)

    val navigationState by splashViewModel.navigationState.collectAsState()
    var hasNavigated by remember { mutableStateOf(false) }

    // One-shot navigation: only after animation completion AND destination resolved.
    // Fast session resolution waits for animation; slow resolution waits on final frame.
    LaunchedEffect(navigationState, isAnimationFinished, hasNavigated) {
        if (hasNavigated || !isAnimationFinished) return@LaunchedEffect

        when (navigationState) {
            is SplashNavigationState.NavigateToHome -> {
                hasNavigated = true
                navController.navigate(Screen.Home.route) {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
            }

            is SplashNavigationState.NavigateToLogin -> {
                hasNavigated = true
                navController.navigate(Screen.Login.route) {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
            }

            SplashNavigationState.Loading,
            SplashNavigationState.TemporaryFailure -> Unit
        }
    }

    // Full-screen transparent container — global BaseLayout remains visible behind.
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Canvas metadata: 184 x 93. Keep aspect ratio; scale responsively.
            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth(0.72f)
                    .widthIn(max = 320.dp)
                    .aspectRatio(184f / 93f)
                    .semantics {
                        contentDescription = "Infinite Track splash animation"
                    }
            )

            // TemporaryFailure recovery only after the branded animation finished.
            // No opaque page background; keep compact + readable over global background.
            if (navigationState is SplashNavigationState.TemporaryFailure && isAnimationFinished) {
                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Unable to verify session",
                    style = headline1,
                    color = Blue_500,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(onClick = { splashViewModel.retrySessionCheck() }) {
                    Text(text = "Retry")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (hasNavigated) return@Button
                        hasNavigated = true
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                ) {
                    Text(text = "Login")
                }
            }
        }
    }
}
