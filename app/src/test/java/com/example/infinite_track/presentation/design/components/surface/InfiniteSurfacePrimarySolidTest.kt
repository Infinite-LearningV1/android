package com.example.infinite_track.presentation.design.components.surface

import androidx.compose.ui.graphics.Color
import com.example.infinite_track.presentation.design.tokens.InfiniteColors
import com.example.infinite_track.presentation.design.tokens.InfiniteSemanticColors
import com.example.infinite_track.presentation.design.tokens.InfiniteSurfaceVariant
import com.example.infinite_track.presentation.theme.White
import org.junit.Assert.assertEquals
import org.junit.Test

class InfiniteSurfacePrimarySolidTest {
    private val semantic = InfiniteSemanticColors(
        container = Color.Red,
        content = Color.Black,
        border = Color.Green,
        accent = Color.Blue
    )

    @Test
    fun `primary solid surface uses existing primary and on-primary tokens`() {
        assertEquals(
            InfiniteColors.Primary,
            InfiniteSurfaceVariant.PrimarySolid.resolveContainerColor(
                semanticColors = semantic,
                fallbackSurface = Color.Yellow
            )
        )
        assertEquals(
            White,
            InfiniteSurfaceVariant.PrimarySolid.resolveContentColor(semantic)
        )
    }
}
