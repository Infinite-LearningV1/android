package com.example.infinite_track.presentation.screen.attendance.face

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.unit.dp
import com.example.infinite_track.presentation.design.tokens.InfiniteColors

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
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
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
            val passed = if (style.railMode == RailMode.ALL_PASSED) {
                state.challengeTotal
            } else {
                (state.challengeIndex - 1).coerceAtLeast(0)
            }
            val active = if (style.railMode == RailMode.ALL_PASSED) 0 else state.challengeIndex
            LivenessProgressRail(
                passedCount = passed,
                activeIndex = active,
                total = state.challengeTotal,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 4.dp)
            )
        }

        if (style.badge == FrameBadge.CHECK) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(InfiniteColors.Surface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = FramePurple,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
