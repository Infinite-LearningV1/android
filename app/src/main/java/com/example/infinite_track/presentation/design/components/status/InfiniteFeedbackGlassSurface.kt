package com.example.infinite_track.presentation.design.components.status

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.infinite_track.presentation.design.tokens.InfiniteElevation
import com.example.infinite_track.presentation.design.tokens.InfiniteRadius
import com.example.infinite_track.presentation.design.tokens.InfiniteSemantic
import com.example.infinite_track.presentation.design.tokens.InfiniteSpacing
import com.example.infinite_track.presentation.design.tokens.infiniteFeedbackPalette

@Composable
fun InfiniteFeedbackGlassSurface(
    semantic: InfiniteSemantic,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(InfiniteRadius.Large),
    shadowElevation: Dp = InfiniteElevation.Soft,
    showAccentRail: Boolean = true,
    showTopHighlight: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val palette = infiniteFeedbackPalette(semantic)
    Box(
        modifier = modifier
            .shadow(shadowElevation, shape, ambientColor = palette.shadow, spotColor = palette.shadow)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(palette.surfaceStart, palette.surfaceEnd)))
            .border(1.dp, palette.border, shape)
    ) {
        if (showAccentRail) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .width(InfiniteSpacing.Default.xs)
                    .background(palette.accent)
            )
        }
        if (showTopHighlight) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(palette.topHighlight)
            )
        }
        content()
    }
}
