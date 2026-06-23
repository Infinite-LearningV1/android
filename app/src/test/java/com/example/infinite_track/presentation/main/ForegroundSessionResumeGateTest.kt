package com.example.infinite_track.presentation.main

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ForegroundSessionResumeGateTest {

    @Test
    fun `skips initial cold start before allowing foreground validation`() {
        val gate = ForegroundSessionResumeGate(nowMillis = { 10_000L }, debounceWindowMs = 2_000L)

        assertFalse(gate.tryAcquire(bootstrapInProgress = false))
        assertTrue(gate.tryAcquire(bootstrapInProgress = false))
    }

    @Test
    fun `acquires once and blocks while validation is still running`() {
        val gate = ForegroundSessionResumeGate(nowMillis = { 10_000L }, debounceWindowMs = 2_000L)

        assertFalse(gate.tryAcquire(bootstrapInProgress = false))
        assertTrue(gate.tryAcquire(bootstrapInProgress = false))
        assertFalse(gate.tryAcquire(bootstrapInProgress = false))
    }

    @Test
    fun `skips while bootstrap is in progress after initial cold start`() {
        val gate = ForegroundSessionResumeGate(nowMillis = { 10_000L }, debounceWindowMs = 2_000L)

        assertFalse(gate.tryAcquire(bootstrapInProgress = false))
        assertFalse(gate.tryAcquire(bootstrapInProgress = true))
    }

    @Test
    fun `debounces fast foreground bounce after release`() {
        var now = 10_000L
        val gate = ForegroundSessionResumeGate(nowMillis = { now }, debounceWindowMs = 2_000L)

        assertFalse(gate.tryAcquire(bootstrapInProgress = false))
        assertTrue(gate.tryAcquire(bootstrapInProgress = false))
        gate.release()

        now = 11_000L
        assertFalse(gate.tryAcquire(bootstrapInProgress = false))

        now = 12_100L
        assertTrue(gate.tryAcquire(bootstrapInProgress = false))
    }
}
