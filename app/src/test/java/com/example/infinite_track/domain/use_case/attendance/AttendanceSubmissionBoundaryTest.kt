package com.example.infinite_track.domain.use_case.attendance

import org.junit.Assert.assertFalse
import org.junit.Test

class AttendanceSubmissionBoundaryTest {

    @Test
    fun `attendance submission use case has no geofence or preference dependency`() {
        listOf(SubmitAttendanceUseCase::class.java).forEach { type ->
            val dependencies = type.declaredConstructors.single().parameterTypes.map { it.name }

            assertFalse(dependencies.any { it.contains("Geofence") })
            assertFalse(dependencies.any { it.contains("AttendancePreference") })
            assertFalse(dependencies.any { it.startsWith("android.") })
        }
    }
}
