package com.example.infinite_track.presentation.design.components.data

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.infinite_track.presentation.design.tokens.InfiniteColors

@Composable
fun InfiniteGlassReportCard(
    modifier: Modifier = Modifier,
    extraContentPadding: Dp = 0.dp,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = InfiniteColors.Primary.copy(alpha = 0.10f),
                spotColor = InfiniteColors.Accent.copy(alpha = 0.08f)
            ),
        shape = RoundedCornerShape(24.dp),
        color = InfiniteColors.AttendanceReportGlassSurface,
        border = BorderStroke(1.dp, InfiniteColors.AttendanceReportGlassBorder)
    ) {
        Box(modifier = Modifier.padding(16.dp + extraContentPadding)) {
            content()
        }
    }
}

@Composable
fun InfiniteAttendanceReportNoticeCard(
    title: String,
    message: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = InfiniteColors.AttendanceReportWarningSurface,
        border = BorderStroke(1.dp, InfiniteColors.AttendanceReportWarningBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                color = InfiniteColors.Text,
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = message,
                color = InfiniteColors.AttendanceReportBodyText,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
