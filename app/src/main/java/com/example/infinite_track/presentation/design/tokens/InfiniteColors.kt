package com.example.infinite_track.presentation.design.tokens

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.infinite_track.presentation.theme.Blue_500
import com.example.infinite_track.presentation.theme.Blue_Accent_500
import com.example.infinite_track.presentation.theme.Green_Success
import com.example.infinite_track.presentation.theme.Orange_500
import com.example.infinite_track.presentation.theme.Purple_500
import com.example.infinite_track.presentation.theme.Red_Error
import com.example.infinite_track.presentation.theme.Violet_100
import com.example.infinite_track.presentation.theme.White
import com.example.infinite_track.presentation.theme.Yellow_Warning

@Immutable
data class InfiniteSemanticColors(
    val container: Color,
    val content: Color,
    val border: Color,
    val accent: Color
)

object InfiniteColors {
    val Primary = Blue_500
    val Secondary = Orange_500
    val Accent = Blue_Accent_500
    val Text = Purple_500
    val Background = Violet_100
    val Surface = White
    val SoftAlert = Color(0xFFFF6B6B)
    val Success = Green_Success
    val Info = Color(0xFF214CE0)
    val Warning = Yellow_Warning
    val Error = Red_Error
    val Neutral = Color(0xFF746D74)

    val GlassGradient: Brush
        get() = Brush.verticalGradient(
            colors = listOf(
                White.copy(alpha = 0.72f),
                Violet_100.copy(alpha = 0.42f)
            )
        )
}

@Composable
fun infiniteSemanticColors(semantic: InfiniteSemantic): InfiniteSemanticColors {
    val fallbackSurface = MaterialTheme.colorScheme.surface
    return when (semantic) {
        InfiniteSemantic.Primary -> InfiniteSemanticColors(
            container = InfiniteColors.Primary.copy(alpha = 0.12f),
            content = InfiniteColors.Text,
            border = InfiniteColors.Primary.copy(alpha = 0.34f),
            accent = InfiniteColors.Primary
        )
        InfiniteSemantic.Secondary -> InfiniteSemanticColors(
            container = InfiniteColors.Secondary.copy(alpha = 0.16f),
            content = InfiniteColors.Text,
            border = InfiniteColors.Secondary.copy(alpha = 0.45f),
            accent = InfiniteColors.Secondary
        )
        InfiniteSemantic.Success -> InfiniteSemanticColors(
            container = InfiniteColors.Success.copy(alpha = 0.16f),
            content = InfiniteColors.Text,
            border = InfiniteColors.Success.copy(alpha = 0.45f),
            accent = InfiniteColors.Success
        )
        InfiniteSemantic.Info -> InfiniteSemanticColors(
            container = InfiniteColors.Primary.copy(alpha = 0.10f),
            content = InfiniteColors.Text,
            border = InfiniteColors.Info.copy(alpha = 0.30f),
            accent = InfiniteColors.Info
        )
        InfiniteSemantic.Warning -> InfiniteSemanticColors(
            container = InfiniteColors.Warning.copy(alpha = 0.24f),
            content = InfiniteColors.Text,
            border = InfiniteColors.Warning.copy(alpha = 0.55f),
            accent = InfiniteColors.Warning
        )
        InfiniteSemantic.Error -> InfiniteSemanticColors(
            container = InfiniteColors.SoftAlert.copy(alpha = 0.14f),
            content = InfiniteColors.Text,
            border = InfiniteColors.SoftAlert.copy(alpha = 0.42f),
            accent = InfiniteColors.SoftAlert
        )
        InfiniteSemantic.Neutral -> InfiniteSemanticColors(
            container = fallbackSurface.copy(alpha = 0.74f),
            content = InfiniteColors.Text,
            border = InfiniteColors.Neutral.copy(alpha = 0.20f),
            accent = InfiniteColors.Neutral
        )
    }
}
