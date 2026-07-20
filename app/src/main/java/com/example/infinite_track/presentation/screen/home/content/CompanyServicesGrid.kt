package com.example.infinite_track.presentation.screen.home.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.infinite_track.presentation.core.headline4
import com.example.infinite_track.presentation.design.components.data.InfiniteServiceItem
import com.example.infinite_track.presentation.design.tokens.InfiniteColors
import com.example.infinite_track.presentation.design.tokens.InfiniteIcons
import com.example.infinite_track.presentation.theme.Purple_500

@Composable
fun CompanyServicesGrid(
    onAttendanceClick: () -> Unit,
    onTimeOffClick: () -> Unit,
    onComingSoonClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val services = listOf(
        CompanyService(
            title = "Attendance",
            subtitle = "",
            icon = InfiniteIcons.Calendar,
            accentColor = InfiniteColors.Primary,
            onClick = onAttendanceClick
        ),
        CompanyService(
            title = "Leave Request",
            subtitle = "",
            icon = InfiniteIcons.Document,
            accentColor = InfiniteColors.Secondary,
            onClick = onTimeOffClick
        ),
        CompanyService(
            title = "Payslip",
            subtitle = "",
            icon = InfiniteIcons.Wallet,
            accentColor = InfiniteColors.Accent,
            onClick = onComingSoonClick
        ),
        CompanyService(
            title = "Documents",
            subtitle = "",
            icon = InfiniteIcons.Folder,
            accentColor = InfiniteColors.Primary,
            onClick = onComingSoonClick
        )
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Company Services",
            style = headline4,
            color = Purple_500
        )

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
                        accentColor = service.accentColor,
                        onClick = service.onClick,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

private data class CompanyService(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accentColor: Color,
    val onClick: () -> Unit
)
