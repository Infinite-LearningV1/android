package com.example.infinite_track.domain.use_case.attendance.permission

import com.example.infinite_track.domain.model.attendance.permission.AttendanceAccess
import com.example.infinite_track.domain.model.attendance.permission.AttendanceAccessReadiness
import com.example.infinite_track.domain.model.attendance.permission.AttendanceAccessRecovery
import com.example.infinite_track.domain.model.attendance.permission.AttendanceAccessStatus
import com.example.infinite_track.domain.model.attendance.permission.AttendancePermissionFailure
import com.example.infinite_track.domain.model.attendance.permission.AttendancePermissionInspectionIssue
import com.example.infinite_track.domain.model.attendance.permission.AttendancePermissionNextAction
import com.example.infinite_track.domain.model.attendance.permission.AttendancePermissionReadiness
import org.junit.Assert.assertEquals
import org.junit.Test

class ResolveNextAttendancePermissionActionUseCaseTest {

    private val useCase = ResolveNextAttendancePermissionActionUseCase()

    @Test
    fun primary_resolvesRequiredAccessesInApprovedOrderBeforeContinuing() {
        assertEquals(
            AttendancePermissionNextAction.RequestPermission(AttendanceAccess.PRECISE_LOCATION),
            useCase.forPrimary(readiness(precise = actionRequired()))
        )
        assertEquals(
            AttendancePermissionNextAction.RequestPermission(AttendanceAccess.CAMERA),
            useCase.forPrimary(readiness(camera = actionRequired()))
        )
        assertEquals(
            AttendancePermissionNextAction.OpenDeviceLocationSettings,
            useCase.forPrimary(readiness(deviceLocation = deviceLocationDisabled()))
        )
        assertEquals(
            AttendancePermissionNextAction.ContinueToWorkMode,
            useCase.forPrimary(readiness())
        )
    }

    @Test
    fun primary_prioritizesPreciseLocationOverCameraWhenBothAreIncomplete() {
        val readiness = readiness(
            precise = actionRequired(),
            camera = actionRequired()
        )

        assertEquals(
            AttendancePermissionNextAction.RequestPermission(AttendanceAccess.PRECISE_LOCATION),
            useCase.forPrimary(readiness)
        )
    }

    @Test
    fun primary_prioritizesCameraOverDeviceLocationWhenBothAreIncomplete() {
        val readiness = readiness(
            camera = actionRequired(),
            deviceLocation = deviceLocationDisabled()
        )

        assertEquals(
            AttendancePermissionNextAction.RequestPermission(AttendanceAccess.CAMERA),
            useCase.forPrimary(readiness)
        )
    }

    @Test
    fun primary_retriesRefreshWhenRequiredAccessHasNoRecoveryAction() {
        val readiness = readiness(
            camera = entry(AttendanceAccessStatus.ACTION_REQUIRED)
        )

        assertEquals(AttendancePermissionNextAction.RetryRefresh, useCase.forPrimary(readiness))
    }

    @Test
    fun primary_retriesRefreshWhenRequiredInspectionFails() {
        val readiness = readiness(
            inspectionIssues = listOf(
                AttendancePermissionInspectionIssue(
                    failure = AttendancePermissionFailure.PLATFORM_STATE_UNAVAILABLE,
                    affectedAccesses = setOf(AttendanceAccess.CAMERA)
                )
            )
        )

        assertEquals(AttendancePermissionNextAction.RetryRefresh, useCase.forPrimary(readiness))
    }

    @Test
    fun primary_ignoresOptionalOnlyInspectionIssueAndOptionalDegradation() {
        val readiness = readiness(
            notification = degraded(),
            inspectionIssues = listOf(
                AttendancePermissionInspectionIssue(
                    failure = AttendancePermissionFailure.UNKNOWN,
                    affectedAccesses = setOf(AttendanceAccess.NOTIFICATION)
                )
            )
        )

        assertEquals(AttendancePermissionNextAction.ContinueToWorkMode, useCase.forPrimary(readiness))
    }

    @Test
    fun access_opensApplicationSettingsForPermanentDenial() {
        val readiness = readiness(
            camera = entry(
                status = AttendanceAccessStatus.PERMANENTLY_DENIED,
                recovery = AttendanceAccessRecovery.OPEN_APPLICATION_SETTINGS
            )
        )

        assertEquals(
            AttendancePermissionNextAction.OpenApplicationSettings(AttendanceAccess.CAMERA),
            useCase.forAccess(readiness, AttendanceAccess.CAMERA)
        )
    }

    @Test
    fun access_returnsNoneForUnsupportedOptionalAccess() {
        val readiness = readiness(
            notification = entry(AttendanceAccessStatus.NOT_REQUIRED_ON_DEVICE)
        )

        assertEquals(
            AttendancePermissionNextAction.None,
            useCase.forAccess(readiness, AttendanceAccess.NOTIFICATION)
        )
    }

    private fun readiness(
        precise: AttendanceAccessReadiness = ready(),
        camera: AttendanceAccessReadiness = ready(),
        deviceLocation: AttendanceAccessReadiness = ready(),
        notification: AttendanceAccessReadiness = ready(),
        background: AttendanceAccessReadiness = ready(),
        inspectionIssues: List<AttendancePermissionInspectionIssue> = emptyList()
    ) = AttendancePermissionReadiness(
        entries = listOf(
            precise.copy(access = AttendanceAccess.PRECISE_LOCATION),
            camera.copy(access = AttendanceAccess.CAMERA),
            deviceLocation.copy(access = AttendanceAccess.DEVICE_LOCATION),
            notification.copy(access = AttendanceAccess.NOTIFICATION),
            background.copy(access = AttendanceAccess.BACKGROUND_LOCATION)
        ),
        inspectionIssues = inspectionIssues
    )

    private fun ready() = entry(AttendanceAccessStatus.READY)

    private fun actionRequired() = entry(
        status = AttendanceAccessStatus.ACTION_REQUIRED,
        recovery = AttendanceAccessRecovery.REQUEST_PERMISSION
    )

    private fun deviceLocationDisabled() = entry(
        status = AttendanceAccessStatus.DEVICE_LOCATION_DISABLED,
        recovery = AttendanceAccessRecovery.OPEN_DEVICE_LOCATION_SETTINGS
    )

    private fun degraded() = entry(AttendanceAccessStatus.DEGRADED)

    private fun entry(
        status: AttendanceAccessStatus,
        recovery: AttendanceAccessRecovery = AttendanceAccessRecovery.NONE
    ) = AttendanceAccessReadiness(
        access = AttendanceAccess.PRECISE_LOCATION,
        status = status,
        recovery = recovery
    )
}
