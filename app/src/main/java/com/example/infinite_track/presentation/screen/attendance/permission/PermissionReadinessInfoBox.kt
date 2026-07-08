package com.example.infinite_track.presentation.screen.attendance.permission

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.infinite_track.presentation.design.components.status.InfiniteInlineAlert
import com.example.infinite_track.presentation.design.tokens.InfiniteSemantic

@Composable
internal fun PermissionReadinessInfoBox(
    uiState: AttendancePermissionReadinessUiState,
    modifier: Modifier = Modifier
) {
    InfiniteInlineAlert(
        title = if (uiState.canContinueToWorkMode) {
            "Akses wajib sudah siap"
        } else {
            "Selesaikan akses wajib"
        },
        message = uiState.warningMessage
            ?: "Notifikasi dan lokasi latar belakang membantu reminder, tetapi tidak memblokir absensi manual.",
        semantic = if (uiState.canContinueToWorkMode) InfiniteSemantic.Info else InfiniteSemantic.Warning,
        modifier = modifier.fillMaxWidth()
    )
}
