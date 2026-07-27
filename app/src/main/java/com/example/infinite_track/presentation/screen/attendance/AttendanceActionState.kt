package com.example.infinite_track.presentation.screen.attendance

typealias AttendanceActionIntent =
    com.example.infinite_track.domain.model.attendance.AttendanceActionIntent

enum class AttendanceBlockReason {
    PERMISSION_REQUIRED,
    TARGET_LOCATION_UNAVAILABLE,
    WFH_LOCATION_MISSING,
    WFA_BOOKING_REQUIRED,
    ALREADY_COMPLETED,
    SERVER_RESTRICTION,
    UNKNOWN
}

sealed interface AttendanceActionState {
    data object Loading : AttendanceActionState

    data class Ready(
        val intent: AttendanceActionIntent,
        val label: String
    ) : AttendanceActionState

    data class Blocked(
        val reason: AttendanceBlockReason,
        val title: String,
        val message: String
    ) : AttendanceActionState

    data class VerifyingFace(
        val intent: AttendanceActionIntent
    ) : AttendanceActionState

    data class Submitting(
        val intent: AttendanceActionIntent,
        val message: String
    ) : AttendanceActionState

    data class Success(
        val intent: AttendanceActionIntent,
        val message: String
    ) : AttendanceActionState

    data class RetryableFailure(
        val intent: AttendanceActionIntent?,
        val title: String,
        val message: String
    ) : AttendanceActionState

    data object Completed : AttendanceActionState
}

val AttendanceActionState.ctaLabel: String
    get() = when (this) {
        AttendanceActionState.Loading -> "Memuat status absensi..."
        is AttendanceActionState.Ready -> label
        is AttendanceActionState.Blocked -> title
        is AttendanceActionState.VerifyingFace -> when (intent) {
            AttendanceActionIntent.CHECK_IN -> "Membuka verifikasi wajah..."
            AttendanceActionIntent.CHECK_OUT -> "Membuka verifikasi wajah..."
        }
        is AttendanceActionState.Submitting -> message
        is AttendanceActionState.Success -> when (intent) {
            AttendanceActionIntent.CHECK_IN -> "Check-in berhasil"
            AttendanceActionIntent.CHECK_OUT -> "Check-out berhasil"
        }
        is AttendanceActionState.RetryableFailure -> "Coba lagi"
        AttendanceActionState.Completed -> "Anda sudah absen hari ini"
    }

val AttendanceActionState.isCtaEnabled: Boolean
    get() = this is AttendanceActionState.Ready ||
        (this is AttendanceActionState.RetryableFailure && intent != null)

val AttendanceActionState.legacyIsCheckInMode: Boolean
    get() = when (this) {
        is AttendanceActionState.Ready -> intent == AttendanceActionIntent.CHECK_IN
        is AttendanceActionState.VerifyingFace -> intent == AttendanceActionIntent.CHECK_IN
        is AttendanceActionState.Submitting -> intent == AttendanceActionIntent.CHECK_IN
        is AttendanceActionState.Success -> intent == AttendanceActionIntent.CHECK_IN
        is AttendanceActionState.RetryableFailure -> intent != AttendanceActionIntent.CHECK_OUT
        AttendanceActionState.Completed -> false
        else -> true
    }

fun AttendanceActionIntent.readyLabel(): String = when (this) {
    AttendanceActionIntent.CHECK_IN -> "Check-in di sini"
    AttendanceActionIntent.CHECK_OUT -> "Check-out di sini"
}

fun AttendanceActionIntent.submittingMessage(): String = when (this) {
    AttendanceActionIntent.CHECK_IN -> "Mengirim check-in..."
    AttendanceActionIntent.CHECK_OUT -> "Mengirim check-out..."
}
