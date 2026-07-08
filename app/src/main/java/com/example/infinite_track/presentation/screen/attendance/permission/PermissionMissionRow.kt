package com.example.infinite_track.presentation.screen.attendance.permission

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.infinite_track.presentation.design.components.button.InfiniteButton
import com.example.infinite_track.presentation.design.components.button.InfiniteButtonVariant
import com.example.infinite_track.presentation.design.components.data.InfiniteInfoRow
import com.example.infinite_track.presentation.design.components.data.InfiniteInfoRowOrientation
import com.example.infinite_track.presentation.design.components.status.InfiniteStatusPill
import com.example.infinite_track.presentation.design.components.status.InfiniteStatusVariant
import com.example.infinite_track.presentation.design.tokens.InfiniteColors
import com.example.infinite_track.presentation.design.tokens.InfiniteSemantic
import com.example.infinite_track.presentation.design.tokens.InfiniteSize

@Composable
internal fun PermissionMissionRow(
    title: String,
    description: String,
    icon: ImageVector,
    isGranted: Boolean,
    isRequired: Boolean,
    actionLabel: String?,
    modifier: Modifier = Modifier,
    onActionClick: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0x33FFFFFF),
        border = BorderStroke(
            width = 1.dp,
            color = if (isGranted) {
                InfiniteColors.Success.copy(alpha = 0.38f)
            } else {
                Color.White.copy(alpha = 0.72f)
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                InfiniteInfoRow(
                    label = title,
                    value = description,
                    icon = icon,
                    semantic = if (isGranted) InfiniteSemantic.Success else InfiniteSemantic.Primary,
                    orientation = InfiniteInfoRowOrientation.Vertical,
                    statusContent = {
                        InfiniteStatusPill(
                            label = if (isRequired) "Wajib" else "Opsional",
                            variant = if (isRequired) InfiniteStatusVariant.Pending else InfiniteStatusVariant.Neutral,
                            size = InfiniteSize.Small
                        )
                    }
                )
                if (!isGranted && actionLabel != null && onActionClick != null) {
                    InfiniteButton(
                        text = actionLabel,
                        onClick = onActionClick,
                        variant = InfiniteButtonVariant.Ghost,
                        size = InfiniteSize.Small
                    )
                }
            }
            Icon(
                imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = if (isGranted) "Siap" else "Belum siap",
                tint = if (isGranted) InfiniteColors.Success else InfiniteColors.Neutral.copy(alpha = 0.56f)
            )
        }
    }
}
