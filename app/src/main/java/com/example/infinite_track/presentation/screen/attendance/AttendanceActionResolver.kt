package com.example.infinite_track.presentation.screen.attendance

object AttendanceActionResolver {
    fun resolve(state: AttendanceScreenState): AttendanceActionState {
        val todayStatus = state.todayStatus ?: return AttendanceActionState.Loading
        val intent = when {
            todayStatus.checkedInAt == null && todayStatus.canCheckIn -> AttendanceActionIntent.CHECK_IN
            todayStatus.checkedInAt != null && todayStatus.canCheckOut -> AttendanceActionIntent.CHECK_OUT
            else -> return AttendanceActionState.Completed
        }

        if (intent == AttendanceActionIntent.CHECK_IN) {
            val blockState = resolveLayerTwoBlocker(state)
            if (blockState != null) return blockState
        }

        return AttendanceActionState.Ready(
            intent = intent,
            label = intent.readyLabel()
        )
    }

    private fun resolveLayerTwoBlocker(
        state: AttendanceScreenState
    ): AttendanceActionState.Blocked? {
        val selectedMode = state.selectedWorkMode
        return when (selectedMode) {
            "Work From Home", "WFH" -> {
                if (state.wfhLocation == null) {
                    AttendanceActionState.Blocked(
                        reason = AttendanceBlockReason.WFH_LOCATION_MISSING,
                        title = "Lokasi WFH belum tersedia",
                        message = "Lengkapi lokasi WFH sebelum melanjutkan verifikasi wajah."
                    )
                } else {
                    null
                }
            }

            "WFA", "Work From Anywhere" -> {
                when {
                    state.selectedWfaLocation == null && state.targetLocation == null -> AttendanceActionState.Blocked(
                        reason = AttendanceBlockReason.TARGET_LOCATION_UNAVAILABLE,
                        title = "Target location tidak tersedia",
                        message = "Pilih lokasi WFA sebelum melanjutkan verifikasi wajah."
                    )
                    state.todayStatus?.todayDate.isNullOrBlank() -> AttendanceActionState.Blocked(
                        reason = AttendanceBlockReason.WFA_BOOKING_REQUIRED,
                        title = "Booking WFA belum disetujui",
                        message = "Booking WFA yang disetujui diperlukan sebelum absen dari lokasi WFA."
                    )
                    else -> null
                }
            }

            else -> {
                if (state.targetLocation == null && state.wfoLocation == null) {
                    AttendanceActionState.Blocked(
                        reason = AttendanceBlockReason.TARGET_LOCATION_UNAVAILABLE,
                        title = "Target location tidak tersedia",
                        message = "Lokasi kerja belum tersedia. Muat ulang status absensi atau coba lagi nanti."
                    )
                } else {
                    null
                }
            }
        }
    }
}
