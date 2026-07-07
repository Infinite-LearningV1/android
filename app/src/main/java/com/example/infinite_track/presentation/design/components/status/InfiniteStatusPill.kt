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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.infinite_track.presentation.design.tokens.InfiniteColors
import com.example.infinite_track.presentation.design.tokens.InfiniteSemantic
import com.example.infinite_track.presentation.design.tokens.InfiniteSize
import com.example.infinite_track.presentation.design.tokens.infiniteSemanticColors

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
    selected: Boolean = false
) {
    val semantic = variant.toSemantic()
    val colors = infiniteSemanticColors(semantic)
    val horizontal = when (size) {
        InfiniteSize.Small -> 8.dp
        InfiniteSize.Medium -> 10.dp
        InfiniteSize.Large -> 12.dp
    }
    val vertical = when (size) {
        InfiniteSize.Small -> 4.dp
        InfiniteSize.Medium -> 6.dp
        InfiniteSize.Large -> 8.dp
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = colors.container,
        contentColor = colors.content,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, colors.border)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = horizontal, vertical = vertical),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            leadingIcon?.let {
                Icon(imageVector = it, contentDescription = null, tint = colors.accent, modifier = Modifier.size(14.dp))
            }
            Text(text = label)
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
