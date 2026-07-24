package com.example.infinite_track.presentation.map.adapter

import com.example.infinite_track.presentation.map.model.AttendanceMapCameraMoveOrigin
import com.google.maps.android.compose.CameraMoveStartedReason
import org.junit.Assert.assertEquals
import org.junit.Test

class AttendanceMapCameraOriginTest {

    @Test
    fun `programmatic start remains bound to its terminal idle`() {
        val tracker = AttendanceMapCameraMovementOriginTracker()

        assertEquals(
            null,
            tracker.onMovementChanged(
                isMoving = true,
                startedOrigin = AttendanceMapCameraMoveOrigin.PROGRAMMATIC
            )
        )
        assertEquals(
            AttendanceMapCameraMoveOrigin.PROGRAMMATIC,
            tracker.onMovementChanged(
                isMoving = false,
                startedOrigin = AttendanceMapCameraMoveOrigin.PROGRAMMATIC
            )
        )
    }

    @Test
    fun `gesture start remains bound to its terminal idle`() {
        val tracker = AttendanceMapCameraMovementOriginTracker()
        tracker.onMovementChanged(
            isMoving = true,
            startedOrigin = AttendanceMapCameraMoveOrigin.USER_GESTURE
        )

        assertEquals(
            AttendanceMapCameraMoveOrigin.USER_GESTURE,
            tracker.onMovementChanged(
                isMoving = false,
                startedOrigin = AttendanceMapCameraMoveOrigin.UNKNOWN
            )
        )
    }

    @Test
    fun `Google map gesture is bound to user origin`() {
        assertEquals(
            AttendanceMapCameraMoveOrigin.USER_GESTURE,
            CameraMoveStartedReason.GESTURE.toAttendanceMapCameraMoveOrigin()
        )
    }

    @Test
    fun `Google API and developer animations are bound to programmatic origin`() {
        listOf(
            CameraMoveStartedReason.API_ANIMATION,
            CameraMoveStartedReason.DEVELOPER_ANIMATION
        ).forEach { reason ->
            assertEquals(
                AttendanceMapCameraMoveOrigin.PROGRAMMATIC,
                reason.toAttendanceMapCameraMoveOrigin()
            )
        }
    }

    @Test
    fun `initial and unknown camera states cannot masquerade as a gesture`() {
        listOf(
            CameraMoveStartedReason.NO_MOVEMENT_YET,
            CameraMoveStartedReason.UNKNOWN
        ).forEach { reason ->
            assertEquals(
                AttendanceMapCameraMoveOrigin.UNKNOWN,
                reason.toAttendanceMapCameraMoveOrigin()
            )
        }
    }
}
