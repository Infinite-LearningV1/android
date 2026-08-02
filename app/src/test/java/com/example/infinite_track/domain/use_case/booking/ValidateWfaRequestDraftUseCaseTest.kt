package com.example.infinite_track.domain.use_case.booking

import com.example.infinite_track.domain.model.booking.WfaCandidateLocation
import com.example.infinite_track.domain.model.booking.WfaRequestConfig
import com.example.infinite_track.domain.model.booking.WfaRequestDraft
import com.example.infinite_track.domain.model.booking.WfaRequestFieldError
import com.example.infinite_track.domain.model.booking.WfaRequestReason
import com.example.infinite_track.domain.model.booking.WfaRequestValidationResult
import com.example.infinite_track.domain.validation.WfaScheduleDatePolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class ValidateWfaRequestDraftUseCaseTest {

    private val useCase = ValidateWfaRequestDraftUseCase()
    private val location = WfaCandidateLocation(-0.9001, 119.877, "Hub", "Palu")
    private val config = WfaRequestConfig(
        radiusMeters = 100,
        reasons = listOf(
            WfaRequestReason(1L, "Client meeting", false),
            WfaRequestReason(2L, "Lainnya", true)
        )
    )

    @Test
    fun `same day and past dates return FUTURE_DATE_REQUIRED`() {
        val today = LocalDate.of(2026, 8, 2)
        val policy = WfaScheduleDatePolicy.fixed(
            Clock.fixed(Instant.parse("2026-08-02T02:00:00Z"), ZoneOffset.UTC)
        )
        val dateAwareUseCase = ValidateWfaRequestDraftUseCase(policy)
        val validDraft = WfaRequestDraft(today.plusDays(1), 1L, "", "", location)

        listOf(today.minusDays(1), today).forEach { date ->
            val result = dateAwareUseCase(validDraft.copy(scheduleDate = date), config)
            val errors = (result as WfaRequestValidationResult.Invalid).errors
            assertEquals(WfaRequestFieldError.FUTURE_DATE_REQUIRED, errors.scheduleDate)
        }
    }

    @Test
    fun `valid draft becomes command and blank optional values become null`() {
        val result = useCase(
            WfaRequestDraft(LocalDate.of(2026, 8, 10), 1L, "", "   ", location),
            config
        ) as WfaRequestValidationResult.Valid

        assertEquals(LocalDate.of(2026, 8, 10), result.command.scheduleDate)
        assertEquals(1L, result.command.reasonId)
        assertEquals(location, result.command.location)
        assertNull(result.command.otherReasonText)
        assertNull(result.command.notes)
    }

    @Test
    fun `Other reason requires explanation`() {
        val result = useCase(
            WfaRequestDraft(LocalDate.of(2026, 8, 10), 2L, "  ", "", location),
            config
        ) as WfaRequestValidationResult.Invalid

        assertEquals(WfaRequestFieldError.OTHER_REASON_REQUIRED, result.errors.otherReason)
    }

    @Test
    fun `reason absent from server config is unavailable`() {
        val result = useCase(
            WfaRequestDraft(LocalDate.of(2026, 8, 10), 99L, "", "", location),
            config
        ) as WfaRequestValidationResult.Invalid

        assertEquals(WfaRequestFieldError.REASON_UNAVAILABLE, result.errors.reason)
    }

    @Test
    fun `missing required fields return field-local errors`() {
        val result = useCase(WfaRequestDraft.Empty, config) as WfaRequestValidationResult.Invalid

        assertEquals(WfaRequestFieldError.REQUIRED, result.errors.scheduleDate)
        assertEquals(WfaRequestFieldError.REQUIRED, result.errors.reason)
        assertEquals(WfaRequestFieldError.REQUIRED, result.errors.location)
    }

    @Test
    fun `invalid location blocks review`() {
        val invalid = location.copy(latitude = 91.0)
        val result = useCase(
            WfaRequestDraft(LocalDate.of(2026, 8, 10), 1L, "", "", invalid),
            config
        ) as WfaRequestValidationResult.Invalid

        assertEquals(WfaRequestFieldError.INVALID_LOCATION, result.errors.location)
        assertTrue(!result.errors.isEmpty)
    }
}
