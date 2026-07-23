package com.example.infinite_track.presentation.screen.attendance.preparation

import com.example.infinite_track.presentation.screen.attendance.AttendanceActionIntent
import com.example.infinite_track.presentation.screen.attendance.AttendanceActionState
import com.example.infinite_track.presentation.screen.attendance.ctaLabel
import com.example.infinite_track.presentation.screen.attendance.isCtaEnabled

/**
 * Resolves the single bottom-sheet CTA from attendance lifecycle state and preparation state.
 * Checkout deliberately bypasses check-in preparation because the attendance session already
 * owns its authoritative target.
 */
object AttendancePrimaryActionUiCombiner {
    fun combine(
        preparation: AttendancePreparationUiModel,
        actionState: AttendanceActionState
    ): AttendancePreparationUiModel = when (actionState) {
        is AttendanceActionState.Ready -> when {
            actionState.intent == AttendanceActionIntent.CHECK_OUT ->
                preparation.withAttendanceAction(actionState)
            preparation.primaryAction ==
                AttendancePreparationPrimaryAction.CONTINUE_TO_FACE_VERIFICATION ->
                preparation.withAttendanceAction(actionState)
            else -> preparation
        }

        is AttendanceActionState.Blocked -> if (preparation.primaryAction.isRecoveryAction()) {
            preparation
        } else {
            preparation.withDisabledAction(actionState)
        }

        is AttendanceActionState.RetryableFailure -> when {
            actionState.intent != null -> preparation.withAttendanceAction(actionState)
            preparation.primaryAction.isRecoveryAction() -> preparation
            else -> preparation.withDisabledAction(actionState)
        }

        AttendanceActionState.Loading,
        is AttendanceActionState.VerifyingFace,
        is AttendanceActionState.Submitting,
        is AttendanceActionState.Success,
        AttendanceActionState.Completed -> preparation.withDisabledAction(actionState)
    }

    private fun AttendancePreparationUiModel.withAttendanceAction(
        actionState: AttendanceActionState
    ): AttendancePreparationUiModel = copy(
        primaryAction = AttendancePreparationPrimaryAction.SUBMIT_ATTENDANCE,
        primaryActionLabel = actionState.ctaLabel,
        isPrimaryActionEnabled = actionState.isCtaEnabled
    )

    private fun AttendancePreparationUiModel.withDisabledAction(
        actionState: AttendanceActionState
    ): AttendancePreparationUiModel = copy(
        primaryAction = AttendancePreparationPrimaryAction.WAIT,
        primaryActionLabel = actionState.ctaLabel,
        isPrimaryActionEnabled = false
    )

    private fun AttendancePreparationPrimaryAction.isRecoveryAction(): Boolean = when (this) {
        AttendancePreparationPrimaryAction.REFRESH_STATUS,
        AttendancePreparationPrimaryAction.REFRESH_PROFILE,
        AttendancePreparationPrimaryAction.REFRESH_LOCATION,
        AttendancePreparationPrimaryAction.FOCUS_TARGET,
        AttendancePreparationPrimaryAction.OPEN_WFA_BOOKING,
        AttendancePreparationPrimaryAction.OPEN_WFA_REQUESTS,
        AttendancePreparationPrimaryAction.CONTACT_ADMIN,
        AttendancePreparationPrimaryAction.RETRY_WFA_DISCOVERY -> true
        AttendancePreparationPrimaryAction.WAIT,
        AttendancePreparationPrimaryAction.CONTINUE_TO_FACE_VERIFICATION,
        AttendancePreparationPrimaryAction.SUBMIT_ATTENDANCE -> false
    }
}
