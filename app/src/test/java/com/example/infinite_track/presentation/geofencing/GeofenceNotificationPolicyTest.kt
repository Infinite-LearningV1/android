package com.example.infinite_track.presentation.geofencing

import org.junit.Assert.assertEquals
import org.junit.Test

class GeofenceNotificationPolicyTest {

    @Test
    fun noActiveSession_enter_returnsShowCheckInReminder() {
        val action = GeofenceNotificationPolicy.decide(
            eventType = "ENTER",
            hasActiveSession = false,
            hasEnteredActiveSessionArea = false
        )

        assertEquals(GeofenceNotificationAction.SHOW_CHECK_IN_REMINDER, action)
    }

    @Test
    fun noActiveSession_exit_returnsNoNotification() {
        val action = GeofenceNotificationPolicy.decide(
            eventType = "EXIT",
            hasActiveSession = false,
            hasEnteredActiveSessionArea = false
        )

        assertEquals(GeofenceNotificationAction.NO_NOTIFICATION, action)
    }

    @Test
    fun activeSession_enter_returnsNoNotification() {
        val action = GeofenceNotificationPolicy.decide(
            eventType = "ENTER",
            hasActiveSession = true,
            hasEnteredActiveSessionArea = false
        )

        assertEquals(GeofenceNotificationAction.NO_NOTIFICATION, action)
    }

    @Test
    fun activeSession_exit_afterEnteringArea_returnsShowExitWarning() {
        val action = GeofenceNotificationPolicy.decide(
            eventType = "EXIT",
            hasActiveSession = true,
            hasEnteredActiveSessionArea = true
        )

        assertEquals(GeofenceNotificationAction.SHOW_EXIT_WARNING, action)
    }

    @Test
    fun activeSession_exit_withoutEnteringArea_returnsNoNotification() {
        val action = GeofenceNotificationPolicy.decide(
            eventType = "EXIT",
            hasActiveSession = true,
            hasEnteredActiveSessionArea = false
        )

        assertEquals(GeofenceNotificationAction.NO_NOTIFICATION, action)
    }

    @Test
    fun noActiveSession_lowercaseEnter_returnsShowCheckInReminder() {
        val action = GeofenceNotificationPolicy.decide(
            eventType = "enter",
            hasActiveSession = false,
            hasEnteredActiveSessionArea = false
        )

        assertEquals(GeofenceNotificationAction.SHOW_CHECK_IN_REMINDER, action)
    }

    @Test
    fun activeSession_lowercaseExit_afterEnteringArea_returnsShowExitWarning() {
        val action = GeofenceNotificationPolicy.decide(
            eventType = "exit",
            hasActiveSession = true,
            hasEnteredActiveSessionArea = true
        )

        assertEquals(GeofenceNotificationAction.SHOW_EXIT_WARNING, action)
    }
}
