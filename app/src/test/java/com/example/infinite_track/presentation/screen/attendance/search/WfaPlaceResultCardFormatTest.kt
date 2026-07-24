package com.example.infinite_track.presentation.screen.attendance.search

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class WfaPlaceResultCardFormatTest {

    @Test
    fun `distance formatting follows the active locale`() {
        assertEquals("1.3 km", formatDistanceMeters(1_250.0, Locale.US))
        assertEquals(
            "1,3 km",
            formatDistanceMeters(1_250.0, Locale.forLanguageTag("id-ID"))
        )
    }
}
