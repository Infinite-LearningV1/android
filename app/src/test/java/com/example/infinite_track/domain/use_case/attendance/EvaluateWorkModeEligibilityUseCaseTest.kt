package com.example.infinite_track.domain.use_case.attendance

import com.example.infinite_track.domain.model.attendance.Location
import com.example.infinite_track.domain.model.attendance.SelectedTargetLocation
import com.example.infinite_track.domain.model.attendance.WorkMode
import com.example.infinite_track.domain.model.attendance.WorkModeRecoveryAction
import com.example.infinite_track.domain.model.booking.BookingHistoryItem
import com.example.infinite_track.domain.model.booking.BookingHistoryPage
import com.example.infinite_track.domain.repository.BookingRepository
import com.example.infinite_track.domain.use_case.booking.ResolveTodayApprovedWfaBookingIdUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EvaluateWorkModeEligibilityUseCaseTest {

    @Test
    fun invoke_allowsWfhWhenHomeTargetExists() {
        runBlocking {
            val useCase = useCaseWithBookings(emptyList())

            val eligibility = useCase(
                mode = WorkMode.WFH,
                selectedTargetLocation = availableTarget(WorkMode.WFH),
                todayDate = "2026-07-09"
            )

            assertTrue(eligibility.canContinueToFaceVerification)
            assertEquals(WorkMode.WFH, eligibility.mode)
        }
    }

    @Test
    fun invoke_blocksWfhBeforeFaceVerificationWhenHomeTargetMissing() {
        runBlocking {
            val useCase = useCaseWithBookings(emptyList())

            val eligibility = useCase(
                mode = WorkMode.WFH,
                selectedTargetLocation = SelectedTargetLocation(
                    mode = WorkMode.WFH,
                    location = null,
                    displayName = "Lokasi WFH belum tersedia",
                    description = null,
                    isAvailable = false,
                    unavailableReason = "Lokasi WFH belum tersedia. Pilih WFO atau perbarui lokasi WFH terlebih dahulu."
                ),
                todayDate = "2026-07-09"
            )

            assertFalse(eligibility.canContinueToFaceVerification)
            assertEquals(WorkModeRecoveryAction.UPDATE_WFH_LOCATION, eligibility.recoveryAction)
        }
    }

    @Test
    fun invoke_blocksWfaWhenApprovedBookingIsMissing() {
        runBlocking {
            val useCase = useCaseWithBookings(emptyList())

            val eligibility = useCase(
                mode = WorkMode.WFA,
                selectedTargetLocation = availableTarget(WorkMode.WFA),
                todayDate = "2026-07-09"
            )

            assertFalse(eligibility.canContinueToFaceVerification)
            assertEquals(WorkModeRecoveryAction.VIEW_WFA_REQUESTS, eligibility.recoveryAction)
        }
    }

    @Test
    fun invoke_allowsWfaWhenApprovedBookingExistsForToday() {
        runBlocking {
            val useCase = useCaseWithBookings(
                listOf(bookingItem(bookingId = 88, scheduleDateRaw = "2026-07-09"))
            )

            val eligibility = useCase(
                mode = WorkMode.WFA,
                selectedTargetLocation = availableTarget(WorkMode.WFA),
                todayDate = "2026-07-09"
            )

            assertTrue(eligibility.canContinueToFaceVerification)
            assertEquals(88, eligibility.approvedWfaBookingId)
        }
    }

    private fun useCaseWithBookings(bookings: List<BookingHistoryItem>): EvaluateWorkModeEligibilityUseCase {
        return EvaluateWorkModeEligibilityUseCase(
            ResolveTodayApprovedWfaBookingIdUseCase(
                FakeBookingRepository(bookings)
            )
        )
    }

    private fun availableTarget(mode: WorkMode) = SelectedTargetLocation(
        mode = mode,
        location = Location(
            locationId = mode.categoryId,
            description = mode.displayLabel,
            latitude = -0.8,
            longitude = 119.9,
            radius = 100,
            category = mode.shortLabel
        ),
        displayName = mode.displayLabel,
        description = mode.shortLabel,
        isAvailable = true
    )

    private fun bookingItem(
        bookingId: Int,
        scheduleDateRaw: String
    ) = BookingHistoryItem(
        id = bookingId.toString(),
        bookingId = bookingId,
        locationDescription = "WFA location",
        scheduleDate = scheduleDateRaw,
        scheduleDateRaw = scheduleDateRaw,
        status = "Approved",
        statusRaw = "approved",
        notes = "",
        suitabilityLabel = ""
    )

    private class FakeBookingRepository(
        private val bookings: List<BookingHistoryItem>
    ) : BookingRepository {
        override suspend fun getBookingHistory(
            status: String?,
            page: Int,
            limit: Int,
            sortBy: String,
            sortOrder: String
        ): Result<BookingHistoryPage> {
            return Result.success(
                BookingHistoryPage(
                    bookings = if (page == 1) bookings else emptyList(),
                    hasNextPage = false
                )
            )
        }

        override suspend fun submitBooking(
            scheduleDate: String,
            latitude: Double,
            longitude: Double,
            radius: Int,
            description: String,
            notes: String
        ): Result<Unit> {
            throw UnsupportedOperationException("Not needed in this test")
        }
    }
}
