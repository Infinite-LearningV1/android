package com.example.infinite_track.presentation.design.tokens

import androidx.compose.ui.graphics.Color
import com.example.infinite_track.presentation.core.body1
import com.example.infinite_track.presentation.core.body2
import com.example.infinite_track.presentation.core.body2_5
import com.example.infinite_track.presentation.core.headline3
import androidx.compose.ui.text.font.FontWeight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InfiniteFeedbackTokensTest {

    @Test
    fun `every semantic resolves a complete feedback palette`() {
        InfiniteSemantic.entries.forEach { semantic ->
            val palette = infiniteFeedbackPalette(semantic)

            assertNotEquals(palette.surface, palette.stateContainer)
            assertNotEquals(palette.content, palette.border)
            assertNotEquals(palette.shadow, palette.topHighlight)
        }
    }

    @Test
    fun `inline and snackbar title tokens use body1 metrics with bold weight`() {
        val typography = InfiniteFeedbackTypography

        assertEquals(body1.copy(fontWeight = FontWeight.Bold), typography.inlineTitle)
        assertEquals(body2, typography.inlineBody)
        assertEquals(body1, typography.snackbarMessage)
        assertEquals(body1.copy(fontWeight = FontWeight.Bold), typography.snackbarTitle)
        assertEquals(body2, typography.supportingBody)
        assertEquals(body2_5, typography.pillLabel)
        assertEquals(body2, typography.actionLabel)
        assertEquals(headline3, typography.dialogTitle)
        assertEquals(body1, typography.dialogBody)
    }

    @Test
    fun `palette content is contrast safe for snackbar text actions and dismiss affordances`() {
        InfiniteSemantic.entries.forEach { semantic ->
            val palette = infiniteFeedbackPalette(semantic)

            assertEquals(InfiniteColors.AttendanceReportGlassSurface, palette.surface)
            assertTrue(
                "${semantic.name} content must meet 4.5 to 1 over the solid feedback surface",
                contrastRatio(palette.content, palette.surface.compositedOver(Color.White)) >= 4.5
            )
        }
    }

    private fun Color.compositedOver(background: Color): Color {
        val alpha = alpha
        return Color(
            red = red * alpha + background.red * (1f - alpha),
            green = green * alpha + background.green * (1f - alpha),
            blue = blue * alpha + background.blue * (1f - alpha)
        )
    }

    private fun contrastRatio(foreground: Color, background: Color): Double {
        val foregroundLuminance = foreground.relativeLuminance()
        val backgroundLuminance = background.relativeLuminance()
        return (maxOf(foregroundLuminance, backgroundLuminance) + 0.05) /
            (minOf(foregroundLuminance, backgroundLuminance) + 0.05)
    }

    private fun Color.relativeLuminance(): Double = listOf(red, green, blue)
        .map { channel ->
            val value = channel.toDouble()
            if (value <= 0.04045) value / 12.92 else Math.pow((value + 0.055) / 1.055, 2.4)
        }
        .let { channels ->
            0.2126 * channels[0] + 0.7152 * channels[1] + 0.0722 * channels[2]
        }
}
