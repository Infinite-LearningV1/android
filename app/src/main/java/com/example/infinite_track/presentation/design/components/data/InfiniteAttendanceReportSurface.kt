package com.example.infinite_track.presentation.design.components.data

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.infinite_track.presentation.design.components.surface.InfiniteCard
import com.example.infinite_track.presentation.design.tokens.InfiniteColors
import com.example.infinite_track.presentation.design.tokens.InfiniteSemantic
import com.example.infinite_track.presentation.design.tokens.InfiniteSize
import com.example.infinite_track.presentation.design.tokens.InfiniteSurfaceVariant

@Composable
fun InfiniteGlassReportCard(
    modifier: Modifier = Modifier,
    extraContentPadding: Dp = 0.dp,
    content: @Composable () -> Unit
) {
    InfiniteCard(
        modifier = modifier.fillMaxWidth(),
        variant = InfiniteSurfaceVariant.Glass,
        semantic = InfiniteSemantic.Neutral,
        size = InfiniteSize.Large,
        showBorder = true,
        showShadow = true
    ) {
        Box(modifier = Modifier.padding(extraContentPadding)) {
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
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = InfiniteColors.AttendanceReportWarningSurface,
        border = BorderStroke(1.dp, InfiniteColors.AttendanceReportWarningBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                color = InfiniteColors.Text,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = message,
                color = InfiniteColors.AttendanceReportBodyText,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
