package com.example.infinite_track.presentation.screen.auth

import com.example.infinite_track.domain.model.auth.ReauthReason

data class ReauthUiCopy(
    val bannerMessage: String,
    val dialogTitle: String,
    val dialogMessage: String
)

fun ReauthReason.toReauthUiCopy(): ReauthUiCopy {
    return when (this) {
        ReauthReason.INACTIVITY_EXPIRED -> ReauthUiCopy(
            bannerMessage = "Sesi tidak aktif lebih dari 48 jam. Silakan login lagi.",
            dialogTitle = "Sesi Tidak Aktif",
            dialogMessage = "Sesi Anda tidak aktif lebih dari 48 jam. Silakan login kembali untuk melanjutkan."
        )

        ReauthReason.REFRESH_INVALID -> ReauthUiCopy(
            bannerMessage = "Sesi tidak valid lagi. Silakan login lagi.",
            dialogTitle = "Sesi Tidak Valid",
            dialogMessage = "Sesi Anda tidak valid lagi. Silakan login kembali untuk melanjutkan."
        )

        ReauthReason.REFRESH_REVOKED -> ReauthUiCopy(
            bannerMessage = "Sesi sudah berakhir. Silakan login lagi.",
            dialogTitle = "Sesi Berakhir",
            dialogMessage = "Sesi Anda sudah berakhir. Silakan login kembali untuk melanjutkan."
        )

        ReauthReason.NETWORK_OFFLINE_AT_REFRESH -> ReauthUiCopy(
            bannerMessage = "Tidak dapat memvalidasi sesi karena jaringan. Coba lagi setelah online.",
            dialogTitle = "Jaringan Tidak Tersedia",
            dialogMessage = "Aplikasi tidak dapat memvalidasi sesi karena jaringan sedang tidak tersedia. Coba lagi setelah online."
        )

        ReauthReason.UNKNOWN -> ReauthUiCopy(
            bannerMessage = "Sesi Anda telah berakhir. Silakan login kembali.",
            dialogTitle = "Sesi Berakhir",
            dialogMessage = "Sesi Anda telah berakhir. Silakan login kembali untuk melanjutkan."
        )
    }
}
