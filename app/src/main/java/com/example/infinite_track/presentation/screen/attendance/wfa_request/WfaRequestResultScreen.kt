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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.infinite_track.R
import com.example.infinite_track.domain.model.booking.WfaRequestFailure
import com.example.infinite_track.domain.model.booking.WfaRequestStatus
import com.example.infinite_track.presentation.design.components.button.InfiniteButton
import com.example.infinite_track.presentation.design.components.button.InfiniteButtonVariant
import com.example.infinite_track.presentation.design.components.data.InfiniteInfoRow
import com.example.infinite_track.presentation.design.components.data.InfiniteSectionHeader
import com.example.infinite_track.presentation.design.components.state.InfiniteErrorState
import com.example.infinite_track.presentation.design.components.state.InfiniteLoadingState
import com.example.infinite_track.presentation.design.components.status.InfiniteStatusPill
import com.example.infinite_track.presentation.design.components.status.InfiniteStatusVariant
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
            InfiniteLoadingState(
                message = stringResource(R.string.wfa_request_submitting),
                modifier = Modifier.testTag("wfaResultSubmitting")
            )
            return@Column
        }

        val result = uiState.submitResult
        if (result != null) {
            InfiniteCard(
                modifier = Modifier.fillMaxWidth().testTag("wfaSuccessResult"),
                semantic = InfiniteSemantic.Success
            ) {
                InfiniteSectionHeader(
                    title = stringResource(R.string.wfa_request_success),
                    leadingIcon = Icons.Outlined.CheckCircle
                )
                InfiniteInfoRow(
                    label = null,
                    value = stringResource(R.string.wfa_request_booking_id, result.bookingId),
                    modifier = Modifier.testTag("wfaBookingId")
                )
                InfiniteInfoRow(
                    label = stringResource(R.string.wfa_request_status_label),
                    value = result.status.displayLabel(),
                    statusContent = {
                        InfiniteStatusPill(
                            label = result.status.displayLabel(),
                            variant = result.status.statusVariant(),
                            useSharedRequestPalette = true
                        )
                    }
                )
                InfiniteInfoRow(null, stringResource(R.string.wfa_request_result_date, result.scheduleDate))
                InfiniteInfoRow(null, stringResource(R.string.wfa_request_result_location, result.location.displayName))
                InfiniteInfoRow(null, stringResource(R.string.wfa_request_result_reason, result.reasonLabel))
                InfiniteInfoRow(null, stringResource(R.string.wfa_request_result_radius, result.radiusMeters))
            }
            InfiniteButton(
                text = stringResource(R.string.wfa_request_done),
                onClick = onDone,
                modifier = Modifier.fillMaxWidth().padding(top = InfiniteSpacing.Default.xl).testTag("wfaDoneAction"),
                fullWidth = true
            )
            InfiniteButton(
                text = stringResource(R.string.wfa_request_home),
                onClick = onHome,
                modifier = Modifier.fillMaxWidth().padding(top = InfiniteSpacing.Default.md).testTag("wfaHomeAction"),
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
    InfiniteErrorState(
        title = copy.title,
        message = copy.message,
        modifier = Modifier.fillMaxWidth().testTag("wfaFailureResult")
    )
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
        modifier = Modifier.fillMaxWidth().padding(top = InfiniteSpacing.Default.xl).testTag("wfaFailurePrimaryAction"),
        fullWidth = true
    )
    InfiniteButton(
        text = stringResource(R.string.wfa_request_done),
        onClick = onBack,
        modifier = Modifier.fillMaxWidth().padding(top = InfiniteSpacing.Default.md),
        variant = InfiniteButtonVariant.Ghost,
        fullWidth = true
    )
}

private fun WfaRequestStatus.displayLabel(): String =
    name.lowercase().replaceFirstChar { it.uppercase() }

private fun WfaRequestStatus.statusVariant(): InfiniteStatusVariant = when (this) {
    WfaRequestStatus.PENDING -> InfiniteStatusVariant.Pending
    WfaRequestStatus.APPROVED -> InfiniteStatusVariant.Approved
    WfaRequestStatus.REJECTED -> InfiniteStatusVariant.Rejected
    WfaRequestStatus.UNKNOWN -> InfiniteStatusVariant.Unknown
}
