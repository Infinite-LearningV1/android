package com.example.infinite_track.data.soucre.local.preferences

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AttendancePreferenceNotificationStateTest {

    @Test
    fun clearNotificationSessionState_keepsAttendanceId_andResetsNotificationFlags() = runBlocking {
        val attendancePreference = AttendancePreference(ApplicationProvider.getApplicationContext())

        attendancePreference.clearActiveAttendanceId()
        attendancePreference.clearNotificationSessionState()

        attendancePreference.saveActiveAttendanceId(123)
        attendancePreference.setUserInsideGeofence(true)
        attendancePreference.setHasEnteredActiveSessionArea(true)

        attendancePreference.clearNotificationSessionState()

        assertEquals(123, attendancePreference.getActiveAttendanceId().first())
        assertEquals(false, attendancePreference.isUserInsideGeofence().first())
        assertEquals(false, attendancePreference.hasEnteredActiveSessionArea().first())

        attendancePreference.clearActiveAttendanceId()
        attendancePreference.clearNotificationSessionState()
    }

    @Test
    fun getLastGeofenceParams_preservesFloatRadius() = runBlocking {
        val attendancePreference = AttendancePreference(ApplicationProvider.getApplicationContext())

        attendancePreference.clearLastGeofenceParams()
        attendancePreference.saveLastGeofenceParams("office", -0.845f.toDouble(), 119.891f.toDouble(), 123.75f)

        val params = attendancePreference.getLastGeofenceParams().first()!!

        assertEquals("office", params.first)
        assertEquals(123.75f, params.third, 0.0001f)

        attendancePreference.clearLastGeofenceParams()
    }

    @Test
    fun addReminderGeofences_replacesExistingEntryWithSameId() = runBlocking {
        val attendancePreference = AttendancePreference(ApplicationProvider.getApplicationContext())

        attendancePreference.clearReminderGeofences()
        attendancePreference.addReminderGeofences(
            listOf(ReminderGeofence("reminder:office", -0.845, 119.891, 100f))
        )
        attendancePreference.addReminderGeofences(
            listOf(ReminderGeofence("reminder:office", -0.846, 119.892, 120.5f))
        )

        val reminders = attendancePreference.getReminderGeofences().first()

        assertEquals(1, reminders.size)
        assertEquals("reminder:office", reminders[0].id)
        assertEquals(-0.846, reminders[0].latitude, 0.0001)
        assertEquals(119.892, reminders[0].longitude, 0.0001)
        assertEquals(120.5f, reminders[0].radiusMeters, 0.0001f)

        attendancePreference.clearReminderGeofences()
    }
}
