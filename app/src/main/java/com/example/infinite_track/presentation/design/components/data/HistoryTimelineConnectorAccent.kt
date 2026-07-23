package com.example.infinite_track.presentation.design.components.data

import androidx.compose.runtime.Immutable

@Immutable
data class HistoryTimelineConnectorAccent(
    val topFraction: Float,
    val nodeComplete: Boolean,
    val bottomFraction: Float
)

@Immutable
internal data class HistoryFocusTransform(
    val alpha: Float,
    val scale: Float,
    val translationYDp: Float,
    val elevationDp: Float
)

internal fun resolveHistoryFocusTransform(
    focusFraction: Float,
    motionEnabled: Boolean
): HistoryFocusTransform {
    if (!motionEnabled) return HistoryFocusTransform(1f, 1f, 0f, 0f)
    val focus = focusFraction.coerceIn(0f, 1f)
    return HistoryFocusTransform(
        alpha = 0.70f + 0.30f * focus,
        scale = 0.965f + 0.035f * focus,
        translationYDp = if (focus == 0f) 0f else -3f * focus,
        elevationDp = 3f * focus
    )
}
