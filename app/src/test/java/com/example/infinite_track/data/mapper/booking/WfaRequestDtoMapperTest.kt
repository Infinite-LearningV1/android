package com.example.infinite_track.data.mapper.booking

import com.example.infinite_track.data.soucre.network.response.booking.WfaRequestLocationDto
import com.example.infinite_track.data.soucre.network.response.booking.WfaRequestReasonResultDto
import com.example.infinite_track.data.soucre.network.response.booking.WfaRequestResponseDataDto
import com.example.infinite_track.data.soucre.network.response.booking.WfaRequestResponseDto
import com.example.infinite_track.domain.model.booking.SubmitWfaRequestCommand
import com.example.infinite_track.domain.model.booking.WfaCandidateLocation
import com.example.infinite_track.domain.model.booking.WfaRequestStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class WfaRequestDtoMapperTest {

    private val location = WfaCandidateLocation(-0.9001, 119.877, "Hub", "Palu")

    @Test
    fun `maps date to ISO and excludes server authoritative fields`() {
        val dto = SubmitWfaRequestCommand(
            scheduleDate = LocalDate.of(2026, 8, 10),
            reasonId = 4L,
            otherReasonText = null,
            notes = null,
            location = location
        ).toDto()

        assertEquals("2026-08-10", dto.scheduleDate)
        assertEquals(4L, dto.reasonId)
        val fields = dto::class.java.declaredFields.map { it.name }
        assertFalse(fields.contains("radius"))
        assertFalse(fields.contains("status"))
        assertFalse(fields.contains("userId"))
    }

    @Test
    fun `maps backend confirmed response to submitted request`() {
        val submittedAt = Instant.parse("2026-08-01T01:02:03Z")
        val dto = WfaRequestResponseDto(
            success = true,
            message = "Diajukan",
            data = WfaRequestResponseDataDto(
                bookingId = 42L,
                scheduleDate = "2026-08-10",
                status = "pending",
                location = WfaRequestLocationDto(-0.9001, 119.877, "Hub", "Palu"),
                reason = WfaRequestReasonResultDto(4L, "Pertemuan klien"),
                radiusMeters = 100,
                submittedAt = submittedAt.toString()
            )
        )

        val result = requireNotNull(dto.toDomainOrNull())
        assertEquals(42L, result.bookingId)
        assertEquals(WfaRequestStatus.PENDING, result.status)
        assertEquals("Pertemuan klien", result.reasonLabel)
        assertEquals(100, result.radiusMeters)
        assertEquals(submittedAt, result.submittedAt)
    }

    @Test
    fun `uses candidate display fallback only when confirmed response omits location`() {
        val dto = WfaRequestResponseDto(
            success = true,
            message = "Diajukan",
            data = WfaRequestResponseDataDto(
                bookingId = 42L,
                scheduleDate = "2026-08-10",
                status = "pending",
                location = null,
                reason = WfaRequestReasonResultDto(4L, "Pertemuan klien"),
                radiusMeters = 100,
                submittedAt = null
            )
        )

        assertEquals(location, dto.toDomainOrNull(location)?.location)
        assertNull(dto.toDomainOrNull())
    }
}
