package com.example.infinite_track.presentation.screen.attendance

import com.example.infinite_track.domain.model.attendance.WorkMode
import com.example.infinite_track.domain.model.attendance.WorkModeRecoveryAction

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
        state.workModeEligibility?.let { eligibility ->
            if (!eligibility.canContinueToFaceVerification) {
                return AttendanceActionState.Blocked(
                    reason = eligibility.recoveryAction.toBlockReason(state.selectedWorkMode),
                    title = eligibility.recoveryAction.toBlockTitle(state.selectedWorkMode),
                    message = eligibility.blockingReason
                        ?: "Mode kerja belum memenuhi syarat untuk melanjutkan verifikasi wajah."
                )
            }
        }

        val selectedMode = state.selectedWorkMode
        return when (selectedMode) {
            WorkMode.WFH -> {
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

            WorkMode.WFA -> {
                if (state.selectedTargetLocation?.location == null) {
                    AttendanceActionState.Blocked(
                        reason = AttendanceBlockReason.WFA_BOOKING_REQUIRED,
                        title = "Booking WFA belum disetujui",
                        message = "Booking WFA yang disetujui diperlukan sebelum absen dari lokasi WFA."
                    )
                } else {
                    null
                }
            }

            WorkMode.WFO -> {
                if (state.selectedTargetLocation?.location == null && state.wfoLocation == null) {
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

    private fun WorkModeRecoveryAction?.toBlockReason(mode: WorkMode): AttendanceBlockReason {
        return when (this) {
            WorkModeRecoveryAction.UPDATE_WFH_LOCATION -> AttendanceBlockReason.WFH_LOCATION_MISSING
            WorkModeRecoveryAction.SELECT_WFA_LOCATION -> AttendanceBlockReason.TARGET_LOCATION_UNAVAILABLE
            WorkModeRecoveryAction.REQUEST_WFA_BOOKING,
            WorkModeRecoveryAction.VIEW_WFA_REQUESTS -> AttendanceBlockReason.WFA_BOOKING_REQUIRED
            WorkModeRecoveryAction.CHOOSE_WFO -> AttendanceBlockReason.SERVER_RESTRICTION
            null -> when (mode) {
                WorkMode.WFH -> AttendanceBlockReason.WFH_LOCATION_MISSING
                WorkMode.WFA -> AttendanceBlockReason.WFA_BOOKING_REQUIRED
                WorkMode.WFO -> AttendanceBlockReason.TARGET_LOCATION_UNAVAILABLE
            }
        }
    }

    private fun WorkModeRecoveryAction?.toBlockTitle(mode: WorkMode): String {
        return when (this) {
            WorkModeRecoveryAction.UPDATE_WFH_LOCATION -> "Lokasi WFH belum tersedia"
            WorkModeRecoveryAction.SELECT_WFA_LOCATION -> "Target location tidak tersedia"
            WorkModeRecoveryAction.REQUEST_WFA_BOOKING,
            WorkModeRecoveryAction.VIEW_WFA_REQUESTS -> "Booking WFA belum disetujui"
            WorkModeRecoveryAction.CHOOSE_WFO -> "Mode kerja belum tersedia"
            null -> when (mode) {
                WorkMode.WFH -> "Lokasi WFH belum tersedia"
                WorkMode.WFA -> "Booking WFA belum disetujui"
                WorkMode.WFO -> "Target location tidak tersedia"
            }
        }
    }
}
