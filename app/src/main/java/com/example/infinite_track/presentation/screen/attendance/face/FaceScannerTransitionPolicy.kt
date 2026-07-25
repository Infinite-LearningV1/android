package com.example.infinite_track.presentation.screen.attendance.face

/**
 * Pure state transitions for detector callbacks and delayed liveness work.
 */
object FaceScannerTransitionPolicy {

    fun onMultipleFaces(state: FaceScannerState): FaceScannerState = state.copy(
        livenessState = LivenessState.WAITING_FOR_LIVENESS,
        instructionText = "Pastikan hanya ada satu wajah di dalam frame",
        errorMessage = null,
        failureReason = FaceVerificationFailureReason.MULTIPLE_FACES,
        readyToVerify = false
    )

    fun onSingleFaceRecovered(state: FaceScannerState): FaceScannerState =
        if (state.failureReason == FaceVerificationFailureReason.MULTIPLE_FACES) {
            state.copy(
                livenessState = LivenessState.WAITING_FOR_LIVENESS,
                failureReason = null,
                errorMessage = null
            )
        } else {
            state
        }

    fun onTimeout(state: FaceScannerState, timeoutSeconds: Int): FaceScannerState = state.copy(
        livenessState = LivenessState.TIMEOUT,
        isProcessing = false,
        instructionText = "Waktu habis",
        errorMessage = "Tidak dapat mendeteksi wajah dalam waktu $timeoutSeconds detik. Silakan coba lagi.",
        failureReason = null,
        showCountdown = false,
        timeRemaining = 0
    )

    fun canAcceptDetection(state: FaceScannerState): Boolean =
        !state.isProcessing && state.livenessState !in TERMINAL_STATES

    fun canAdvanceHold(
        state: FaceScannerState,
        expectedChallenge: LivenessChallenge
    ): Boolean =
        state.livenessState == LivenessState.LIVENESS_DETECTED &&
            state.currentChallenge == expectedChallenge &&
            state.failureReason != FaceVerificationFailureReason.MULTIPLE_FACES

    private val TERMINAL_STATES = setOf(
        LivenessState.SUCCESS,
        LivenessState.FAILURE,
        LivenessState.TIMEOUT
    )
}
