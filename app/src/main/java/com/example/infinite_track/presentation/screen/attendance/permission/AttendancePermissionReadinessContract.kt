package com.example.infinite_track.presentation.screen.attendance.permission

import com.example.infinite_track.domain.model.attendance.permission.AttendanceAccess
import com.example.infinite_track.domain.model.attendance.permission.AttendancePermissionRequestOutcome
import com.example.infinite_track.presentation.design.tokens.InfiniteSemantic

enum class PermissionIconKey { LOCATION, CAMERA, DEVICE_LOCATION, NOTIFICATION, BACKGROUND_LOCATION }

enum class AttendanceFeedbackDuration { SHORT, LONG }

enum class AttendanceSettingsDestination { APPLICATION, DEVICE_LOCATION }

enum class AttendancePermissionFeedbackAction { RETRY_REFRESH, OPEN_APPLICATION_SETTINGS }

enum class PermissionGuidanceAction { RETRY_REFRESH }

data class PermissionItemUiModel(
    val access: AttendanceAccess,
    val title: String,
    val supportingText: String,
    val requirementLabel: String,
    val statusLabel: String,
    val actionLabel: String?,
    val iconKey: PermissionIconKey,
    val semantic: InfiniteSemantic,
    val stateDescription: String,
    val isReady: Boolean
)

data class AttendancePermissionFeedback(
    val id: String,
    val message: String,
    val semantic: InfiniteSemantic,
    val duration: AttendanceFeedbackDuration,
    val action: AttendancePermissionFeedbackAction? = null,
    val actionLabel: String? = null
)

data class PermissionGuidanceUiModel(
    val title: String,
    val message: String,
    val semantic: InfiniteSemantic,
    val actionLabel: String? = null,
    val action: PermissionGuidanceAction? = null
)

sealed interface AttendancePermissionReadinessEvent {
    data object ScreenResumed : AttendancePermissionReadinessEvent
    data object PrimaryActionClicked : AttendancePermissionReadinessEvent
    data class PermissionItemClicked(val access: AttendanceAccess) : AttendancePermissionReadinessEvent
    data class PermissionResultReceived(
        val access: AttendanceAccess,
        val outcome: AttendancePermissionRequestOutcome
    ) : AttendancePermissionReadinessEvent
    data object ReturnedFromSettings : AttendancePermissionReadinessEvent
    data object RetryRefresh : AttendancePermissionReadinessEvent
    data class SettingsLaunchFailed(
        val destination: AttendanceSettingsDestination
    ) : AttendancePermissionReadinessEvent
    data class SnackbarFinished(val feedbackId: String) : AttendancePermissionReadinessEvent
    data class SnackbarActionClicked(
        val feedbackId: String,
        val action: AttendancePermissionFeedbackAction
    ) : AttendancePermissionReadinessEvent
    data object NavigationHandled : AttendancePermissionReadinessEvent
}

sealed interface AttendancePermissionReadinessEffect {
    data object RequestPreciseLocation : AttendancePermissionReadinessEffect
    data object RequestCamera : AttendancePermissionReadinessEffect
    data object RequestNotification : AttendancePermissionReadinessEffect
    data object RequestBackgroundLocation : AttendancePermissionReadinessEffect
    data class OpenApplicationSettings(
        val access: AttendanceAccess
    ) : AttendancePermissionReadinessEffect
    data object OpenDeviceLocationSettings : AttendancePermissionReadinessEffect
    data object NavigateToWorkMode : AttendancePermissionReadinessEffect
    data class ShowSnackbar(
        val feedback: AttendancePermissionFeedback
    ) : AttendancePermissionReadinessEffect
}
