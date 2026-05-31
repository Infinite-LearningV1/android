package com.example.infinite_track.presentation.main

import android.os.Build
import com.example.infinite_track.presentation.screen.splash.SplashNavigationState

fun shouldRequestPostNotifications(
    sdkInt: Int,
    alreadyRequestedThisLaunch: Boolean,
    isGranted: Boolean,
    navigationState: SplashNavigationState
): Boolean = sdkInt >= Build.VERSION_CODES.TIRAMISU &&
    !alreadyRequestedThisLaunch &&
    !isGranted &&
    navigationState is SplashNavigationState.NavigateToHome
