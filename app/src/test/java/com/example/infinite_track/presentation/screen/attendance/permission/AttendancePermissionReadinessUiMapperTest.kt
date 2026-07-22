package com.example.infinite_track.presentation.screen.attendance.permission

import com.example.infinite_track.domain.model.attendance.permission.AttendanceAccess
import com.example.infinite_track.domain.model.attendance.permission.AttendanceAccessReadiness
import com.example.infinite_track.domain.model.attendance.permission.AttendanceAccessReason
import com.example.infinite_track.domain.model.attendance.permission.AttendanceAccessRecovery
import com.example.infinite_track.domain.model.attendance.permission.AttendanceAccessStatus
import com.example.infinite_track.domain.model.attendance.permission.AttendancePermissionFailure
import com.example.infinite_track.domain.model.attendance.permission.AttendancePermissionInspectionIssue
import com.example.infinite_track.domain.model.attendance.permission.AttendancePermissionNextAction
import com.example.infinite_track.domain.model.attendance.permission.AttendancePermissionReadiness
import com.example.infinite_track.presentation.design.tokens.InfiniteSemantic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AttendancePermissionReadinessUiMapperTest {
    private val mapper = AttendancePermissionReadinessUiMapper()

    @Test
    fun `maps partial required progress and does not count optional entries`() {
        val state = mapper.map(
            readiness = readiness(
                precise = AttendanceAccessStatus.READY,
                camera = AttendanceAccessStatus.ACTION_REQUIRED,
                device = AttendanceAccessStatus.READY,
                notification = AttendanceAccessStatus.DEGRADED,
                background = AttendanceAccessStatus.DEGRADED
            ),
            nextAction = AttendancePermissionNextAction.RequestPermission(AttendanceAccess.CAMERA)
        )

        assertEquals(2, state.requiredReadyCount)
        assertEquals(3, state.requiredTotalCount)
        assertEquals(3, state.requiredItems.size)
        assertEquals(2, state.optionalItems.size)
        assertEquals("Lanjutkan Setup", state.primaryActionLabel)
        assertTrue(state.primaryActionEnabled)
        assertFalse(state.canContinue)
    }

    @Test
    fun `maps all required ready with optional degradation without blocking continuation`() {
        val state = mapper.map(
            readiness = readiness(
                precise = AttendanceAccessStatus.READY,
                camera = AttendanceAccessStatus.READY,
                device = AttendanceAccessStatus.READY,
                notification = AttendanceAccessStatus.DEGRADED,
                background = AttendanceAccessStatus.DEGRADED
            ),
            nextAction = AttendancePermissionNextAction.ContinueToWorkMode
        )

        assertTrue(state.canContinue)
        assertEquals("Lanjut ke Mode Kerja", state.primaryActionLabel)
        assertTrue(state.primaryActionEnabled)
        assertEquals("Opsional", state.optionalItems.first().requirementLabel)
        assertEquals(InfiniteSemantic.Warning, state.optionalItems.first().semantic)
    }

    @Test
    fun `maps approximate only location with precise recovery copy`() {
        val state = mapper.map(
            readiness = readiness(
                precise = AttendanceAccessStatus.ACTION_REQUIRED,
                preciseReason = AttendanceAccessReason.APPROXIMATE_LOCATION_ONLY
            ),
            nextAction = AttendancePermissionNextAction.RequestPermission(AttendanceAccess.PRECISE_LOCATION)
        )

        val item = state.requiredItems.first()
        assertEquals("Lokasi presisi", item.title)
        assertEquals("Lokasi perkiraan aktif. Izinkan lokasi presisi untuk absensi.", item.supportingText)
        assertEquals("Minta izin", item.actionLabel)
        assertEquals(PermissionIconKey.LOCATION, item.iconKey)
    }

    @Test
    fun `maps denied and permanently denied permission copy`() {
        val denied = mapper.map(
            readiness = readiness(camera = AttendanceAccessStatus.DENIED),
            nextAction = AttendancePermissionNextAction.RequestPermission(AttendanceAccess.CAMERA)
        ).requiredItems[1]
        val permanentlyDenied = mapper.map(
            readiness = readiness(
                camera = AttendanceAccessStatus.PERMANENTLY_DENIED,
                cameraRecovery = AttendanceAccessRecovery.OPEN_APPLICATION_SETTINGS
            ),
            nextAction = AttendancePermissionNextAction.OpenApplicationSettings(AttendanceAccess.CAMERA)
        ).requiredItems[1]

        assertEquals("Izin ditolak", denied.statusLabel)
        assertEquals("Minta izin", denied.actionLabel)
        assertEquals("Izin diblokir", permanentlyDenied.statusLabel)
        assertEquals("Buka pengaturan", permanentlyDenied.actionLabel)
    }

    @Test
    fun `maps unsupported optional access and gps disabled`() {
        val state = mapper.map(
            readiness = readiness(
                device = AttendanceAccessStatus.DEVICE_LOCATION_DISABLED,
                deviceRecovery = AttendanceAccessRecovery.OPEN_DEVICE_LOCATION_SETTINGS,
                notification = AttendanceAccessStatus.NOT_REQUIRED_ON_DEVICE
            ),
            nextAction = AttendancePermissionNextAction.OpenDeviceLocationSettings
        )

        assertEquals("GPS belum aktif", state.requiredItems[2].statusLabel)
        assertEquals("Buka pengaturan", state.requiredItems[2].actionLabel)
        assertEquals("Tidak diperlukan di perangkat ini", state.optionalItems[0].statusLabel)
        assertNull(state.optionalItems[0].actionLabel)
        assertEquals(PermissionIconKey.NOTIFICATION, state.optionalItems[0].iconKey)
    }

    @Test
    fun `maps required inspection issue to retry failure guidance`() {
        val state = mapper.map(
            readiness = readiness(
                issues = listOf(
                    AttendancePermissionInspectionIssue(
                        failure = AttendancePermissionFailure.PLATFORM_STATE_UNAVAILABLE,
                        affectedAccesses = setOf(AttendanceAccess.CAMERA)
                    )
                )
            ),
            nextAction = AttendancePermissionNextAction.RetryRefresh
        )

        assertEquals("Status akses belum dapat diperiksa", state.recoverableFailure?.title)
        assertEquals("Coba lagi", state.recoverableFailure?.actionLabel)
        assertEquals(PermissionGuidanceAction.RETRY_REFRESH, state.recoverableFailure?.action)
        assertNull(state.contextualGuidance)
    }

    @Test
    fun `maps optional only inspection issue as non blocking contextual guidance`() {
        val state = mapper.map(
            readiness = readiness(
                issues = listOf(
                    AttendancePermissionInspectionIssue(
                        failure = AttendancePermissionFailure.PLATFORM_STATE_UNAVAILABLE,
                        affectedAccesses = setOf(AttendanceAccess.NOTIFICATION)
                    )
                )
            ),
            nextAction = AttendancePermissionNextAction.ContinueToWorkMode
        )

        assertTrue(state.canContinue)
        assertNull(state.recoverableFailure)
        assertEquals("Pengingat opsional belum dapat diperiksa", state.contextualGuidance?.title)
        assertEquals(InfiniteSemantic.Warning, state.contextualGuidance?.semantic)
    }

    @Test
    fun `does not show optional guidance when a required inspection issue also blocks`() {
        val state = mapper.map(
            readiness = readiness(
                issues = listOf(
                    AttendancePermissionInspectionIssue(
                        failure = AttendancePermissionFailure.PLATFORM_STATE_UNAVAILABLE,
                        affectedAccesses = setOf(AttendanceAccess.CAMERA)
                    ),
                    AttendancePermissionInspectionIssue(
                        failure = AttendancePermissionFailure.PLATFORM_STATE_UNAVAILABLE,
                        affectedAccesses = setOf(AttendanceAccess.NOTIFICATION)
                    )
                )
            ),
            nextAction = AttendancePermissionNextAction.RetryRefresh
        )

        assertTrue(state.recoverableFailure != null)
        assertNull(state.contextualGuidance)
    }

    @Test
    fun `maps a stable icon key for every access`() {
        val state = mapper.map(
            readiness = readiness(),
            nextAction = AttendancePermissionNextAction.ContinueToWorkMode
        )

        assertEquals(
            mapOf(
                AttendanceAccess.PRECISE_LOCATION to PermissionIconKey.LOCATION,
                AttendanceAccess.CAMERA to PermissionIconKey.CAMERA,
                AttendanceAccess.DEVICE_LOCATION to PermissionIconKey.DEVICE_LOCATION,
                AttendanceAccess.NOTIFICATION to PermissionIconKey.NOTIFICATION,
                AttendanceAccess.BACKGROUND_LOCATION to PermissionIconKey.BACKGROUND_LOCATION
            ),
            (state.requiredItems + state.optionalItems).associate { it.access to it.iconKey }
        )
    }

    private fun readiness(
        precise: AttendanceAccessStatus = AttendanceAccessStatus.READY,
        preciseReason: AttendanceAccessReason = AttendanceAccessReason.NONE,
        camera: AttendanceAccessStatus = AttendanceAccessStatus.READY,
        cameraRecovery: AttendanceAccessRecovery = recoveryFor(camera),
        device: AttendanceAccessStatus = AttendanceAccessStatus.READY,
        deviceRecovery: AttendanceAccessRecovery = recoveryFor(device),
        notification: AttendanceAccessStatus = AttendanceAccessStatus.READY,
        background: AttendanceAccessStatus = AttendanceAccessStatus.READY,
        issues: List<AttendancePermissionInspectionIssue> = emptyList()
    ) = AttendancePermissionReadiness(
        entries = listOf(
            AttendanceAccessReadiness(AttendanceAccess.PRECISE_LOCATION, precise, preciseReason, recoveryFor(precise)),
            AttendanceAccessReadiness(AttendanceAccess.CAMERA, camera, recovery = cameraRecovery),
            AttendanceAccessReadiness(AttendanceAccess.DEVICE_LOCATION, device, recovery = deviceRecovery),
            AttendanceAccessReadiness(AttendanceAccess.NOTIFICATION, notification, recovery = recoveryFor(notification)),
            AttendanceAccessReadiness(AttendanceAccess.BACKGROUND_LOCATION, background, recovery = recoveryFor(background))
        ),
        inspectionIssues = issues
    )

    private fun recoveryFor(status: AttendanceAccessStatus) = when (status) {
        AttendanceAccessStatus.ACTION_REQUIRED,
        AttendanceAccessStatus.DENIED,
        AttendanceAccessStatus.DEGRADED -> AttendanceAccessRecovery.REQUEST_PERMISSION
        AttendanceAccessStatus.PERMANENTLY_DENIED -> AttendanceAccessRecovery.OPEN_APPLICATION_SETTINGS
        AttendanceAccessStatus.DEVICE_LOCATION_DISABLED -> AttendanceAccessRecovery.OPEN_DEVICE_LOCATION_SETTINGS
        AttendanceAccessStatus.READY,
        AttendanceAccessStatus.NOT_REQUIRED_ON_DEVICE -> AttendanceAccessRecovery.NONE
    }
}
