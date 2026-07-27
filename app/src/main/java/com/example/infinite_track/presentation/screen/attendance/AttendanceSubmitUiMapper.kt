package com.example.infinite_track.presentation.screen.attendance

import com.example.infinite_track.domain.model.attendance.AttendanceActionIntent
import com.example.infinite_track.domain.model.attendance.AttendanceSubmitFailure

data class AttendanceSubmitUiFailure(
    val title: String,
    val message: String
)

/** Maps typed Layer 5 failures to localized user copy. Owns UI wording only. */
object AttendanceSubmitUiMapper {

    fun map(
        intent: AttendanceActionIntent,
        failure: AttendanceSubmitFailure
    ): AttendanceSubmitUiFailure {
        val title = when (intent) {
            AttendanceActionIntent.CHECK_IN -> "Check-in gagal"
            AttendanceActionIntent.CHECK_OUT -> "Check-out gagal"
        }
        val message = when (failure) {
            AttendanceSubmitFailure.CurrentLocationUnavailable ->
                "Lokasi belum dapat dibaca. Periksa GPS lalu coba lagi."
            AttendanceSubmitFailure.SessionUnavailable ->
                "Sesi Anda tidak tersedia. Silakan masuk ulang lalu coba lagi."
            AttendanceSubmitFailure.ActiveAttendanceUnavailable ->
                "Sesi absensi aktif tidak ditemukan. Muat ulang status absensi lalu coba lagi."
            AttendanceSubmitFailure.TargetModeMismatch ->
                "Target lokasi belum sesuai dengan mode kerja terpilih. Muat ulang lalu coba lagi."
            AttendanceSubmitFailure.WfaBookingRequired ->
                "Check-in WFA membutuhkan booking yang sudah disetujui."
            AttendanceSubmitFailure.DuplicateAttendance ->
                "Anda sudah melakukan check-in hari ini."
            AttendanceSubmitFailure.OutsideAllowedRadius ->
                "Anda berada di luar radius lokasi yang diizinkan."
            AttendanceSubmitFailure.WfaBookingRejected ->
                "Booking WFA Anda belum disetujui untuk hari ini."
            AttendanceSubmitFailure.AlreadyCheckedOut ->
                "Anda sudah melakukan check-out hari ini."
            AttendanceSubmitFailure.NetworkUnavailable ->
                "Koneksi internet bermasalah. Periksa jaringan lalu coba lagi."
            AttendanceSubmitFailure.ServerUnavailable ->
                "Server sedang bermasalah. Silakan coba lagi beberapa saat lagi."
            is AttendanceSubmitFailure.BackendRejected -> failure.safeReason
            AttendanceSubmitFailure.Unknown ->
                "Absensi belum berhasil. Silakan coba lagi."
        }
        return AttendanceSubmitUiFailure(title = title, message = message)
    }
}
