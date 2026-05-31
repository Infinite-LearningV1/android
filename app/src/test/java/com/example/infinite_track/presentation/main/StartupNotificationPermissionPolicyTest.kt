package com.example.infinite_track.presentation.main

import android.os.Build
import com.example.infinite_track.presentation.screen.splash.SplashNavigationState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StartupNotificationPermissionPolicyTest {

    @Test
    fun `requests post notifications only after authenticated startup on Android 13 plus`() {
        val shouldRequest = shouldRequestPostNotifications(
            sdkInt = Build.VERSION_CODES.TIRAMISU,
            alreadyRequestedThisLaunch = false,
            isGranted = false,
            navigationState = SplashNavigationState.NavigateToHome
        )

        assertTrue(shouldRequest)
    }

    @Test
    fun `does not request post notifications when startup resolves to login`() {
        val shouldRequest = shouldRequestPostNotifications(
            sdkInt = Build.VERSION_CODES.TIRAMISU,
            alreadyRequestedThisLaunch = false,
            isGranted = false,
            navigationState = SplashNavigationState.NavigateToLogin
        )

        assertFalse(shouldRequest)
    }

    @Test
    fun `does not request post notifications twice in the same launch`() {
        val shouldRequest = shouldRequestPostNotifications(
            sdkInt = Build.VERSION_CODES.TIRAMISU,
            alreadyRequestedThisLaunch = true,
            isGranted = false,
            navigationState = SplashNavigationState.NavigateToHome
        )

        assertFalse(shouldRequest)
    }

    @Test
    fun `does not request post notifications before Android 13`() {
        val shouldRequest = shouldRequestPostNotifications(
            sdkInt = Build.VERSION_CODES.TIRAMISU - 1,
            alreadyRequestedThisLaunch = false,
            isGranted = false,
            navigationState = SplashNavigationState.NavigateToHome
        )

        assertFalse(shouldRequest)
    }

    @Test
    fun `does not request post notifications when already granted`() {
        val shouldRequest = shouldRequestPostNotifications(
            sdkInt = Build.VERSION_CODES.TIRAMISU,
            alreadyRequestedThisLaunch = false,
            isGranted = true,
            navigationState = SplashNavigationState.NavigateToHome
        )

        assertFalse(shouldRequest)
    }
}
