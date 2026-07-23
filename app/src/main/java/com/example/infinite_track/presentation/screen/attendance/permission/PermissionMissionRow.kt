package com.example.infinite_track.presentation.screen.attendance.permission

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.example.infinite_track.presentation.design.components.button.InfiniteButton
import com.example.infinite_track.presentation.design.components.button.InfiniteButtonVariant
import com.example.infinite_track.presentation.design.components.data.InfiniteInfoRow
import com.example.infinite_track.presentation.design.components.data.InfiniteInfoRowOrientation
import com.example.infinite_track.presentation.design.components.status.InfiniteStatusPill
import com.example.infinite_track.presentation.design.components.status.InfiniteStatusVariant
import com.example.infinite_track.presentation.design.tokens.InfiniteSemantic
import com.example.infinite_track.presentation.design.tokens.InfiniteSize
import com.example.infinite_track.presentation.design.tokens.infiniteSemanticColors

@Composable
internal fun PermissionMissionRow(
    item: PermissionItemUiModel,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onActionClick: (() -> Unit)? = null
) {
    val colors = infiniteSemanticColors(item.semantic)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = item.title
                stateDescription = item.stateDescription
            },
        shape = RoundedCornerShape(16.dp),
        color = colors.container,
        border = BorderStroke(1.dp, colors.border)
    ) {
        BoxWithConstraints(modifier = Modifier.padding(14.dp)) {
            val compact = maxWidth < 360.dp || LocalDensity.current.fontScale >= 1.5f
            val action: @Composable () -> Unit = {
                if (item.actionLabel != null && onActionClick != null) {
                    InfiniteButton(
                        text = item.actionLabel,
                        onClick = onActionClick,
                        modifier = Modifier
                            .then(if (compact) Modifier.fillMaxWidth() else Modifier)
                            .sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                        variant = InfiniteButtonVariant.Ghost,
                        size = InfiniteSize.Small
                    )
                }
            }
            if (compact) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PermissionMissionInfo(item = item, icon = icon)
                    action()
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PermissionMissionInfo(
                        item = item,
                        icon = icon,
                        modifier = Modifier.weight(1f)
                    )
                    action()
                }
            }
        }
    }
}

@Composable
private fun PermissionMissionInfo(
    item: PermissionItemUiModel,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    InfiniteInfoRow(
        label = item.title,
        value = item.supportingText,
        modifier = modifier,
        icon = icon,
        semantic = item.semantic,
        orientation = InfiniteInfoRowOrientation.Vertical,
        statusContent = {
            InfiniteStatusPill(
                label = "${item.requirementLabel} · ${item.statusLabel}",
                variant = item.semantic.toStatusVariant(),
                size = InfiniteSize.Small,
                leadingIcon = if (item.isReady) Icons.Default.CheckCircle else null
            )
        }
    )
}

private fun InfiniteSemantic.toStatusVariant(): InfiniteStatusVariant = when (this) {
    InfiniteSemantic.Success -> InfiniteStatusVariant.Completed
    InfiniteSemantic.Warning -> InfiniteStatusVariant.NeedsReview
    InfiniteSemantic.Error -> InfiniteStatusVariant.Rejected
    InfiniteSemantic.Info -> InfiniteStatusVariant.Recommended
    InfiniteSemantic.Primary,
    InfiniteSemantic.Secondary,
    InfiniteSemantic.Neutral -> InfiniteStatusVariant.Neutral
}
