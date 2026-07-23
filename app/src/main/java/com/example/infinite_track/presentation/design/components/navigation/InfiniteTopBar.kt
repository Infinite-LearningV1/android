package com.example.infinite_track.presentation.design.components.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.infinite_track.presentation.core.headline4
import com.example.infinite_track.presentation.design.tokens.InfiniteColors
import com.example.infinite_track.presentation.theme.Purple_500

/**
 * Global glass navigation app bar used across secondary screens.
 * Matches the Attendance top bar reference:
 * circular back control, centered title, optional circular trailing action.
 */
@Composable
fun InfiniteTopBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: ImageVector = Icons.AutoMirrored.Outlined.ArrowBack,
    navigationContentDescription: String? = "Back",
    onNavigationClick: (() -> Unit)? = null,
    actionIcon: ImageVector? = null,
    actionContentDescription: String? = null,
    onActionClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(999.dp),
                ambientColor = InfiniteColors.Primary.copy(alpha = 0.10f),
                spotColor = InfiniteColors.Accent.copy(alpha = 0.08f)
            )
            .clip(RoundedCornerShape(999.dp))
            .background(InfiniteColors.AttendanceReportGlassSurface)
            .border(
                BorderStroke(1.dp, InfiniteColors.AttendanceReportGlassBorder),
                RoundedCornerShape(999.dp)
            )
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        if (onNavigationClick != null) {
            InfiniteTopBarActionButton(
                icon = navigationIcon,
                contentDescription = navigationContentDescription,
                onClick = onNavigationClick,
                modifier = Modifier.align(Alignment.CenterStart)
            )
        }

        Text(
            text = title,
            style = headline4,
            color = Purple_500,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 52.dp)
        )

        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (actionIcon != null && onActionClick != null) {
                InfiniteTopBarActionButton(
                    icon = actionIcon,
                    contentDescription = actionContentDescription,
                    onClick = onActionClick
                )
            }
            actions()
        }
    }
}

@Composable
fun InfiniteTopBarActionButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(42.dp)
            .shadow(
                elevation = 4.dp,
                shape = CircleShape,
                ambientColor = InfiniteColors.Primary.copy(alpha = 0.12f),
                spotColor = InfiniteColors.Accent.copy(alpha = 0.10f)
            )
            .clip(CircleShape)
            .background(InfiniteColors.AttendanceReportGlassSurface)
            .border(
                BorderStroke(1.dp, InfiniteColors.AttendanceReportGlassBorder),
                CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Purple_500,
            modifier = Modifier.size(20.dp)
        )
    }
}
