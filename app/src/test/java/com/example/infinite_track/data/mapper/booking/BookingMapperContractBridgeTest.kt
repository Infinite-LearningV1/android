package com.example.infinite_track.data.mapper.booking

import com.example.infinite_track.data.soucre.network.response.booking.BookingItem
import com.example.infinite_track.data.soucre.network.response.booking.BookingLocation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.fail
import org.junit.Test

class BookingMapperContractBridgeTest {

    @Test
    fun `toDomain preserves raw booking identity fields for downstream attendance bridge`() {
        val dto = BookingItem(
            bookingId = 321,
            userId = 10,
            userFullName = "Test User",
            userEmail = null,
            userNipNim = null,
            userPositionName = null,
            userRoleName = null,
            scheduleDate = "2026-06-21",
            status = "approved",
            location = BookingLocation(
                locationId = 99,
                latitude = -0.9,
                longitude = 119.9,
                radius = 100f,
                description = "Cafe Palu"
            ),
            notes = "Focus work",
            suitabilityScore = 90f,
            suitabilityLabel = "Sangat Sesuai",
            createdAt = "2026-06-20T10:00:00Z",
            processedAt = "2026-06-20T11:00:00Z",
            approvedBy = 5
        )

        val domain = dto.toDomain()
        val domainClass = domain::class.java

        val bookingIdField: java.lang.reflect.Field = runCatching { domainClass.getDeclaredField("bookingId") }
            .getOrElse {
                fail("BookingHistoryItem must preserve raw bookingId for WFA attendance bridge")
                throw AssertionError("unreachable")
            }
        val scheduleDateRawField: java.lang.reflect.Field = runCatching { domainClass.getDeclaredField("scheduleDateRaw") }
            .getOrElse {
                fail("BookingHistoryItem must preserve raw scheduleDateRaw for WFA attendance bridge")
                throw AssertionError("unreachable")
            }
        val statusRawField: java.lang.reflect.Field = runCatching { domainClass.getDeclaredField("statusRaw") }
            .getOrElse {
                fail("BookingHistoryItem must preserve raw statusRaw for WFA attendance bridge")
                throw AssertionError("unreachable")
            }

        bookingIdField.setAccessible(true)
        scheduleDateRawField.setAccessible(true)
        statusRawField.setAccessible(true)

        assertNotNull(bookingIdField.get(domain))
        assertEquals(321, bookingIdField.get(domain))
        assertEquals("2026-06-21", scheduleDateRawField.get(domain))
        assertEquals("approved", statusRawField.get(domain))
    }
}
