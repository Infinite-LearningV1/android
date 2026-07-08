package com.example.infinite_track.presentation.design.components.data

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.infinite_track.presentation.design.components.status.InfiniteStatusPill
import com.example.infinite_track.presentation.design.components.status.InfiniteStatusVariant
import com.example.infinite_track.presentation.design.tokens.InfiniteColors
import com.example.infinite_track.presentation.design.tokens.InfiniteSize

@Composable
fun InfiniteAttendanceReportHeroCard(
    modifier: Modifier = Modifier,
    brandName: String = "Infinite Track",
    eyebrow: String = "Personal Report",
    title: String = "My Attendance Report",
    subtitle: String = "Personal attendance summary and history",
    badgeLabel: String = "Factual"
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 18.dp,
                shape = RoundedCornerShape(32.dp),
                ambientColor = InfiniteColors.Primary.copy(alpha = 0.20f),
                spotColor = InfiniteColors.Accent.copy(alpha = 0.16f)
            ),
        shape = RoundedCornerShape(32.dp),
        color = InfiniteColors.AttendanceReportGlassSurface,
        border = BorderStroke(1.dp, InfiniteColors.AttendanceReportGlassBorder)
    ) {
        Box(
            modifier = Modifier
                .background(InfiniteColors.AttendanceReportHeroGradient)
                .padding(22.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(InfiniteColors.Primary, CircleShape)
                                .border(1.dp, InfiniteColors.Surface.copy(alpha = 0.78f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "IT",
                                color = InfiniteColors.Surface,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column {
                            Text(
                                text = brandName,
                                color = InfiniteColors.AttendanceReportBodyText,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = eyebrow,
                                color = InfiniteColors.Text.copy(alpha = 0.48f),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                    InfiniteStatusPill(
                        label = badgeLabel,
                        variant = InfiniteStatusVariant.Neutral,
                        size = InfiniteSize.Small
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = title,
                        color = InfiniteColors.Text,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = subtitle,
                        color = InfiniteColors.Text.copy(alpha = 0.66f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
