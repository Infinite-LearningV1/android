package com.example.infinite_track.presentation.screen.attendance.preparation

import com.example.infinite_track.domain.model.attendance.WorkMode

data class AttendancePreparationUiModel(
    val modeOptions: List<WorkModeOptionUiModel>,
    val targetSummary: TargetLocationSummaryUiModel?,
    val statusMessage: String,
    val wfaDiscovery: WfaDiscoveryUiModel,
    val primaryAction: AttendancePreparationPrimaryAction,
    val primaryActionLabel: String,
    val isPrimaryActionEnabled: Boolean,
    val secondaryAction: AttendancePreparationSecondaryAction?,
    val secondaryActionLabel: String?
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
    val rangeText: String?
)

sealed interface WfaDiscoveryUiModel {
    data object Hidden : WfaDiscoveryUiModel
    data object Loading : WfaDiscoveryUiModel
    data class Empty(val message: String) : WfaDiscoveryUiModel
    data class Failure(val message: String, val retryable: Boolean) : WfaDiscoveryUiModel

    data class Content(
        val rows: List<WfaRecommendationUiModel>,
        val selectedKey: String?,
        val searchPreviewName: String?
    ) : WfaDiscoveryUiModel
}

data class WfaRecommendationUiModel(
    val stableKey: String,
    val name: String,
    val supportingText: String,
    val suitabilityText: String
)

fun WfaDiscoveryUiModel.Content.isRecommendationSelected(
    recommendation: WfaRecommendationUiModel
): Boolean = recommendation.stableKey == selectedKey

enum class AttendancePreparationPrimaryAction {
    WAIT,
    CONTINUE_TO_FACE_VERIFICATION,
    SUBMIT_ATTENDANCE,
    REFRESH_STATUS,
    REFRESH_PROFILE,
    REFRESH_LOCATION,
    FOCUS_TARGET,
    OPEN_WFA_BOOKING,
    OPEN_WFA_REQUESTS,
    CONTACT_ADMIN,
    RETRY_WFA_DISCOVERY
}

enum class AttendancePreparationSecondaryAction {
    SEARCH_WFA_LOCATION
}
