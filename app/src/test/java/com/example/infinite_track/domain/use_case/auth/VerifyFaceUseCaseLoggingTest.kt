package com.example.infinite_track.domain.use_case.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class VerifyFaceUseCaseLoggingTest {

    @Test
    fun `diagnostic payload is null in release`() {
        val payload = FaceMatchDiagnosticsFactory.build(
            isDebug = false,
            similarity = 0.83f,
            threshold = 0.15f,
            isMatch = true
        )

        assertNull(payload)
    }

    @Test
    fun `diagnostic payload is present in debug`() {
        val payload = FaceMatchDiagnosticsFactory.build(
            isDebug = true,
            similarity = 0.83f,
            threshold = 0.15f,
            isMatch = true
        )

        assertNotNull(payload)
        assertEquals(0.83f, payload!!.similarity, 0.0001f)
        assertEquals(0.15f, payload.threshold, 0.0001f)
        assertEquals(true, payload.isMatch)
    }
}
