package com.example.infinite_track.presentation.screen.attendance.face

/**
 * Terminal outcome of a face match attempt, mapping VerifyFaceUseCase's `Result<Boolean>`
 * to a scanner [LivenessState] plus an optional differentiated [FaceVerificationFailureReason].
 *
 * Pure and free of Android types so it can be unit-tested on the JVM.
 */
data class FaceMatchOutcome(
    val livenessState: LivenessState,
    val failureReason: FaceVerificationFailureReason?
)

object FaceOutcomeMapper {
    fun fromMatch(result: Result<Boolean>): FaceMatchOutcome = result.fold(
        onSuccess = { isMatch ->
            if (isMatch) {
                FaceMatchOutcome(LivenessState.SUCCESS, null)
            } else {
                FaceMatchOutcome(
                    LivenessState.FAILURE,
                    FaceVerificationFailureReason.NOT_MATCHED
                )
            }
        },
        onFailure = {
            FaceMatchOutcome(
                LivenessState.FAILURE,
                FaceVerificationFailureReason.TECHNICAL_FAILURE
            )
        }
    )
}

/**
 * Guidance based on how many faces are currently in frame. More than one face pauses
 * verification and asks the user to keep a single face in frame. Pure/JVM-testable.
 */
object FaceCountGuidance {
    fun reasonFor(faceCount: Int): FaceVerificationFailureReason? =
        if (faceCount > 1) FaceVerificationFailureReason.MULTIPLE_FACES else null
}
