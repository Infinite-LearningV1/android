package com.example.infinite_track.presentation.screen.attendance

import androidx.compose.runtime.Immutable
import com.example.infinite_track.presentation.design.tokens.InfiniteSemantic

@Immutable
data class AttendanceTransientFeedback(
    val id: Long,
    val message: String,
    val semantic: InfiniteSemantic,
    val duration: AttendanceTransientFeedbackDuration,
    val actionLabel: String? = null,
    val action: AttendanceTransientFeedbackAction? = null
)

enum class AttendanceTransientFeedbackDuration { SHORT, LONG }

enum class AttendanceTransientFeedbackAction { NAVIGATE_HOME }

internal enum class AttendanceTransientFeedbackKind {
    CHECK_IN_SUCCESS,
    CHECK_OUT_SUCCESS,
    ATTENDANCE_ERROR,
    LOCATION_ERROR,
    FACE_FAILED,
    FACE_TIMEOUT,
    FACE_UNKNOWN
}

internal object AttendanceTransientFeedbackFactory {
    fun create(
        id: Long,
        kind: AttendanceTransientFeedbackKind
    ): AttendanceTransientFeedback = when (kind) {
        AttendanceTransientFeedbackKind.CHECK_IN_SUCCESS -> success(
            id = id,
            message = "Check-in berhasil! Selamat bekerja hari ini."
        )
        AttendanceTransientFeedbackKind.CHECK_OUT_SUCCESS -> success(
            id = id,
            message = "Check-out berhasil! Terima kasih atas kerja keras Anda hari ini."
        )
        AttendanceTransientFeedbackKind.ATTENDANCE_ERROR -> error(
            id = id,
            message = "Absensi belum berhasil. Silakan coba lagi."
        )
        AttendanceTransientFeedbackKind.LOCATION_ERROR -> error(
            id = id,
            message = "Lokasi belum dapat dibaca. Periksa GPS lalu coba lagi."
        )
        AttendanceTransientFeedbackKind.FACE_FAILED -> error(
            id = id,
            message = "Verifikasi wajah gagal. Silakan coba lagi."
        )
        AttendanceTransientFeedbackKind.FACE_TIMEOUT -> error(
            id = id,
            message = "Waktu verifikasi wajah habis. Silakan coba lagi."
        )
        AttendanceTransientFeedbackKind.FACE_UNKNOWN -> error(
            id = id,
            message = "Hasil verifikasi wajah tidak dikenali. Silakan coba lagi."
        )
    }

    private fun success(id: Long, message: String) = AttendanceTransientFeedback(
        id = id,
        message = message,
        semantic = InfiniteSemantic.Success,
        duration = AttendanceTransientFeedbackDuration.SHORT,
        actionLabel = "BERANDA",
        action = AttendanceTransientFeedbackAction.NAVIGATE_HOME
    )

    private fun error(id: Long, message: String) = AttendanceTransientFeedback(
        id = id,
        message = message,
        semantic = InfiniteSemantic.Error,
        duration = AttendanceTransientFeedbackDuration.LONG
    )
}
