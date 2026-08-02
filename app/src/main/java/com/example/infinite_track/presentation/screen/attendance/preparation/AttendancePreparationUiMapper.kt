package com.example.infinite_track.presentation.screen.attendance.preparation

import androidx.annotation.StringRes
import com.example.infinite_track.R
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
import com.example.infinite_track.presentation.design.tokens.InfiniteSemantic
import java.text.NumberFormat
import java.util.Locale

interface AttendancePreparationTextResolver {
    val locale: Locale

    fun text(@StringRes resourceId: Int, vararg formatArgs: Any): String
}

object AttendancePreparationUiMapper {
    fun map(
        preparation: AttendancePreparationState,
        strings: AttendancePreparationTextResolver
    ): AttendancePreparationUiModel {
        val eligibilityPresentation = preparation.eligibility.toPresentation(strings)
        val retryDiscovery = preparation.eligibility is AttendancePreparationEligibility.Ready &&
            preparation.selectedMode == WorkMode.WFA &&
            (preparation.wfaDiscovery as? WfaDiscoveryState.Failure)?.retryable == true
        val primary = if (retryDiscovery) {
            PrimaryPresentation(
                action = AttendancePreparationPrimaryAction.RETRY_WFA_DISCOVERY,
                label = strings.text(R.string.attendance_action_retry),
                enabled = true,
                statusMessage = strings.text(R.string.attendance_status_recommendation_failed)
            )
        } else {
            eligibilityPresentation
        }
        val hasWfaSecondaryAction = preparation.selectedMode == WorkMode.WFA
        val primaryEnabled = if (
            primary.action == AttendancePreparationPrimaryAction.OPEN_WFA_BOOKING
        ) {
            preparation.hasSelectedWfaDraft()
        } else {
            primary.enabled
        }

        return AttendancePreparationUiModel(
            modeOptions = WorkMode.values().map { mode ->
                mode.toUiModel(mode == preparation.selectedMode, strings)
            },
            targetSummary = (preparation.targetResolution as? TargetLocationResolution.Resolved)
                ?.target
                ?.toUiModel(preparation.rangeStatus, strings),
            statusMessage = primary.statusMessage,
            wfaDiscovery = preparation.wfaDiscovery.toUiModel(strings),
            primaryAction = primary.action,
            primaryActionLabel = primary.label,
            isPrimaryActionEnabled = primaryEnabled,
            secondaryAction = AttendancePreparationSecondaryAction.SEARCH_WFA_LOCATION
                .takeIf { hasWfaSecondaryAction },
            secondaryActionLabel = strings.text(R.string.attendance_action_search_wfa)
                .takeIf { hasWfaSecondaryAction }
        )
    }

    private fun AttendancePreparationEligibility.toPresentation(
        strings: AttendancePreparationTextResolver
    ): PrimaryPresentation = when (this) {
        AttendancePreparationEligibility.Resolving -> PrimaryPresentation(
            action = AttendancePreparationPrimaryAction.WAIT,
            label = strings.text(R.string.attendance_action_wait),
            enabled = false,
            statusMessage = strings.text(R.string.attendance_status_preparing)
        )
        is AttendancePreparationEligibility.Ready -> PrimaryPresentation(
            action = AttendancePreparationPrimaryAction.CONTINUE_TO_FACE_VERIFICATION,
            label = strings.text(R.string.attendance_action_continue_face),
            enabled = true,
            statusMessage = strings.text(R.string.attendance_status_ready)
        )
        is AttendancePreparationEligibility.Blocked -> recovery.toPresentation(reason, strings)
    }

    private fun AttendancePreparationRecovery.toPresentation(
        reason: AttendancePreparationBlockReason,
        strings: AttendancePreparationTextResolver
    ): PrimaryPresentation {
        val (action, labelResource) = when (this) {
            AttendancePreparationRecovery.REFRESH_STATUS ->
                AttendancePreparationPrimaryAction.REFRESH_STATUS to R.string.attendance_action_reload
            AttendancePreparationRecovery.REFRESH_PROFILE ->
                AttendancePreparationPrimaryAction.REFRESH_PROFILE to R.string.attendance_action_reload
            AttendancePreparationRecovery.REFRESH_LOCATION ->
                AttendancePreparationPrimaryAction.REFRESH_LOCATION to R.string.attendance_action_refresh_location
            AttendancePreparationRecovery.FOCUS_TARGET ->
                AttendancePreparationPrimaryAction.FOCUS_TARGET to R.string.attendance_action_focus_target
            AttendancePreparationRecovery.OPEN_WFA_BOOKING ->
                AttendancePreparationPrimaryAction.OPEN_WFA_BOOKING to R.string.attendance_action_submit_wfa
            AttendancePreparationRecovery.OPEN_WFA_REQUESTS ->
                AttendancePreparationPrimaryAction.OPEN_WFA_REQUESTS to R.string.attendance_action_view_wfa_requests
            AttendancePreparationRecovery.CONTACT_ADMIN ->
                AttendancePreparationPrimaryAction.CONTACT_ADMIN to R.string.attendance_action_contact_admin
        }
        return PrimaryPresentation(
            action = action,
            label = strings.text(labelResource),
            enabled = true,
            statusMessage = strings.text(reason.copyResource())
        )
    }

    private fun AttendancePreparationState.hasSelectedWfaDraft(): Boolean {
        val content = wfaDiscovery as? WfaDiscoveryState.Content ?: return false
        return content.searchPreview != null || content.selectedKey?.let { selected ->
            content.recommendations.any { it.stableKey == selected }
        } == true
    }

    @StringRes
    private fun AttendancePreparationBlockReason.copyResource(): Int = when (this) {
        AttendancePreparationBlockReason.WFO_NOT_ASSIGNED -> R.string.attendance_block_wfo_not_assigned
        AttendancePreparationBlockReason.WFH_PROFILE_CONTRACT -> R.string.attendance_block_wfh_profile
        AttendancePreparationBlockReason.WFA_NOT_REQUESTED -> R.string.attendance_block_wfa_not_requested
        AttendancePreparationBlockReason.WFA_PENDING -> R.string.attendance_block_wfa_pending
        AttendancePreparationBlockReason.WFA_REJECTED -> R.string.attendance_block_wfa_rejected
        AttendancePreparationBlockReason.WFA_APPROVAL_MISSING_FOR_DATE -> R.string.attendance_block_wfa_missing_date
        AttendancePreparationBlockReason.CURRENT_LOCATION_UNAVAILABLE -> R.string.attendance_block_location_unavailable
        AttendancePreparationBlockReason.CURRENT_LOCATION_STALE -> R.string.attendance_block_location_stale
        AttendancePreparationBlockReason.OUTSIDE_TARGET_RANGE -> R.string.attendance_block_outside_range
        AttendancePreparationBlockReason.STATUS_REFRESH_FAILED -> R.string.attendance_block_status_failed
        AttendancePreparationBlockReason.PROFILE_REFRESH_FAILED -> R.string.attendance_block_profile_failed
        AttendancePreparationBlockReason.BOOKING_REFRESH_FAILED -> R.string.attendance_block_booking_failed
        AttendancePreparationBlockReason.TARGET_CONTRACT_INVALID -> R.string.attendance_block_target_invalid
    }

    private fun WorkMode.toUiModel(
        isSelected: Boolean,
        strings: AttendancePreparationTextResolver
    ): WorkModeOptionUiModel = when (this) {
        WorkMode.WFO -> WorkModeOptionUiModel(
            mode = this,
            title = strings.text(R.string.attendance_work_mode_wfo),
            supportingText = strings.text(R.string.attendance_wfo_supporting),
            isSelected = isSelected
        )
        WorkMode.WFH -> WorkModeOptionUiModel(
            mode = this,
            title = strings.text(R.string.attendance_work_mode_wfh),
            supportingText = strings.text(R.string.attendance_wfh_supporting),
            isSelected = isSelected
        )
        WorkMode.WFA -> WorkModeOptionUiModel(
            mode = this,
            title = strings.text(R.string.attendance_work_mode_wfa),
            supportingText = strings.text(R.string.attendance_wfa_supporting),
            isSelected = isSelected
        )
    }

    private fun AuthoritativeTargetLocation.toUiModel(
        rangeStatus: TargetRangeStatus?,
        strings: AttendancePreparationTextResolver
    ): TargetLocationSummaryUiModel = TargetLocationSummaryUiModel(
        displayName = displayName,
        sourceLabel = when (source) {
            TargetLocationSource.STATUS_TODAY ->
                strings.text(R.string.attendance_target_source_status_today)
            TargetLocationSource.ADMIN_PROFILE ->
                strings.text(R.string.attendance_target_source_admin_profile)
            TargetLocationSource.APPROVED_WFA_BOOKING ->
                strings.text(R.string.attendance_target_source_approved_wfa)
        },
        radiusText = strings.text(
            R.string.attendance_target_radius,
            formatDistance(radius, strings)
        ),
        distanceText = when (rangeStatus) {
            is TargetRangeStatus.Inside -> strings.text(
                R.string.attendance_target_distance,
                formatDistance(rangeStatus.distance, strings)
            )
            is TargetRangeStatus.Outside -> strings.text(
                R.string.attendance_target_distance,
                formatDistance(rangeStatus.distance, strings)
            )
            is TargetRangeStatus.Unknown, null -> null
        },
        rangeText = when (rangeStatus) {
            is TargetRangeStatus.Inside -> strings.text(R.string.attendance_range_inside)
            is TargetRangeStatus.Outside -> strings.text(R.string.attendance_range_outside)
            is TargetRangeStatus.Unknown -> when (rangeStatus.reason) {
                TargetRangeUnknownReason.CURRENT_LOCATION_UNAVAILABLE ->
                    strings.text(R.string.attendance_range_unavailable)
                TargetRangeUnknownReason.CURRENT_LOCATION_STALE ->
                    strings.text(R.string.attendance_range_stale)
            }
            null -> null
        },
        rangeSemantic = when (rangeStatus) {
            is TargetRangeStatus.Inside -> InfiniteSemantic.Success
            is TargetRangeStatus.Outside -> InfiniteSemantic.Error
            is TargetRangeStatus.Unknown -> InfiniteSemantic.Neutral
            null -> null
        },
        bookingStatusText = approvedWfaContext?.let {
            strings.text(R.string.attendance_target_booking_status_approved)
        },
        bookingDateText = approvedWfaContext?.let { context ->
            strings.text(R.string.attendance_target_booking_date, context.scheduleDateDisplay)
        }
    )

    private fun WfaDiscoveryState.toUiModel(
        strings: AttendancePreparationTextResolver
    ): WfaDiscoveryUiModel = when (this) {
        WfaDiscoveryState.Hidden -> WfaDiscoveryUiModel.Hidden
        WfaDiscoveryState.Loading -> WfaDiscoveryUiModel.Loading
        WfaDiscoveryState.Empty -> WfaDiscoveryUiModel.Empty(
            strings.text(R.string.attendance_wfa_recommendation_empty)
        )
        is WfaDiscoveryState.Failure -> WfaDiscoveryUiModel.Failure(
            message = strings.text(R.string.attendance_wfa_recommendation_failure),
            retryable = retryable
        )
        is WfaDiscoveryState.Content -> WfaDiscoveryUiModel.Content(
            rows = recommendations.mapNotNull { recommendation ->
                recommendation.toUiModel(strings)
            },
            selectedKey = selectedKey,
            searchPreviewName = searchPreview?.placeName
        )
    }

    private fun WfaRecommendation.toUiModel(
        strings: AttendancePreparationTextResolver
    ): WfaRecommendationUiModel? {
        val score = finalScore ?: return null
        val label = finalLabel ?: return null
        val suitability = WfaSuitabilityPresentationMapper.map(score / 100.0)
        return WfaRecommendationUiModel(
            stableKey = stableKey,
            name = name,
            supportingText = strings.text(
                R.string.attendance_wfa_recommendation_detail,
                placeType,
                formatDistance(distanceMeters, strings)
            ),
            suitabilityText = strings.text(
                R.string.attendance_wfa_suitability,
                suitability.percentage,
                label
            ),
            suitabilitySemantic = suitability.semantic,
            categoryIcon = placeType.toCategoryIcon()
        )
    }

    private fun String.toCategoryIcon(): WfaRecommendationCategoryIcon {
        val normalized = trim().lowercase(Locale.ROOT)
        return when {
            normalized.contains("cafe") || normalized.contains("kafe") ||
                normalized.contains("coffee") -> WfaRecommendationCategoryIcon.CAFE
            normalized.contains("cowork") || normalized.contains("office") ->
                WfaRecommendationCategoryIcon.COWORKING
            normalized.contains("library") || normalized.contains("perpustakaan") ->
                WfaRecommendationCategoryIcon.LIBRARY
            normalized.contains("park") || normalized.contains("taman") ->
                WfaRecommendationCategoryIcon.PARK
            else -> WfaRecommendationCategoryIcon.WORK
        }
    }

    private fun formatDistance(
        distance: DistanceMeters,
        strings: AttendancePreparationTextResolver
    ): String {
        val formatter = NumberFormat.getNumberInstance(strings.locale).apply {
            maximumFractionDigits = 2
            minimumFractionDigits = 0
        }
        return if (distance.value >= METERS_PER_KILOMETER) {
            strings.text(
                R.string.attendance_distance_kilometers,
                formatter.format(distance.value / METERS_PER_KILOMETER)
            )
        } else {
            strings.text(
                R.string.attendance_distance_meters,
                formatter.format(distance.value)
            )
        }
    }

    private data class PrimaryPresentation(
        val action: AttendancePreparationPrimaryAction,
        val label: String,
        val enabled: Boolean,
        val statusMessage: String
    )

    private const val METERS_PER_KILOMETER = 1_000.0
}
