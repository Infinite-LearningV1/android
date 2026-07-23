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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
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
    val clickAction = if (isCurrentAction) onClick else null
    val enabled = clickAction != null
    val futureInactive = !item.isReady && !isCurrentAction
    val shape = RoundedCornerShape(18.dp)
    val accessKey = item.access.name.lowercase()
    val stateCopy = buildList {
        item.requirementLabel.takeIf(String::isNotBlank)?.let(::add)
        item.statusLabel.takeIf(String::isNotBlank)?.let(::add)
        if (enabled) {
            item.actionLabel?.takeIf(String::isNotBlank)?.let { add("Aksi $it") }
        }
    }.joinToString(", ")
    val surfaceColor = if (futureInactive) {
        colors.container.copy(alpha = 0.55f)
    } else {
        colors.container
    }
    val borderColor = if (futureInactive) {
        colors.border.copy(alpha = 0.55f)
    } else {
        colors.border
    }
    val contentColor = if (futureInactive) {
        colors.content.copy(alpha = 0.72f)
    } else {
        colors.content
    }
    val supportingColor = if (futureInactive) {
        colors.content.copy(alpha = 0.62f)
    } else {
        colors.content.copy(alpha = 0.74f)
    }
    val labelColor = if (futureInactive) contentColor else colors.accent
    val labelTag = if (enabled) {
        "permission-step-action-$accessKey"
    } else {
        "permission-step-status-$accessKey"
    }
    val railAccent = if (futureInactive) colors.accent.copy(alpha = 0.55f) else colors.accent

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .testTag("permission-step-$accessKey")
            .semantics(mergeDescendants = true) {
                contentDescription = item.title
                stateDescription = stateCopy
                if (enabled) role = Role.Button
            }
    ) {
        val density = LocalDensity.current
        val compact = maxWidth < 360.dp || density.fontScale >= 1.5f
        val nodeDiameter = maxOf(
            32.dp,
            with(density) { body1.lineHeight.toDp() } + InfiniteSpacing.Default.sm
        )
        val railWidth = nodeDiameter + InfiniteSpacing.Default.xs

        Box(Modifier.fillMaxWidth()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = railWidth + InfiniteSpacing.Default.sm,
                        top = InfiniteSpacing.Default.xs,
                        bottom = InfiniteSpacing.Default.xs
                    )
                    .then(
                        if (clickAction != null) {
                            Modifier.clickable(
                                role = Role.Button,
                                onClick = clickAction
                            )
                        } else {
                            Modifier
                        }
                    )
                    .sizeIn(minHeight = 48.dp),
                shape = shape,
                color = surfaceColor,
                border = BorderStroke(1.dp, borderColor),
                shadowElevation = if (enabled) 3.dp else 0.dp
            ) {
                Box(Modifier.padding(InfiniteSpacing.Default.lg)) {
                    val copy: @Composable (Modifier) -> Unit = { copyModifier ->
                        Column(
                            modifier = copyModifier
                                .clearAndSetSemantics {}
                                .testTag("permission-step-copy-$accessKey"),
                            verticalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.xs)
                        ) {
                            Text(item.title, style = headline4, color = contentColor)
                            Text(
                                item.supportingText,
                                style = body2,
                                color = supportingColor
                            )
                        }
                    }
                    val label: @Composable () -> Unit = {
                        Text(
                            text = if (enabled) {
                                item.actionLabel ?: item.statusLabel
                            } else {
                                item.statusLabel
                            },
                            style = body1,
                            color = labelColor,
                            modifier = Modifier
                                .clearAndSetSemantics {}
                                .testTag(labelTag)
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
            Box(Modifier.matchParentSize()) {
                PermissionTimelineRail(
                    stepNumber = stepNumber,
                    complete = item.isReady,
                    showTop = showTopConnector,
                    showBottom = showBottomConnector,
                    topComplete = topConnectorComplete,
                    bottomComplete = bottomConnectorComplete,
                    accent = railAccent,
                    nodeDiameter = nodeDiameter,
                    modifier = Modifier
                        .width(railWidth)
                        .fillMaxHeight(),
                    nodeTag = "permission-step-node-$accessKey",
                    nodeLabelTag = "permission-step-node-label-$accessKey"
                )
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
    nodeDiameter: Dp,
    modifier: Modifier = Modifier,
    nodeTag: String,
    nodeLabelTag: String
) {
    Box(
        modifier = modifier,
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
                .size(nodeDiameter)
                .testTag(nodeTag),
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
                    Text(
                        stepNumber.toString(),
                        style = body1,
                        color = accent,
                        modifier = Modifier
                            .clearAndSetSemantics {}
                            .testTag(nodeLabelTag)
                    )
                }
            }
        }
    }
}
