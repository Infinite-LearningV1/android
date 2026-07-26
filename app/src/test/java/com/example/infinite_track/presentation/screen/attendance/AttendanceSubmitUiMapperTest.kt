package com.example.infinite_track.presentation.screen.attendance

import com.example.infinite_track.domain.model.attendance.AttendanceActionIntent
import com.example.infinite_track.domain.model.attendance.AttendanceSubmitFailure
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AttendanceSubmitUiMapperTest {

    @Test
    fun `title follows intent`() {
        assertEquals(
            "Check-in gagal",
            AttendanceSubmitUiMapper.map(
                AttendanceActionIntent.CHECK_IN,
                AttendanceSubmitFailure.Unknown
            ).title
        )
        assertEquals(
            "Check-out gagal",
            AttendanceSubmitUiMapper.map(
                AttendanceActionIntent.CHECK_OUT,
                AttendanceSubmitFailure.Unknown
            ).title
        )
    }

    @Test
    fun `backend rejected shows safe reason verbatim`() {
        val ui = AttendanceSubmitUiMapper.map(
            AttendanceActionIntent.CHECK_IN,
            AttendanceSubmitFailure.BackendRejected("Absensi ditolak kebijakan kantor")
        )
        assertEquals("Absensi ditolak kebijakan kantor", ui.message)
    }

    @Test
    fun `every failure type maps to non-blank copy and business rejections stay distinct`() {
        val failures = listOf(
            AttendanceSubmitFailure.CurrentLocationUnavailable,
            AttendanceSubmitFailure.SessionUnavailable,
            AttendanceSubmitFailure.ActiveAttendanceUnavailable,
            AttendanceSubmitFailure.TargetModeMismatch,
            AttendanceSubmitFailure.WfaBookingRequired,
            AttendanceSubmitFailure.DuplicateAttendance,
            AttendanceSubmitFailure.OutsideAllowedRadius,
            AttendanceSubmitFailure.WfaBookingRejected,
            AttendanceSubmitFailure.AlreadyCheckedOut,
            AttendanceSubmitFailure.NetworkUnavailable,
            AttendanceSubmitFailure.ServerUnavailable,
            AttendanceSubmitFailure.Unknown
        )
        val messages = failures.map { failure ->
            AttendanceSubmitUiMapper.map(AttendanceActionIntent.CHECK_IN, failure).message
        }

        messages.forEach { message -> assertTrue(message.isNotBlank()) }

        val genericMessage = AttendanceSubmitUiMapper
            .map(AttendanceActionIntent.CHECK_IN, AttendanceSubmitFailure.Unknown)
            .message
        assertEquals(1, messages.count { it == genericMessage })
    }
}
