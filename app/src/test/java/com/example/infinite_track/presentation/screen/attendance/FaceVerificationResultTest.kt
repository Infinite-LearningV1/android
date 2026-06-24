package com.example.infinite_track.presentation.screen.attendance

import com.example.infinite_track.presentation.screen.attendance.face.LivenessState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FaceVerificationResultTest {

    @Test
    fun `saved state parsing matches supported values`() {
        assertEquals(FaceVerificationResult.SUCCESS, FaceVerificationResult.fromSavedState("success"))
        assertEquals(FaceVerificationResult.FAILED, FaceVerificationResult.fromSavedState("failed"))
        assertEquals(FaceVerificationResult.TIMEOUT, FaceVerificationResult.fromSavedState("timeout"))
        assertEquals(FaceVerificationResult.CANCELLED, FaceVerificationResult.fromSavedState("cancelled"))
        assertNull(FaceVerificationResult.fromSavedState("unknown"))
    }

    @Test
    fun `scanner exit states map to expected attendance behavior`() {
        val success = FaceVerificationResult.fromScannerExitState(LivenessState.SUCCESS)
        assertTrue(success.submitsAttendance)
        assertTrue(success.returnsImmediatelyToAttendance)
        assertFalse(success.allowsRetryOnScanner)
        assertNull(success.attendanceErrorMessage)

        val failure = FaceVerificationResult.fromScannerExitState(LivenessState.FAILURE)
        assertFalse(failure.submitsAttendance)
        assertFalse(failure.returnsImmediatelyToAttendance)
        assertTrue(failure.allowsRetryOnScanner)
        assertEquals(
            "Verifikasi wajah gagal. Silakan coba lagi.",
            failure.attendanceErrorMessage
        )

        val timeout = FaceVerificationResult.fromScannerExitState(LivenessState.TIMEOUT)
        assertFalse(timeout.submitsAttendance)
        assertFalse(timeout.returnsImmediatelyToAttendance)
        assertTrue(timeout.allowsRetryOnScanner)
        assertEquals(
            "Waktu verifikasi wajah habis. Silakan coba lagi.",
            timeout.attendanceErrorMessage
        )

        val cancelled = FaceVerificationResult.fromScannerExitState(LivenessState.IDLE)
        assertFalse(cancelled.submitsAttendance)
        assertFalse(cancelled.returnsImmediatelyToAttendance)
        assertFalse(cancelled.allowsRetryOnScanner)
        assertNull(cancelled.attendanceErrorMessage)
    }
}
