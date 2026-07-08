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
import com.example.infinite_track.presentation.design.components.surface.InfiniteCard
import com.example.infinite_track.presentation.design.tokens.InfiniteColors
import com.example.infinite_track.presentation.design.tokens.InfiniteDensity
import com.example.infinite_track.presentation.design.tokens.InfiniteSemantic
import com.example.infinite_track.presentation.design.tokens.InfiniteSurfaceVariant

@Composable
internal fun PermissionProgressHeader(
    uiState: AttendancePermissionReadinessUiState,
    modifier: Modifier = Modifier
) {
    InfiniteCard(
        variant = InfiniteSurfaceVariant.StatusTint,
        semantic = if (uiState.canContinueToWorkMode) InfiniteSemantic.Success else InfiniteSemantic.Warning,
        density = InfiniteDensity.Comfortable,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = uiState.progressCopy,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = InfiniteColors.Text
                )
                Text(
                    text = if (uiState.canContinueToWorkMode) "Siap" else "Perlu setup",
                    style = MaterialTheme.typography.labelLarge,
                    color = InfiniteColors.Text.copy(alpha = 0.68f)
                )
            }
            LinearProgressIndicator(
                progress = { uiState.requiredReadyCount / uiState.requiredTotalCount.toFloat() },
                modifier = Modifier.fillMaxWidth(),
                color = if (uiState.canContinueToWorkMode) InfiniteColors.Success else InfiniteColors.Primary,
                trackColor = InfiniteColors.Primary.copy(alpha = 0.14f)
            )
        }
    }
}
