package com.example.infinite_track.presentation.design.tokens

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.example.infinite_track.presentation.core.body1
import com.example.infinite_track.presentation.core.body2
import com.example.infinite_track.presentation.core.body2_5
import com.example.infinite_track.presentation.core.headline3
import com.example.infinite_track.presentation.theme.White

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
    val inlineTitle = body1.copy(fontWeight = FontWeight.Bold)
    val inlineBody = body2
    val snackbarMessage = body1
    val snackbarTitle = body1.copy(fontWeight = FontWeight.Bold)
    val supportingBody = body2
    val pillLabel = body2_5
    val actionLabel = body2
    val dialogTitle = headline3
    val dialogBody = body1
}

fun infiniteFeedbackPalette(semantic: InfiniteSemantic): InfiniteFeedbackPalette {
    val semanticColors = semanticColors(semantic)
    return InfiniteFeedbackPalette(
        surface = InfiniteColors.AttendanceReportGlassSurface,
        stateContainer = semanticColors.container,
        content = semanticColors.content,
        supportingContent = semanticColors.content.copy(alpha = 0.76f),
        border = semanticColors.border.copy(alpha = 0.52f),
        accent = semanticColors.accent,
        shadow = InfiniteColors.Text.copy(alpha = 0.08f),
        topHighlight = White.copy(alpha = 0.56f)
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
