package com.example.infinite_track.presentation.design.components.data

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.infinite_track.presentation.core.body2
import com.example.infinite_track.presentation.core.headline4
import com.example.infinite_track.presentation.design.tokens.InfiniteColors

@Composable
fun InfiniteSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leadingIcon: ImageVector? = null,
    trailingText: String? = null,
    trailingIcon: ImageVector? = null,
    onTrailingClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        leadingIcon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                tint = InfiniteColors.Primary,
                modifier = Modifier.size(22.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.Medium,
                color = InfiniteColors.Text,
                style = headline4
            )
            subtitle?.let {
                Text(
                    text = it,
                    color = InfiniteColors.AttendanceReportMutedText,
                    style = body2,
                    fontWeight = FontWeight.Normal
                )
            }
        }
        if (trailingText != null || trailingIcon != null) {
            Row(
                modifier = Modifier.clickable(enabled = onTrailingClick != null) { onTrailingClick?.invoke() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                trailingText?.let {
                    Text(
                        text = it,
                        color = InfiniteColors.Primary,
                        style = body2,
                        fontWeight = FontWeight.Medium
                    )
                }
                trailingIcon?.let {
                    Icon(
                        imageVector = it,
                        contentDescription = trailingText,
                        tint = InfiniteColors.Primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        } else {
            Spacer(modifier = Modifier.size(0.dp))
        }
    }
}
