package com.example.infinite_track.domain.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class WfaScheduleDatePolicyTest {
    private val today = LocalDate.of(2026, 8, 2)

    @Test
    fun `tomorrow is the minimum date`() {
        val policy = WfaScheduleDatePolicy.fixed(
            Clock.fixed(Instant.parse("2026-08-02T02:00:00Z"), ZoneOffset.UTC)
        )

        assertEquals(today, policy.today())
        assertEquals(today.plusDays(1), policy.minimumDate())
    }

    @Test
    fun `today and past are invalid while future is valid`() {
        val policy = WfaScheduleDatePolicy.fixed(
            Clock.fixed(Instant.parse("2026-08-02T02:00:00Z"), ZoneOffset.UTC)
        )

        assertFalse(policy.isSelectable(today.minusDays(1)))
        assertFalse(policy.isSelectable(today))
        assertTrue(policy.isSelectable(today.plusDays(1)))
        assertTrue(policy.isSelectable(today.plusDays(30)))
    }
}
