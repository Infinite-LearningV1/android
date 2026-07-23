package com.example.infinite_track.presentation.design.tokens

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.infinite_track.presentation.theme.White
import com.example.infinite_track.presentation.theme.sfCompact_font

@Immutable
data class InfiniteFeedbackPalette(
    val surface: Color,
    val stateContainer: Color,
    val content: Color,
    val supportingContent: Color,
    val border: Color,
    val accent: Color,
    val shadow: Color,
    val topHighlight: Color
)

object InfiniteFeedbackTypography {
    val snackbarMessage = TextStyle(
        fontFamily = sfCompact_font,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )
    val snackbarTitle = TextStyle(
        fontFamily = sfCompact_font,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 20.sp
    )
    val supportingBody = TextStyle(
        fontFamily = sfCompact_font,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )
    val pillLabel = TextStyle(
        fontFamily = sfCompact_font,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp
    )
    val actionLabel = TextStyle(
        fontFamily = sfCompact_font,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )
    val dialogTitle = TextStyle(
        fontFamily = sfCompact_font,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 24.sp
    )
    val dialogBody = TextStyle(
        fontFamily = sfCompact_font,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )
}

fun infiniteFeedbackPalette(semantic: InfiniteSemantic): InfiniteFeedbackPalette {
    val semanticColors = semanticColors(semantic)
    return InfiniteFeedbackPalette(
        surface = White.copy(alpha = 0.94f),
        stateContainer = semanticColors.container,
        content = semanticColors.content,
        supportingContent = semanticColors.content.copy(alpha = 0.76f),
        border = semanticColors.border.copy(alpha = 0.72f),
        accent = semanticColors.accent,
        shadow = InfiniteColors.Text.copy(alpha = 0.10f),
        topHighlight = White.copy(alpha = 0.88f)
    )
}

private fun semanticColors(semantic: InfiniteSemantic): InfiniteSemanticColors = when (semantic) {
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
        container = InfiniteColors.Surface.copy(alpha = 0.74f),
        content = InfiniteColors.Text,
        border = InfiniteColors.Neutral.copy(alpha = 0.20f),
        accent = InfiniteColors.Neutral
    )
}
