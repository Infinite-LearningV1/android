package com.example.infinite_track.presentation.design.components.status

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.infinite_track.presentation.design.tokens.InfiniteFeedbackTypography
import com.example.infinite_track.presentation.design.tokens.InfiniteSemantic
import com.example.infinite_track.presentation.design.tokens.InfiniteSize
import com.example.infinite_track.presentation.design.tokens.infiniteFeedbackPalette
import com.example.infinite_track.presentation.theme.toAttendanceBadgeColor

enum class InfiniteStatusVariant {
    Active,
    Completed,
    NotStarted,
    Unavailable,
    OnTime,
    Late,
    Early,
    Alpha,
    Pending,
    Approved,
    Rejected,
    Inside,
    Outside,
    Unknown,
    Recommended,
    Excellent,
    Good,
    NeedsReview,
    PdfReady,
    Neutral
}

@Composable
fun InfiniteStatusPill(
    label: String,
    variant: InfiniteStatusVariant,
    modifier: Modifier = Modifier,
    size: InfiniteSize = InfiniteSize.Medium,
    leadingIcon: ImageVector? = null,
    selected: Boolean = false,
    /**
     * When true, reuse the same badge palette as WFA request status pills
     * (Status_Approved / Status_Pending / Status_Rejected / Status_Default).
     */
    useSharedRequestPalette: Boolean = false,
    colorOverride: Color? = null
) {
    val sharedColor = colorOverride ?: variant.toAttendanceBadgeColor()
    val semantic = variant.toSemantic()
    val semanticColors = infiniteFeedbackPalette(semantic)
    val containerColor = if (useSharedRequestPalette || colorOverride != null) {
        sharedColor.copy(alpha = 0.13f)
    } else {
        semanticColors.surfaceEnd
    }
    val contentColor = if (useSharedRequestPalette || colorOverride != null) {
        sharedColor
    } else {
        semanticColors.content
    }
    val borderColor = if (useSharedRequestPalette || colorOverride != null) {
        sharedColor.copy(alpha = 0.35f)
    } else {
        semanticColors.border
    }
    val iconTint = if (useSharedRequestPalette || colorOverride != null) {
        sharedColor
    } else {
        semanticColors.accent
    }
    val horizontal = when (size) {
        InfiniteSize.Small -> 6.dp
        InfiniteSize.Medium -> 10.dp
        InfiniteSize.Large -> 12.dp
    }
    val vertical = when (size) {
        InfiniteSize.Small -> 2.dp
        InfiniteSize.Medium -> 6.dp
        InfiniteSize.Large -> 8.dp
    }
    Surface(
        modifier = modifier.semantics { contentDescription = label },
        shape = RoundedCornerShape(999.dp),
        color = containerColor,
        contentColor = contentColor,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = horizontal, vertical = vertical),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            leadingIcon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(if (size == InfiniteSize.Small) 11.dp else 14.dp)
                )
            }
            Text(
                text = label,
                style = InfiniteFeedbackTypography.pillLabel,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

fun InfiniteStatusVariant.toSemantic(): InfiniteSemantic = when (this) {
    InfiniteStatusVariant.Active,
    InfiniteStatusVariant.Approved,
    InfiniteStatusVariant.Inside,
    InfiniteStatusVariant.Completed,
    InfiniteStatusVariant.OnTime,
    InfiniteStatusVariant.Excellent,
    InfiniteStatusVariant.PdfReady -> InfiniteSemantic.Success
    InfiniteStatusVariant.Pending,
    InfiniteStatusVariant.NeedsReview,
    InfiniteStatusVariant.Late,
    InfiniteStatusVariant.Early -> InfiniteSemantic.Warning
    InfiniteStatusVariant.Rejected,
    InfiniteStatusVariant.Outside,
    InfiniteStatusVariant.Alpha -> InfiniteSemantic.Error
    InfiniteStatusVariant.Recommended,
    InfiniteStatusVariant.Good -> InfiniteSemantic.Info
    InfiniteStatusVariant.NotStarted,
    InfiniteStatusVariant.Unavailable,
    InfiniteStatusVariant.Unknown,
    InfiniteStatusVariant.Neutral -> InfiniteSemantic.Neutral
}
