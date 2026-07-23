package com.example.infinite_track.presentation.screen.attendance

import com.example.infinite_track.domain.model.attendance.AttendancePreparationBlockReason
import com.example.infinite_track.domain.model.attendance.AttendancePreparationEligibility

object AttendanceActionResolver {
    fun resolve(state: AttendanceScreenState): AttendanceActionState {
        val todayStatus = state.todayStatus ?: return AttendanceActionState.Loading
        val intent = when {
            todayStatus.checkedInAt == null && todayStatus.canCheckIn -> AttendanceActionIntent.CHECK_IN
            todayStatus.checkedInAt != null && todayStatus.canCheckOut -> AttendanceActionIntent.CHECK_OUT
            else -> return AttendanceActionState.Completed
        }

        if (intent == AttendanceActionIntent.CHECK_IN) {
            when (val eligibility = state.preparation.eligibility) {
                AttendancePreparationEligibility.Resolving -> return AttendanceActionState.Loading
                is AttendancePreparationEligibility.Blocked -> return eligibility.toActionState()
                is AttendancePreparationEligibility.Ready -> Unit
            }
        }

        return AttendanceActionState.Ready(
            intent = intent,
            label = intent.readyLabel()
        )
    }

    private fun AttendancePreparationEligibility.Blocked.toActionState(): AttendanceActionState.Blocked {
        val presentation = when (reason) {
            AttendancePreparationBlockReason.WFH_PROFILE_CONTRACT,
            AttendancePreparationBlockReason.PROFILE_REFRESH_FAILED -> BlockPresentation(
                reason = AttendanceBlockReason.WFH_LOCATION_MISSING,
                title = "Lokasi WFH belum tersedia",
                message = "Lokasi WFH dari profil belum tersedia. Muat ulang atau hubungi admin."
            )

            AttendancePreparationBlockReason.WFA_NOT_REQUESTED,
            AttendancePreparationBlockReason.WFA_PENDING,
            AttendancePreparationBlockReason.WFA_REJECTED,
            AttendancePreparationBlockReason.WFA_APPROVAL_MISSING_FOR_DATE,
            AttendancePreparationBlockReason.BOOKING_REFRESH_FAILED -> BlockPresentation(
                reason = AttendanceBlockReason.WFA_BOOKING_REQUIRED,
                title = "Booking WFA belum disetujui",
                message = "Booking WFA yang disetujui diperlukan sebelum absen dari lokasi WFA."
            )

            AttendancePreparationBlockReason.WFO_NOT_ASSIGNED,
            AttendancePreparationBlockReason.CURRENT_LOCATION_UNAVAILABLE,
            AttendancePreparationBlockReason.CURRENT_LOCATION_STALE,
            AttendancePreparationBlockReason.OUTSIDE_TARGET_RANGE,
            AttendancePreparationBlockReason.STATUS_REFRESH_FAILED,
            AttendancePreparationBlockReason.TARGET_CONTRACT_INVALID -> BlockPresentation(
                reason = AttendanceBlockReason.TARGET_LOCATION_UNAVAILABLE,
                title = "Lokasi attendance belum siap",
                message = "Lokasi attendance belum memenuhi syarat. Ikuti tindakan pemulihan yang tersedia."
            )
        }
        return AttendanceActionState.Blocked(
            reason = presentation.reason,
            title = presentation.title,
            message = presentation.message
        )
    }

    private data class BlockPresentation(
        val reason: AttendanceBlockReason,
        val title: String,
        val message: String
    )
}
