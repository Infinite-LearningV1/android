package com.example.infinite_track.domain.use_case.geofence

import com.example.infinite_track.domain.model.attendance.CheckinWindow
import com.example.infinite_track.domain.model.attendance.Location
import com.example.infinite_track.domain.model.attendance.TodayStatus
import com.example.infinite_track.domain.model.attendance.WorkMode
import com.example.infinite_track.domain.model.auth.UserModel
import com.example.infinite_track.domain.model.booking.BookingHistoryItem
import com.example.infinite_track.domain.model.geofence.GeofenceRuntimeFailure
import com.example.infinite_track.domain.model.geofence.ReminderCandidateSource
import com.example.infinite_track.domain.model.wfa.WfaBookingForDate
import com.example.infinite_track.domain.use_case.attendance.ResolveAuthoritativeTargetLocationUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class BuildReminderGeofenceCandidatesUseCaseTest {

    private val useCase = BuildReminderGeofenceCandidatesUseCase(
        ResolveAuthoritativeTargetLocationUseCase()
    )

    @Test
    fun `builds WFO WFH and approved WFA candidates`() {
        val result = useCase(
            todayStatus = todayStatus(locationId = 11),
            profile = user(),
            wfaBooking = WfaBookingForDate.Approved(booking(bookingId = 31, locationId = 22))
        )

        assertEquals(
            listOf(
                "reminder:primary:11",
                "reminder:wfa:31:22",
                "reminder:wfh:user_home:7"
            ),
            result.candidates.map { it.logicalId }
        )
    }

    @Test
    fun `deduplicates WFA against primary by stable location id`() {
        val result = useCase(
            todayStatus = todayStatus(locationId = 22),
            profile = null,
            wfaBooking = WfaBookingForDate.Approved(booking(bookingId = 31, locationId = 22))
        )

        assertEquals(listOf("reminder:primary:22"), result.candidates.map { it.logicalId })
    }

    @Test
    fun `deduplicates targets within ten meters`() {
        val result = useCase(
            todayStatus = todayStatus(latitude = -0.870000, longitude = 119.860000),
            profile = user(latitude = -0.870045, longitude = 119.860000),
            wfaBooking = WfaBookingForDate.NotRequested
        )

        assertEquals(listOf("reminder:primary:11"), result.candidates.map { it.logicalId })
    }

    @Test
    fun `keeps targets outside ten meters`() {
        val result = useCase(
            todayStatus = todayStatus(latitude = -0.870000, longitude = 119.860000),
            profile = user(latitude = -0.870100, longitude = 119.860000),
            wfaBooking = WfaBookingForDate.NotRequested
        )

        assertEquals(
            listOf("reminder:primary:11", "reminder:wfh:user_home:7"),
            result.candidates.map { it.logicalId }
        )
    }

    @Test
    fun `uses primary then WFA then WFH source priority`() {
        val result = useCase(
            todayStatus = todayStatus(),
            profile = user(),
            wfaBooking = WfaBookingForDate.Approved(booking())
        )

        assertEquals(
            listOf(
                ReminderCandidateSource.STATUS_TODAY,
                ReminderCandidateSource.APPROVED_WFA_BOOKING,
                ReminderCandidateSource.USER_PROFILE
            ),
            result.candidates.map { it.source }
        )
    }

    @Test
    fun `reports invalid WFA radius without default`() {
        val result = useCase(
            todayStatus = todayStatus(),
            profile = null,
            wfaBooking = WfaBookingForDate.Approved(booking(radiusMeters = null))
        )

        assertFalse(result.candidates.any { it.source == ReminderCandidateSource.APPROVED_WFA_BOOKING })
        assertEquals(
            listOf(GeofenceRuntimeFailure.InvalidAuthoritativeRadius(ReminderCandidateSource.APPROVED_WFA_BOOKING)),
            result.failures
        )
    }

    @Test
    fun `reports invalid WFH coordinate and keeps valid WFO`() {
        val result = useCase(
            todayStatus = todayStatus(),
            profile = user(latitude = 91.0),
            wfaBooking = WfaBookingForDate.NotRequested
        )

        assertEquals(listOf("reminder:primary:11"), result.candidates.map { it.logicalId })
        assertEquals(
            listOf(GeofenceRuntimeFailure.InvalidCoordinate(ReminderCandidateSource.USER_PROFILE)),
            result.failures
        )
    }

    @Test
    fun `uses stable logical ids`() {
        val result = useCase(
            todayStatus = todayStatus(locationId = 11),
            profile = user(),
            wfaBooking = WfaBookingForDate.Approved(booking(bookingId = 31, locationId = null))
        )

        assertEquals(
            listOf(
                "reminder:primary:11",
                "reminder:wfa:31:0",
                "reminder:wfh:user_home:7"
            ),
            result.candidates.map { it.logicalId }
        )
    }

    private fun todayStatus(
        locationId: Int = 11,
        latitude: Double = -0.87,
        longitude: Double = 119.86
    ) = TodayStatus(
        canCheckIn = true,
        canCheckOut = false,
        checkedInAt = null,
        checkedOutAt = null,
        activeMode = WorkMode.WFO.shortLabel,
        activeLocation = Location(
            locationId = locationId,
            description = "Kantor Infinite Track",
            latitude = latitude,
            longitude = longitude,
            radius = 100,
            category = "Office"
        ),
        todayDate = "2026-07-23",
        isHoliday = false,
        holidayCheckinEnabled = false,
        currentTime = "08:00:00",
        checkinWindow = CheckinWindow(startTime = "08:00", endTime = "17:00"),
        checkoutAutoTime = "17:00"
    )

    private fun user(
        latitude: Double? = -0.90,
        longitude: Double? = 119.88,
        radius: Int? = 100
    ) = UserModel(
        id = 7,
        fullName = "Test Employee",
        email = "employee@example.test",
        roleName = "Employee",
        positionName = null,
        programName = null,
        divisionName = null,
        nipNim = "123",
        phone = null,
        photoUrl = null,
        photoUpdatedAt = null,
        latitude = latitude,
        longitude = longitude,
        radius = radius,
        locationDescription = "Rumah",
        locationCategoryName = "Home"
    )

    private fun booking(
        bookingId: Int = 31,
        locationId: Int? = 22,
        latitude: Double? = -0.92,
        longitude: Double? = 119.89,
        radiusMeters: Float? = 150f
    ) = BookingHistoryItem(
        id = bookingId.toString(),
        locationDescription = "Cafe Produktif",
        scheduleDate = "23 Jul 2026",
        status = "Approved",
        notes = "",
        suitabilityLabel = "Suitable",
        bookingId = bookingId,
        scheduleDateRaw = "2026-07-23",
        locationId = locationId,
        latitude = latitude,
        longitude = longitude,
        radiusMeters = radiusMeters
    )
}
