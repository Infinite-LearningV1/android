package com.example.infinite_track.domain.model.booking

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class WfaRequestContractTest {

    private val location = WfaCandidateLocation(
        latitude = -0.9001,
        longitude = 119.877,
        displayName = "Infinity Creative Hub",
        formattedAddress = "Palu"
    )

    @Test
    fun `empty draft contains no server-authoritative fields`() {
        assertNull(WfaRequestDraft.Empty.scheduleDate)
        assertNull(WfaRequestDraft.Empty.reasonId)
        assertNull(WfaRequestDraft.Empty.location)
    }

    @Test
    fun `submit command has no editable radius`() {
        val properties = SubmitWfaRequestCommand::class.java.declaredFields.map { it.name }
        assertFalse(properties.contains("radius"))
        assertFalse(properties.contains("radiusMeters"))
    }

    @Test
    fun `success carries backend confirmed request`() {
        val request = SubmittedWfaRequest(
            bookingId = 42L,
            scheduleDate = LocalDate.of(2026, 8, 10),
            status = WfaRequestStatus.PENDING,
            location = location,
            reasonLabel = "Pertemuan dengan klien",
            radiusMeters = 100,
            submittedAt = null
        )

        assertEquals(request, WfaRequestResult.Success(request).request)
    }
}
