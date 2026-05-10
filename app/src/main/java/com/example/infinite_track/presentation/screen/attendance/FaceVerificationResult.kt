package com.example.infinite_track.presentation.screen.attendance

import com.example.infinite_track.presentation.screen.attendance.face.LivenessState

const val FACE_VERIFICATION_RESULT_KEY = "face_verification_result"

enum class FaceVerificationResult(
    val savedStateValue: String,
    val submitsAttendance: Boolean,
    val returnsImmediatelyToAttendance: Boolean,
    val allowsRetryOnScanner: Boolean,
    val attendanceErrorMessage: String?
) {
    SUCCESS(
        savedStateValue = "success",
        submitsAttendance = true,
        returnsImmediatelyToAttendance = true,
        allowsRetryOnScanner = false,
        attendanceErrorMessage = null
    ),
    FAILED(
        savedStateValue = "failed",
        submitsAttendance = false,
        returnsImmediatelyToAttendance = false,
        allowsRetryOnScanner = true,
        attendanceErrorMessage = "Verifikasi wajah gagal. Silakan coba lagi."
    ),
    TIMEOUT(
        savedStateValue = "timeout",
        submitsAttendance = false,
        returnsImmediatelyToAttendance = false,
        allowsRetryOnScanner = true,
        attendanceErrorMessage = "Waktu verifikasi wajah habis. Silakan coba lagi."
    ),
    CANCELLED(
        savedStateValue = "cancelled",
        submitsAttendance = false,
        returnsImmediatelyToAttendance = false,
        allowsRetryOnScanner = false,
        attendanceErrorMessage = null
    );

    companion object {
        fun fromSavedState(value: String): FaceVerificationResult? {
            return entries.firstOrNull { it.savedStateValue == value }
        }

        fun fromScannerExitState(state: LivenessState): FaceVerificationResult {
            return when (state) {
                LivenessState.SUCCESS -> SUCCESS
                LivenessState.FAILURE -> FAILED
                LivenessState.TIMEOUT -> TIMEOUT
                else -> CANCELLED
            }
        }
    }
}
