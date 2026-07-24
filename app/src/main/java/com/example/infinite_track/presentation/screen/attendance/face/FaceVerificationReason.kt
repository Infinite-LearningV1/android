package com.example.infinite_track.presentation.screen.attendance.face

/**
 * Differentiated reasons a face verification attempt can fail.
 *
 * These live inside the scanner and drive user-facing guidance/result copy. They never
 * cross the navigation boundary to Attendance (see [FaceVerificationResult]).
 */
enum class FaceVerificationFailureReason {
    NO_FACE,
    MULTIPLE_FACES,
    LOW_LIGHT,
    NOT_MATCHED,
    TECHNICAL_FAILURE
}
