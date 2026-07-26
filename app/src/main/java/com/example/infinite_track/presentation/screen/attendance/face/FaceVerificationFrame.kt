package com.example.infinite_track.presentation.screen.attendance.face

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.infinite_track.R

enum class RailMode { HIDDEN, PROGRESS, ALL_PASSED }
enum class FrameBadge { NONE, CHECK, CROSS }
enum class FrameInnerContent { NONE, SILHOUETTE, TIMEOUT_INFO }

/**
 * Visual style of the face frame for a given phase. Derived purely so it can be unit-tested.
 */
data class FrameStyle(
    val color: Color,
    val dashed: Boolean,
    val railMode: RailMode,
    val badge: FrameBadge,
    val inner: FrameInnerContent
)

private val FramePurple = Color(0xFF8A3DFF)
private val FrameCyan = Color(0xFF38F9F5)

private val FrameRed = Color(0xFFFF5C5C)

fun frameStyleFor(state: FaceScannerState): FrameStyle = when (state.livenessState) {
    LivenessState.IDLE,
    LivenessState.DETECTING_FACE -> FrameStyle(
        color = FramePurple, dashed = true,
        railMode = RailMode.HIDDEN, badge = FrameBadge.NONE,
        inner = FrameInnerContent.SILHOUETTE
    )

    LivenessState.WAITING_FOR_LIVENESS -> FrameStyle(
        color = FramePurple, dashed = false,
        railMode = RailMode.PROGRESS, badge = FrameBadge.NONE,
        inner = FrameInnerContent.NONE
    )

    LivenessState.LOW_LIGHT -> FrameStyle(
        color = FramePurple, dashed = false,
        railMode = RailMode.HIDDEN, badge = FrameBadge.NONE,
        inner = FrameInnerContent.NONE
    )

    LivenessState.LIVENESS_DETECTED -> FrameStyle(
        color = FramePurple, dashed = false,
        railMode = if (state.readyToVerify) RailMode.ALL_PASSED else RailMode.PROGRESS,
        badge = FrameBadge.NONE, inner = FrameInnerContent.NONE
    )

    LivenessState.VERIFYING_FACE -> FrameStyle(
        color = FramePurple, dashed = false,
        railMode = RailMode.ALL_PASSED, badge = FrameBadge.NONE,
        inner = FrameInnerContent.NONE
    )

    LivenessState.SUCCESS -> FrameStyle(
        color = FrameCyan, dashed = false,
        railMode = RailMode.HIDDEN, badge = FrameBadge.CHECK,
        inner = FrameInnerContent.NONE
    )

    LivenessState.FAILURE -> FrameStyle(
        color = FrameCyan, dashed = false,
        railMode = RailMode.HIDDEN, badge = FrameBadge.CROSS,
        inner = FrameInnerContent.NONE
    )

    LivenessState.TIMEOUT -> FrameStyle(
        color = FramePurple, dashed = false,
        railMode = RailMode.HIDDEN, badge = FrameBadge.NONE,
        inner = FrameInnerContent.TIMEOUT_INFO
    )
}

/**
 * Neon rounded face frame that changes style by phase (solid purple during liveness, cyan on
 * success, dashed purple with a silhouette while no face is detected). Overlays the numbered
 * rail during liveness and a check badge on success.
 */
@Composable
fun FaceVerificationFrame(
    state: FaceScannerState,
    modifier: Modifier = Modifier
) {
    val style = frameStyleFor(state)
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val frameInset = 8.dp
        val nodeSize = 28.dp

        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = if (style.dashed) {
                Stroke(
                    width = 6.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(24.dp.toPx(), 16.dp.toPx()), 0f
                    )
                )
            } else {
                Stroke(width = 6.dp.toPx())
            }
            val inset = 8.dp.toPx()
            drawRoundRect(
                color = style.color,
                topLeft = Offset(inset, inset),
                size = Size(size.width - inset * 2, size.height - inset * 2),
                cornerRadius = CornerRadius(48.dp.toPx(), 48.dp.toPx()),
                style = stroke
            )
        }

        if (style.inner == FrameInnerContent.SILHOUETTE) {
            Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = null,
                tint = FramePurple,
                modifier = Modifier.size(96.dp)
            )
        }

        if (style.railMode != RailMode.HIDDEN) {
            val nodeStates = if (style.railMode == RailMode.ALL_PASSED) {
                railNodeStates(state.challengeTotal, 0, state.challengeTotal)
            } else {
                railNodeStates(
                    (state.challengeIndex - 1).coerceAtLeast(0),
                    state.challengeIndex,
                    state.challengeTotal
                )
            }
            val placements = railNodePlacements(state.challengeTotal)

            placements.forEachIndexed { i, placement ->
                val nodeCenterY = maxHeight * placement.heightFraction
                val nodeX = when (placement.side) {
                    RailSide.LEFT -> frameInset - nodeSize / 2
                    RailSide.RIGHT -> maxWidth - frameInset - nodeSize / 2
                }
                val tickX = when (placement.side) {
                    RailSide.LEFT -> nodeX + nodeSize
                    RailSide.RIGHT -> nodeX - 12.dp
                }

                // Short horizontal tick connecting the node to the frame interior.
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = tickX, y = nodeCenterY - 1.dp)
                        .size(width = 12.dp, height = 2.dp)
                        .background(style.color)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = nodeX, y = nodeCenterY - nodeSize / 2)
                ) {
                    RailNode(number = i + 1, state = nodeStates[i])
                }
            }
        }

        if (style.badge != FrameBadge.NONE) {
            val badgeColor = when (style.badge) {
                FrameBadge.CHECK -> FramePurple
                FrameBadge.CROSS -> FrameRed
                FrameBadge.NONE -> Color.Transparent
            }
            val badgeIcon = when (style.badge) {
                FrameBadge.CROSS -> Icons.Filled.Close
                else -> Icons.Filled.Check
            }
            val badgeDescription = when (style.badge) {
                FrameBadge.CROSS -> stringResource(R.string.face_frame_badge_not_matched)
                else -> stringResource(R.string.face_frame_badge_verified)
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(badgeColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = badgeIcon,
                    contentDescription = badgeDescription,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
