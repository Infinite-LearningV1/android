package com.example.infinite_track.presentation.screen.attendance

import com.example.infinite_track.presentation.screen.attendance.face.LivenessState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FaceVerificationResultContractTest {

    @Test
    fun fromSavedState_mapsKnownValuesAndRejectsUnknown() {
        assertEquals(
            FaceVerificationResult.SUCCESS,
            FaceVerificationResult.fromSavedState(FaceVerificationResult.SUCCESS.savedStateValue)
        )
        assertEquals(
            FaceVerificationResult.FAILED,
            FaceVerificationResult.fromSavedState(FaceVerificationResult.FAILED.savedStateValue)
        )
        assertEquals(
            FaceVerificationResult.TIMEOUT,
            FaceVerificationResult.fromSavedState(FaceVerificationResult.TIMEOUT.savedStateValue)
        )
        assertEquals(
            FaceVerificationResult.CANCELLED,
            FaceVerificationResult.fromSavedState(FaceVerificationResult.CANCELLED.savedStateValue)
        )
        assertNull(FaceVerificationResult.fromSavedState("unexpected"))
    }

    @Test
    fun fromScannerExitState_mapsScannerStatesToExplicitContractResults() {
        assertEquals(
            FaceVerificationResult.SUCCESS,
            FaceVerificationResult.fromScannerExitState(LivenessState.SUCCESS)
        )
        assertEquals(
            FaceVerificationResult.FAILED,
            FaceVerificationResult.fromScannerExitState(LivenessState.FAILURE)
        )
        assertEquals(
            FaceVerificationResult.TIMEOUT,
            FaceVerificationResult.fromScannerExitState(LivenessState.TIMEOUT)
        )
        assertEquals(
            FaceVerificationResult.CANCELLED,
            FaceVerificationResult.fromScannerExitState(LivenessState.IDLE)
        )
        assertEquals(
            FaceVerificationResult.CANCELLED,
            FaceVerificationResult.fromScannerExitState(LivenessState.VERIFYING_FACE)
        )
    }

    @Test
    fun contractFlags_reflectAttendanceAndScannerSemantics() {
        assertTrue(FaceVerificationResult.SUCCESS.submitsAttendance)
        assertTrue(FaceVerificationResult.SUCCESS.returnsImmediatelyToAttendance)
        assertFalse(FaceVerificationResult.SUCCESS.allowsRetryOnScanner)
        assertNull(FaceVerificationResult.SUCCESS.attendanceErrorMessage)

        assertFalse(FaceVerificationResult.FAILED.submitsAttendance)
        assertFalse(FaceVerificationResult.FAILED.returnsImmediatelyToAttendance)
        assertTrue(FaceVerificationResult.FAILED.allowsRetryOnScanner)
        assertEquals(
            "Verifikasi wajah gagal. Silakan coba lagi.",
            FaceVerificationResult.FAILED.attendanceErrorMessage
        )

        assertFalse(FaceVerificationResult.TIMEOUT.submitsAttendance)
        assertFalse(FaceVerificationResult.TIMEOUT.returnsImmediatelyToAttendance)
        assertTrue(FaceVerificationResult.TIMEOUT.allowsRetryOnScanner)
        assertEquals(
            "Waktu verifikasi wajah habis. Silakan coba lagi.",
            FaceVerificationResult.TIMEOUT.attendanceErrorMessage
        )

        assertFalse(FaceVerificationResult.CANCELLED.submitsAttendance)
        assertFalse(FaceVerificationResult.CANCELLED.returnsImmediatelyToAttendance)
        assertFalse(FaceVerificationResult.CANCELLED.allowsRetryOnScanner)
        assertNull(FaceVerificationResult.CANCELLED.attendanceErrorMessage)
    }
}
