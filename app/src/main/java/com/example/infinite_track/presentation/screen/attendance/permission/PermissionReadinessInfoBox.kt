package com.example.infinite_track.presentation.screen.attendance.permission

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.infinite_track.presentation.design.components.status.InfiniteInlineAlert

@Composable
internal fun PermissionReadinessInfoBox(
    guidance: PermissionGuidanceUiModel,
    onAction: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    if (guidance.actionLabel != null && onAction != null) {
        InfiniteInlineAlert(
            title = guidance.title,
            message = guidance.message,
            semantic = guidance.semantic,
            actionLabel = guidance.actionLabel,
            onAction = onAction,
            modifier = modifier.fillMaxWidth()
        )
    } else {
        InfiniteInlineAlert(
            title = guidance.title,
            message = guidance.message,
            semantic = guidance.semantic,
            modifier = modifier.fillMaxWidth()
        )
    }
}
