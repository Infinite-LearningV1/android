package com.example.infinite_track.presentation.screen.attendance.preparation

import com.example.infinite_track.R
import com.example.infinite_track.domain.model.attendance.AttendancePreparationBlockReason
import com.example.infinite_track.domain.model.attendance.AttendancePreparationEligibility
import com.example.infinite_track.domain.model.attendance.AttendancePreparationRecovery
import com.example.infinite_track.domain.model.attendance.ApprovedWfaTargetContext
import com.example.infinite_track.domain.model.attendance.AuthoritativeTargetLocation
import com.example.infinite_track.domain.model.attendance.TargetLocationId
import com.example.infinite_track.domain.model.attendance.TargetLocationResolution
import com.example.infinite_track.domain.model.attendance.TargetLocationSource
import com.example.infinite_track.domain.model.attendance.TargetRangeStatus
import com.example.infinite_track.domain.model.attendance.WorkMode
import com.example.infinite_track.domain.model.location.DistanceMeters
import com.example.infinite_track.domain.model.location.GeoCoordinate
import com.example.infinite_track.domain.model.wfa.WfaRecommendation
import com.example.infinite_track.presentation.design.tokens.InfiniteSemantic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class AttendancePreparationUiMapperTest {

    @Test
    fun `mapper exposes exactly one selected mode and one primary action`() {
        val mapped = map(readyPreparation(WorkMode.WFO))

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
        val mapped = map(
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
    fun `WFA booking action is disabled until a draft location is selected`() {
        val mapped = map(
            blockedPreparation(
                mode = WorkMode.WFA,
                reason = AttendancePreparationBlockReason.WFA_NOT_REQUESTED,
                recovery = AttendancePreparationRecovery.OPEN_WFA_BOOKING
            )
        )

        assertEquals(AttendancePreparationPrimaryAction.OPEN_WFA_BOOKING, mapped.primaryAction)
        assertFalse(mapped.isPrimaryActionEnabled)
    }

    @Test
    fun `selected recommendation enables WFA booking action`() {
        val recommendation = recommendation()
        val mapped = map(
            blockedPreparation(
                mode = WorkMode.WFA,
                reason = AttendancePreparationBlockReason.WFA_NOT_REQUESTED,
                recovery = AttendancePreparationRecovery.OPEN_WFA_BOOKING
            ).copy(
                wfaDiscovery = WfaDiscoveryState.Content(
                    recommendations = listOf(recommendation),
                    selectedKey = recommendation.stableKey
                )
            )
        )

        assertTrue(mapped.isPrimaryActionEnabled)
    }

    @Test
    fun `search preview enables WFA booking action`() {
        val mapped = map(
            blockedPreparation(
                mode = WorkMode.WFA,
                reason = AttendancePreparationBlockReason.WFA_NOT_REQUESTED,
                recovery = AttendancePreparationRecovery.OPEN_WFA_BOOKING
            ).copy(
                wfaDiscovery = WfaDiscoveryState.Content(
                    recommendations = emptyList(),
                    searchPreview = com.example.infinite_track.domain.model.location.LocationResult(
                        placeName = "Selected place",
                        address = "Palu",
                        latitude = -0.90,
                        longitude = 119.88
                    )
                )
            )
        )

        assertTrue(mapped.isPrimaryActionEnabled)
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
            val mapped = map(
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

        val mapped = map(preparation)

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

        val mapped = map(preparation)

        assertEquals(AttendancePreparationPrimaryAction.OPEN_WFA_REQUESTS, mapped.primaryAction)
        assertEquals("Lihat status permintaan", mapped.primaryActionLabel)
    }

    @Test
    fun `WFA content maps provider-neutral recommendation rows with stable selection`() {
        val recommendation = recommendation()
        val mapped = map(
            readyPreparation(WorkMode.WFA).copy(
                wfaDiscovery = WfaDiscoveryState.Content(
                    recommendations = listOf(recommendation),
                    selectedKey = recommendation.stableKey
                )
            )
        )

        val content = mapped.wfaDiscovery as WfaDiscoveryUiModel.Content
        assertEquals(recommendation.stableKey, content.rows.single().stableKey)
        assertTrue(content.isRecommendationSelected(content.rows.single()))
        assertTrue(content.rows.single().supportingText.contains("1,25 km"))
        assertFalse(content.rows.single().suitabilityText.contains("rating", ignoreCase = true))
        assertEquals(InfiniteSemantic.Success, content.rows.single().suitabilitySemantic)
        assertEquals(WfaRecommendationCategoryIcon.CAFE, content.rows.single().categoryIcon)
    }

    @Test
    fun `approved WFA summary renders display evidence while preserving raw authority date`() {
        val mapped = map(readyPreparation(WorkMode.WFA))

        assertEquals("Booking disetujui", mapped.targetSummary?.bookingStatusText)
        assertEquals("Tanggal 23 Jul 2026", mapped.targetSummary?.bookingDateText)
        assertFalse(mapped.targetSummary?.bookingDateText.orEmpty().contains("2026-07-23"))
        assertEquals(InfiniteSemantic.Success, mapped.targetSummary?.rangeSemantic)
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

    private fun map(preparation: AttendancePreparationState): AttendancePreparationUiModel =
        AttendancePreparationUiMapper.map(preparation, IndonesianTextResolver)

    private object IndonesianTextResolver : AttendancePreparationTextResolver {
        override val locale: Locale = Locale.forLanguageTag("id-ID")

        override fun text(resourceId: Int, vararg formatArgs: Any): String {
            val raw = when (resourceId) {
                R.string.attendance_work_mode_wfo -> "Work From Office"
                R.string.attendance_work_mode_wfh -> "Work From Home"
                R.string.attendance_work_mode_wfa -> "Work From Anywhere"
                R.string.attendance_wfo_supporting -> "Check-in dari lokasi kantor yang ditetapkan"
                R.string.attendance_wfh_supporting -> "Gunakan lokasi rumah yang terdaftar"
                R.string.attendance_wfa_supporting -> "Memerlukan booking yang disetujui"
                R.string.attendance_target_source_status_today -> "Status kehadiran hari ini"
                R.string.attendance_target_source_admin_profile -> "Profil yang ditetapkan admin"
                R.string.attendance_target_source_approved_wfa -> "Booking WFA disetujui"
                R.string.attendance_target_booking_status_approved -> "Booking disetujui"
                R.string.attendance_target_booking_date -> "Tanggal %1\$s"
                R.string.attendance_target_radius -> "Radius %1\$s"
                R.string.attendance_target_distance -> "Jarak %1\$s"
                R.string.attendance_distance_meters -> "%1\$s m"
                R.string.attendance_distance_kilometers -> "%1\$s km"
                R.string.attendance_range_inside -> "Di dalam jangkauan"
                R.string.attendance_range_outside -> "Di luar jangkauan"
                R.string.attendance_range_unavailable -> "Lokasi saat ini belum tersedia"
                R.string.attendance_range_stale -> "Lokasi saat ini tidak terkini"
                R.string.attendance_action_wait -> "Menyiapkan lokasi..."
                R.string.attendance_action_continue_face -> "Lanjut ke Verifikasi Wajah"
                R.string.attendance_action_reload -> "Muat ulang"
                R.string.attendance_action_refresh_location -> "Muat ulang lokasi"
                R.string.attendance_action_focus_target -> "Fokus ke lokasi target"
                R.string.attendance_action_submit_wfa -> "Ajukan WFA"
                R.string.attendance_action_view_wfa_requests -> "Lihat status permintaan"
                R.string.attendance_action_contact_admin -> "Hubungi admin"
                R.string.attendance_action_retry -> "Coba lagi"
                R.string.attendance_action_search_wfa -> "Cari lokasi WFA"
                R.string.attendance_status_preparing -> "Lokasi target sedang disiapkan."
                R.string.attendance_status_ready -> "Lokasi target siap digunakan untuk kehadiran."
                R.string.attendance_status_recommendation_failed ->
                    "Rekomendasi lokasi WFA gagal dimuat. Coba lagi."
                R.string.attendance_block_wfo_not_assigned ->
                    "Lokasi kantor belum ditetapkan. Muat ulang status kehadiran."
                R.string.attendance_block_wfh_profile ->
                    "Lokasi WFH belum tersedia dari profil. Muat ulang atau hubungi admin."
                R.string.attendance_block_wfa_not_requested ->
                    "Belum ada permintaan WFA untuk tanggal kehadiran ini."
                R.string.attendance_block_wfa_pending ->
                    "Permintaan WFA masih menunggu persetujuan."
                R.string.attendance_block_wfa_rejected ->
                    "Permintaan WFA ditolak. Lihat status permintaan untuk langkah berikutnya."
                R.string.attendance_block_wfa_missing_date ->
                    "Persetujuan WFA tidak tersedia untuk tanggal kehadiran ini."
                R.string.attendance_block_location_unavailable ->
                    "Lokasi saat ini belum tersedia. Muat ulang lokasi."
                R.string.attendance_block_location_stale ->
                    "Lokasi saat ini sudah tidak terkini. Muat ulang lokasi."
                R.string.attendance_block_outside_range ->
                    "Anda berada di luar jangkauan lokasi target."
                R.string.attendance_block_status_failed ->
                    "Status kehadiran gagal dimuat. Coba muat ulang."
                R.string.attendance_block_profile_failed ->
                    "Profil lokasi gagal dimuat. Coba muat ulang."
                R.string.attendance_block_booking_failed ->
                    "Status permintaan WFA gagal dimuat. Lihat status permintaan."
                R.string.attendance_block_target_invalid ->
                    "Data lokasi target tidak valid. Hubungi admin."
                R.string.attendance_wfa_recommendation_empty ->
                    "Belum ada rekomendasi lokasi WFA."
                R.string.attendance_wfa_recommendation_failure ->
                    "Rekomendasi lokasi WFA gagal dimuat."
                R.string.attendance_wfa_recommendation_detail -> "%1\$s • %2\$s"
                R.string.attendance_wfa_suitability -> "Skor WFA %1\$d • %2\$s"
                else -> error("Missing test string for resource $resourceId")
            }
            return if (formatArgs.isEmpty()) raw else String.format(locale, raw, *formatArgs)
        }
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

    private fun recommendation() = WfaRecommendation(
        stableKey = "cafe@-0.900000,119.880000",
        name = "Cafe Palu",
        address = "Palu",
        coordinate = GeoCoordinate(-0.90, 119.88),
        category = "Cafe",
        suitabilityScore = 0.91,
        suitabilityLabel = "Sangat sesuai",
        distanceMeters = DistanceMeters(1_250.0)
    )

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
        displayName = "WFA Disetujui",
        approvedWfaContext = ApprovedWfaTargetContext(
            bookingId = 42,
            scheduleDate = "2026-07-23",
            scheduleDateDisplay = "23 Jul 2026"
        )
    )
}
