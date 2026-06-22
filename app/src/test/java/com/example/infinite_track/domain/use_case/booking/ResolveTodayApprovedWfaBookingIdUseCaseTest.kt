package com.example.infinite_track.domain.use_case.booking

import com.example.infinite_track.domain.model.booking.BookingHistoryItem
import com.example.infinite_track.domain.model.booking.BookingHistoryPage
import com.example.infinite_track.domain.repository.BookingRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ResolveTodayApprovedWfaBookingIdUseCaseTest {

    @Test
    fun invoke_returnsApprovedBookingIdForRequestedDateEvenWhenItAppearsOnLaterPage() {
        runBlocking {
            val repository = FakeBookingRepository(
                pages = mapOf(
                    1 to BookingHistoryPage(
                        bookings = listOf(
                            bookingItem(
                                bookingId = 100,
                                scheduleDateRaw = "2026-06-20",
                                statusRaw = "approved"
                            )
                        ),
                        hasNextPage = true
                    ),
                    2 to BookingHistoryPage(
                        bookings = listOf(
                            bookingItem(
                                bookingId = 321,
                                scheduleDateRaw = "2026-06-22",
                                statusRaw = "approved"
                            )
                        ),
                        hasNextPage = false
                    )
                )
            )

            val useCase = ResolveTodayApprovedWfaBookingIdUseCase(repository)

            val result = useCase("2026-06-22")

            assertEquals(321, result.getOrThrow())
            assertEquals(listOf(1, 2), repository.requestedPages)
        }
    }

    @Test
    fun invoke_failsWhenApprovedBookingForDateDoesNotExist() {
        runBlocking {
            val repository = FakeBookingRepository(
                pages = mapOf(
                    1 to BookingHistoryPage(
                        bookings = listOf(
                            bookingItem(
                                bookingId = 200,
                                scheduleDateRaw = "2026-06-21",
                                statusRaw = "approved"
                            ),
                            bookingItem(
                                bookingId = 201,
                                scheduleDateRaw = "2026-06-22",
                                statusRaw = "pending"
                            )
                        ),
                        hasNextPage = false
                    )
                )
            )

            val useCase = ResolveTodayApprovedWfaBookingIdUseCase(repository)

            val result = useCase("2026-06-22")

            kotlin.runCatching { result.getOrThrow() }
                .onSuccess { throw AssertionError("Expected missing approved booking to fail") }
                .onFailure { error: Throwable ->
                    assertEquals(
                        "Approved WFA booking for 2026-06-22 was not found.",
                        error.message
                    )
                }
        }
    }

    private fun bookingItem(
        bookingId: Int,
        scheduleDateRaw: String,
        statusRaw: String
    ) = BookingHistoryItem(
        id = bookingId.toString(),
        bookingId = bookingId,
        locationDescription = "Test location",
        scheduleDate = scheduleDateRaw,
        scheduleDateRaw = scheduleDateRaw,
        status = statusRaw.replaceFirstChar { it.uppercase() },
        statusRaw = statusRaw,
        notes = "",
        suitabilityLabel = ""
    )

    private class FakeBookingRepository(
        private val pages: Map<Int, BookingHistoryPage>
    ) : BookingRepository {
        val requestedPages = mutableListOf<Int>()

        override suspend fun getBookingHistory(
            status: String?,
            page: Int,
            limit: Int,
            sortBy: String,
            sortOrder: String
        ): Result<BookingHistoryPage> {
            requestedPages += page
            return Result.success(
                pages[page] ?: BookingHistoryPage(emptyList(), hasNextPage = false)
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
