package com.example.infinite_track.presentation.main

import android.os.Bundle
import com.example.infinite_track.presentation.screen.splash.SplashNavigationState

private const val NOTIFICATION_PERMISSION_REQUESTED_THIS_LAUNCH_KEY =
    "notification_permission_requested_this_launch"

fun restoreNotificationPermissionRequestedThisLaunch(savedInstanceState: Bundle?): Boolean =
    restoreNotificationPermissionRequestedThisLaunch(
        savedAlreadyRequestedThisLaunch = savedInstanceState?.getBoolean(
            NOTIFICATION_PERMISSION_REQUESTED_THIS_LAUNCH_KEY
        )
    )

fun restoreNotificationPermissionRequestedThisLaunch(
    savedAlreadyRequestedThisLaunch: Boolean?
): Boolean = savedAlreadyRequestedThisLaunch ?: false

fun saveNotificationPermissionRequestedThisLaunch(
    outState: Bundle,
    alreadyRequestedThisLaunch: Boolean
) {
    outState.putBoolean(
        NOTIFICATION_PERMISSION_REQUESTED_THIS_LAUNCH_KEY,
        alreadyRequestedThisLaunch
    )
}

fun shouldRequestPostNotifications(
    sdkInt: Int,
    alreadyRequestedThisLaunch: Boolean,
    isGranted: Boolean,
    navigationState: SplashNavigationState
): Boolean = false
