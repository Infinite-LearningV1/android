package com.example.infinite_track.presentation.screen.attendance.permission

import com.example.infinite_track.domain.model.attendance.permission.AttendanceAccess

enum class AttendancePermissionAction {
    REQUEST_FOREGROUND_LOCATION,
    REQUEST_CAMERA,
    OPEN_DEVICE_LOCATION_SETTINGS,
    REQUEST_NOTIFICATION,
    REQUEST_BACKGROUND_LOCATION,
    CONTINUE_TO_WORK_MODE
}

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
    val recoverableFailure: PermissionGuidanceUiModel? = null,
    private val legacyForegroundLocationGranted: Boolean? = null,
    private val legacyCameraGranted: Boolean? = null,
    private val legacyDeviceLocationEnabled: Boolean? = null,
    private val legacyNotificationGranted: Boolean? = null,
    private val legacyBackgroundLocationGranted: Boolean? = null
) {
    // LegacyPermissionReadinessProjection: delete after Task 8 migrates the old Screen.
    val foregroundLocationGranted: Boolean
        get() = legacyForegroundLocationGranted ?: itemReady(AttendanceAccess.PRECISE_LOCATION)
    val cameraGranted: Boolean
        get() = legacyCameraGranted ?: itemReady(AttendanceAccess.CAMERA)
    val deviceLocationEnabled: Boolean
        get() = legacyDeviceLocationEnabled ?: itemReady(AttendanceAccess.DEVICE_LOCATION)
    val notificationGranted: Boolean
        get() = legacyNotificationGranted ?: itemReady(AttendanceAccess.NOTIFICATION)
    val backgroundLocationGranted: Boolean
        get() = legacyBackgroundLocationGranted ?: itemReady(AttendanceAccess.BACKGROUND_LOCATION)
    val canContinueToWorkMode: Boolean
        get() = canContinue
    val nextRequiredAction: AttendancePermissionAction?
        get() = when {
            !foregroundLocationGranted -> AttendancePermissionAction.REQUEST_FOREGROUND_LOCATION
            !cameraGranted -> AttendancePermissionAction.REQUEST_CAMERA
            !deviceLocationEnabled -> AttendancePermissionAction.OPEN_DEVICE_LOCATION_SETTINGS
            else -> AttendancePermissionAction.CONTINUE_TO_WORK_MODE
        }
    val progressCopy: String
        get() = "$requiredReadyCount/$requiredTotalCount akses wajib siap"
    val warningMessage: String?
        get() = when {
            !notificationGranted && !backgroundLocationGranted ->
                "Pengingat notifikasi dan pemantauan geofence berjalan terbatas. Absensi manual tetap bisa dilanjutkan."
            !notificationGranted ->
                "Pengingat notifikasi belum aktif. Absensi manual tetap bisa dilanjutkan."
            !backgroundLocationGranted ->
                "Pemantauan geofence latar belakang belum aktif. Absensi manual tetap bisa dilanjutkan."
            else -> null
        }

    private fun itemReady(access: AttendanceAccess): Boolean =
        (requiredItems + optionalItems).firstOrNull { it.access == access }?.isReady ?: false
}

private const val REQUIRED_ATTENDANCE_ACCESS_COUNT = 3

fun toAttendancePermissionReadinessUiState(
    foregroundLocationGranted: Boolean,
    cameraGranted: Boolean,
    deviceLocationEnabled: Boolean,
    notificationGranted: Boolean,
    backgroundLocationGranted: Boolean
): AttendancePermissionReadinessUiState {
    val requiredReadyCount = listOf(
        foregroundLocationGranted,
        cameraGranted,
        deviceLocationEnabled
    ).count { it }
    return AttendancePermissionReadinessUiState(
        isLoading = false,
        requiredReadyCount = requiredReadyCount,
        canContinue = requiredReadyCount == REQUIRED_ATTENDANCE_ACCESS_COUNT,
        primaryActionLabel = if (requiredReadyCount == REQUIRED_ATTENDANCE_ACCESS_COUNT) {
            "Lanjut ke Mode Kerja"
        } else {
            "Lanjutkan Setup"
        },
        primaryActionEnabled = true,
        legacyForegroundLocationGranted = foregroundLocationGranted,
        legacyCameraGranted = cameraGranted,
        legacyDeviceLocationEnabled = deviceLocationEnabled,
        legacyNotificationGranted = notificationGranted,
        legacyBackgroundLocationGranted = backgroundLocationGranted
    )
}
