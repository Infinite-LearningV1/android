package com.example.infinite_track.presentation.screen.attendance.wfa_request

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.infinite_track.R
import com.example.infinite_track.domain.model.booking.WfaRequestFieldError
import com.example.infinite_track.domain.model.booking.WfaRequestFailure
import com.example.infinite_track.presentation.components.button.DatePickerButton
import com.example.infinite_track.presentation.components.button.RadioButtonWithText
import com.example.infinite_track.presentation.components.textfield.InfiniteTrackTextArea
import com.example.infinite_track.presentation.design.components.button.InfiniteButton
import com.example.infinite_track.presentation.design.components.data.InfiniteInfoRow
import com.example.infinite_track.presentation.design.components.data.InfiniteInfoRowOrientation
import com.example.infinite_track.presentation.design.components.data.InfiniteSectionHeader
import com.example.infinite_track.presentation.design.components.navigation.InfiniteTopBar
import com.example.infinite_track.presentation.design.components.surface.InfiniteCard
import com.example.infinite_track.presentation.design.tokens.InfiniteColors
import com.example.infinite_track.presentation.design.tokens.InfiniteSemantic
import com.example.infinite_track.presentation.design.tokens.InfiniteSpacing

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
            uiState.failure != null && uiState.config == null -> ConfigFailure(uiState.failure, onEvent, onBack)
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
        contentPadding = PaddingValues(InfiniteSpacing.Default.lg),
        verticalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.lg)
    ) {
        item {
            InfiniteCard(modifier = Modifier.fillMaxWidth().testTag("wfaEmployeeCard")) {
                InfiniteSectionHeader(
                    title = stringResource(R.string.wfa_request_employee),
                    leadingIcon = Icons.Outlined.Person
                )
                InfiniteInfoRow("Nama", employee.fullName)
                InfiniteInfoRow("Divisi", employee.division)
            }
        }
        item {
            InfiniteCard(modifier = Modifier.fillMaxWidth().testTag("wfaLocationCard")) {
                InfiniteSectionHeader(
                    title = stringResource(R.string.wfa_request_location),
                    leadingIcon = Icons.Outlined.LocationOn
                )
                InfiniteInfoRow(
                    label = location.displayName,
                    value = location.formattedAddress,
                    orientation = InfiniteInfoRowOrientation.Vertical
                )
                InfiniteInfoRow(
                    label = stringResource(R.string.wfa_request_policy_title),
                    value = stringResource(R.string.wfa_request_radius_policy, config.radiusMeters),
                    semantic = InfiniteSemantic.Primary,
                    modifier = Modifier.testTag("wfaRadiusReadOnly")
                )
            }
        }
        item {
            InfiniteCard(
                modifier = Modifier.fillMaxWidth(),
                semantic = InfiniteSemantic.Info,
                showShadow = false
            ) {
                InfiniteSectionHeader(
                    title = stringResource(R.string.wfa_request_policy_title),
                    leadingIcon = Icons.Outlined.Info
                )
                Text(
                    stringResource(R.string.wfa_request_policy_body, config.radiusMeters),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        item {
            InfiniteSectionHeader(title = stringResource(R.string.wfa_request_date))
            DatePickerButton(
                selectedDate = uiState.draft.scheduleDate,
                onDateSelected = { onEvent(WfaRequestEvent.ScheduleDateChanged(it)) },
                placeholder = stringResource(R.string.wfa_request_date_placeholder),
                calendarContentDescription = stringResource(R.string.wfa_request_date_placeholder),
                modifier = Modifier.testTag("wfaScheduleDate")
            )
            FieldErrorText(uiState.fieldErrors.scheduleDate)
        }
        item {
            InfiniteSectionHeader(title = stringResource(R.string.wfa_request_reason))
            config.reasons.forEach { reason ->
                RadioButtonWithText(
                    text = reason.label,
                    selected = reason.id == uiState.draft.reasonId,
                    onClick = { onEvent(WfaRequestEvent.ReasonSelected(reason.id)) },
                    modifier = Modifier.testTag("wfaReason-${reason.id}")
                )
            }
            FieldErrorText(uiState.fieldErrors.reason)
        }
        if (selectedReason?.isOther == true) {
            item {
                InfiniteTrackTextArea(
                    value = uiState.draft.otherReasonText,
                    label = stringResource(R.string.wfa_request_other_reason),
                    placeholder = stringResource(R.string.wfa_request_other_reason_placeholder),
                    onValueChange = { onEvent(WfaRequestEvent.OtherReasonChanged(it)) },
                    modifier = Modifier.testTag("wfaOtherReason")
                )
                FieldErrorText(uiState.fieldErrors.otherReason)
            }
        }
        item {
            InfiniteTrackTextArea(
                value = uiState.draft.notes,
                label = stringResource(R.string.wfa_request_notes),
                placeholder = stringResource(R.string.wfa_request_notes_placeholder),
                onValueChange = { onEvent(WfaRequestEvent.NotesChanged(it)) },
                modifier = Modifier.testTag("wfaNotes")
            )
            FieldErrorText(uiState.fieldErrors.notes)
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
private fun FieldErrorText(error: WfaRequestFieldError?) {
    if (error == null) return
    Text(
        text = when (error) {
            WfaRequestFieldError.REQUIRED -> stringResource(R.string.wfa_request_error_required)
            WfaRequestFieldError.REASON_UNAVAILABLE -> stringResource(R.string.wfa_request_error_reason_unavailable)
            WfaRequestFieldError.OTHER_REASON_REQUIRED -> stringResource(R.string.wfa_request_error_other_required)
            WfaRequestFieldError.TOO_LONG -> stringResource(R.string.wfa_request_error_too_long)
            WfaRequestFieldError.INVALID_LOCATION -> stringResource(R.string.wfa_request_error_location)
        },
        style = MaterialTheme.typography.bodySmall,
        color = InfiniteColors.Error,
        modifier = Modifier.padding(top = InfiniteSpacing.Default.xs)
    )
}

@Composable
private fun ConfigFailure(
    failure: WfaRequestFailure,
    onEvent: (WfaRequestEvent) -> Unit,
    onBack: () -> Unit
) {
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
            text = if (copy.primaryAction == WfaRequestFailureAction.BACK) {
                stringResource(R.string.wfa_request_back)
            } else {
                stringResource(R.string.wfa_request_retry)
            },
            onClick = {
                if (copy.primaryAction == WfaRequestFailureAction.BACK) onBack()
                else onEvent(WfaRequestEvent.RetryConfigClicked)
            },
            modifier = Modifier.fillMaxWidth(),
            fullWidth = true
        )
    }
}
