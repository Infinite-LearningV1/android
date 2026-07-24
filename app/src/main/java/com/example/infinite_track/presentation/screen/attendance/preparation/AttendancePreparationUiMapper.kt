package com.example.infinite_track.presentation.screen.attendance.preparation

import com.example.infinite_track.domain.model.attendance.AttendancePreparationBlockReason
import com.example.infinite_track.domain.model.attendance.AttendancePreparationEligibility
import com.example.infinite_track.domain.model.attendance.AttendancePreparationRecovery
import com.example.infinite_track.domain.model.attendance.AuthoritativeTargetLocation
import com.example.infinite_track.domain.model.attendance.TargetLocationResolution
import com.example.infinite_track.domain.model.attendance.TargetLocationSource
import com.example.infinite_track.domain.model.attendance.TargetRangeStatus
import com.example.infinite_track.domain.model.attendance.TargetRangeUnknownReason
import com.example.infinite_track.domain.model.attendance.WorkMode
import com.example.infinite_track.domain.model.location.DistanceMeters
import com.example.infinite_track.domain.model.wfa.WfaRecommendation
import java.text.NumberFormat
import java.util.Locale

object AttendancePreparationUiMapper {
    fun map(preparation: AttendancePreparationState): AttendancePreparationUiModel {
        val eligibilityPresentation = preparation.eligibility.toPresentation()
        val retryDiscovery = preparation.eligibility is AttendancePreparationEligibility.Ready &&
            preparation.selectedMode == WorkMode.WFA &&
            (preparation.wfaDiscovery as? WfaDiscoveryState.Failure)?.retryable == true
        val primary = if (retryDiscovery) {
            PrimaryPresentation(
                action = AttendancePreparationPrimaryAction.RETRY_WFA_DISCOVERY,
                label = "Coba lagi",
                enabled = true,
                statusMessage = "Rekomendasi lokasi WFA gagal dimuat. Coba lagi."
            )
        } else {
            eligibilityPresentation
        }
        val hasWfaSecondaryAction = preparation.selectedMode == WorkMode.WFA

        return AttendancePreparationUiModel(
            modeOptions = WorkMode.values().map { mode -> mode.toUiModel(mode == preparation.selectedMode) },
            targetSummary = (preparation.targetResolution as? TargetLocationResolution.Resolved)
                ?.target
                ?.toUiModel(preparation.rangeStatus),
            statusMessage = primary.statusMessage,
            wfaDiscovery = preparation.wfaDiscovery.toUiModel(),
            primaryAction = primary.action,
            primaryActionLabel = primary.label,
            isPrimaryActionEnabled = primary.enabled,
            secondaryAction = AttendancePreparationSecondaryAction.SEARCH_WFA_LOCATION
                .takeIf { hasWfaSecondaryAction },
            secondaryActionLabel = "Cari lokasi WFA".takeIf { hasWfaSecondaryAction }
        )
    }

    private fun AttendancePreparationEligibility.toPresentation(): PrimaryPresentation = when (this) {
        AttendancePreparationEligibility.Resolving -> PrimaryPresentation(
            action = AttendancePreparationPrimaryAction.WAIT,
            label = "Menyiapkan lokasi...",
            enabled = false,
            statusMessage = "Lokasi target sedang disiapkan."
        )
        is AttendancePreparationEligibility.Ready -> PrimaryPresentation(
            action = AttendancePreparationPrimaryAction.CONTINUE_TO_FACE_VERIFICATION,
            label = "Lanjut ke Verifikasi Wajah",
            enabled = true,
            statusMessage = "Lokasi target siap digunakan untuk kehadiran."
        )
        is AttendancePreparationEligibility.Blocked -> recovery.toPresentation(reason)
    }

    private fun AttendancePreparationRecovery.toPresentation(
        reason: AttendancePreparationBlockReason
    ): PrimaryPresentation {
        val (action, label) = when (this) {
            AttendancePreparationRecovery.REFRESH_STATUS ->
                AttendancePreparationPrimaryAction.REFRESH_STATUS to "Muat ulang"
            AttendancePreparationRecovery.REFRESH_PROFILE ->
                AttendancePreparationPrimaryAction.REFRESH_PROFILE to "Muat ulang"
            AttendancePreparationRecovery.REFRESH_LOCATION ->
                AttendancePreparationPrimaryAction.REFRESH_LOCATION to "Muat ulang lokasi"
            AttendancePreparationRecovery.FOCUS_TARGET ->
                AttendancePreparationPrimaryAction.FOCUS_TARGET to "Fokus ke lokasi target"
            AttendancePreparationRecovery.OPEN_WFA_BOOKING ->
                AttendancePreparationPrimaryAction.OPEN_WFA_BOOKING to "Ajukan WFA"
            AttendancePreparationRecovery.OPEN_WFA_REQUESTS ->
                AttendancePreparationPrimaryAction.OPEN_WFA_REQUESTS to "Lihat status permintaan"
            AttendancePreparationRecovery.CONTACT_ADMIN ->
                AttendancePreparationPrimaryAction.CONTACT_ADMIN to "Hubungi admin"
        }
        return PrimaryPresentation(
            action = action,
            label = label,
            enabled = true,
            statusMessage = reason.indonesianCopy()
        )
    }

    private fun AttendancePreparationBlockReason.indonesianCopy(): String = when (this) {
        AttendancePreparationBlockReason.WFO_NOT_ASSIGNED ->
            "Lokasi kantor belum ditetapkan. Muat ulang status kehadiran."
        AttendancePreparationBlockReason.WFH_PROFILE_CONTRACT ->
            "Lokasi WFH belum tersedia dari profil. Muat ulang atau hubungi admin."
        AttendancePreparationBlockReason.WFA_NOT_REQUESTED ->
            "Belum ada permintaan WFA untuk tanggal kehadiran ini."
        AttendancePreparationBlockReason.WFA_PENDING ->
            "Permintaan WFA masih menunggu persetujuan."
        AttendancePreparationBlockReason.WFA_REJECTED ->
            "Permintaan WFA ditolak. Lihat status permintaan untuk langkah berikutnya."
        AttendancePreparationBlockReason.WFA_APPROVAL_MISSING_FOR_DATE ->
            "Persetujuan WFA tidak tersedia untuk tanggal kehadiran ini."
        AttendancePreparationBlockReason.CURRENT_LOCATION_UNAVAILABLE ->
            "Lokasi saat ini belum tersedia. Muat ulang lokasi."
        AttendancePreparationBlockReason.CURRENT_LOCATION_STALE ->
            "Lokasi saat ini sudah tidak terkini. Muat ulang lokasi."
        AttendancePreparationBlockReason.OUTSIDE_TARGET_RANGE ->
            "Anda berada di luar jangkauan lokasi target."
        AttendancePreparationBlockReason.STATUS_REFRESH_FAILED ->
            "Status kehadiran gagal dimuat. Coba muat ulang."
        AttendancePreparationBlockReason.PROFILE_REFRESH_FAILED ->
            "Profil lokasi gagal dimuat. Coba muat ulang."
        AttendancePreparationBlockReason.BOOKING_REFRESH_FAILED ->
            "Status permintaan WFA gagal dimuat. Lihat status permintaan."
        AttendancePreparationBlockReason.TARGET_CONTRACT_INVALID ->
            "Data lokasi target tidak valid. Hubungi admin."
    }

    private fun WorkMode.toUiModel(isSelected: Boolean): WorkModeOptionUiModel = when (this) {
        WorkMode.WFO -> WorkModeOptionUiModel(
            mode = this,
            title = displayLabel,
            supportingText = "Lokasi kantor yang ditetapkan",
            isSelected = isSelected
        )
        WorkMode.WFH -> WorkModeOptionUiModel(
            mode = this,
            title = displayLabel,
            supportingText = "Lokasi rumah yang ditetapkan admin",
            isSelected = isSelected
        )
        WorkMode.WFA -> WorkModeOptionUiModel(
            mode = this,
            title = displayLabel,
            supportingText = "Memerlukan booking yang disetujui",
            isSelected = isSelected
        )
    }

    private fun AuthoritativeTargetLocation.toUiModel(
        rangeStatus: TargetRangeStatus?
    ): TargetLocationSummaryUiModel = TargetLocationSummaryUiModel(
        displayName = displayName,
        sourceLabel = when (source) {
            TargetLocationSource.STATUS_TODAY -> "Status kehadiran hari ini"
            TargetLocationSource.ADMIN_PROFILE -> "Profil yang ditetapkan admin"
            TargetLocationSource.APPROVED_WFA_BOOKING -> "Booking WFA disetujui"
        },
        radiusText = "Radius ${formatDistance(radius)}",
        distanceText = when (rangeStatus) {
            is TargetRangeStatus.Inside -> "Jarak ${formatDistance(rangeStatus.distance)}"
            is TargetRangeStatus.Outside -> "Jarak ${formatDistance(rangeStatus.distance)}"
            is TargetRangeStatus.Unknown, null -> null
        },
        rangeText = when (rangeStatus) {
            is TargetRangeStatus.Inside -> "Di dalam jangkauan"
            is TargetRangeStatus.Outside -> "Di luar jangkauan"
            is TargetRangeStatus.Unknown -> when (rangeStatus.reason) {
                TargetRangeUnknownReason.CURRENT_LOCATION_UNAVAILABLE -> "Lokasi saat ini belum tersedia"
                TargetRangeUnknownReason.CURRENT_LOCATION_STALE -> "Lokasi saat ini tidak terkini"
            }
            null -> null
        }
    )

    private fun WfaDiscoveryState.toUiModel(): WfaDiscoveryUiModel = when (this) {
        WfaDiscoveryState.Hidden -> WfaDiscoveryUiModel.Hidden
        WfaDiscoveryState.Loading -> WfaDiscoveryUiModel.Loading
        WfaDiscoveryState.Empty -> WfaDiscoveryUiModel.Empty("Belum ada rekomendasi lokasi WFA.")
        is WfaDiscoveryState.Failure -> WfaDiscoveryUiModel.Failure(
            message = "Rekomendasi lokasi WFA gagal dimuat.",
            retryable = retryable
        )
        is WfaDiscoveryState.Content -> WfaDiscoveryUiModel.Content(
            rows = recommendations.map { recommendation -> recommendation.toUiModel() },
            selectedKey = selectedKey,
            searchPreviewName = searchPreview?.placeName
        )
    }

    private fun WfaRecommendation.toUiModel(): WfaRecommendationUiModel {
        val suitability = WfaSuitabilityPresentationMapper.map(suitabilityScore)
        return WfaRecommendationUiModel(
            stableKey = stableKey,
            name = name,
            supportingText = "$category • ${formatDistance(distanceMeters)}",
            suitabilityText = "Skor WFA ${suitability.percentage} • $suitabilityLabel",
        )
    }

    private fun formatDistance(distance: DistanceMeters): String {
        val formatter = NumberFormat.getNumberInstance(INDONESIAN_LOCALE).apply {
            maximumFractionDigits = 2
            minimumFractionDigits = 0
        }
        return if (distance.value >= METERS_PER_KILOMETER) {
            "${formatter.format(distance.value / METERS_PER_KILOMETER)} km"
        } else {
            "${formatter.format(distance.value)} m"
        }
    }

    private data class PrimaryPresentation(
        val action: AttendancePreparationPrimaryAction,
        val label: String,
        val enabled: Boolean,
        val statusMessage: String
    )

    private val INDONESIAN_LOCALE = Locale("id", "ID")
    private const val METERS_PER_KILOMETER = 1_000.0
}
