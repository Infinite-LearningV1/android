package com.example.infinite_track.presentation.screen.attendance.face

import org.junit.Assert.assertEquals
import org.junit.Test

class FaceLightingQualityTest {

    @Test
    fun evaluate_returnsLowLightForDarkFacePixels() {
        val darkPixels = IntArray(100) { rgb(18, 18, 18) }

        assertEquals(
            LightingQuality.LOW_LIGHT,
            FaceLightingQuality.evaluate(darkPixels)
        )
    }

    @Test
    fun evaluate_returnsAcceptableForBrightFacePixels() {
        val brightPixels = IntArray(100) { rgb(140, 140, 140) }

        assertEquals(
            LightingQuality.ACCEPTABLE,
            FaceLightingQuality.evaluate(brightPixels)
        )
    }

    @Test
    fun evaluate_returnsAcceptableWhenEnoughPixelsAreBright() {
        val mixedPixels = IntArray(100) { index ->
            if (index < 20) rgb(120, 120, 120) else rgb(25, 25, 25)
        }

        assertEquals(
            LightingQuality.ACCEPTABLE,
            FaceLightingQuality.evaluate(mixedPixels)
        )
    }

    private fun rgb(red: Int, green: Int, blue: Int): Int {
        return (red shl 16) or (green shl 8) or blue
    }
}
