package com.example.infinite_track.presentation.screen.attendance.permission

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.infinite_track.presentation.design.tokens.InfiniteColors

@Composable
internal fun PermissionProgressHeader(
    uiState: AttendancePermissionReadinessUiState,
    modifier: Modifier = Modifier
) {
    val progressCopy = "${uiState.requiredReadyCount}/${uiState.requiredTotalCount} akses wajib siap"
    val progress = if (uiState.requiredTotalCount == 0) {
        0f
    } else {
        uiState.requiredReadyCount / uiState.requiredTotalCount.toFloat()
    }
    PermissionGlassCard(
        modifier = modifier.fillMaxWidth(),
        shadowElevation = if (uiState.canContinue) 3.dp else 0.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = progressCopy,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = InfiniteColors.Text.copy(alpha = 0.86f)
                )
                Text(
                    text = if (uiState.canContinue) "Siap" else "Perlu setup",
                    style = MaterialTheme.typography.labelLarge,
                    color = InfiniteColors.Text.copy(alpha = 0.68f)
                )
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = if (uiState.canContinue) InfiniteColors.Success else InfiniteColors.Primary,
                trackColor = InfiniteColors.Primary.copy(alpha = 0.14f)
            )
        }
    }
}
