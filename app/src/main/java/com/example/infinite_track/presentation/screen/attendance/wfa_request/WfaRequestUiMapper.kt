package com.example.infinite_track.presentation.screen.attendance.wfa_request

import com.example.infinite_track.domain.model.booking.WfaRequestFailure

enum class WfaRequestFailureAction {
    EDIT,
    RETRY,
    BACK
}

data class WfaRequestUiFailure(
    val title: String,
    val message: String,
    val primaryAction: WfaRequestFailureAction
)

object WfaRequestUiMapper {
    fun map(failure: WfaRequestFailure): WfaRequestUiFailure = when (failure) {
        WfaRequestFailure.ConfigUnavailable -> WfaRequestUiFailure(
            title = "Form belum tersedia",
            message = "Konfigurasi WFA belum dapat dimuat. Silakan coba lagi.",
            primaryAction = WfaRequestFailureAction.RETRY
        )
        WfaRequestFailure.InvalidDate -> WfaRequestUiFailure(
            title = "Tanggal tidak valid",
            message = "Pilih tanggal lain sesuai kebijakan permintaan WFA.",
            primaryAction = WfaRequestFailureAction.EDIT
        )
        WfaRequestFailure.ReasonUnavailable -> WfaRequestUiFailure(
            title = "Alasan tidak tersedia",
            message = "Pilih kembali alasan permintaan yang masih tersedia.",
            primaryAction = WfaRequestFailureAction.EDIT
        )
        WfaRequestFailure.DuplicateRequest -> WfaRequestUiFailure(
            title = "Permintaan sudah ada",
            message = "Anda sudah memiliki permintaan WFA aktif pada tanggal tersebut.",
            primaryAction = WfaRequestFailureAction.EDIT
        )
        WfaRequestFailure.NetworkUnavailable -> WfaRequestUiFailure(
            title = "Koneksi bermasalah",
            message = "Periksa koneksi internet lalu coba kirim kembali.",
            primaryAction = WfaRequestFailureAction.RETRY
        )
        WfaRequestFailure.ServerUnavailable -> WfaRequestUiFailure(
            title = "Server belum tersedia",
            message = "Server sedang bermasalah. Silakan coba lagi beberapa saat lagi.",
            primaryAction = WfaRequestFailureAction.RETRY
        )
        is WfaRequestFailure.ValidationRejected -> WfaRequestUiFailure(
            title = "Periksa kembali data",
            message = "Beberapa data permintaan perlu diperbaiki sebelum dikirim.",
            primaryAction = WfaRequestFailureAction.EDIT
        )
        is WfaRequestFailure.BackendRejected -> WfaRequestUiFailure(
            title = "Permintaan ditolak",
            message = failure.safeMessage,
            primaryAction = WfaRequestFailureAction.EDIT
        )
        WfaRequestFailure.Unknown -> WfaRequestUiFailure(
            title = "Permintaan belum berhasil",
            message = "Terjadi kendala yang tidak dikenali. Silakan coba lagi.",
            primaryAction = WfaRequestFailureAction.RETRY
        )
    }
}
