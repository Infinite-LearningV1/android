package com.example.infinite_track.presentation.screen.home.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.infinite_track.presentation.design.components.data.InfiniteSectionHeader
import com.example.infinite_track.presentation.design.components.data.InfiniteServiceItem
import com.example.infinite_track.presentation.design.components.surface.InfiniteCard
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
                            InfiniteServiceItem(
                                title = service.title,
                                subtitle = service.subtitle,
                                icon = service.icon,
                                onClick = service.onClick,
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

private data class CompanyService(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)
