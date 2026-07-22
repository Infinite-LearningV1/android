package com.example.infinite_track.presentation.screen.attendance.permission

data class AttendancePermissionReadinessUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val requiredItems: List<PermissionItemUiModel> = emptyList(),
    val optionalItems: List<PermissionItemUiModel> = emptyList(),
    val requiredReadyCount: Int = 0,
    val requiredTotalCount: Int = REQUIRED_ATTENDANCE_ACCESS_COUNT,
    val canContinue: Boolean = false,
    val primaryActionLabel: String = "Memuat status akses",
    val primaryActionEnabled: Boolean = false,
    val contextualGuidance: PermissionGuidanceUiModel? = null,
    val recoverableFailure: PermissionGuidanceUiModel? = null
)

private const val REQUIRED_ATTENDANCE_ACCESS_COUNT = 3
