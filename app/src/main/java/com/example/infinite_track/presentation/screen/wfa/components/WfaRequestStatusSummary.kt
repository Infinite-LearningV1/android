package com.example.infinite_track.presentation.screen.wfa.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.infinite_track.domain.model.booking.BookingHistorySummary
import com.example.infinite_track.presentation.core.body2
import com.example.infinite_track.presentation.core.body3
import com.example.infinite_track.presentation.core.headline4
import com.example.infinite_track.presentation.design.tokens.InfiniteColors
import com.example.infinite_track.presentation.theme.Purple_300
import com.example.infinite_track.presentation.theme.Purple_500
import com.example.infinite_track.presentation.theme.Status_Approved
import com.example.infinite_track.presentation.theme.Status_Pending
import com.example.infinite_track.presentation.theme.Status_Rejected

@Composable
fun WfaRequestStatusSummary(
    summary: BookingHistorySummary,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SummaryCard(
            label = "Pending",
            value = summary.pending,
            caption = "Waiting review",
            icon = Icons.Outlined.Schedule,
            color = Status_Pending,
            modifier = Modifier.weight(1f)
        )
        SummaryCard(
            label = "Approved",
            value = summary.approved,
            caption = "Ready for schedule",
            icon = Icons.Outlined.CheckCircle,
            color = Status_Approved,
            modifier = Modifier.weight(1f)
        )
        SummaryCard(
            label = "Rejected",
            value = summary.rejected,
            caption = "Needs follow-up",
            icon = Icons.Outlined.Cancel,
            color = Status_Rejected,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SummaryCard(
    label: String,
    value: Int,
    caption: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = InfiniteColors.Primary.copy(alpha = 0.10f),
                spotColor = InfiniteColors.Accent.copy(alpha = 0.08f)
            )
            .background(InfiniteColors.AttendanceReportGlassSurface, RoundedCornerShape(24.dp))
            .border(BorderStroke(1.dp, InfiniteColors.AttendanceReportGlassBorder), RoundedCornerShape(24.dp))
            .padding(horizontal = 10.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
            }
            Column {
                Text(
                    text = label,
                    style = body2,
                    color = Purple_500
                )
                Text(
                    text = value.toString(),
                    style = headline4,
                    color = Purple_500
                )
            }
        }
        Text(
            text = caption,
            style = body3,
            color = Purple_300
        )
    }
}
