package com.example.infinite_track.presentation.screen.attendance

import com.example.infinite_track.presentation.design.tokens.InfiniteSemantic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AttendanceTransientFeedbackTest {

    @Test
    fun checkInSuccess_isShortSuccessWithOptionalHomeAction() {
        val feedback = AttendanceTransientFeedbackFactory.create(
            id = 7L,
            kind = AttendanceTransientFeedbackKind.CHECK_IN_SUCCESS
        )

        assertEquals(7L, feedback.id)
        assertEquals("Check-in berhasil! Selamat bekerja hari ini.", feedback.message)
        assertEquals(InfiniteSemantic.Success, feedback.semantic)
        assertEquals(AttendanceTransientFeedbackDuration.SHORT, feedback.duration)
        assertEquals("BERANDA", feedback.actionLabel)
        assertEquals(AttendanceTransientFeedbackAction.NAVIGATE_HOME, feedback.action)
    }

    @Test
    fun checkOutSuccess_usesApprovedCopyAndSameTransientContract() {
        val feedback = AttendanceTransientFeedbackFactory.create(
            id = 8L,
            kind = AttendanceTransientFeedbackKind.CHECK_OUT_SUCCESS
        )

        assertEquals("Check-out berhasil! Terima kasih atas kerja keras Anda hari ini.", feedback.message)
        assertEquals(InfiniteSemantic.Success, feedback.semantic)
        assertEquals(AttendanceTransientFeedbackDuration.SHORT, feedback.duration)
        assertEquals("BERANDA", feedback.actionLabel)
        assertEquals(AttendanceTransientFeedbackAction.NAVIGATE_HOME, feedback.action)
    }

    @Test
    fun recoverableErrors_areLongErrorsWithoutActionsAndNeverExposeSentinel() {
        val sentinel = "SECRET-SERVER-ID-9384"
        val kinds = listOf(
            AttendanceTransientFeedbackKind.ATTENDANCE_ERROR,
            AttendanceTransientFeedbackKind.LOCATION_ERROR,
            AttendanceTransientFeedbackKind.FACE_FAILED,
            AttendanceTransientFeedbackKind.FACE_TIMEOUT,
            AttendanceTransientFeedbackKind.FACE_UNKNOWN
        )

        kinds.forEachIndexed { index, kind ->
            val feedback = AttendanceTransientFeedbackFactory.create(index.toLong(), kind)

            assertEquals(InfiniteSemantic.Error, feedback.semantic)
            assertEquals(AttendanceTransientFeedbackDuration.LONG, feedback.duration)
            assertEquals(null, feedback.actionLabel)
            assertEquals(null, feedback.action)
            assertFalse(feedback.message.contains(sentinel))
        }
    }

    @Test
    fun factoryUsesFixedIndonesianCopyForEveryRecoverableCategory() {
        assertEquals(
            "Absensi belum berhasil. Silakan coba lagi.",
            AttendanceTransientFeedbackFactory.create(
                1L,
                AttendanceTransientFeedbackKind.ATTENDANCE_ERROR
            ).message
        )
        assertEquals(
            "Lokasi belum dapat dibaca. Periksa GPS lalu coba lagi.",
            AttendanceTransientFeedbackFactory.create(
                2L,
                AttendanceTransientFeedbackKind.LOCATION_ERROR
            ).message
        )
        assertEquals(
            "Verifikasi wajah gagal. Silakan coba lagi.",
            AttendanceTransientFeedbackFactory.create(
                3L,
                AttendanceTransientFeedbackKind.FACE_FAILED
            ).message
        )
        assertEquals(
            "Waktu verifikasi wajah habis. Silakan coba lagi.",
            AttendanceTransientFeedbackFactory.create(
                4L,
                AttendanceTransientFeedbackKind.FACE_TIMEOUT
            ).message
        )
        assertEquals(
            "Hasil verifikasi wajah tidak dikenali. Silakan coba lagi.",
            AttendanceTransientFeedbackFactory.create(
                5L,
                AttendanceTransientFeedbackKind.FACE_UNKNOWN
            ).message
        )
    }
}
