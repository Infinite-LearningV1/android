package com.example.infinite_track.domain.model.geofence

import com.example.infinite_track.domain.model.attendance.WorkMode
import com.example.infinite_track.domain.model.location.DistanceMeters
import com.example.infinite_track.domain.model.location.GeoCoordinate
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class GeofenceRuntimeContractTest {

    @Test
    fun `active mode requires a positive attendance id`() {
        assertFailsWith<IllegalArgumentException> {
            GeofenceRuntimeMode.ActiveMonitoring(
                effectiveDate = LocalDate.parse("2026-07-24"),
                attendanceId = 0,
                target = activeTarget()
            )
        }
    }

    @Test
    fun `candidate logical id cannot be blank`() {
        assertFailsWith<IllegalArgumentException> {
            reminderCandidate(logicalId = " ")
        }
    }

    @Test
    fun `notification denial is independent from registration readiness`() {
        val readiness = GeofenceRuntimeReadiness(
            registration = RegistrationReadiness.Ready,
            notification = NotificationReadiness.PERMISSION_REQUIRED
        )
        assertEquals(RegistrationReadiness.Ready, readiness.registration)
        assertEquals(NotificationReadiness.PERMISSION_REQUIRED, readiness.notification)
    }

    private fun activeTarget() = ActiveMonitoringTarget(
        identity = GeofenceTargetIdentity(null, "user-home:7"),
        mode = WorkMode.WFH,
        label = "Home",
        coordinate = GeoCoordinate(-0.9, 119.8),
        radius = DistanceMeters(100.0)
    )

    private fun reminderCandidate(logicalId: String) = ReminderGeofenceCandidate(
        logicalId = logicalId,
        identity = GeofenceTargetIdentity(null, "user-home:7"),
        mode = WorkMode.WFH,
        label = "Home",
        coordinate = GeoCoordinate(-0.9, 119.8),
        radius = DistanceMeters(100.0),
        source = ReminderCandidateSource.USER_PROFILE
    )

    private inline fun <reified T : Throwable> assertFailsWith(block: () -> Unit): T {
        try {
            block()
        } catch (throwable: Throwable) {
            if (throwable is T) return throwable
            throw AssertionError(
                "Expected ${T::class.java.name}, but was ${throwable::class.java.name}",
                throwable
            )
        }
        throw AssertionError("Expected ${T::class.java.name} to be thrown")
    }
}
