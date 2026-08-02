package com.example.infinite_track.presentation.screen.attendance.preparation

import com.example.infinite_track.domain.model.attendance.WorkMode
import com.example.infinite_track.presentation.design.tokens.InfiniteSemantic

data class AttendancePreparationUiModel(
    val modeOptions: List<WorkModeOptionUiModel>,
    val targetSummary: TargetLocationSummaryUiModel?,
    val statusMessage: String,
    val primaryAction: AttendancePreparationPrimaryAction,
    val primaryActionLabel: String,
    val isPrimaryActionEnabled: Boolean
)

data class WorkModeOptionUiModel(
    val mode: WorkMode,
    val title: String,
    val supportingText: String,
    val isSelected: Boolean
)

data class TargetLocationSummaryUiModel(
    val displayName: String,
    val sourceLabel: String,
    val radiusText: String,
    val distanceText: String?,
    val rangeText: String?,
    val rangeSemantic: InfiniteSemantic? = null,
    val bookingStatusText: String? = null,
    val bookingDateText: String? = null
)

enum class AttendancePreparationPrimaryAction {
    WAIT,
    CONTINUE_TO_FACE_VERIFICATION,
    SUBMIT_ATTENDANCE,
    REFRESH_STATUS,
    REFRESH_PROFILE,
    REFRESH_LOCATION,
    FOCUS_TARGET,
    OPEN_WFA_REQUESTS,
    CONTACT_ADMIN
}
