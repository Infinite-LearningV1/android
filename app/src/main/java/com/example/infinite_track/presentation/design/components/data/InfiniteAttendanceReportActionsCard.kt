package com.example.infinite_track.presentation.design.components.data

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.infinite_track.presentation.core.body2
import com.example.infinite_track.presentation.core.headline4
import com.example.infinite_track.presentation.design.components.button.InfiniteButton
import com.example.infinite_track.presentation.design.components.button.InfiniteButtonState
import com.example.infinite_track.presentation.design.components.button.InfiniteButtonVariant
import com.example.infinite_track.presentation.design.tokens.InfiniteSize
import com.example.infinite_track.presentation.theme.Purple_300
import com.example.infinite_track.presentation.theme.Purple_500

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
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = title,
                style = headline4,
                color = Purple_500
            )
            subtitle?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = body2,
                    color = Purple_300
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                InfiniteButton(
                    text = "Export PDF",
                    onClick = onExportClick,
                    variant = InfiniteButtonVariant.Primary,
                    size = InfiniteSize.Small,
                    state = if (exportEnabled) InfiniteButtonState.Enabled else InfiniteButtonState.Disabled,
                    modifier = Modifier.weight(1f)
                )
                InfiniteButton(
                    text = "Share Report",
                    onClick = onShareClick,
                    variant = InfiniteButtonVariant.Glass,
                    size = InfiniteSize.Small,
                    state = if (shareEnabled) InfiniteButtonState.Enabled else InfiniteButtonState.Disabled,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
