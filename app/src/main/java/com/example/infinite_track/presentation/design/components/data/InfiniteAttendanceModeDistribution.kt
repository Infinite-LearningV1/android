package com.example.infinite_track.presentation.design.components.data

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.infinite_track.presentation.core.body1
import com.example.infinite_track.presentation.core.body2
import com.example.infinite_track.presentation.core.headline4
import com.example.infinite_track.presentation.design.tokens.InfiniteColors
import com.example.infinite_track.presentation.theme.Purple_300
import com.example.infinite_track.presentation.theme.Purple_500
import com.example.infinite_track.presentation.theme.attendanceModeColor

@Composable
fun InfiniteAttendanceModeDistributionCard(
    wfoCount: Int,
    wfaCount: Int,
    modifier: Modifier = Modifier,
    wfhCount: Int? = null,
    title: String = "Attendance Mode",
    subtitle: String? = null,
    unavailableModeMessage: String? = null
) {
    val totalKnown = wfoCount + wfaCount + (wfhCount ?: 0)

    InfiniteGlassReportCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    style = headline4,
                    color = Purple_500
                )
                Icon(
                    imageVector = Icons.Outlined.WorkOutline,
                    contentDescription = null,
                    tint = Purple_500,
                    modifier = Modifier.size(20.dp)
                )
            }

            subtitle?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = body2,
                    color = Purple_300
                )
            }

            InfiniteAttendanceModeDistributionRow(
                label = "WFO",
                count = wfoCount,
                total = totalKnown,
                color = attendanceModeColor("wfo"),
                unitLabel = "days"
            )
            InfiniteAttendanceModeDistributionRow(
                label = "WFA",
                count = wfaCount,
                total = totalKnown,
                color = attendanceModeColor("wfa"),
                unitLabel = "days"
            )
            wfhCount?.let { count ->
                InfiniteAttendanceModeDistributionRow(
                    label = "WFH",
                    count = count,
                    total = totalKnown,
                    color = attendanceModeColor("wfh"),
                    unitLabel = "days"
                )
            } ?: unavailableModeMessage?.let { message ->
                Text(
                    text = message,
                    color = Purple_300,
                    style = body2
                )
            }
        }
    }
}

@Composable
fun InfiniteAttendanceModeDistributionRow(
    label: String,
    count: Int,
    total: Int,
    modifier: Modifier = Modifier,
    color: Color = InfiniteColors.Primary,
    unitLabel: String = "records"
) {
    val progress = if (total > 0) count.toFloat() / total.toFloat() else 0f
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = label,
            color = Purple_500,
            style = body1,
            modifier = Modifier.width(36.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(10.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(InfiniteColors.Surface.copy(alpha = 0.72f))
        ) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp),
                color = color,
                trackColor = Color.Transparent
            )
        }
        Text(
            text = "$count $unitLabel",
            color = Purple_300,
            style = body2,
            modifier = Modifier.width(58.dp)
        )
    }
}
