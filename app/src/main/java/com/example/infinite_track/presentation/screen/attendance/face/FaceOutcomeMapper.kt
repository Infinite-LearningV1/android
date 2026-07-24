package com.example.infinite_track.presentation.screen.attendance.face

import com.example.infinite_track.domain.use_case.auth.VerifyFaceMatch

/**
 * Terminal outcome of a face match attempt, mapping VerifyFaceUseCase's `Result<VerifyFaceMatch>`
 * to a scanner [LivenessState], an optional differentiated [FaceVerificationFailureReason], and
 * the similarity/threshold to surface in the diagnostics card.
 *
 * Pure and free of Android types so it can be unit-tested on the JVM.
 */
data class FaceMatchOutcome(
    val livenessState: LivenessState,
    val failureReason: FaceVerificationFailureReason?,
    val similarity: Float? = null,
    val threshold: Float? = null
)

object FaceOutcomeMapper {
    fun fromMatch(result: Result<VerifyFaceMatch>): FaceMatchOutcome = result.fold(
        onSuccess = { match ->
            if (match.isMatch) {
                FaceMatchOutcome(
                    livenessState = LivenessState.SUCCESS,
                    failureReason = null,
                    similarity = match.similarity,
                    threshold = match.threshold
                )
            } else {
                FaceMatchOutcome(
                    livenessState = LivenessState.FAILURE,
                    failureReason = FaceVerificationFailureReason.NOT_MATCHED,
                    similarity = match.similarity,
                    threshold = match.threshold
                )
            }
        },
        onFailure = {
            FaceMatchOutcome(
                livenessState = LivenessState.FAILURE,
                failureReason = FaceVerificationFailureReason.TECHNICAL_FAILURE
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
