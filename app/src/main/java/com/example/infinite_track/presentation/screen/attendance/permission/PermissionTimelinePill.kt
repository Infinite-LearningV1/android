package com.example.infinite_track.presentation.screen.attendance.permission

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.example.infinite_track.presentation.core.body1
import com.example.infinite_track.presentation.core.body2
import com.example.infinite_track.presentation.core.headline4
import com.example.infinite_track.presentation.design.tokens.InfiniteColors
import com.example.infinite_track.presentation.design.tokens.InfiniteMotion
import com.example.infinite_track.presentation.design.tokens.InfiniteSpacing
import com.example.infinite_track.presentation.design.tokens.infiniteSemanticColors

@Composable
fun PermissionTimelinePill(
    item: PermissionItemUiModel,
    stepNumber: Int,
    isCurrentAction: Boolean,
    showTopConnector: Boolean,
    showBottomConnector: Boolean,
    topConnectorComplete: Boolean,
    bottomConnectorComplete: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val colors = infiniteSemanticColors(item.semantic)
    val enabled = isCurrentAction && onClick != null
    val shape = RoundedCornerShape(18.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .testTag("permission-step-${item.access.name.lowercase()}")
            .semantics(mergeDescendants = true) {
                contentDescription = item.title
                stateDescription = item.stateDescription
                if (enabled) role = Role.Button
            }
    ) {
        PermissionTimelineRail(
            stepNumber = stepNumber,
            complete = item.isReady,
            showTop = showTopConnector,
            showBottom = showBottomConnector,
            topComplete = topConnectorComplete,
            bottomComplete = bottomConnectorComplete,
            accent = colors.accent,
            modifier = Modifier.fillMaxHeight(),
            nodeTag = "permission-step-node-${item.access.name.lowercase()}"
        )
        Spacer(Modifier.width(InfiniteSpacing.Default.sm))
        Surface(
            modifier = Modifier
                .weight(1f)
                .then(
                    if (enabled) {
                        Modifier.clickable { onClick?.invoke() }
                    } else {
                        Modifier
                    }
                )
                .sizeIn(minHeight = 48.dp),
            shape = shape,
            color = colors.container,
            border = BorderStroke(1.dp, colors.border),
            shadowElevation = if (isCurrentAction) 3.dp else 0.dp
        ) {
            BoxWithConstraints(Modifier.padding(InfiniteSpacing.Default.lg)) {
                val compact = maxWidth < 360.dp || LocalDensity.current.fontScale >= 1.5f
                val copy: @Composable (Modifier) -> Unit = { copyModifier ->
                    Column(
                        modifier = copyModifier,
                        verticalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.xs)
                    ) {
                        Text(item.title, style = headline4, color = colors.content)
                        Text(
                            item.supportingText,
                            style = body2,
                            color = colors.content.copy(alpha = 0.74f)
                        )
                    }
                }
                val label: @Composable () -> Unit = {
                    Text(
                        text = item.actionLabel ?: item.statusLabel,
                        style = body1,
                        color = colors.accent,
                        modifier = Modifier
                            .sizeIn(minHeight = 48.dp)
                            .wrapContentHeight()
                    )
                }
                if (compact) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.sm)
                    ) {
                        copy(Modifier.fillMaxWidth())
                        label()
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.md)
                    ) {
                        copy(Modifier.weight(1f))
                        label()
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionTimelineRail(
    stepNumber: Int,
    complete: Boolean,
    showTop: Boolean,
    showBottom: Boolean,
    topComplete: Boolean,
    bottomComplete: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    nodeTag: String
) {
    Box(
        modifier = modifier.width(36.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val x = size.width / 2f
            val centerY = size.height / 2f
            val stroke = 2.dp.toPx()
            val muted = InfiniteColors.Neutral.copy(alpha = 0.22f)
            if (showTop) {
                drawLine(muted, Offset(x, 0f), Offset(x, centerY), stroke)
                if (topComplete) {
                    drawLine(accent, Offset(x, 0f), Offset(x, centerY), stroke)
                }
            }
            if (showBottom) {
                drawLine(muted, Offset(x, centerY), Offset(x, size.height), stroke)
                if (bottomComplete) {
                    drawLine(accent, Offset(x, centerY), Offset(x, size.height), stroke)
                }
            }
        }
        Surface(
            modifier = Modifier
                .size(32.dp)
                .testTag(nodeTag)
                .semantics {
                    contentDescription = if (complete) "Selesai" else "Langkah $stepNumber"
                },
            shape = CircleShape,
            color = accent.copy(alpha = if (complete) 0.20f else 0.12f),
            border = BorderStroke(1.dp, accent.copy(alpha = 0.42f))
        ) {
            AnimatedContent(
                targetState = complete,
                transitionSpec = {
                    (fadeIn(InfiniteMotion.stateChangeTween()) +
                        scaleIn(
                            InfiniteMotion.stateChangeTween(),
                            initialScale = 0.82f
                        ))
                        .togetherWith(
                            fadeOut(InfiniteMotion.stateChangeTween()) +
                                scaleOut(
                                    InfiniteMotion.stateChangeTween(),
                                    targetScale = 0.82f
                                )
                        )
                },
                contentAlignment = Alignment.Center,
                label = "permission-step-state"
            ) { isComplete ->
                if (isComplete) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = accent)
                } else {
                    Text(stepNumber.toString(), style = body1, color = accent)
                }
            }
        }
    }
}
