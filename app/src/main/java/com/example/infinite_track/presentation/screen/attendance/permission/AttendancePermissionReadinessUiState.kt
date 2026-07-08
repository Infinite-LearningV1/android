package com.example.infinite_track.presentation.screen.attendance.permission

enum class AttendancePermissionAction {
    REQUEST_FOREGROUND_LOCATION,
    REQUEST_CAMERA,
    OPEN_DEVICE_LOCATION_SETTINGS,
    REQUEST_NOTIFICATION,
    REQUEST_BACKGROUND_LOCATION,
    CONTINUE_TO_WORK_MODE
}

data class AttendancePermissionReadinessUiState(
    val foregroundLocationGranted: Boolean,
    val cameraGranted: Boolean,
    val deviceLocationEnabled: Boolean,
    val notificationGranted: Boolean,
    val backgroundLocationGranted: Boolean,
    val requiredReadyCount: Int,
    val requiredTotalCount: Int = REQUIRED_ATTENDANCE_ACCESS_COUNT,
    val canContinueToWorkMode: Boolean,
    val nextRequiredAction: AttendancePermissionAction?,
    val warningMessage: String? = null
) {
    val progressCopy: String
        get() = "$requiredReadyCount/$requiredTotalCount akses wajib siap"
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
    val canContinue = requiredReadyCount == REQUIRED_ATTENDANCE_ACCESS_COUNT
    val nextRequiredAction = when {
        !foregroundLocationGranted -> AttendancePermissionAction.REQUEST_FOREGROUND_LOCATION
        !cameraGranted -> AttendancePermissionAction.REQUEST_CAMERA
        !deviceLocationEnabled -> AttendancePermissionAction.OPEN_DEVICE_LOCATION_SETTINGS
        else -> AttendancePermissionAction.CONTINUE_TO_WORK_MODE
    }
    val warningMessage = when {
        !notificationGranted && !backgroundLocationGranted ->
            "Pengingat notifikasi dan pemantauan geofence berjalan terbatas. Absensi manual tetap bisa dilanjutkan."
        !notificationGranted ->
            "Pengingat notifikasi belum aktif. Absensi manual tetap bisa dilanjutkan."
        !backgroundLocationGranted ->
            "Pemantauan geofence latar belakang belum aktif. Absensi manual tetap bisa dilanjutkan."
        else -> null
    }

    return AttendancePermissionReadinessUiState(
        foregroundLocationGranted = foregroundLocationGranted,
        cameraGranted = cameraGranted,
        deviceLocationEnabled = deviceLocationEnabled,
        notificationGranted = notificationGranted,
        backgroundLocationGranted = backgroundLocationGranted,
        requiredReadyCount = requiredReadyCount,
        canContinueToWorkMode = canContinue,
        nextRequiredAction = nextRequiredAction,
        warningMessage = warningMessage
    )
}
