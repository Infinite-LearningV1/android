package com.example.infinite_track.presentation.screen.attendance.preparation

import com.example.infinite_track.domain.model.attendance.AttendancePreparationBlockReason
import com.example.infinite_track.domain.model.attendance.AttendancePreparationEligibility
import com.example.infinite_track.domain.model.attendance.AttendancePreparationRecovery
import com.example.infinite_track.domain.model.attendance.AuthoritativeTargetLocation
import com.example.infinite_track.domain.model.attendance.TargetLocationId
import com.example.infinite_track.domain.model.attendance.TargetLocationResolution
import com.example.infinite_track.domain.model.attendance.TargetLocationSource
import com.example.infinite_track.domain.model.attendance.TargetRangeStatus
import com.example.infinite_track.domain.model.attendance.WorkMode
import com.example.infinite_track.domain.model.location.DistanceMeters
import com.example.infinite_track.domain.model.location.GeoCoordinate
import com.example.infinite_track.domain.model.wfa.WfaRecommendation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AttendancePreparationUiMapperTest {

    @Test
    fun `mapper exposes exactly one selected mode and one primary action`() {
        val mapped = AttendancePreparationUiMapper.map(readyPreparation(WorkMode.WFO))

        assertEquals(3, mapped.modeOptions.size)
        assertEquals(listOf(WorkMode.WFO), mapped.modeOptions.filter { it.isSelected }.map { it.mode })
        assertEquals(
            AttendancePreparationPrimaryAction.CONTINUE_TO_FACE_VERIFICATION,
            mapped.primaryAction
        )
        assertEquals("Lanjut ke Verifikasi Wajah", mapped.primaryActionLabel)
        assertTrue(mapped.isPrimaryActionEnabled)
        assertNull(mapped.secondaryAction)
        assertNull(mapped.secondaryActionLabel)
    }

    @Test
    fun `WFA not requested maps to booking action and WFA-only search`() {
        val mapped = AttendancePreparationUiMapper.map(
            blockedPreparation(
                mode = WorkMode.WFA,
                reason = AttendancePreparationBlockReason.WFA_NOT_REQUESTED,
                recovery = AttendancePreparationRecovery.OPEN_WFA_BOOKING
            )
        )

        assertEquals(AttendancePreparationPrimaryAction.OPEN_WFA_BOOKING, mapped.primaryAction)
        assertEquals("Ajukan WFA", mapped.primaryActionLabel)
        assertTrue(mapped.statusMessage.contains("belum ada permintaan", ignoreCase = true))
        assertEquals(AttendancePreparationSecondaryAction.SEARCH_WFA_LOCATION, mapped.secondaryAction)
        assertEquals("Cari lokasi WFA", mapped.secondaryActionLabel)
    }

    @Test
    fun `typed blocking reasons map to Indonesian copy and recovery actions`() {
        val cases = listOf(
            Case(AttendancePreparationBlockReason.WFO_NOT_ASSIGNED, AttendancePreparationRecovery.REFRESH_STATUS, AttendancePreparationPrimaryAction.REFRESH_STATUS, "Muat ulang", "kantor"),
            Case(AttendancePreparationBlockReason.WFH_PROFILE_CONTRACT, AttendancePreparationRecovery.REFRESH_PROFILE, AttendancePreparationPrimaryAction.REFRESH_PROFILE, "Muat ulang", "profil"),
            Case(AttendancePreparationBlockReason.WFA_PENDING, AttendancePreparationRecovery.OPEN_WFA_REQUESTS, AttendancePreparationPrimaryAction.OPEN_WFA_REQUESTS, "Lihat status permintaan", "menunggu"),
            Case(AttendancePreparationBlockReason.WFA_REJECTED, AttendancePreparationRecovery.OPEN_WFA_REQUESTS, AttendancePreparationPrimaryAction.OPEN_WFA_REQUESTS, "Lihat status permintaan", "ditolak"),
            Case(AttendancePreparationBlockReason.WFA_APPROVAL_MISSING_FOR_DATE, AttendancePreparationRecovery.OPEN_WFA_REQUESTS, AttendancePreparationPrimaryAction.OPEN_WFA_REQUESTS, "Lihat status permintaan", "tanggal"),
            Case(AttendancePreparationBlockReason.CURRENT_LOCATION_UNAVAILABLE, AttendancePreparationRecovery.REFRESH_LOCATION, AttendancePreparationPrimaryAction.REFRESH_LOCATION, "Muat ulang lokasi", "belum tersedia"),
            Case(AttendancePreparationBlockReason.CURRENT_LOCATION_STALE, AttendancePreparationRecovery.REFRESH_LOCATION, AttendancePreparationPrimaryAction.REFRESH_LOCATION, "Muat ulang lokasi", "tidak terkini"),
            Case(AttendancePreparationBlockReason.OUTSIDE_TARGET_RANGE, AttendancePreparationRecovery.FOCUS_TARGET, AttendancePreparationPrimaryAction.FOCUS_TARGET, "Fokus ke lokasi target", "di luar jangkauan"),
            Case(AttendancePreparationBlockReason.STATUS_REFRESH_FAILED, AttendancePreparationRecovery.REFRESH_STATUS, AttendancePreparationPrimaryAction.REFRESH_STATUS, "Muat ulang", "status"),
            Case(AttendancePreparationBlockReason.PROFILE_REFRESH_FAILED, AttendancePreparationRecovery.REFRESH_PROFILE, AttendancePreparationPrimaryAction.REFRESH_PROFILE, "Muat ulang", "profil"),
            Case(AttendancePreparationBlockReason.BOOKING_REFRESH_FAILED, AttendancePreparationRecovery.OPEN_WFA_REQUESTS, AttendancePreparationPrimaryAction.OPEN_WFA_REQUESTS, "Lihat status permintaan", "permintaan"),
            Case(AttendancePreparationBlockReason.TARGET_CONTRACT_INVALID, AttendancePreparationRecovery.CONTACT_ADMIN, AttendancePreparationPrimaryAction.CONTACT_ADMIN, "Hubungi admin", "tidak valid")
        )

        cases.forEach { case ->
            val mapped = AttendancePreparationUiMapper.map(
                blockedPreparation(
                    mode = modeFor(case.reason),
                    reason = case.reason,
                    recovery = case.recovery
                )
            )

            assertEquals(case.action, mapped.primaryAction)
            assertEquals(case.label, mapped.primaryActionLabel)
            assertTrue(
                "Expected '${mapped.statusMessage}' to contain '${case.copyFragment}'",
                mapped.statusMessage.contains(case.copyFragment, ignoreCase = true)
            )
        }
    }

    @Test
    fun `recommendation failure becomes retry primary action without changing target`() {
        val preparation = readyPreparation(WorkMode.WFA).copy(
            wfaDiscovery = WfaDiscoveryState.Failure(retryable = true)
        )

        val mapped = AttendancePreparationUiMapper.map(preparation)

        assertEquals(AttendancePreparationPrimaryAction.RETRY_WFA_DISCOVERY, mapped.primaryAction)
        assertEquals("Coba lagi", mapped.primaryActionLabel)
        assertEquals(approvedWfaTarget.displayName, mapped.targetSummary?.displayName)
        assertTrue(mapped.wfaDiscovery is WfaDiscoveryUiModel.Failure)
    }

    @Test
    fun `attendance blocker takes priority over recommendation failure`() {
        val preparation = blockedPreparation(
            mode = WorkMode.WFA,
            reason = AttendancePreparationBlockReason.WFA_PENDING,
            recovery = AttendancePreparationRecovery.OPEN_WFA_REQUESTS
        ).copy(wfaDiscovery = WfaDiscoveryState.Failure(retryable = true))

        val mapped = AttendancePreparationUiMapper.map(preparation)

        assertEquals(AttendancePreparationPrimaryAction.OPEN_WFA_REQUESTS, mapped.primaryAction)
        assertEquals("Lihat status permintaan", mapped.primaryActionLabel)
    }

    @Test
    fun `WFA content maps provider-neutral recommendation rows with stable selection`() {
        val recommendation = WfaRecommendation(
            stableKey = "cafe@-0.900000,119.880000",
            name = "Cafe Palu",
            address = "Palu",
            coordinate = GeoCoordinate(-0.90, 119.88),
            category = "Cafe",
            suitabilityScore = 0.91,
            suitabilityLabel = "Sangat sesuai",
            distanceMeters = DistanceMeters(1_250.0)
        )
        val mapped = AttendancePreparationUiMapper.map(
            readyPreparation(WorkMode.WFA).copy(
                wfaDiscovery = WfaDiscoveryState.Content(
                    recommendations = listOf(recommendation),
                    selectedKey = recommendation.stableKey
                )
            )
        )

        val content = mapped.wfaDiscovery as WfaDiscoveryUiModel.Content
        assertEquals(recommendation.stableKey, content.rows.single().stableKey)
        assertTrue(content.rows.single().isSelected)
        assertTrue(content.rows.single().supportingText.contains("1,25 km"))
        assertFalse(content.rows.single().suitabilityText.contains("rating", ignoreCase = true))
    }

    private fun readyPreparation(mode: WorkMode): AttendancePreparationState {
        val target = if (mode == WorkMode.WFA) approvedWfaTarget else officeTarget.copy(mode = mode)
        val range = TargetRangeStatus.Inside(DistanceMeters(25.0))
        return AttendancePreparationState(
            selectedMode = mode,
            targetResolution = TargetLocationResolution.Resolved(target),
            rangeStatus = range,
            wfaDiscovery = if (mode == WorkMode.WFA) WfaDiscoveryState.Empty else WfaDiscoveryState.Hidden,
            eligibility = AttendancePreparationEligibility.Ready(target, range)
        )
    }

    private fun blockedPreparation(
        mode: WorkMode,
        reason: AttendancePreparationBlockReason,
        recovery: AttendancePreparationRecovery
    ) = AttendancePreparationState(
        selectedMode = mode,
        targetResolution = TargetLocationResolution.Resolving(mode),
        wfaDiscovery = if (mode == WorkMode.WFA) WfaDiscoveryState.Empty else WfaDiscoveryState.Hidden,
        eligibility = AttendancePreparationEligibility.Blocked(reason, recovery)
    )

    private fun modeFor(reason: AttendancePreparationBlockReason): WorkMode = when (reason) {
        AttendancePreparationBlockReason.WFH_PROFILE_CONTRACT,
        AttendancePreparationBlockReason.PROFILE_REFRESH_FAILED -> WorkMode.WFH
        AttendancePreparationBlockReason.WFA_NOT_REQUESTED,
        AttendancePreparationBlockReason.WFA_PENDING,
        AttendancePreparationBlockReason.WFA_REJECTED,
        AttendancePreparationBlockReason.WFA_APPROVAL_MISSING_FOR_DATE,
        AttendancePreparationBlockReason.BOOKING_REFRESH_FAILED -> WorkMode.WFA
        else -> WorkMode.WFO
    }

    private data class Case(
        val reason: AttendancePreparationBlockReason,
        val recovery: AttendancePreparationRecovery,
        val action: AttendancePreparationPrimaryAction,
        val label: String,
        val copyFragment: String
    )

    private val officeTarget = AuthoritativeTargetLocation(
        targetId = TargetLocationId("office:1"),
        mode = WorkMode.WFO,
        source = TargetLocationSource.STATUS_TODAY,
        coordinate = GeoCoordinate(-0.89, 119.87),
        radius = DistanceMeters(100.0),
        displayName = "Kantor Palu"
    )

    private val approvedWfaTarget = AuthoritativeTargetLocation(
        targetId = TargetLocationId("wfa:42"),
        mode = WorkMode.WFA,
        source = TargetLocationSource.APPROVED_WFA_BOOKING,
        coordinate = GeoCoordinate(-0.88, 119.86),
        radius = DistanceMeters(100.0),
        displayName = "WFA Disetujui"
    )
}
