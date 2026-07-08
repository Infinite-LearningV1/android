package com.example.infinite_track.presentation.main

import android.os.Build
import com.example.infinite_track.presentation.screen.splash.SplashNavigationState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StartupNotificationPermissionPolicyTest {

    @Test
    fun `does not request post notifications after authenticated startup`() {
        val shouldRequest = shouldRequestPostNotifications(
            sdkInt = Build.VERSION_CODES.TIRAMISU,
            alreadyRequestedThisLaunch = false,
            isGranted = false,
            navigationState = SplashNavigationState.NavigateToHome
        )

        assertFalse(shouldRequest)
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
    fun `does not request post notifications below Android 13`() {
        val shouldRequest = shouldRequestPostNotifications(
            sdkInt = Build.VERSION_CODES.S_V2,
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

    @Test
    fun `restores already requested guard after activity recreation`() {
        val alreadyRequested = restoreNotificationPermissionRequestedThisLaunch(
            savedAlreadyRequestedThisLaunch = true
        )

        assertTrue(alreadyRequested)
    }

    @Test
    fun `defaults already requested guard to false without saved value`() {
        val alreadyRequested = restoreNotificationPermissionRequestedThisLaunch(
            savedAlreadyRequestedThisLaunch = null
        )

        assertFalse(alreadyRequested)
    }
}
