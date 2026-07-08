package com.example.infinite_track.domain.use_case.attendance

import com.example.infinite_track.domain.model.attendance.SelectedTargetLocation
import com.example.infinite_track.domain.model.attendance.WorkMode
import com.example.infinite_track.domain.model.attendance.WorkModeEligibility
import com.example.infinite_track.domain.model.attendance.WorkModeRecoveryAction
import com.example.infinite_track.domain.use_case.booking.ResolveTodayApprovedWfaBookingIdUseCase
import javax.inject.Inject

class EvaluateWorkModeEligibilityUseCase @Inject constructor(
    private val resolveTodayApprovedWfaBookingIdUseCase: ResolveTodayApprovedWfaBookingIdUseCase
) {
    suspend operator fun invoke(
        mode: WorkMode,
        selectedTargetLocation: SelectedTargetLocation,
        todayDate: String?
    ): WorkModeEligibility {
        if (!selectedTargetLocation.isAvailable) {
            return WorkModeEligibility(
                mode = mode,
                canContinueToFaceVerification = false,
                blockingReason = selectedTargetLocation.unavailableReason ?: missingTargetReason(mode),
                recoveryAction = missingTargetRecovery(mode)
            )
        }

        if (mode != WorkMode.WFA) {
            return WorkModeEligibility(
                mode = mode,
                canContinueToFaceVerification = true
            )
        }

        if (todayDate.isNullOrBlank()) {
            return WorkModeEligibility(
                mode = mode,
                canContinueToFaceVerification = false,
                blockingReason = "Tanggal attendance hari ini tidak tersedia untuk memvalidasi booking WFA.",
                recoveryAction = WorkModeRecoveryAction.VIEW_WFA_REQUESTS
            )
        }

        return resolveTodayApprovedWfaBookingIdUseCase(todayDate)
            .fold(
                onSuccess = { bookingId ->
                    WorkModeEligibility(
                        mode = mode,
                        canContinueToFaceVerification = true,
                        approvedWfaBookingId = bookingId
                    )
                },
                onFailure = {
                    WorkModeEligibility(
                        mode = mode,
                        canContinueToFaceVerification = false,
                        blockingReason = "Booking WFA yang sudah disetujui untuk hari ini belum tersedia. Ajukan atau cek request WFA terlebih dahulu.",
                        recoveryAction = WorkModeRecoveryAction.VIEW_WFA_REQUESTS
                    )
                }
            )
    }

    private fun missingTargetReason(mode: WorkMode): String {
        return when (mode) {
            WorkMode.WFO -> "Lokasi WFO belum tersedia. Coba muat ulang status attendance terlebih dahulu."
            WorkMode.WFH -> "Lokasi WFH belum tersedia. Pilih WFO atau perbarui lokasi WFH terlebih dahulu."
            WorkMode.WFA -> "Pilih lokasi WFA terlebih dahulu."
        }
    }

    private fun missingTargetRecovery(mode: WorkMode): WorkModeRecoveryAction {
        return when (mode) {
            WorkMode.WFO -> WorkModeRecoveryAction.CHOOSE_WFO
            WorkMode.WFH -> WorkModeRecoveryAction.UPDATE_WFH_LOCATION
            WorkMode.WFA -> WorkModeRecoveryAction.SELECT_WFA_LOCATION
        }
    }
}
