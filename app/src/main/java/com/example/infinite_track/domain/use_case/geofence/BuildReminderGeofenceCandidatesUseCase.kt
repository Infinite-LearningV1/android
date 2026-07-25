package com.example.infinite_track.domain.use_case.geofence

import com.example.infinite_track.domain.model.attendance.AuthoritativeTargetLocation
import com.example.infinite_track.domain.model.attendance.TargetLocationResolution
import com.example.infinite_track.domain.model.attendance.TargetResolutionFailure
import com.example.infinite_track.domain.model.attendance.TodayStatus
import com.example.infinite_track.domain.model.attendance.WorkMode
import com.example.infinite_track.domain.model.auth.UserModel
import com.example.infinite_track.domain.model.geofence.GeofenceRuntimeFailure
import com.example.infinite_track.domain.model.geofence.GeofenceTargetIdentity
import com.example.infinite_track.domain.model.geofence.ReminderCandidateSource
import com.example.infinite_track.domain.model.geofence.ReminderGeofenceCandidate
import com.example.infinite_track.domain.model.location.GeoCoordinate
import com.example.infinite_track.domain.model.wfa.WfaBookingForDate
import com.example.infinite_track.domain.use_case.attendance.ResolveAuthoritativeTargetLocationUseCase
import javax.inject.Inject
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class ReminderCandidateBuildResult(
    val candidates: List<ReminderGeofenceCandidate>,
    val failures: List<GeofenceRuntimeFailure>
)

class BuildReminderGeofenceCandidatesUseCase @Inject constructor(
    private val resolveTarget: ResolveAuthoritativeTargetLocationUseCase
) {
    operator fun invoke(
        todayStatus: TodayStatus,
        profile: UserModel?,
        wfaBooking: WfaBookingForDate
    ): ReminderCandidateBuildResult {
        val failures = mutableListOf<GeofenceRuntimeFailure>()
        val raw = listOfNotNull(
            resolveCandidate(WorkMode.WFO, todayStatus, profile, wfaBooking, failures),
            resolveCandidate(WorkMode.WFA, todayStatus, profile, wfaBooking, failures),
            resolveCandidate(WorkMode.WFH, todayStatus, profile, wfaBooking, failures)
        )
        val ordered = raw.sortedBy { sourcePriority(it.source) }
        val deduplicated = ordered.fold(mutableListOf<ReminderGeofenceCandidate>()) { kept, candidate ->
            val duplicate = kept.any { existing ->
                sameNonNullStableLocation(existing, candidate) ||
                    existing.identity.ownerKey == candidate.identity.ownerKey ||
                    distanceMeters(existing.coordinate, candidate.coordinate) <= 10.0
            }
            if (!duplicate) kept += candidate
            kept
        }

        return ReminderCandidateBuildResult(candidates = deduplicated, failures = failures)
    }

    private fun resolveCandidate(
        mode: WorkMode,
        todayStatus: TodayStatus,
        profile: UserModel?,
        wfaBooking: WfaBookingForDate,
        failures: MutableList<GeofenceRuntimeFailure>
    ): ReminderGeofenceCandidate? {
        return when (val resolution = resolveTarget(mode, todayStatus, profile, wfaBooking)) {
            is TargetLocationResolution.Resolved -> candidateFor(
                target = resolution.target,
                mode = mode,
                todayStatus = todayStatus,
                profile = profile,
                wfaBooking = wfaBooking
            )

            is TargetLocationResolution.Failed -> {
                failureFor(mode, resolution.failure)?.let(failures::add)
                null
            }

            is TargetLocationResolution.Unavailable -> null
            is TargetLocationResolution.Resolving -> null
        }
    }

    private fun candidateFor(
        target: AuthoritativeTargetLocation,
        mode: WorkMode,
        todayStatus: TodayStatus,
        profile: UserModel?,
        wfaBooking: WfaBookingForDate
    ): ReminderGeofenceCandidate {
        val source = sourceFor(mode)
        val identity = when (mode) {
            WorkMode.WFO -> GeofenceTargetIdentity(
                todayStatus.activeLocation?.locationId,
                "primary:${target.targetId.value}"
            )

            WorkMode.WFA -> {
                val booking = (wfaBooking as? WfaBookingForDate.Approved)?.booking
                    ?: error("Resolved WFA target must have an approved booking")
                GeofenceTargetIdentity(booking.locationId, "wfa:${booking.bookingId}")
            }

            WorkMode.WFH -> GeofenceTargetIdentity(null, "user-home:${profile?.id}")
        }

        return ReminderGeofenceCandidate(
            logicalId = logicalIdFor(mode, todayStatus, profile, wfaBooking),
            identity = identity,
            mode = mode,
            label = target.displayName,
            coordinate = target.coordinate,
            radius = target.radius,
            source = source
        )
    }

    private fun logicalIdFor(
        mode: WorkMode,
        todayStatus: TodayStatus,
        profile: UserModel?,
        wfaBooking: WfaBookingForDate
    ): String = when (mode) {
        WorkMode.WFO -> "reminder:primary:${todayStatus.activeLocation?.locationId}"
        WorkMode.WFA -> {
            val booking = (wfaBooking as WfaBookingForDate.Approved).booking
            "reminder:wfa:${booking.bookingId}:${booking.locationId ?: 0}"
        }

        WorkMode.WFH -> "reminder:wfh:user_home:${profile?.id}"
    }

    private fun failureFor(
        mode: WorkMode,
        failure: TargetResolutionFailure
    ): GeofenceRuntimeFailure? = when (failure) {
        TargetResolutionFailure.STATUS_REFRESH_FAILED,
        TargetResolutionFailure.PROFILE_REFRESH_FAILED,
        TargetResolutionFailure.BOOKING_REFRESH_FAILED -> null
        TargetResolutionFailure.INVALID_COORDINATE -> GeofenceRuntimeFailure.InvalidCoordinate(sourceFor(mode))
        TargetResolutionFailure.INVALID_RADIUS -> GeofenceRuntimeFailure.InvalidAuthoritativeRadius(sourceFor(mode))
    }

    private fun sourceFor(mode: WorkMode): ReminderCandidateSource = when (mode) {
        WorkMode.WFO -> ReminderCandidateSource.STATUS_TODAY
        WorkMode.WFA -> ReminderCandidateSource.APPROVED_WFA_BOOKING
        WorkMode.WFH -> ReminderCandidateSource.USER_PROFILE
    }

    private fun sourcePriority(source: ReminderCandidateSource): Int = when (source) {
        ReminderCandidateSource.STATUS_TODAY -> 0
        ReminderCandidateSource.APPROVED_WFA_BOOKING -> 1
        ReminderCandidateSource.USER_PROFILE -> 2
    }

    private fun sameNonNullStableLocation(
        first: ReminderGeofenceCandidate,
        second: ReminderGeofenceCandidate
    ): Boolean = first.identity.stableLocationId != null &&
        first.identity.stableLocationId == second.identity.stableLocationId

    private fun distanceMeters(first: GeoCoordinate, second: GeoCoordinate): Double {
        val latitudeDelta = Math.toRadians(second.latitude - first.latitude)
        val longitudeDelta = Math.toRadians(second.longitude - first.longitude)
        val latitude1 = Math.toRadians(first.latitude)
        val latitude2 = Math.toRadians(second.latitude)
        val haversine = sin(latitudeDelta / 2).pow(2) +
            cos(latitude1) * cos(latitude2) * sin(longitudeDelta / 2).pow(2)
        return 2 * EARTH_RADIUS_METERS * asin(sqrt(haversine))
    }

    private companion object {
        const val EARTH_RADIUS_METERS = 6_371_000.0
    }
}
