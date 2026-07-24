package com.example.infinite_track.domain.use_case.attendance

import org.junit.Assert.assertFalse
import org.junit.Test

class AttendanceSubmissionBoundaryTest {

    @Test
    fun `attendance submission use cases have no geofence or preference dependency`() {
        listOf(CheckInUseCase::class.java, CheckOutUseCase::class.java).forEach { type ->
            val dependencies = type.declaredConstructors.single().parameterTypes.map { it.name }

            assertFalse(dependencies.any { it.contains("GeofenceManager") })
            assertFalse(dependencies.any { it.contains("AttendancePreference") })
            assertFalse(dependencies.any { it.startsWith("android.") })
        }
    }
}
