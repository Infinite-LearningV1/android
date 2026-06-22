package com.example.infinite_track.domain.use_case.booking

import com.example.infinite_track.domain.model.booking.BookingHistoryItem
import com.example.infinite_track.domain.model.booking.BookingHistoryPage
import com.example.infinite_track.domain.repository.BookingRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ResolveTodayApprovedWfaBookingUseCaseTest {

    @Test
    fun `returns approved booking id for the requested schedule date`() = runTest {
        val repository = FakeBookingRepository(
            listOf(
                Result.success(
                    BookingHistoryPage(
                        bookings = listOf(
                            bookingItem(88, "2026-06-20", "approved"),
                            bookingItem(99, "2026-06-21", "approved"),
                            bookingItem(101, "2026-06-21", "pending")
                        ),
                        hasNextPage = false
                    )
                )
            )
        )

        val useCase = ResolveTodayApprovedWfaBookingUseCase(repository)

        val result = useCase("2026-06-21")

        assertEquals(99, result.getOrNull())
        assertEquals("approved", repository.lastRequestedStatus)
    }

    @Test
    fun `returns approved booking id from a later page when the first page does not contain today's booking`() = runTest {
        val repository = FakeBookingRepository(
            listOf(
                Result.success(
                    BookingHistoryPage(
                        bookings = listOf(
                            bookingItem(88, "2026-06-23", "approved")
                        ),
                        hasNextPage = true
                    )
                ),
                Result.success(
                    BookingHistoryPage(
                        bookings = listOf(
                            bookingItem(144, "2026-06-21", "approved")
                        ),
                        hasNextPage = false
                    )
                )
            )
        )

        val useCase = ResolveTodayApprovedWfaBookingUseCase(repository)

        val result = useCase("2026-06-21")

        assertEquals(144, result.getOrNull())
        assertEquals(listOf(1, 2), repository.requestedPages)
        assertEquals("approved", repository.lastRequestedStatus)
    }

    @Test
    fun `returns null when no approved booking matches the requested schedule date`() = runTest {
        val repository = FakeBookingRepository(
            listOf(
                Result.success(
                    BookingHistoryPage(
                        bookings = listOf(
                            bookingItem(88, "2026-06-20", "approved")
                        ),
                        hasNextPage = false
                    )
                )
            )
        )

        val useCase = ResolveTodayApprovedWfaBookingUseCase(repository)

        val result = useCase("2026-06-21")

        assertNull(result.getOrNull())
        assertEquals("approved", repository.lastRequestedStatus)
    }

    private fun bookingItem(bookingId: Int, scheduleDateRaw: String, statusRaw: String): BookingHistoryItem {
        return BookingHistoryItem(
            id = bookingId.toString(),
            locationDescription = "Cafe Palu",
            scheduleDate = "21 Jun 2026",
            status = statusRaw.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
            notes = "Focus work",
            suitabilityLabel = "Sangat Sesuai",
            bookingId = bookingId,
            scheduleDateRaw = scheduleDateRaw,
            statusRaw = statusRaw
        )
    }

    private class FakeBookingRepository(
        private val results: List<Result<BookingHistoryPage>>
    ) : BookingRepository {
        var lastRequestedStatus: String? = null
        val requestedPages = mutableListOf<Int>()
        private var callCount = 0

        override suspend fun getBookingHistory(
            status: String?,
            page: Int,
            limit: Int,
            sortBy: String,
            sortOrder: String
        ): Result<BookingHistoryPage> {
            lastRequestedStatus = status
            requestedPages += page
            val index = callCount.coerceAtMost(results.lastIndex)
            callCount++
            return results[index]
        }

        override suspend fun submitBooking(
            scheduleDate: String,
            latitude: Double,
            longitude: Double,
            radius: Int,
            description: String,
            notes: String
        ): Result<Unit> = Result.success(Unit)
    }
}
