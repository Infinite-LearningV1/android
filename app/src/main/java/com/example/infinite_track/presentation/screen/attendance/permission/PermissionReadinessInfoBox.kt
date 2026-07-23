package com.example.infinite_track.presentation.screen.attendance.permission

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.infinite_track.presentation.core.body2
import com.example.infinite_track.presentation.core.headline4
import com.example.infinite_track.presentation.design.components.button.InfiniteButton
import com.example.infinite_track.presentation.design.components.button.InfiniteButtonVariant
import com.example.infinite_track.presentation.design.tokens.InfiniteSize
import com.example.infinite_track.presentation.design.tokens.InfiniteSpacing
import com.example.infinite_track.presentation.design.tokens.infiniteSemanticColors

@Composable
internal fun PermissionReadinessInfoBox(
    guidance: PermissionGuidanceUiModel,
    onAction: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val colors = infiniteSemanticColors(guidance.semantic)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = colors.container,
        border = BorderStroke(1.dp, colors.border)
    ) {
        Column(
            modifier = Modifier.padding(InfiniteSpacing.Default.lg),
            verticalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.sm)
        ) {
            Text(guidance.title, style = headline4, color = colors.content)
            Text(
                guidance.message,
                style = body2,
                color = colors.content.copy(alpha = 0.76f)
            )
            if (guidance.actionLabel != null && onAction != null) {
                InfiniteButton(
                    text = guidance.actionLabel,
                    onClick = onAction,
                    variant = InfiniteButtonVariant.Ghost,
                    size = InfiniteSize.Medium,
                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                )
            }
        }
    }
}
