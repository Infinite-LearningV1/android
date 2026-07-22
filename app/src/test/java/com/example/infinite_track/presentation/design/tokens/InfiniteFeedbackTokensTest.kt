package com.example.infinite_track.presentation.design.tokens

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import com.example.infinite_track.presentation.theme.sfCompact_font
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InfiniteFeedbackTokensTest {

    @Test
    fun `every semantic resolves a complete feedback palette`() {
        InfiniteSemantic.entries.forEach { semantic ->
            val palette = infiniteFeedbackPalette(semantic)

            assertNotEquals(palette.surfaceStart, palette.surfaceEnd)
            assertNotEquals(palette.content, palette.border)
            assertNotEquals(palette.accent, palette.glow)
            assertNotEquals(palette.shadow, palette.topHighlight)
        }
    }

    @Test
    fun `feedback typography uses registered medium and bold styles with approved line heights`() {
        val typography = InfiniteFeedbackTypography

        listOf(
            typography.snackbarMessage,
            typography.snackbarTitle,
            typography.supportingBody,
            typography.pillLabel,
            typography.actionLabel,
            typography.dialogTitle,
            typography.dialogBody
        ).forEach { style ->
            assertEquals(sfCompact_font, style.fontFamily)
            check(style.fontWeight == FontWeight.Medium || style.fontWeight == FontWeight.Bold)
        }
        assertEquals(14.sp, typography.snackbarMessage.fontSize)
        assertEquals(20.sp, typography.snackbarMessage.lineHeight)
        assertEquals(16.sp, typography.snackbarTitle.fontSize)
        assertEquals(20.sp, typography.snackbarTitle.lineHeight)
        assertEquals(14.sp, typography.supportingBody.fontSize)
        assertEquals(20.sp, typography.supportingBody.lineHeight)
        assertEquals(11.sp, typography.pillLabel.fontSize)
        assertEquals(14.sp, typography.pillLabel.lineHeight)
        assertEquals(12.sp, typography.actionLabel.fontSize)
        assertEquals(16.sp, typography.actionLabel.lineHeight)
        assertEquals(20.sp, typography.dialogTitle.fontSize)
        assertEquals(24.sp, typography.dialogTitle.lineHeight)
        assertEquals(14.sp, typography.dialogBody.fontSize)
        assertEquals(20.sp, typography.dialogBody.lineHeight)
    }

    @Test
    fun `palette content is contrast safe for snackbar text actions and dismiss affordances`() {
        InfiniteSemantic.entries.forEach { semantic ->
            val palette = infiniteFeedbackPalette(semantic)

            listOf(palette.surfaceStart, palette.surfaceEnd).forEach { surface ->
                assertTrue(
                    "${semantic.name} content must meet 4.5 to 1 over glass surface",
                    contrastRatio(palette.content, surface.compositedOver(Color.White)) >= 4.5
                )
            }
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
