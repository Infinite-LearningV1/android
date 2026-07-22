package com.example.infinite_track.presentation.design.tokens

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.infinite_track.presentation.theme.sfCompact_font
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
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
}
