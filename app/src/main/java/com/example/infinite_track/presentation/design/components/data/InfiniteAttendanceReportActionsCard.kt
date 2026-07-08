package com.example.infinite_track.presentation.design.components.data

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.infinite_track.domain.model.attendance.AttendanceReportExportContract
import com.example.infinite_track.presentation.design.components.button.InfiniteButton
import com.example.infinite_track.presentation.design.components.button.InfiniteButtonState
import com.example.infinite_track.presentation.design.components.button.InfiniteButtonVariant
import com.example.infinite_track.presentation.design.tokens.InfiniteColors

@Composable
fun InfiniteAttendanceReportActionsCard(
    selectedPeriod: String,
    modifier: Modifier = Modifier,
    exportEnabled: Boolean = false,
    shareEnabled: Boolean = false,
    onExportClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    title: String = "Export / Share Report",
    subtitle: String? = "PDF contract is shown honestly until runtime wiring is verified",
    supportMessage: String = "Expected export endpoint: ${AttendanceReportExportContract.exportPdfPath(selectedPeriod)}. Preview endpoint: ${AttendanceReportExportContract.PERSONAL_PDF_PREVIEW_PATH}. Wiring and backend runtime readiness need verification before enabling these actions."
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
            Text(
                text = supportMessage,
                color = InfiniteColors.AttendanceReportMutedText,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
