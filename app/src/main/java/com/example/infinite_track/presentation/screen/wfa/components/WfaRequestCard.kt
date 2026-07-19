package com.example.infinite_track.presentation.screen.wfa.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.infinite_track.domain.model.booking.BookingHistoryItem
import com.example.infinite_track.presentation.core.body2
import com.example.infinite_track.presentation.core.body3
import com.example.infinite_track.presentation.core.headline4
import com.example.infinite_track.presentation.design.components.status.InfiniteStatusPill
import com.example.infinite_track.presentation.design.components.status.InfiniteStatusVariant
import com.example.infinite_track.presentation.design.tokens.InfiniteColors
import com.example.infinite_track.presentation.design.tokens.InfiniteSize
import com.example.infinite_track.presentation.theme.Blue_500
import com.example.infinite_track.presentation.theme.Purple_300
import com.example.infinite_track.presentation.theme.Purple_500
import com.example.infinite_track.presentation.theme.requestStatusColor

@Composable
fun WfaRequestCard(
    booking: BookingHistoryItem,
    modifier: Modifier = Modifier
) {
    val statusColor = requestStatusColor(booking.statusKey)
    val statusVariant = when (booking.statusKey.orEmpty().lowercase()) {
        "approved" -> InfiniteStatusVariant.Approved
        "rejected" -> InfiniteStatusVariant.Rejected
        "pending" -> InfiniteStatusVariant.Pending
        else -> InfiniteStatusVariant.Neutral
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = InfiniteColors.Primary.copy(alpha = 0.10f),
                spotColor = InfiniteColors.Accent.copy(alpha = 0.08f)
            )
            .clip(RoundedCornerShape(24.dp))
            .background(InfiniteColors.AttendanceReportGlassSurface)
            .border(
                BorderStroke(1.dp, InfiniteColors.AttendanceReportGlassBorder),
                RoundedCornerShape(24.dp)
            )
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(statusColor)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = booking.scheduleDate,
                        style = headline4,
                        color = Purple_500
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = Blue_500,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = booking.locationDescription,
                            style = body2,
                            color = Purple_300,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                InfiniteStatusPill(
                    label = booking.statusLabel,
                    variant = statusVariant,
                    size = InfiniteSize.Small,
                    useSharedRequestPalette = true,
                    colorOverride = statusColor
                )
            }

            HorizontalDivider(color = InfiniteColors.AttendanceReportGlassBorder)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Suitability Score",
                    style = body2,
                    color = Purple_300
                )
                ScoreProgressBadge(
                    score = booking.suitabilityScore,
                    color = statusColor
                )
                Text(
                    text = booking.suitabilityLabel,
                    style = body2,
                    color = statusColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            HorizontalDivider(color = InfiniteColors.AttendanceReportGlassBorder)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Notes",
                    style = body2,
                    color = Purple_300,
                    modifier = Modifier.width(48.dp)
                )
                Text(
                    text = booking.notes.ifBlank { "-" },
                    style = body2,
                    color = Purple_500,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MetadataItem(
                    iconTint = Blue_500,
                    icon = Icons.Outlined.CalendarMonth,
                    text = "Submitted: ${booking.createdAt}",
                    modifier = Modifier.weight(1f)
                )
                MetadataItem(
                    iconTint = Blue_500,
                    icon = Icons.Outlined.HourglassEmpty,
                    text = "Processed: ${booking.processedAt}",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ScoreProgressBadge(
    score: Double?,
    color: Color
) {
    val progress = ((score ?: 0.0) / 100.0).toFloat().coerceIn(0f, 1f)
    Box(
        modifier = Modifier.size(36.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = { 1f },
            modifier = Modifier.fillMaxSize(),
            color = color.copy(alpha = 0.16f),
            strokeWidth = 3.dp,
            trackColor = Color.Transparent,
            strokeCap = StrokeCap.Round
        )
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxSize(),
            color = color,
            strokeWidth = 3.dp,
            trackColor = Color.Transparent,
            strokeCap = StrokeCap.Round
        )
        Text(
            text = score?.let { String.format("%.1f", it) } ?: "-",
            style = body3,
            color = color
        )
    }
}

@Composable
private fun MetadataItem(
    iconTint: Color,
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = text,
            style = body3,
            color = Purple_300,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
