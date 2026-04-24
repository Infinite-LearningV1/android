package com.example.infinite_track.presentation.geofencing

import java.util.Locale

enum class GeofenceNotificationAction {
    SHOW_CHECK_IN_REMINDER,
    SHOW_EXIT_WARNING,
    NO_NOTIFICATION
}

object GeofenceNotificationPolicy {
    fun decide(
        eventType: String,
        hasActiveSession: Boolean,
        hasEnteredActiveSessionArea: Boolean
    ): GeofenceNotificationAction {
        return when (eventType.uppercase(Locale.ROOT)) {
            "ENTER" -> {
                if (hasActiveSession) {
                    GeofenceNotificationAction.NO_NOTIFICATION
                } else {
                    GeofenceNotificationAction.SHOW_CHECK_IN_REMINDER
                }
            }

            "EXIT" -> {
                if (hasActiveSession && hasEnteredActiveSessionArea) {
                    GeofenceNotificationAction.SHOW_EXIT_WARNING
                } else {
                    GeofenceNotificationAction.NO_NOTIFICATION
                }
            }

            else -> GeofenceNotificationAction.NO_NOTIFICATION
        }
    }
}
