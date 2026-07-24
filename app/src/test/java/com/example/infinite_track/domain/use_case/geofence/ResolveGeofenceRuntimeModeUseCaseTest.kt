package com.example.infinite_track.domain.use_case.geofence

import com.example.infinite_track.domain.model.attendance.AttendanceSessionState
import com.example.infinite_track.domain.model.attendance.CheckinWindow
import com.example.infinite_track.domain.model.attendance.Location
import com.example.infinite_track.domain.model.attendance.TodayStatus
import com.example.infinite_track.domain.model.attendance.WorkMode
import com.example.infinite_track.domain.model.booking.BookingHistoryItem
import com.example.infinite_track.domain.model.geofence.BackendTruthSource
import com.example.infinite_track.domain.model.geofence.GeofenceDisabledReason
import com.example.infinite_track.domain.model.geofence.GeofenceReconcileReason
import com.example.infinite_track.domain.model.geofence.GeofenceRuntimeFailure
import com.example.infinite_track.domain.model.geofence.GeofenceRuntimeInputs
import com.example.infinite_track.domain.model.geofence.GeofenceRuntimeMode
import com.example.infinite_track.domain.model.geofence.GeofenceRuntimeModeResolution
import com.example.infinite_track.domain.model.geofence.GeofenceRuntimeReadiness
import com.example.infinite_track.domain.model.geofence.GeofenceTargetIdentity
import com.example.infinite_track.domain.model.geofence.NotificationReadiness
import com.example.infinite_track.domain.model.geofence.RegistrationReadiness
import com.example.infinite_track.domain.model.wfa.WfaBookingForDate
import com.example.infinite_track.domain.use_case.attendance.ResolveAuthoritativeTargetLocationUseCase
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ResolveGeofenceRuntimeModeUseCaseTest {

    private val useCase = ResolveGeofenceRuntimeModeUseCase(
        BuildReminderGeofenceCandidatesUseCase(ResolveAuthoritativeTargetLocationUseCase()),
        ResolveAuthoritativeTargetLocationUseCase()
    )

    @Test
    fun `not started eligible status resolves reminder`() {
        val result = useCase(inputs())

        val mode = result.mode as GeofenceRuntimeMode.Reminder
        assertEquals(LocalDate.parse("2026-07-24"), mode.effectiveDate)
        assertNull(result.blockingFailure)
    }

    @Test
    fun `active id and active state resolve one active target`() {
        val result = useCase(inputs(attendanceId = 91, stateKey = "active", canCheckIn = false))

        val mode = result.mode as GeofenceRuntimeMode.ActiveMonitoring
        assertEquals(LocalDate.parse("2026-07-24"), mode.effectiveDate)
        assertEquals(91, mode.attendanceId)
        assertEquals(WorkMode.WFO, mode.target.mode)
        assertEquals("Kantor Infinite Track", mode.target.label)
        assertNull(result.blockingFailure)
    }

    @Test
    fun `active WFA target keeps authoritative booking location identity`() {
        val result = useCase(
            inputs(
                attendanceId = 91,
                stateKey = "active",
                canCheckIn = false,
                activeMode = WorkMode.WFA,
                approvedWfaBooking = WfaBookingForDate.Approved(booking(locationId = 22))
            )
        )

        val mode = result.mode as GeofenceRuntimeMode.ActiveMonitoring
        assertEquals(GeofenceTargetIdentity(22, "wfa:31"), mode.target.identity)
        assertEquals("Cafe Produktif", mode.target.label)
        assertEquals(WorkMode.WFA, mode.target.mode)
    }

    @Test
    fun `active location without active session never resolves active`() {
        val result = useCase(inputs(canCheckIn = false))

        assertEquals(
            GeofenceRuntimeMode.Disabled(GeofenceDisabledReason.NO_ELIGIBLE_SESSION),
            result.mode
        )
        assertNull(result.blockingFailure)
    }

    @Test
    fun `completed state resolves completed`() {
        val result = useCase(inputs(stateKey = "completed", canCheckIn = false))

        assertEquals(GeofenceRuntimeMode.Completed(LocalDate.parse("2026-07-24")), result.mode)
        assertNull(result.blockingFailure)
    }

    @Test
    fun `checkout success and can check in false resolves completed`() {
        val result = useCase(
            inputs(
                canCheckIn = false,
                reason = GeofenceReconcileReason.CHECK_OUT_SUCCEEDED
            )
        )

        assertEquals(GeofenceRuntimeMode.Completed(LocalDate.parse("2026-07-24")), result.mode)
        assertNull(result.blockingFailure)
    }

    @Test
    fun `non checkout can check in false resolves disabled`() {
        val result = useCase(inputs(canCheckIn = false))

        assertEquals(
            GeofenceRuntimeMode.Disabled(GeofenceDisabledReason.NO_ELIGIBLE_SESSION),
            result.mode
        )
        assertNull(result.blockingFailure)
    }

    @Test
    fun `active id with non active state resolves safe disabled with typed failure`() {
        val result = useCase(inputs(attendanceId = 91, stateKey = "completed", canCheckIn = false))

        assertInconsistentTruth(result)
    }

    @Test
    fun `active state without attendance id resolves safe disabled with typed failure`() {
        val result = useCase(inputs(stateKey = "active", canCheckIn = false))

        assertEquals(
            GeofenceRuntimeMode.Disabled(GeofenceDisabledReason.INCONSISTENT_SESSION_TRUTH),
            result.mode
        )
        assertEquals(
            GeofenceRuntimeFailure.InconsistentSessionTruth(null, "active"),
            result.blockingFailure
        )
    }

    @Test
    fun `invalid active target resolves safe disabled with typed failure`() {
        val result = useCase(
            inputs(
                attendanceId = 91,
                stateKey = "active",
                canCheckIn = false,
                locationRadius = 0
            )
        )

        assertEquals(
            GeofenceRuntimeMode.Disabled(GeofenceDisabledReason.ACTIVE_TARGET_UNAVAILABLE),
            result.mode
        )
        assertEquals(GeofenceRuntimeFailure.ActiveTargetUnavailable, result.blockingFailure)
    }

    @Test
    fun `invalid today date resolves safe disabled with backend truth failure`() {
        val result = useCase(inputs(todayDate = "24-07-2026"))

        assertEquals(
            GeofenceRuntimeMode.Disabled(GeofenceDisabledReason.NO_ELIGIBLE_SESSION),
            result.mode
        )
        assertEquals(
            GeofenceRuntimeFailure.BackendTruthUnavailable(BackendTruthSource.STATUS_TODAY),
            result.blockingFailure
        )
    }

    private fun assertInconsistentTruth(result: GeofenceRuntimeModeResolution) {
        assertEquals(
            GeofenceRuntimeMode.Disabled(GeofenceDisabledReason.INCONSISTENT_SESSION_TRUTH),
            result.mode
        )
        assertEquals(
            GeofenceRuntimeFailure.InconsistentSessionTruth(91, "completed"),
            result.blockingFailure
        )
    }

    private fun inputs(
        attendanceId: Int? = null,
        stateKey: String = "not_started",
        canCheckIn: Boolean = true,
        locationRadius: Int = 100,
        todayDate: String = "2026-07-24",
        reason: GeofenceReconcileReason = GeofenceReconcileReason.FOREGROUND_REFRESH,
        activeMode: WorkMode = WorkMode.WFO,
        approvedWfaBooking: WfaBookingForDate = WfaBookingForDate.NotRequested
    ) = GeofenceRuntimeInputs(
        todayStatus = TodayStatus(
            canCheckIn = canCheckIn,
            canCheckOut = attendanceId != null,
            checkedInAt = null,
            checkedOutAt = null,
            activeMode = activeMode.shortLabel,
            activeLocation = Location(
                locationId = 11,
                description = "Kantor Infinite Track",
                latitude = -0.87,
                longitude = 119.86,
                radius = locationRadius,
                category = "Office"
            ),
            todayDate = todayDate,
            isHoliday = false,
            holidayCheckinEnabled = false,
            currentTime = "08:00:00",
            checkinWindow = CheckinWindow(startTime = "08:00", endTime = "17:00"),
            checkoutAutoTime = "17:00",
            attendanceSessionState = AttendanceSessionState(id = 1, key = stateKey, label = stateKey),
            activeAttendanceId = attendanceId
        ),
        profile = null,
        approvedWfaBooking = approvedWfaBooking,
        readiness = GeofenceRuntimeReadiness(
            registration = RegistrationReadiness.Ready,
            notification = NotificationReadiness.READY
        ),
        reason = reason
    )

    private fun booking(locationId: Int?) = BookingHistoryItem(
        id = "31",
        locationDescription = "Cafe Produktif",
        scheduleDate = "24 Jul 2026",
        status = "Approved",
        notes = "",
        suitabilityLabel = "Suitable",
        bookingId = 31,
        scheduleDateRaw = "2026-07-24",
        locationId = locationId,
        latitude = -0.92,
        longitude = 119.89,
        radiusMeters = 150f
    )
}
