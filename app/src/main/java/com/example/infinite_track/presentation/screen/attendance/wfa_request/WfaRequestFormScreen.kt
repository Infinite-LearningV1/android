package com.example.infinite_track.presentation.screen.attendance.wfa_request

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.infinite_track.R
import com.example.infinite_track.presentation.design.components.button.InfiniteButton
import com.example.infinite_track.presentation.design.components.navigation.InfiniteTopBar
import com.example.infinite_track.presentation.design.tokens.InfiniteColors
import com.example.infinite_track.presentation.design.tokens.InfiniteSpacing
import com.example.infinite_track.presentation.screen.attendance.wfa_request.components.WfaDateField
import com.example.infinite_track.presentation.screen.attendance.wfa_request.components.WfaEmployeeCard
import com.example.infinite_track.presentation.screen.attendance.wfa_request.components.WfaLocationCard
import com.example.infinite_track.presentation.screen.attendance.wfa_request.components.WfaPolicyNotice
import com.example.infinite_track.presentation.screen.attendance.wfa_request.components.WfaReasonSelector
import com.example.infinite_track.presentation.screen.attendance.wfa_request.components.WfaTextInput

@Composable
fun WfaRequestFormScreen(
    uiState: WfaRequestUiState,
    onEvent: (WfaRequestEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxSize().background(InfiniteColors.AttendanceReportBackground)) {
        InfiniteTopBar(title = stringResource(R.string.wfa_request_form_title), onNavigationClick = onBack)
        when {
            uiState.phase == WfaRequestPhase.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.testTag("wfaFormLoading"))
            }
            uiState.failure != null && uiState.config == null -> ConfigFailure(uiState.failure, onEvent)
            else -> FormContent(uiState, onEvent)
        }
    }
}

@Composable
private fun FormContent(uiState: WfaRequestUiState, onEvent: (WfaRequestEvent) -> Unit) {
    val config = uiState.config ?: return
    val employee = uiState.employee ?: return
    val location = uiState.location ?: uiState.draft.location ?: return
    val selectedReason = config.reasons.firstOrNull { it.id == uiState.draft.reasonId }
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("wfaFormContent"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(InfiniteSpacing.Default.lg),
        verticalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.lg)
    ) {
        item { WfaEmployeeCard(employee) }
        item { WfaLocationCard(location, config.radiusMeters) }
        item { WfaPolicyNotice(config.radiusMeters) }
        item {
            WfaDateField(uiState.draft.scheduleDate, uiState.fieldErrors.scheduleDate) {
                onEvent(WfaRequestEvent.ScheduleDateChanged(it))
            }
        }
        item {
            WfaReasonSelector(config.reasons, uiState.draft.reasonId, uiState.fieldErrors.reason) {
                onEvent(WfaRequestEvent.ReasonSelected(it))
            }
        }
        if (selectedReason?.isOther == true) {
            item {
                WfaTextInput(
                    value = uiState.draft.otherReasonText,
                    label = stringResource(R.string.wfa_request_other_reason),
                    placeholder = stringResource(R.string.wfa_request_other_reason_placeholder),
                    tag = "wfaOtherReason",
                    error = uiState.fieldErrors.otherReason,
                    singleLine = false
                ) { onEvent(WfaRequestEvent.OtherReasonChanged(it)) }
            }
        }
        item {
            WfaTextInput(
                value = uiState.draft.notes,
                label = stringResource(R.string.wfa_request_notes),
                placeholder = stringResource(R.string.wfa_request_notes_placeholder),
                tag = "wfaNotes",
                error = uiState.fieldErrors.notes,
                singleLine = false
            ) { onEvent(WfaRequestEvent.NotesChanged(it)) }
        }
        item {
            InfiniteButton(
                text = stringResource(R.string.wfa_request_review_action),
                onClick = { onEvent(WfaRequestEvent.ReviewClicked) },
                modifier = Modifier.fillMaxWidth().testTag("wfaReviewAction"),
                fullWidth = true
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ConfigFailure(failure: com.example.infinite_track.domain.model.booking.WfaRequestFailure, onEvent: (WfaRequestEvent) -> Unit) {
    val copy = WfaRequestUiMapper.map(failure)
    Column(
        modifier = Modifier.fillMaxSize().padding(InfiniteSpacing.Default.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(copy.title)
        Spacer(Modifier.height(8.dp))
        Text(copy.message)
        Spacer(Modifier.height(16.dp))
        InfiniteButton(
            text = stringResource(R.string.wfa_request_retry),
            onClick = { onEvent(WfaRequestEvent.RetryConfigClicked) },
            modifier = Modifier.fillMaxWidth(),
            fullWidth = true
        )
    }
}
