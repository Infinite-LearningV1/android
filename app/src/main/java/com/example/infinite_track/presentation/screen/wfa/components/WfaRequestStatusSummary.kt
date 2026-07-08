package com.example.infinite_track.presentation.screen.wfa.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.infinite_track.domain.model.booking.BookingHistorySummary
import com.example.infinite_track.presentation.theme.Blue_100
import com.example.infinite_track.presentation.theme.Blue_500
import com.example.infinite_track.presentation.theme.Purple_300
import com.example.infinite_track.presentation.theme.Purple_500
import com.example.infinite_track.presentation.theme.Status_Approved
import com.example.infinite_track.presentation.theme.Status_Pending
import com.example.infinite_track.presentation.theme.Status_Rejected
import com.example.infinite_track.presentation.theme.White

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
            icon = Icons.Default.Schedule,
            color = Status_Pending,
            modifier = Modifier.weight(1f)
        )
        SummaryCard(
            label = "Approved",
            value = summary.approved,
            caption = "Ready schedule",
            icon = Icons.Default.CheckCircle,
            color = Status_Approved,
            modifier = Modifier.weight(1f)
        )
        SummaryCard(
            label = "Rejected",
            value = summary.rejected,
            caption = "Needs follow-up",
            icon = Icons.Default.Cancel,
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
                elevation = 6.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = Blue_500.copy(alpha = 0.18f),
                spotColor = Blue_500.copy(alpha = 0.12f)
            )
            .background(White.copy(alpha = 0.34f), RoundedCornerShape(18.dp))
            .border(BorderStroke(1.dp, Blue_100.copy(alpha = 0.9f)), RoundedCornerShape(18.dp))
            .padding(horizontal = 8.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = Purple_500,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.headlineSmall,
            color = Purple_500,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = caption,
            style = MaterialTheme.typography.labelSmall,
            color = Purple_300,
            textAlign = TextAlign.Center
        )
    }
}
