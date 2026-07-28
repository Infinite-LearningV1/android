package com.example.infinite_track.presentation.screen.attendance.wfa_request

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.infinite_track.R
import com.example.infinite_track.domain.model.booking.WfaRequestFailure
import com.example.infinite_track.presentation.design.components.button.InfiniteButton
import com.example.infinite_track.presentation.design.components.button.InfiniteButtonVariant
import com.example.infinite_track.presentation.design.components.surface.InfiniteCard
import com.example.infinite_track.presentation.design.tokens.InfiniteColors
import com.example.infinite_track.presentation.design.tokens.InfiniteSemantic
import com.example.infinite_track.presentation.design.tokens.InfiniteSpacing

@Composable
fun WfaRequestResultScreen(
    uiState: WfaRequestUiState,
    onEvent: (WfaRequestEvent) -> Unit,
    onDone: () -> Unit,
    onHome: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().background(InfiniteColors.AttendanceReportBackground)
            .verticalScroll(rememberScrollState())
            .padding(InfiniteSpacing.Default.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (uiState.phase == WfaRequestPhase.Submitting) {
            CircularProgressIndicator(modifier = Modifier.testTag("wfaResultSubmitting"))
            Text(
                stringResource(R.string.wfa_request_submitting),
                modifier = Modifier.padding(top = InfiniteSpacing.Default.lg)
            )
            return@Column
        }

        val result = uiState.submitResult
        if (result != null) {
            InfiniteCard(
                modifier = Modifier.fillMaxWidth().testTag("wfaSuccessResult"),
                semantic = InfiniteSemantic.Success
            ) {
                Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = InfiniteColors.Success)
                Text(stringResource(R.string.wfa_request_success), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.wfa_request_booking_id, result.bookingId), modifier = Modifier.testTag("wfaBookingId"))
                Text(stringResource(R.string.wfa_request_status, result.status.name.lowercase().replaceFirstChar { it.uppercase() }))
                Text(stringResource(R.string.wfa_request_result_date, result.scheduleDate))
                Text(stringResource(R.string.wfa_request_result_location, result.location.displayName))
                Text(stringResource(R.string.wfa_request_result_reason, result.reasonLabel))
                Text(stringResource(R.string.wfa_request_result_radius, result.radiusMeters))
            }
            InfiniteButton(
                text = stringResource(R.string.wfa_request_done),
                onClick = onDone,
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp).testTag("wfaDoneAction"),
                fullWidth = true
            )
            InfiniteButton(
                text = stringResource(R.string.wfa_request_home),
                onClick = onHome,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp).testTag("wfaHomeAction"),
                variant = InfiniteButtonVariant.Outlined,
                fullWidth = true
            )
        } else {
            FailureResult(uiState.failure ?: WfaRequestFailure.Unknown, onEvent, onBack)
        }
    }
}

@Composable
private fun FailureResult(
    failure: WfaRequestFailure,
    onEvent: (WfaRequestEvent) -> Unit,
    onBack: () -> Unit
) {
    val copy = WfaRequestUiMapper.map(failure)
    InfiniteCard(
        modifier = Modifier.fillMaxWidth().testTag("wfaFailureResult"),
        semantic = InfiniteSemantic.Error
    ) {
        Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = InfiniteColors.Error)
        Text(copy.title, style = MaterialTheme.typography.headlineSmall)
        Text(copy.message)
    }
    InfiniteButton(
        text = when (copy.primaryAction) {
            WfaRequestFailureAction.EDIT -> stringResource(R.string.wfa_request_edit)
            WfaRequestFailureAction.RETRY -> stringResource(R.string.wfa_request_retry)
            WfaRequestFailureAction.BACK -> stringResource(R.string.wfa_request_back)
        },
        onClick = {
            when (copy.primaryAction) {
                WfaRequestFailureAction.EDIT -> onEvent(WfaRequestEvent.EditClicked)
                WfaRequestFailureAction.RETRY -> onEvent(WfaRequestEvent.RetrySubmitClicked)
                WfaRequestFailureAction.BACK -> onBack()
            }
        },
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp).testTag("wfaFailurePrimaryAction"),
        fullWidth = true
    )
    InfiniteButton(
        text = stringResource(R.string.wfa_request_done),
        onClick = onBack,
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        variant = InfiniteButtonVariant.Ghost,
        fullWidth = true
    )
}
