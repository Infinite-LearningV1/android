package com.example.infinite_track.data.face

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FaceDetectorHelperFaceCountTest {

    @Test
    fun `selects largest by area and reports count`() {
        val result = FaceSelection.select(listOf(10, 40, 25)) { it }

        assertEquals(3, result!!.totalFaces)
        assertEquals(40, result.primary)
    }

    @Test
    fun `empty list yields null`() {
        assertNull(FaceSelection.select(emptyList<Int>()) { it })
    }
}
