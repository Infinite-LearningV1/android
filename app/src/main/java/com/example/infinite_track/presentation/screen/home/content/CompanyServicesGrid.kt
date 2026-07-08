package com.example.infinite_track.presentation.screen.home.content

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.infinite_track.presentation.design.components.data.InfiniteSectionHeader
import com.example.infinite_track.presentation.design.components.surface.InfiniteCard
import com.example.infinite_track.presentation.design.tokens.InfiniteColors
import com.example.infinite_track.presentation.design.tokens.InfiniteDensity
import com.example.infinite_track.presentation.design.tokens.InfiniteIcons
import com.example.infinite_track.presentation.design.tokens.InfiniteSemantic
import com.example.infinite_track.presentation.design.tokens.InfiniteSize
import com.example.infinite_track.presentation.design.tokens.InfiniteSurfaceVariant

@Composable
fun CompanyServicesGrid(
    onAttendanceClick: () -> Unit,
    onTimeOffClick: () -> Unit,
    onAttendanceHistoryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val services = listOf(
        CompanyService(
            title = "Attendance",
            subtitle = "Check-in / check-out",
            icon = InfiniteIcons.Success,
            onClick = onAttendanceClick
        ),
        CompanyService(
            title = "Time Off",
            subtitle = "Ajukan cuti",
            icon = InfiniteIcons.Calendar,
            onClick = onTimeOffClick
        ),
        CompanyService(
            title = "Attendance History",
            subtitle = "Lihat riwayat",
            icon = InfiniteIcons.Time,
            onClick = onAttendanceHistoryClick
        )
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        InfiniteSectionHeader(
            title = "Company Services",
            subtitle = "Akses layanan utama perusahaan",
            leadingIcon = InfiniteIcons.More
        )

        InfiniteCard(
            modifier = Modifier.fillMaxWidth(),
            variant = InfiniteSurfaceVariant.Glass,
            semantic = InfiniteSemantic.Primary,
            size = InfiniteSize.Medium,
            density = InfiniteDensity.Comfortable
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                services.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowItems.forEach { service ->
                            CompanyServiceItem(
                                service = service,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (rowItems.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompanyServiceItem(
    service: CompanyService,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = service.onClick)
            .background(InfiniteColors.Surface.copy(alpha = 0.72f))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = service.icon,
            contentDescription = null,
            tint = InfiniteColors.Primary,
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(InfiniteColors.Primary.copy(alpha = 0.10f))
                .padding(8.dp)
                .size(22.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = service.title,
                color = InfiniteColors.Text,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = service.subtitle,
                color = InfiniteColors.Text.copy(alpha = 0.60f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private data class CompanyService(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)
