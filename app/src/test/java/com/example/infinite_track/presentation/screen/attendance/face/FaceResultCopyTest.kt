package com.example.infinite_track.presentation.screen.attendance.face

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FaceResultCopyTest {

    @Test
    fun `not matched is retryable error`() {
        val copy = FaceResultCopy.forFailure(FaceVerificationFailureReason.NOT_MATCHED)

        assertEquals(FaceResultRole.ERROR, copy.role)
        assertTrue(copy.retryable)
    }

    @Test
    fun `low light is retryable warning`() {
        val copy = FaceResultCopy.forFailure(FaceVerificationFailureReason.LOW_LIGHT)

        assertEquals(FaceResultRole.WARNING, copy.role)
        assertTrue(copy.retryable)
    }

    @Test
    fun `technical failure is retryable error`() {
        val copy = FaceResultCopy.forFailure(FaceVerificationFailureReason.TECHNICAL_FAILURE)

        assertEquals(FaceResultRole.ERROR, copy.role)
        assertTrue(copy.retryable)
    }

    @Test
    fun `success is non-retryable success role`() {
        val copy = FaceResultCopy.success()

        assertEquals(FaceResultRole.SUCCESS, copy.role)
        assertFalse(copy.retryable)
    }

    @Test
    fun `every failure reason maps to distinct string resources`() {
        val pairs = FaceVerificationFailureReason.entries.map {
            val copy = FaceResultCopy.forFailure(it)
            copy.titleRes to copy.messageRes
        }

        assertEquals(pairs.size, pairs.toSet().size)
    }
}
