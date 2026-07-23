package com.example.infinite_track.domain.use_case.attendance

import com.example.infinite_track.domain.model.attendance.CheckinWindow
import com.example.infinite_track.domain.model.attendance.Location
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
import com.example.infinite_track.domain.model.wfa.WfaBookingForDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ResolveAuthoritativeTargetLocationUseCaseTest {

    private val resolver = ResolveAuthoritativeTargetLocationUseCase()
    private val office = Location(
        locationId = 1,
        description = "Kantor Infinite Track",
        latitude = -0.87,
        longitude = 119.86,
        radius = 100,
        category = "Office"
    )

    @Test
    fun `WFO resolves only from status today active location`() {
        val result = resolver(
            mode = WorkMode.WFO,
            todayStatus = todayStatus(activeLocation = office),
            profile = user(homeLatitude = -0.90, homeLongitude = 119.88, radius = 100),
            wfaBooking = WfaBookingForDate.NotRequested
        ) as TargetLocationResolution.Resolved

        assertEquals(TargetLocationSource.STATUS_TODAY, result.target.source)
        assertEquals(TargetLocationId("status:1"), result.target.targetId)
        assertEquals(office.coordinate, result.target.coordinate)
    }

    @Test
    fun `WFH resolves only from the authenticated profile`() {
        val result = resolver(
            mode = WorkMode.WFH,
            todayStatus = todayStatus(activeLocation = office),
            profile = user(homeLatitude = -0.90, homeLongitude = 119.88, radius = 120),
            wfaBooking = WfaBookingForDate.NotRequested
        ) as TargetLocationResolution.Resolved

        assertEquals(TargetLocationSource.ADMIN_PROFILE, result.target.source)
        assertEquals(TargetLocationId("profile:7"), result.target.targetId)
        assertEquals("Rumah", result.target.displayName)
        assertEquals(120.0, result.target.radius.value, 0.0)
    }

    @Test
    fun `WFH reports profile contract violation when home location is missing`() {
        val result = resolver(
            mode = WorkMode.WFH,
            todayStatus = todayStatus(activeLocation = office),
            profile = user(homeLatitude = null, homeLongitude = null, radius = null),
            wfaBooking = WfaBookingForDate.NotRequested
        )

        assertEquals(
            TargetLocationResolution.Unavailable(
                mode = WorkMode.WFH,
                reason = TargetUnavailableReason.WFH_PROFILE_CONTRACT_VIOLATION,
                recovery = TargetRecoveryAction.REFRESH_PROFILE
            ),
            result
        )
    }

    @Test
    fun `WFA resolves only from approved booking for attendance date`() {
        val result = resolver(
            mode = WorkMode.WFA,
            todayStatus = todayStatus(todayDate = "2026-07-23"),
            profile = user(homeLatitude = -0.90, homeLongitude = 119.88, radius = 100),
            wfaBooking = WfaBookingForDate.Approved(
                booking(
                    bookingId = 23,
                    scheduleDate = "2026-07-23",
                    latitude = -0.92,
                    longitude = 119.89,
                    radiusMeters = 150f
                )
            )
        ) as TargetLocationResolution.Resolved

        assertEquals(TargetLocationSource.APPROVED_WFA_BOOKING, result.target.source)
        assertEquals(TargetLocationId("booking:23"), result.target.targetId)
        assertEquals(23, result.target.approvedWfaContext?.bookingId)
        assertEquals("2026-07-23", result.target.approvedWfaContext?.scheduleDate)
    }

    @Test
    fun `WFA rejects approved booking from another date`() {
        val result = resolver(
            mode = WorkMode.WFA,
            todayStatus = todayStatus(todayDate = "2026-07-23"),
            profile = user(homeLatitude = -0.90, homeLongitude = 119.88, radius = 100),
            wfaBooking = WfaBookingForDate.Approved(
                booking(
                    bookingId = 23,
                    scheduleDate = "2026-07-24",
                    latitude = -0.92,
                    longitude = 119.89,
                    radiusMeters = 150f
                )
            )
        )

        assertEquals(
            TargetLocationResolution.Unavailable(
                mode = WorkMode.WFA,
                reason = TargetUnavailableReason.WFA_APPROVAL_MISSING_FOR_DATE,
                recovery = TargetRecoveryAction.OPEN_WFA_REQUESTS
            ),
            result
        )
    }

    @Test
    fun `resolver rejects a non-positive authoritative radius`() {
        val result = resolver(
            mode = WorkMode.WFO,
            todayStatus = todayStatus(activeLocation = office.copy(radius = 0)),
            profile = user(homeLatitude = -0.90, homeLongitude = 119.88, radius = 100),
            wfaBooking = WfaBookingForDate.NotRequested
        )

        assertEquals(
            TargetLocationResolution.Failed(
                mode = WorkMode.WFO,
                failure = TargetResolutionFailure.INVALID_RADIUS
            ),
            result
        )
    }

    @Test
    fun `resolver rejects a missing WFA radius without using a default`() {
        val result = resolver(
            mode = WorkMode.WFA,
            todayStatus = todayStatus(todayDate = "2026-07-23"),
            profile = user(homeLatitude = -0.90, homeLongitude = 119.88, radius = 100),
            wfaBooking = WfaBookingForDate.Approved(
                booking(
                    bookingId = 23,
                    scheduleDate = "2026-07-23",
                    latitude = -0.92,
                    longitude = 119.89,
                    radiusMeters = null
                )
            )
        )

        assertEquals(
            TargetLocationResolution.Failed(
                mode = WorkMode.WFA,
                failure = TargetResolutionFailure.INVALID_RADIUS
            ),
            result
        )
    }

    @Test
    fun `recommendation type is not accepted by authoritative resolver`() {
        val parameters = ResolveAuthoritativeTargetLocationUseCase::class.java.methods
            .single { it.name == "invoke" }
            .parameterTypes

        assertFalse(parameters.any { it.simpleName == "WfaRecommendation" })
    }

    private fun todayStatus(
        activeLocation: Location? = null,
        todayDate: String = "2026-07-23"
    ) = TodayStatus(
        canCheckIn = true,
        canCheckOut = false,
        checkedInAt = null,
        checkedOutAt = null,
        activeMode = WorkMode.WFO.shortLabel,
        activeLocation = activeLocation,
        todayDate = todayDate,
        isHoliday = false,
        holidayCheckinEnabled = false,
        currentTime = "08:00:00",
        checkinWindow = CheckinWindow(startTime = "08:00", endTime = "17:00"),
        checkoutAutoTime = "17:00"
    )

    private fun user(
        homeLatitude: Double?,
        homeLongitude: Double?,
        radius: Int?
    ) = UserModel(
        id = 7,
        fullName = "Febriyadi",
        email = "febriyadi@example.com",
        roleName = "Employee",
        positionName = null,
        programName = null,
        divisionName = null,
        nipNim = "123",
        phone = null,
        photoUrl = null,
        photoUpdatedAt = null,
        latitude = homeLatitude,
        longitude = homeLongitude,
        radius = radius,
        locationDescription = "Rumah",
        locationCategoryName = "Home"
    )

    private fun booking(
        bookingId: Int,
        scheduleDate: String,
        latitude: Double?,
        longitude: Double?,
        radiusMeters: Float?
    ) = BookingHistoryItem(
        id = bookingId.toString(),
        locationDescription = "Cafe Produktif",
        scheduleDate = scheduleDate,
        status = "Approved",
        notes = "",
        suitabilityLabel = "Suitable",
        bookingId = bookingId,
        scheduleDateRaw = scheduleDate,
        latitude = latitude,
        longitude = longitude,
        radiusMeters = radiusMeters
    )
}
