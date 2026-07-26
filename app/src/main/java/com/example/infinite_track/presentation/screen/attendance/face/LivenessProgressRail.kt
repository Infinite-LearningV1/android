package com.example.infinite_track.presentation.screen.attendance.face

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.infinite_track.presentation.design.tokens.InfiniteColors

enum class RailNodeState { PASSED, ACTIVE, PENDING }

/**
 * Pure mapping of the numbered rail node states from liveness progress. Node numbering is
 * 1-based. Kept free of Android types for JVM unit testing.
 */
fun railNodeStates(passedCount: Int, activeIndex: Int, total: Int = 4): List<RailNodeState> =
    (1..total).map { n ->
        when {
            n <= passedCount -> RailNodeState.PASSED
            n == activeIndex -> RailNodeState.ACTIVE
            else -> RailNodeState.PENDING
        }
    }

enum class RailSide { LEFT, RIGHT }

data class RailNodePlacement(val side: RailSide, val heightFraction: Float)

/**
 * Border positions for the liveness nodes, as fractions of the frame height. The 4-challenge
 * layout mirrors the approved mockups (1-2 on the left edge, 3-4 on the right); other totals
 * alternate sides with even spacing. Pure for JVM unit testing.
 */
fun railNodePlacements(total: Int): List<RailNodePlacement> =
    if (total == 4) {
        listOf(
            RailNodePlacement(RailSide.LEFT, 0.20f),
            RailNodePlacement(RailSide.LEFT, 0.45f),
            RailNodePlacement(RailSide.RIGHT, 0.35f),
            RailNodePlacement(RailSide.RIGHT, 0.55f)
        )
    } else {
        (1..total).map { n ->
            RailNodePlacement(
                side = if (n % 2 == 1) RailSide.LEFT else RailSide.RIGHT,
                heightFraction = n.toFloat() / (total + 1)
            )
        }
    }

@Composable
internal fun RailNode(number: Int, state: RailNodeState) {
    val container = when (state) {
        RailNodeState.PASSED -> InfiniteColors.Accent
        RailNodeState.ACTIVE -> InfiniteColors.Secondary
        RailNodeState.PENDING -> Color.Transparent
    }
    val border = when (state) {
        RailNodeState.PENDING -> InfiniteColors.Surface.copy(alpha = 0.9f)
        else -> container
    }
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(container)
            .border(2.dp, border, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (state == RailNodeState.PASSED) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = InfiniteColors.Text,
                modifier = Modifier.size(16.dp)
            )
        } else {
            Text(
                text = number.toString(),
                color = if (state == RailNodeState.ACTIVE) InfiniteColors.Text else InfiniteColors.Surface,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
    }
}
