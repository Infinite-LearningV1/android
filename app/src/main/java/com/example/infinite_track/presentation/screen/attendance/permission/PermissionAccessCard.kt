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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.example.infinite_track.presentation.design.components.button.InfiniteButton
import com.example.infinite_track.presentation.design.components.button.InfiniteButtonVariant
import com.example.infinite_track.presentation.design.tokens.InfiniteColors
import com.example.infinite_track.presentation.design.tokens.InfiniteFeedbackTypography
import com.example.infinite_track.presentation.design.tokens.InfiniteSize
import com.example.infinite_track.presentation.design.tokens.InfiniteSpacing
import com.example.infinite_track.presentation.design.tokens.infiniteSemanticColors

@Composable
internal fun PermissionAccessCard(
    item: PermissionItemUiModel,
    modifier: Modifier = Modifier,
    onActionClick: (() -> Unit)? = null
) {
    val stateColors = infiniteSemanticColors(item.semantic)
    val accessKey = item.access.name.lowercase()
    val actionAvailable = item.actionLabel != null && onActionClick != null
    val containerColor = permissionAccessContainerColor(
        isReady = item.isReady,
        stateContainer = stateColors.container
    )
    val borderColor = if (item.isReady) {
        stateColors.border.compositeOver(InfiniteColors.Surface)
    } else {
        InfiniteColors.Neutral.copy(alpha = 0.18f).compositeOver(InfiniteColors.Surface)
    }
    val statusColor = if (item.isReady) stateColors.accent else InfiniteColors.Neutral

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("permission-access-card-$accessKey")
            .semantics(mergeDescendants = true) {
                contentDescription = item.title
                stateDescription = listOf(
                    item.requirementLabel,
                    item.statusLabel
                ).filter(String::isNotBlank).joinToString(", ")
            },
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = if (item.isReady) 2.dp else 1.dp
    ) {
        BoxWithConstraints(modifier = Modifier.padding(InfiniteSpacing.Default.lg)) {
            val compact = maxWidth < 360.dp || LocalDensity.current.fontScale >= 1.5f
            val copy: @Composable (Modifier) -> Unit = { copyModifier ->
                Column(
                    modifier = copyModifier,
                    verticalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.xs)
                ) {
                    Text(
                        text = item.title,
                        style = InfiniteFeedbackTypography.snackbarTitle,
                        color = InfiniteColors.Text
                    )
                    Text(
                        text = item.supportingText,
                        style = InfiniteFeedbackTypography.supportingBody,
                        color = InfiniteColors.Text.copy(alpha = 0.72f)
                    )
                    Text(
                        text = "${item.requirementLabel} · ${item.statusLabel}",
                        style = InfiniteFeedbackTypography.pillLabel,
                        color = statusColor
                    )
                }
            }
            val action: @Composable () -> Unit = {
                if (actionAvailable) {
                    InfiniteButton(
                        text = requireNotNull(item.actionLabel),
                        onClick = requireNotNull(onActionClick),
                        modifier = Modifier
                            .then(if (compact) Modifier.fillMaxWidth() else Modifier)
                            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                            .testTag("permission-access-action-$accessKey"),
                        variant = InfiniteButtonVariant.Ghost,
                        size = InfiniteSize.Small
                    )
                }
            }

            if (compact) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.md)
                ) {
                    copy(Modifier.fillMaxWidth())
                    action()
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    copy(Modifier.weight(1f))
                    action()
                }
            }
        }
    }
}

internal fun permissionAccessContainerColor(
    isReady: Boolean,
    stateContainer: Color,
    surface: Color = InfiniteColors.Surface
): Color = if (isReady) stateContainer.compositeOver(surface) else surface
