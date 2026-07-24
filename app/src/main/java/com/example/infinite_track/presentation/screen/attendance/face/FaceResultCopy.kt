package com.example.infinite_track.presentation.screen.attendance.face

import androidx.annotation.StringRes
import com.example.infinite_track.R

/**
 * Semantic role of a result surface, used to pick tokens/icons without relying on colour alone.
 */
enum class FaceResultRole { SUCCESS, ERROR, WARNING }

data class FaceResultCopyModel(
    @StringRes val titleRes: Int,
    @StringRes val messageRes: Int,
    val role: FaceResultRole,
    val retryable: Boolean
)

/**
 * Pure mapping from a verification outcome to display copy + semantic role. Kept free of
 * Android/Compose runtime types so it can be unit-tested on the JVM.
 */
object FaceResultCopy {
    fun success() = FaceResultCopyModel(
        titleRes = R.string.face_result_success_title,
        messageRes = R.string.face_result_success_message,
        role = FaceResultRole.SUCCESS,
        retryable = false
    )

    fun forFailure(reason: FaceVerificationFailureReason): FaceResultCopyModel = when (reason) {
        FaceVerificationFailureReason.NO_FACE -> FaceResultCopyModel(
            titleRes = R.string.face_result_no_face_title,
            messageRes = R.string.face_result_no_face_message,
            role = FaceResultRole.WARNING,
            retryable = true
        )
        FaceVerificationFailureReason.MULTIPLE_FACES -> FaceResultCopyModel(
            titleRes = R.string.face_result_multiple_faces_title,
            messageRes = R.string.face_result_multiple_faces_message,
            role = FaceResultRole.WARNING,
            retryable = true
        )
        FaceVerificationFailureReason.LOW_LIGHT -> FaceResultCopyModel(
            titleRes = R.string.face_result_low_light_title,
            messageRes = R.string.face_result_low_light_message,
            role = FaceResultRole.WARNING,
            retryable = true
        )
        FaceVerificationFailureReason.NOT_MATCHED -> FaceResultCopyModel(
            titleRes = R.string.face_result_not_matched_title,
            messageRes = R.string.face_result_not_matched_message,
            role = FaceResultRole.ERROR,
            retryable = true
        )
        FaceVerificationFailureReason.TECHNICAL_FAILURE -> FaceResultCopyModel(
            titleRes = R.string.face_result_technical_title,
            messageRes = R.string.face_result_technical_message,
            role = FaceResultRole.ERROR,
            retryable = true
        )
    }
}
