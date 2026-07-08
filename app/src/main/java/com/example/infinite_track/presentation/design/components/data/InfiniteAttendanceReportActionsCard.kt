package com.example.infinite_track.presentation.design.components.data

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.infinite_track.presentation.design.components.button.InfiniteButton
import com.example.infinite_track.presentation.design.components.button.InfiniteButtonState
import com.example.infinite_track.presentation.design.components.button.InfiniteButtonVariant

@Composable
fun InfiniteAttendanceReportActionsCard(
    selectedPeriod: String,
    modifier: Modifier = Modifier,
    exportEnabled: Boolean = false,
    shareEnabled: Boolean = false,
    onExportClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    title: String = "Export / Share Report",
    subtitle: String? = null,
    supportMessage: String = ""
) {
    InfiniteGlassReportCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            InfiniteSectionHeader(
                title = title,
                subtitle = subtitle
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                InfiniteButton(
                    text = "Export PDF",
                    onClick = onExportClick,
                    variant = InfiniteButtonVariant.Primary,
                    state = if (exportEnabled) InfiniteButtonState.Enabled else InfiniteButtonState.Disabled,
                    modifier = Modifier.weight(1f)
                )
                InfiniteButton(
                    text = "Share Report",
                    onClick = onShareClick,
                    variant = InfiniteButtonVariant.Glass,
                    state = if (shareEnabled) InfiniteButtonState.Enabled else InfiniteButtonState.Disabled,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
