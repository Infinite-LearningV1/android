package com.example.infinite_track.presentation.screen.attendance

const val FACE_VERIFICATION_RESULT_KEY = "face_verification_result"

enum class FaceVerificationResult(val savedStateValue: String) {
    SUCCESS("success"),
    FAILED("failed"),
    TIMEOUT("timeout"),
    CANCELLED("cancelled");

    companion object {
        fun fromSavedState(value: String): FaceVerificationResult? {
            return values().firstOrNull { it.savedStateValue == value }
        }
    }
}
