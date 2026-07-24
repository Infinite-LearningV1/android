package com.example.infinite_track.domain.use_case.geofence

import com.example.infinite_track.domain.model.attendance.AuthoritativeTargetLocation
import com.example.infinite_track.domain.model.attendance.TargetLocationResolution
import com.example.infinite_track.domain.model.attendance.TargetResolutionFailure
import com.example.infinite_track.domain.model.attendance.WorkMode
import com.example.infinite_track.domain.model.geofence.ActiveMonitoringTarget
import com.example.infinite_track.domain.model.geofence.BackendTruthSource
import com.example.infinite_track.domain.model.geofence.GeofenceDisabledReason
import com.example.infinite_track.domain.model.geofence.GeofenceReconcileReason
import com.example.infinite_track.domain.model.geofence.GeofenceRuntimeFailure
import com.example.infinite_track.domain.model.geofence.GeofenceRuntimeInputs
import com.example.infinite_track.domain.model.geofence.GeofenceRuntimeMode
import com.example.infinite_track.domain.model.geofence.GeofenceRuntimeModeResolution
import com.example.infinite_track.domain.model.geofence.GeofenceTargetIdentity
import com.example.infinite_track.domain.model.geofence.ReminderCandidateSource
import com.example.infinite_track.domain.model.wfa.WfaBookingForDate
import com.example.infinite_track.domain.use_case.attendance.ResolveAuthoritativeTargetLocationUseCase
import java.time.LocalDate
import javax.inject.Inject

class ResolveGeofenceRuntimeModeUseCase @Inject constructor(
    private val buildCandidates: BuildReminderGeofenceCandidatesUseCase,
    private val resolveTarget: ResolveAuthoritativeTargetLocationUseCase
) {
    operator fun invoke(inputs: GeofenceRuntimeInputs): GeofenceRuntimeModeResolution {
        val effectiveDate = runCatching { LocalDate.parse(inputs.todayStatus.todayDate) }.getOrNull()
            ?: return disabledForUnavailableStatusTruth()
        val attendanceId = inputs.todayStatus.activeAttendanceId?.takeIf { it > 0 }
        val stateKey = inputs.todayStatus.attendanceSessionState?.key
        val hasActiveState = stateKey == SESSION_STATE_ACTIVE

        if (attendanceId != null && hasActiveState) {
            return resolveActiveMonitoring(inputs, effectiveDate, attendanceId)
        }

        if ((attendanceId != null) != hasActiveState) {
            return inconsistentSessionTruth(inputs.todayStatus.activeAttendanceId, stateKey)
        }

        if (stateKey == SESSION_STATE_COMPLETED) {
            return GeofenceRuntimeModeResolution(GeofenceRuntimeMode.Completed(effectiveDate))
        }

        if (inputs.reason == GeofenceReconcileReason.CHECK_OUT_SUCCEEDED && !inputs.todayStatus.canCheckIn) {
            return GeofenceRuntimeModeResolution(GeofenceRuntimeMode.Completed(effectiveDate))
        }

        if (inputs.todayStatus.activeAttendanceId == null &&
            stateKey == SESSION_STATE_NOT_STARTED &&
            inputs.todayStatus.canCheckIn
        ) {
            val candidates = buildCandidates(
                inputs.todayStatus,
                inputs.profile,
                inputs.approvedWfaBooking
            )
            return GeofenceRuntimeModeResolution(
                mode = GeofenceRuntimeMode.Reminder(effectiveDate, candidates.candidates),
                warnings = candidates.failures
            )
        }

        return GeofenceRuntimeModeResolution(
            GeofenceRuntimeMode.Disabled(GeofenceDisabledReason.NO_ELIGIBLE_SESSION)
        )
    }

    private fun resolveActiveMonitoring(
        inputs: GeofenceRuntimeInputs,
        effectiveDate: LocalDate,
        attendanceId: Int
    ): GeofenceRuntimeModeResolution {
        val mode = WorkMode.fromRaw(inputs.todayStatus.activeMode) ?: return activeTargetUnavailable()
        val target = when (
            val resolution = resolveTarget(
                mode,
                inputs.todayStatus,
                inputs.profile,
                inputs.approvedWfaBooking
            )
        ) {
            is TargetLocationResolution.Resolved -> resolution.target
            is TargetLocationResolution.Failed -> return activeTargetUnavailable(
                failureForActiveTarget(resolution.failure, mode)
            )

            is TargetLocationResolution.Resolving,
            is TargetLocationResolution.Unavailable -> return activeTargetUnavailable()
        }

        return GeofenceRuntimeModeResolution(
            GeofenceRuntimeMode.ActiveMonitoring(
                effectiveDate = effectiveDate,
                attendanceId = attendanceId,
                target = target.toActiveMonitoringTarget(inputs)
            )
        )
    }

    private fun AuthoritativeTargetLocation.toActiveMonitoringTarget(
        inputs: GeofenceRuntimeInputs
    ): ActiveMonitoringTarget = ActiveMonitoringTarget(
        identity = identityFor(inputs),
        mode = mode,
        label = displayName,
        coordinate = coordinate,
        radius = radius
    )

    private fun AuthoritativeTargetLocation.identityFor(inputs: GeofenceRuntimeInputs): GeofenceTargetIdentity = when (mode) {
        WorkMode.WFO -> GeofenceTargetIdentity(
            stableLocationId = inputs.todayStatus.activeLocation?.locationId,
            ownerKey = "primary:${targetId.value}"
        )

        WorkMode.WFA -> {
            val booking = (inputs.approvedWfaBooking as? WfaBookingForDate.Approved)?.booking
            GeofenceTargetIdentity(
                stableLocationId = booking?.locationId,
                ownerKey = "wfa:${booking?.bookingId ?: targetId.value}"
            )
        }

        WorkMode.WFH -> GeofenceTargetIdentity(
            stableLocationId = null,
            ownerKey = "user-home:${inputs.profile?.id}"
        )
    }

    private fun inconsistentSessionTruth(
        attendanceId: Int?,
        stateKey: String?
    ) = GeofenceRuntimeModeResolution(
        mode = GeofenceRuntimeMode.Disabled(GeofenceDisabledReason.INCONSISTENT_SESSION_TRUTH),
        blockingFailure = GeofenceRuntimeFailure.InconsistentSessionTruth(attendanceId, stateKey)
    )

    private fun failureForActiveTarget(
        failure: TargetResolutionFailure,
        mode: WorkMode
    ): GeofenceRuntimeFailure = when (failure) {
        TargetResolutionFailure.INVALID_RADIUS -> GeofenceRuntimeFailure.InvalidAuthoritativeRadius(sourceFor(mode))
        else -> GeofenceRuntimeFailure.ActiveTargetUnavailable
    }

    private fun sourceFor(mode: WorkMode): ReminderCandidateSource = when (mode) {
        WorkMode.WFO -> ReminderCandidateSource.STATUS_TODAY
        WorkMode.WFA -> ReminderCandidateSource.APPROVED_WFA_BOOKING
        WorkMode.WFH -> ReminderCandidateSource.USER_PROFILE
    }

    private fun activeTargetUnavailable(
        blockingFailure: GeofenceRuntimeFailure = GeofenceRuntimeFailure.ActiveTargetUnavailable
    ) = GeofenceRuntimeModeResolution(
        mode = GeofenceRuntimeMode.Disabled(GeofenceDisabledReason.ACTIVE_TARGET_UNAVAILABLE),
        blockingFailure = blockingFailure
    )

    private fun disabledForUnavailableStatusTruth() = GeofenceRuntimeModeResolution(
        mode = GeofenceRuntimeMode.Disabled(GeofenceDisabledReason.NO_ELIGIBLE_SESSION),
        blockingFailure = GeofenceRuntimeFailure.BackendTruthUnavailable(BackendTruthSource.STATUS_TODAY)
    )

    private companion object {
        const val SESSION_STATE_ACTIVE = "active"
        const val SESSION_STATE_COMPLETED = "completed"
        const val SESSION_STATE_NOT_STARTED = "not_started"
    }
}
