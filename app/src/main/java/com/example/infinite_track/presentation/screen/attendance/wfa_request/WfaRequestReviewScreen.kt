package com.example.infinite_track.presentation.screen.attendance.wfa_request

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.infinite_track.R
import com.example.infinite_track.presentation.design.components.button.InfiniteButton
import com.example.infinite_track.presentation.design.components.button.InfiniteButtonState
import com.example.infinite_track.presentation.design.components.button.InfiniteButtonVariant
import com.example.infinite_track.presentation.design.components.navigation.InfiniteTopBar
import com.example.infinite_track.presentation.design.components.surface.InfiniteCard
import com.example.infinite_track.presentation.design.tokens.InfiniteColors
import com.example.infinite_track.presentation.design.tokens.InfiniteSpacing
import com.example.infinite_track.presentation.screen.attendance.wfa_request.components.WfaPolicyNotice
import com.example.infinite_track.presentation.screen.attendance.wfa_request.components.WfaReviewRow

@Composable
fun WfaRequestReviewScreen(
    uiState: WfaRequestUiState,
    onEvent: (WfaRequestEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSubmitting = uiState.phase == WfaRequestPhase.Submitting
    val reason = uiState.config?.reasons?.firstOrNull { it.id == uiState.draft.reasonId }
    Column(modifier.fillMaxSize().background(InfiniteColors.AttendanceReportBackground)) {
        InfiniteTopBar(title = stringResource(R.string.wfa_request_review_title), onNavigationClick = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize().testTag("wfaReviewContent"),
            contentPadding = PaddingValues(InfiniteSpacing.Default.lg),
            verticalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.lg)
        ) {
            item {
                Text(stringResource(R.string.wfa_request_review_guidance), style = MaterialTheme.typography.bodyMedium)
            }
            item {
                InfiniteCard(modifier = Modifier.fillMaxWidth()) {
                    WfaReviewRow("Nama", uiState.employee?.fullName.orEmpty())
                    WfaReviewRow("Divisi", uiState.employee?.division.orEmpty())
                    WfaReviewRow("Tanggal", uiState.draft.scheduleDate?.toString().orEmpty())
                    WfaReviewRow("Lokasi", uiState.draft.location?.displayName ?: uiState.location?.displayName.orEmpty())
                    WfaReviewRow("Alamat", uiState.draft.location?.formattedAddress ?: uiState.location?.formattedAddress.orEmpty())
                    WfaReviewRow("Alasan", reason?.label.orEmpty())
                    if (reason?.isOther == true) WfaReviewRow("Detail alasan", uiState.draft.otherReasonText)
                    if (uiState.draft.notes.isNotBlank()) WfaReviewRow("Catatan", uiState.draft.notes)
                }
            }
            uiState.config?.let { config -> item { WfaPolicyNotice(config.radiusMeters) } }
            item {
                InfiniteButton(
                    text = stringResource(if (isSubmitting) R.string.wfa_request_submitting else R.string.wfa_request_submit),
                    onClick = { onEvent(WfaRequestEvent.SubmitConfirmed) },
                    modifier = Modifier.fillMaxWidth().testTag("wfaConfirmAction"),
                    state = if (isSubmitting) InfiniteButtonState.Loading else InfiniteButtonState.Enabled,
                    fullWidth = true
                )
            }
            item {
                InfiniteButton(
                    text = stringResource(R.string.wfa_request_edit),
                    onClick = { onEvent(WfaRequestEvent.EditClicked) },
                    modifier = Modifier.fillMaxWidth().testTag("wfaEditAction"),
                    variant = InfiniteButtonVariant.Outlined,
                    state = if (isSubmitting) InfiniteButtonState.Disabled else InfiniteButtonState.Enabled,
                    fullWidth = true
                )
            }
        }
    }
}
