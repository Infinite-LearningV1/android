package com.example.infinite_track.presentation.design.tokens

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.infinite_track.presentation.theme.Blue_50
import com.example.infinite_track.presentation.theme.Blue_100
import com.example.infinite_track.presentation.theme.Blue_500
import com.example.infinite_track.presentation.theme.Blue_600
import com.example.infinite_track.presentation.theme.Blue_Accent_500
import com.example.infinite_track.presentation.theme.Green_Success
import com.example.infinite_track.presentation.theme.Orange_500
import com.example.infinite_track.presentation.theme.Purple_500
import com.example.infinite_track.presentation.theme.Purple_900
import com.example.infinite_track.presentation.theme.Red_Error
import com.example.infinite_track.presentation.theme.Violet_50
import com.example.infinite_track.presentation.theme.Violet_100
import com.example.infinite_track.presentation.theme.Violet_200
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
    val Transparent = Color.Transparent
    val SoftAlert = Color(0xFFFF6B6B)
    val Success = Green_Success
    val Info = Color(0xFF214CE0)
    val Warning = Yellow_Warning
    val Error = Red_Error
    val Neutral = Color(0xFF746D74)

    val AccountHubTitle = Purple_900
    val AccountHubPrimary = Blue_500
    val AccountHubPrimaryStrong = Blue_600
    val AccountHubAccent = Blue_Accent_500
    val AccountHubSurface = White
    val AccountHubDivider = Purple_500
    val AccountHubDestructive = Red_Error

    val AccountHubBackgroundGradient: Brush
        get() = Brush.verticalGradient(
            colors = listOf(
                Blue_50,
                Violet_100,
                Violet_50
            )
        )

    val AccountHubHeroGradient: Brush
        get() = Brush.linearGradient(
            colors = listOf(
                White.copy(alpha = 0.78f),
                Blue_100.copy(alpha = 0.68f),
                Blue_Accent_500.copy(alpha = 0.18f)
            )
        )

    val AccountHubAvatarRingGradient: Brush
        get() = Brush.linearGradient(
            colors = listOf(
                Blue_500.copy(alpha = 0.70f),
                Blue_Accent_500.copy(alpha = 0.70f)
            )
        )

    val AccountHubDecorativeTint = Blue_Accent_500.copy(alpha = 0.12f)
    val AccountHubSectionAccent = Blue_600
    val AccountHubMutedText = Purple_500.copy(alpha = 0.62f)
    val AccountHubBodyText = Purple_500.copy(alpha = 0.82f)
    val AccountHubIconText = Purple_500.copy(alpha = 0.76f)
    val AccountHubOutline = White.copy(alpha = 0.88f)
    val AccountHubStrongOutline = White.copy(alpha = 0.94f)
    val AccountHubHeroOutline = White.copy(alpha = 0.92f)
    val AccountHubHeroSurface = White.copy(alpha = 0.78f)
    val AccountHubSummarySurface = White.copy(alpha = 0.76f)
    val AccountHubIconSurface = White.copy(alpha = 0.86f)
    val AccountHubFloatingSurface = White.copy(alpha = 0.82f)
    val AccountHubPillContainer = Blue_500.copy(alpha = 0.10f)
    val AccountHubPillBorder = Blue_500.copy(alpha = 0.22f)
    val AccountHubIconBorder = Blue_500.copy(alpha = 0.14f)
    val AccountHubDestructiveContainer = Red_Error.copy(alpha = 0.12f)
    val AccountHubDestructiveArrow = Red_Error.copy(alpha = 0.82f)
    val AccountHubDividerColor = Violet_200.copy(alpha = 0.62f)

    val AboutPurple = Color(0xFF7B4DF3)
    val AboutPurpleSoft = Color(0xFF8D5CFF)
    val AboutCyan = Color(0xFF22C7D2)
    val AboutCyanSoft = Color(0xFF8FEAF0)
    val AboutLavender = Color(0xFFB9A7FF)
    val AboutCreatorAvatar = Color(0xFF9D8BDC)
    val AboutGold = Color(0xFFFFB300)
    val AboutGoldStrong = Color(0xFFC08A00)
    val AboutGoldMuted = Color(0xFF9D7000)
    val AboutGoldSurface = Color(0xFFFFF7D7)
    val AboutGoldBorder = Color(0xFFFFCF5A)
    val AboutGoldGlow = Color(0xFFFFD24A)
    val AboutGoldSoft = Color(0xFFFFE9A7)
    val AboutGoldLight = Color(0xFFFFE39A)
    val AboutGoldPale = Color(0xFFFFF9E7)
    val AboutGlassSurface = White.copy(alpha = 0.58f)
    val AboutGlassSurfaceStrong = White.copy(alpha = 0.82f)
    val AboutGlassSurfaceMuted = White.copy(alpha = 0.46f)
    val AboutGlassBorder = White.copy(alpha = 0.88f)
    val AboutGlassBorderStrong = White.copy(alpha = 0.92f)

    val GlassGradient: Brush
        get() = Brush.verticalGradient(
            colors = listOf(
                White.copy(alpha = 0.72f),
                Violet_100.copy(alpha = 0.42f)
            )
        )

    val AttendanceReportBackground: Brush
        get() = Brush.verticalGradient(
            colors = listOf(
                Violet_100,
                Violet_50,
                Blue_50.copy(alpha = 0.62f)
            )
        )

    val AttendanceReportHeroGradient: Brush
        get() = Brush.linearGradient(
            colors = listOf(
                White.copy(alpha = 0.78f),
                Blue_500.copy(alpha = 0.10f),
                Blue_Accent_500.copy(alpha = 0.08f)
            )
        )

    val AttendanceReportGlassSurface = White.copy(alpha = 0.68f)
    val AttendanceReportGlassBorder = White.copy(alpha = 0.92f)
    val AttendanceReportMutedText = Purple_500.copy(alpha = 0.62f)
    val AttendanceReportBodyText = Purple_500.copy(alpha = 0.72f)
    val AttendanceReportWarningSurface = Orange_500.copy(alpha = 0.18f)
    val AttendanceReportWarningBorder = Orange_500.copy(alpha = 0.42f)
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
