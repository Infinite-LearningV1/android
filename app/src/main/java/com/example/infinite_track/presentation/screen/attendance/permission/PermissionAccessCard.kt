package com.example.infinite_track.presentation.screen.attendance.permission

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.GpsFixed
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.LocationSearching
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.example.infinite_track.presentation.core.body1
import com.example.infinite_track.presentation.core.body2
import com.example.infinite_track.presentation.core.body2_5
import com.example.infinite_track.presentation.design.tokens.InfiniteColors
import com.example.infinite_track.presentation.design.tokens.InfiniteSpacing
import com.example.infinite_track.presentation.design.tokens.infiniteSemanticColors

@Composable
internal fun PermissionAccessCard(
    item: PermissionItemUiModel,
    modifier: Modifier = Modifier,
    onActionClick: (() -> Unit)? = null
) {
    val stateColors = infiniteSemanticColors(item.semantic)
    val accessKey = item.access.name.lowercase()
    val actionAvailable = item.actionLabel != null && onActionClick != null
    val containerColor = permissionAccessContainerColor(
        isReady = item.isReady,
        stateContainer = stateColors.container
    )
    val borderColor = if (item.isReady) {
        stateColors.border.compositeOver(InfiniteColors.Surface)
    } else {
        InfiniteColors.Neutral.copy(alpha = 0.16f).compositeOver(InfiniteColors.Surface)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("permission-access-card-$accessKey")
            .semantics(mergeDescendants = true) {
                contentDescription = item.title
                stateDescription = item.stateDescription
            }
            .clickable(
                enabled = actionAvailable && !item.usesToggle,
                onClick = { onActionClick?.invoke() }
            ),
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = InfiniteSpacing.Default.md,
                vertical = InfiniteSpacing.Default.sm
            ),
            horizontalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = stateColors.container.compositeOver(InfiniteColors.Surface)
            ) {
                Box(
                    modifier = Modifier.size(42.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = permissionIcon(item.iconKey),
                        contentDescription = null,
                        tint = stateColors.accent,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = item.title,
                    style = body1,
                    color = InfiniteColors.Text
                )
                Text(
                    text = item.supportingText,
                    style = body2,
                    color = InfiniteColors.Text.copy(alpha = 0.66f),
                    maxLines = 2
                )
                if (item.usesToggle) {
                    Text(
                        text = item.statusLabel,
                        style = body2_5,
                        color = stateColors.accent
                    )
                }
            }

            if (item.usesToggle) {
                Switch(
                    checked = item.isReady,
                    onCheckedChange = if (actionAvailable) {
                        { onActionClick?.invoke() }
                    } else {
                        null
                    },
                    modifier = Modifier.testTag("permission-access-toggle-$accessKey"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = InfiniteColors.Surface,
                        checkedTrackColor = InfiniteColors.Success,
                        uncheckedThumbColor = InfiniteColors.Surface,
                        uncheckedTrackColor = InfiniteColors.Neutral.copy(alpha = 0.36f),
                        uncheckedBorderColor = InfiniteColors.Neutral.copy(alpha = 0.28f)
                    )
                )
            } else {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = stateColors.container.compositeOver(InfiniteColors.Surface)
                ) {
                    Text(
                        text = item.actionLabel ?: item.statusLabel,
                        style = body2_5,
                        color = stateColors.accent,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                    )
                }
            }
        }
    }
}

private fun permissionIcon(iconKey: PermissionIconKey): ImageVector = when (iconKey) {
    PermissionIconKey.LOCATION -> Icons.Outlined.LocationOn
    PermissionIconKey.CAMERA -> Icons.Outlined.CameraAlt
    PermissionIconKey.DEVICE_LOCATION -> Icons.Outlined.GpsFixed
    PermissionIconKey.NOTIFICATION -> Icons.Outlined.Notifications
    PermissionIconKey.BACKGROUND_LOCATION -> Icons.Outlined.LocationSearching
}

internal fun permissionAccessContainerColor(
    isReady: Boolean,
    stateContainer: Color,
    surface: Color = InfiniteColors.Surface
): Color = if (isReady) stateContainer.compositeOver(surface) else surface
