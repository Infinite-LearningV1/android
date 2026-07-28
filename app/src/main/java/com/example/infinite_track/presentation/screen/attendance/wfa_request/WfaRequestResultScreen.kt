package com.example.infinite_track.presentation.screen.attendance.wfa_request

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Info
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
import com.example.infinite_track.presentation.design.components.navigation.InfiniteTopBar
import com.example.infinite_track.presentation.design.components.state.InfiniteErrorState
import com.example.infinite_track.presentation.design.components.state.InfiniteLoadingState
import com.example.infinite_track.presentation.design.components.state.InfiniteResultHero
import com.example.infinite_track.presentation.design.components.status.InfiniteStatusPill
import com.example.infinite_track.presentation.design.components.status.InfiniteStatusVariant
import com.example.infinite_track.presentation.design.components.surface.InfiniteCard
import com.example.infinite_track.presentation.design.tokens.InfiniteSemantic
import com.example.infinite_track.presentation.design.tokens.InfiniteSpacing

@Composable
fun WfaRequestResultScreen(
    uiState: WfaRequestUiState,
    onEvent: (WfaRequestEvent) -> Unit,
    onDone: () -> Unit,
    onHome: () -> Unit,
    onBack: () -> Unit,
    onClose: () -> Unit = onBack,
    modifier: Modifier = Modifier
) {
    val isSubmitting = uiState.phase == WfaRequestPhase.Submitting

    Column(modifier.fillMaxSize()) {
        InfiniteTopBar(
            title = stringResource(R.string.wfa_request_result_title),
            navigationContentDescription = stringResource(R.string.wfa_request_back),
            onNavigationClick = { if (!isSubmitting) onBack() },
            actionIcon = Icons.Default.Close,
            actionContentDescription = stringResource(R.string.wfa_request_close),
            onActionClick = { if (!isSubmitting) onClose() }
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize().testTag("wfaResultContent"),
            contentPadding = PaddingValues(InfiniteSpacing.Default.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.lg)
        ) {
            if (isSubmitting) {
                item {
                    InfiniteLoadingState(
                        message = stringResource(R.string.wfa_request_submitting),
                        modifier = Modifier.testTag("wfaResultSubmitting")
                    )
                }
                return@LazyColumn
            }

            val result = uiState.submitResult
            if (result != null) {
                item {
                    InfiniteResultHero(
                        title = stringResource(R.string.wfa_request_success),
                        message = stringResource(R.string.wfa_request_success_message),
                        semantic = InfiniteSemantic.Success,
                        modifier = Modifier.testTag("wfaSuccessHero")
                    )
                }
                item {
                    InfiniteCard(
                        modifier = Modifier.fillMaxWidth().testTag("wfaResultDetailsCard")
                    ) {
                        Column(
                            modifier = Modifier.testTag("wfaSuccessResult"),
                            verticalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.md)
                        ) {
                            InfiniteSectionHeader(
                                title = stringResource(R.string.wfa_request_result_details_title),
                                leadingIcon = Icons.Outlined.Info
                            )
                            InfiniteInfoRow(
                                label = null,
                                value = stringResource(
                                    R.string.wfa_request_booking_id,
                                    result.bookingId
                                ),
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
                            InfiniteInfoRow(
                                null,
                                stringResource(
                                    R.string.wfa_request_result_date,
                                    result.scheduleDate
                                )
                            )
                            InfiniteInfoRow(
                                null,
                                stringResource(
                                    R.string.wfa_request_result_reason,
                                    result.reasonLabel
                                )
                            )
                            InfiniteInfoRow(
                                null,
                                stringResource(
                                    R.string.wfa_request_result_radius,
                                    result.radiusMeters
                                )
                            )
                        }
                    }
                }
                item {
                    InfiniteButton(
                        text = stringResource(R.string.wfa_request_view_status),
                        onClick = onDone,
                        modifier = Modifier.fillMaxWidth().testTag("wfaDoneAction"),
                        variant = InfiniteButtonVariant.Outlined,
                        fullWidth = true
                    )
                }
                item {
                    InfiniteButton(
                        text = stringResource(R.string.wfa_request_home),
                        onClick = onHome,
                        modifier = Modifier.fillMaxWidth().testTag("wfaHomeAction"),
                        fullWidth = true
                    )
                }
            } else {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        FailureResult(
                            uiState.failure ?: WfaRequestFailure.Unknown,
                            onEvent,
                            onBack
                        )
                    }
                }
            }
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
