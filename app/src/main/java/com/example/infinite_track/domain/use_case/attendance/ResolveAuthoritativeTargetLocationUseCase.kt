package com.example.infinite_track.domain.use_case.attendance

import com.example.infinite_track.domain.model.attendance.ApprovedWfaTargetContext
import com.example.infinite_track.domain.model.attendance.AuthoritativeTargetLocation
import com.example.infinite_track.domain.model.attendance.TargetLocationId
import com.example.infinite_track.domain.model.attendance.TargetLocationResolution
import com.example.infinite_track.domain.model.attendance.TargetLocationSource
import com.example.infinite_track.domain.model.attendance.TargetRecoveryAction
import com.example.infinite_track.domain.model.attendance.TargetResolutionFailure
import com.example.infinite_track.domain.model.attendance.TargetUnavailableReason
import com.example.infinite_track.domain.model.attendance.TodayStatus
import com.example.infinite_track.domain.model.attendance.WorkMode
import com.example.infinite_track.domain.model.auth.UserModel
import com.example.infinite_track.domain.model.booking.BookingHistoryItem
import com.example.infinite_track.domain.model.location.DistanceMeters
import com.example.infinite_track.domain.model.location.GeoCoordinate
import com.example.infinite_track.domain.model.wfa.WfaBookingForDate
import javax.inject.Inject

class ResolveAuthoritativeTargetLocationUseCase @Inject constructor() {
    operator fun invoke(
        mode: WorkMode,
        todayStatus: TodayStatus?,
        profile: UserModel?,
        wfaBooking: WfaBookingForDate
    ): TargetLocationResolution {
        return when (mode) {
            WorkMode.WFO -> resolveWfo(todayStatus)
            WorkMode.WFH -> resolveWfh(profile)
            WorkMode.WFA -> resolveWfa(todayStatus, wfaBooking)
        }
    }

    private fun resolveWfo(todayStatus: TodayStatus?): TargetLocationResolution {
        val location = todayStatus?.activeLocation ?: return unavailable(
            mode = WorkMode.WFO,
            reason = TargetUnavailableReason.WFO_NOT_ASSIGNED,
            recovery = TargetRecoveryAction.REFRESH_STATUS
        )
        val radius = positiveRadius(location.radius.toDouble()) ?: return invalidRadius(WorkMode.WFO)

        return resolved(
            targetId = TargetLocationId("status:${location.locationId}"),
            mode = WorkMode.WFO,
            source = TargetLocationSource.STATUS_TODAY,
            coordinate = location.coordinate,
            radius = radius,
            displayName = location.description
        )
    }

    private fun resolveWfh(profile: UserModel?): TargetLocationResolution {
        val user = profile ?: return invalidWfhProfile()
        val latitude = user.latitude ?: return invalidWfhProfile()
        val longitude = user.longitude ?: return invalidWfhProfile()
        val radiusValue = user.radius ?: return invalidRadius(WorkMode.WFH)
        val coordinate = coordinateOrNull(latitude, longitude) ?: return invalidCoordinate(WorkMode.WFH)
        val radius = positiveRadius(radiusValue.toDouble()) ?: return invalidRadius(WorkMode.WFH)

        return resolved(
            targetId = TargetLocationId("profile:${user.id}"),
            mode = WorkMode.WFH,
            source = TargetLocationSource.ADMIN_PROFILE,
            coordinate = coordinate,
            radius = radius,
            displayName = user.locationDescription ?: user.locationCategoryName ?: "WFH"
        )
    }

    private fun resolveWfa(
        todayStatus: TodayStatus?,
        wfaBooking: WfaBookingForDate
    ): TargetLocationResolution {
        return when (wfaBooking) {
            WfaBookingForDate.NotRequested -> unavailable(
                mode = WorkMode.WFA,
                reason = TargetUnavailableReason.WFA_NOT_REQUESTED,
                recovery = TargetRecoveryAction.OPEN_WFA_BOOKING
            )

            is WfaBookingForDate.Pending -> unavailable(
                mode = WorkMode.WFA,
                reason = TargetUnavailableReason.WFA_PENDING,
                recovery = TargetRecoveryAction.OPEN_WFA_REQUESTS
            )

            is WfaBookingForDate.Rejected -> unavailable(
                mode = WorkMode.WFA,
                reason = TargetUnavailableReason.WFA_REJECTED,
                recovery = TargetRecoveryAction.OPEN_WFA_REQUESTS
            )

            is WfaBookingForDate.Approved -> resolveApprovedWfa(todayStatus, wfaBooking.booking)

            is WfaBookingForDate.Failed -> TargetLocationResolution.Failed(
                mode = WorkMode.WFA,
                failure = TargetResolutionFailure.BOOKING_REFRESH_FAILED
            )
        }
    }

    private fun resolveApprovedWfa(
        todayStatus: TodayStatus?,
        booking: BookingHistoryItem
    ): TargetLocationResolution {
        if (booking.scheduleDateRaw != todayStatus?.todayDate) {
            return unavailable(
                mode = WorkMode.WFA,
                reason = TargetUnavailableReason.WFA_APPROVAL_MISSING_FOR_DATE,
                recovery = TargetRecoveryAction.OPEN_WFA_REQUESTS
            )
        }

        val latitude = booking.latitude ?: return invalidCoordinate(WorkMode.WFA)
        val longitude = booking.longitude ?: return invalidCoordinate(WorkMode.WFA)
        val coordinate = coordinateOrNull(latitude, longitude) ?: return invalidCoordinate(WorkMode.WFA)
        val radius = positiveRadius(booking.radiusMeters?.toDouble()) ?: return invalidRadius(WorkMode.WFA)

        return resolved(
            targetId = TargetLocationId("booking:${booking.bookingId}"),
            mode = WorkMode.WFA,
            source = TargetLocationSource.APPROVED_WFA_BOOKING,
            coordinate = coordinate,
            radius = radius,
            displayName = booking.locationDescription,
            approvedWfaContext = ApprovedWfaTargetContext(
                bookingId = booking.bookingId,
                scheduleDate = booking.scheduleDateRaw,
                scheduleDateDisplay = booking.scheduleDate
            )
        )
    }

    private fun resolved(
        targetId: TargetLocationId,
        mode: WorkMode,
        source: TargetLocationSource,
        coordinate: GeoCoordinate,
        radius: DistanceMeters,
        displayName: String,
        approvedWfaContext: ApprovedWfaTargetContext? = null
    ) = TargetLocationResolution.Resolved(
        AuthoritativeTargetLocation(
            targetId = targetId,
            mode = mode,
            source = source,
            coordinate = coordinate,
            radius = radius,
            displayName = displayName,
            approvedWfaContext = approvedWfaContext
        )
    )

    private fun unavailable(
        mode: WorkMode,
        reason: TargetUnavailableReason,
        recovery: TargetRecoveryAction
    ) = TargetLocationResolution.Unavailable(mode, reason, recovery)

    private fun invalidWfhProfile() = unavailable(
        mode = WorkMode.WFH,
        reason = TargetUnavailableReason.WFH_PROFILE_CONTRACT_VIOLATION,
        recovery = TargetRecoveryAction.REFRESH_PROFILE
    )

    private fun invalidCoordinate(mode: WorkMode) = TargetLocationResolution.Failed(
        mode = mode,
        failure = TargetResolutionFailure.INVALID_COORDINATE
    )

    private fun invalidRadius(mode: WorkMode) = TargetLocationResolution.Failed(
        mode = mode,
        failure = TargetResolutionFailure.INVALID_RADIUS
    )

    private fun coordinateOrNull(latitude: Double, longitude: Double): GeoCoordinate? = try {
        GeoCoordinate(latitude, longitude)
    } catch (_: IllegalArgumentException) {
        null
    }

    private fun positiveRadius(value: Double?): DistanceMeters? {
        if (value == null || !value.isFinite() || value <= 0.0) return null
        return DistanceMeters(value)
    }
}
