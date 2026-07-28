package com.example.infinite_track.presentation.screen.attendance.wfa_request

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.infinite_track.R
import com.example.infinite_track.domain.model.booking.WfaRequestFieldError
import com.example.infinite_track.domain.model.booking.WfaRequestFailure
import com.example.infinite_track.domain.model.location.GeoCoordinate
import com.example.infinite_track.presentation.components.button.DatePickerButton
import com.example.infinite_track.presentation.components.map.ReadOnlyLocationMap
import com.example.infinite_track.presentation.components.textfield.InfiniteTrackDropDown
import com.example.infinite_track.presentation.components.textfield.InfiniteTrackDropDownOption
import com.example.infinite_track.presentation.components.textfield.InfiniteTrackTextArea
import com.example.infinite_track.presentation.design.components.button.InfiniteButton
import com.example.infinite_track.presentation.design.components.data.InfiniteChecklistCard
import com.example.infinite_track.presentation.design.components.data.InfiniteChecklistItem
import com.example.infinite_track.presentation.design.components.data.InfiniteInfoRow
import com.example.infinite_track.presentation.design.components.data.InfiniteInfoRowOrientation
import com.example.infinite_track.presentation.design.components.data.InfiniteSectionHeader
import com.example.infinite_track.presentation.design.components.input.InfiniteSupportingText
import com.example.infinite_track.presentation.design.components.navigation.InfiniteTopBar
import com.example.infinite_track.presentation.design.components.state.InfiniteErrorState
import com.example.infinite_track.presentation.design.components.state.InfiniteLoadingState
import com.example.infinite_track.presentation.design.components.status.InfiniteStatusPill
import com.example.infinite_track.presentation.design.components.status.InfiniteStatusVariant
import com.example.infinite_track.presentation.design.components.surface.InfiniteCard
import com.example.infinite_track.presentation.design.tokens.InfiniteSemantic
import com.example.infinite_track.presentation.design.tokens.InfiniteSize
import com.example.infinite_track.presentation.design.tokens.InfiniteSpacing

@Composable
fun WfaRequestFormScreen(
    uiState: WfaRequestUiState,
    onEvent: (WfaRequestEvent) -> Unit,
    onBack: () -> Unit,
    onClose: () -> Unit = onBack,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxSize()) {
        InfiniteTopBar(
            title = stringResource(R.string.wfa_request_form_title),
            navigationContentDescription = stringResource(R.string.wfa_request_back),
            onNavigationClick = onBack,
            actionIcon = Icons.Default.Close,
            actionContentDescription = stringResource(R.string.wfa_request_close),
            onActionClick = onClose
        )
        when {
            uiState.phase == WfaRequestPhase.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                InfiniteLoadingState(
                    message = stringResource(R.string.wfa_request_loading),
                    modifier = Modifier.padding(InfiniteSpacing.Default.xl).testTag("wfaFormLoading")
                )
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
    val locationCoordinate = if (location.hasValidCoordinates) {
        GeoCoordinate(location.latitude, location.longitude)
    } else {
        null
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("wfaFormContent"),
        contentPadding = PaddingValues(InfiniteSpacing.Default.lg),
        verticalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.lg)
    ) {
        item {
            InfiniteCard(modifier = Modifier.fillMaxWidth().testTag("wfaLocationCard")) {
                Column(verticalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.lg)) {
                    InfiniteSectionHeader(
                        title = stringResource(R.string.wfa_request_location),
                        leadingIcon = Icons.Outlined.LocationOn
                    )
                    InfiniteInfoRow(
                        label = location.displayName,
                        value = location.formattedAddress,
                        orientation = InfiniteInfoRowOrientation.Vertical
                    )
                    ReadOnlyLocationMap(
                        coordinate = locationCoordinate,
                        radiusMeters = config.radiusMeters,
                        title = location.displayName,
                        address = location.formattedAddress,
                        modifier = Modifier.fillMaxWidth().testTag("wfaLocationMap"),
                        contentDescription = stringResource(
                            R.string.wfa_request_location_map_content_description,
                            location.displayName,
                            config.radiusMeters
                        )
                    )
                    InfiniteInfoRow(
                        label = stringResource(R.string.wfa_request_policy_title),
                        value = stringResource(R.string.wfa_request_radius_policy, config.radiusMeters),
                        semantic = InfiniteSemantic.Primary,
                        modifier = Modifier.testTag("wfaRadiusReadOnly")
                    )
                    InfiniteInfoRow(
                        label = stringResource(R.string.wfa_request_location_status),
                        value = stringResource(R.string.wfa_request_location_valid),
                        semantic = InfiniteSemantic.Success,
                        statusContent = {
                            InfiniteStatusPill(
                                label = stringResource(R.string.wfa_request_location_valid),
                                variant = InfiniteStatusVariant.Active,
                                size = InfiniteSize.Small,
                                leadingIcon = Icons.Outlined.CheckCircle
                            )
                        }
                    )
                }
            }
        }
        item {
            InfiniteCard(modifier = Modifier.fillMaxWidth().testTag("wfaEmployeeCard")) {
                Column(verticalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.md)) {
                    InfiniteSectionHeader(
                        title = stringResource(R.string.wfa_request_employee),
                        leadingIcon = Icons.Outlined.Person
                    )
                    InfiniteInfoRow(
                        label = stringResource(R.string.wfa_request_employee_name),
                        value = employee.fullName
                    )
                    InfiniteInfoRow(
                        label = stringResource(R.string.wfa_request_employee_division),
                        value = employee.division
                    )
                    InfiniteSupportingText(
                        text = stringResource(R.string.wfa_request_employee_profile_source)
                    )
                }
            }
        }
        item {
            InfiniteCard(modifier = Modifier.fillMaxWidth().testTag("wfaRequestDetails")) {
                Column(verticalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.lg)) {
                    InfiniteSectionHeader(
                        title = stringResource(R.string.wfa_request_details),
                        leadingIcon = Icons.Outlined.Info
                    )
                    Column {
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
                    Column {
                        InfiniteTrackDropDown(
                            selectedKey = uiState.draft.reasonId,
                            onSelected = { reasonId ->
                                onEvent(WfaRequestEvent.ReasonSelected(reasonId))
                            },
                            options = config.reasons.map { reason ->
                                InfiniteTrackDropDownOption(
                                    key = reason.id,
                                    label = reason.label,
                                    testTag = "wfaReason-${reason.id}"
                                )
                            },
                            label = stringResource(R.string.wfa_request_reason),
                            placeholder = stringResource(R.string.wfa_request_reason_placeholder),
                            modifier = Modifier.testTag("wfaReasonDropdown")
                        )
                        FieldErrorText(uiState.fieldErrors.reason)
                    }
                    if (selectedReason?.isOther == true) {
                        Column {
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
                    Column {
                        InfiniteTrackTextArea(
                            value = uiState.draft.notes,
                            label = stringResource(R.string.wfa_request_notes),
                            placeholder = stringResource(R.string.wfa_request_notes_placeholder),
                            onValueChange = { onEvent(WfaRequestEvent.NotesChanged(it)) },
                            maxLength = 250,
                            showCharacterCount = true,
                            modifier = Modifier.testTag("wfaNotes")
                        )
                        FieldErrorText(uiState.fieldErrors.notes)
                    }
                }
            }
        }
        item {
            InfiniteChecklistCard(
                title = stringResource(R.string.wfa_request_eligibility_title),
                items = listOf(
                    InfiniteChecklistItem(
                        text = stringResource(R.string.wfa_request_eligibility_valid_location)
                    ),
                    InfiniteChecklistItem(
                        text = stringResource(R.string.wfa_request_eligibility_server_radius)
                    ),
                    InfiniteChecklistItem(
                        text = stringResource(R.string.wfa_request_eligibility_review_before_submission)
                    )
                ),
                modifier = Modifier.testTag("wfaEligibilityCard")
            )
        }
        item {
            InfiniteButton(
                text = stringResource(R.string.wfa_request_continue),
                onClick = { onEvent(WfaRequestEvent.ReviewClicked) },
                modifier = Modifier.fillMaxWidth().testTag("wfaReviewAction"),
                fullWidth = true
            )
        }
    }
}

@Composable
private fun FieldErrorText(error: WfaRequestFieldError?) {
    if (error == null) return
    InfiniteSupportingText(
        text = when (error) {
            WfaRequestFieldError.REQUIRED -> stringResource(R.string.wfa_request_error_required)
            WfaRequestFieldError.REASON_UNAVAILABLE -> stringResource(R.string.wfa_request_error_reason_unavailable)
            WfaRequestFieldError.OTHER_REASON_REQUIRED -> stringResource(R.string.wfa_request_error_other_required)
            WfaRequestFieldError.TOO_LONG -> stringResource(R.string.wfa_request_error_too_long)
            WfaRequestFieldError.INVALID_LOCATION -> stringResource(R.string.wfa_request_error_location)
        },
        semantic = InfiniteSemantic.Error,
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
    Box(
        modifier = Modifier.fillMaxSize().padding(InfiniteSpacing.Default.xl),
        contentAlignment = Alignment.Center
    ) {
        InfiniteErrorState(
            title = copy.title,
            message = copy.message,
            actionLabel = if (copy.primaryAction == WfaRequestFailureAction.BACK) {
                stringResource(R.string.wfa_request_back)
            } else {
                stringResource(R.string.wfa_request_retry)
            },
            onAction = {
                if (copy.primaryAction == WfaRequestFailureAction.BACK) onBack()
                else onEvent(WfaRequestEvent.RetryConfigClicked)
            }
        )
    }
}
