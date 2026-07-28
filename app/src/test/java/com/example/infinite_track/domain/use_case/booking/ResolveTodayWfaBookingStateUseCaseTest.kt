package com.example.infinite_track.domain.use_case.booking

import com.example.infinite_track.domain.model.booking.BookingHistoryItem
import com.example.infinite_track.domain.model.booking.BookingHistoryPage
import com.example.infinite_track.domain.model.wfa.WfaBookingForDate
import com.example.infinite_track.domain.repository.BookingRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ResolveTodayWfaBookingStateUseCaseTest {

    @Test
    fun `not requested when no booking exists for attendance date`() = runTest {
        val state = useCase(
            pages = mapOf(1 to BookingHistoryPage(bookings = listOf(booking(1, "2026-07-22", "approved"))))
        )("2026-07-23")

        assertEquals(WfaBookingForDate.NotRequested, state)
    }

    @Test
    fun `pending booking for attendance date is pending`() = runTest {
        val state = useCase(
            pages = mapOf(1 to BookingHistoryPage(bookings = listOf(booking(12, "2026-07-23", "PENDING"))))
        )("2026-07-23")

        assertEquals(12, (state as WfaBookingForDate.Pending).booking.bookingId)
    }

    @Test
    fun `rejected booking for attendance date is rejected`() = runTest {
        val state = useCase(
            pages = mapOf(1 to BookingHistoryPage(bookings = listOf(booking(24, "2026-07-23", "rejected"))))
        )("2026-07-23")

        assertEquals(24, (state as WfaBookingForDate.Rejected).booking.bookingId)
    }

    @Test
    fun `latest approved booking for attendance date preserves target evidence`() = runTest {
        val state = useCase(
            pages = mapOf(
                1 to BookingHistoryPage(
                    bookings = listOf(
                        booking(87, "2026-07-23", "pending"),
                        booking(12, "2026-07-23", "approved")
                    ),
                    hasNextPage = true
                ),
                2 to BookingHistoryPage(
                    bookings = listOf(booking(88, "2026-07-23", "approved", latitude = -0.89)),
                    hasNextPage = false
                )
            )
        )("2026-07-23")
        val approved = state as WfaBookingForDate.Approved

        assertEquals(88, approved.booking.bookingId)
        assertEquals("2026-07-23", approved.booking.scheduleDateRaw)
        assertEquals(-0.89, approved.booking.latitude!!, 0.0)
    }

    @Test
    fun `repository failure is exposed as failed state`() = runTest {
        val failure = IllegalStateException("offline")
        val state = ResolveTodayWfaBookingStateUseCase(FakeBookingRepository(failure = failure))("2026-07-23")

        assertTrue(state is WfaBookingForDate.Failed)
        assertSame(failure, (state as WfaBookingForDate.Failed).cause)
    }

    private fun useCase(pages: Map<Int, BookingHistoryPage>) =
        ResolveTodayWfaBookingStateUseCase(FakeBookingRepository(pages = pages))

    private fun booking(
        bookingId: Int,
        scheduleDate: String,
        statusKey: String,
        latitude: Double? = null
    ) = BookingHistoryItem(
        id = bookingId.toString(),
        bookingId = bookingId,
        locationDescription = "Test location",
        scheduleDate = scheduleDate,
        scheduleDateRaw = scheduleDate,
        status = statusKey,
        statusRaw = statusKey,
        statusKey = statusKey,
        notes = "",
        suitabilityLabel = "",
        latitude = latitude
    )

    private class FakeBookingRepository(
        private val pages: Map<Int, BookingHistoryPage> = emptyMap(),
        private val failure: Throwable? = null
    ) : BookingRepository {
        override suspend fun getBookingHistory(
            status: String?,
            page: Int,
            limit: Int,
            sortBy: String,
            sortOrder: String
        ): Result<BookingHistoryPage> = failure?.let(Result.Companion::failure)
            ?: Result.success(pages[page] ?: BookingHistoryPage(emptyList(), hasNextPage = false))

        override suspend fun getWfaRequestConfig():
            com.example.infinite_track.domain.model.booking.WfaRequestConfigResult = error("Not needed")

        override suspend fun submitWfaRequest(
            command: com.example.infinite_track.domain.model.booking.SubmitWfaRequestCommand
        ): com.example.infinite_track.domain.model.booking.WfaRequestResult = error("Not needed")

    }
}
