package com.example.infinite_track.presentation.screen.attendance.wfa_request

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.infinite_track.R
import com.example.infinite_track.domain.model.location.GeoCoordinate
import com.example.infinite_track.presentation.components.map.ReadOnlyLocationMap
import com.example.infinite_track.presentation.design.components.button.InfiniteButton
import com.example.infinite_track.presentation.design.components.button.InfiniteButtonState
import com.example.infinite_track.presentation.design.components.button.InfiniteButtonVariant
import com.example.infinite_track.presentation.design.components.data.InfiniteInfoRow
import com.example.infinite_track.presentation.design.components.data.InfiniteInfoRowOrientation
import com.example.infinite_track.presentation.design.components.data.InfiniteSectionHeader
import com.example.infinite_track.presentation.design.components.input.InfiniteSupportingText
import com.example.infinite_track.presentation.design.components.navigation.InfiniteTopBar
import com.example.infinite_track.presentation.design.components.status.InfiniteStatusPill
import com.example.infinite_track.presentation.design.components.status.InfiniteStatusVariant
import com.example.infinite_track.presentation.design.components.surface.InfiniteCard
import com.example.infinite_track.presentation.design.tokens.InfiniteSemantic
import com.example.infinite_track.presentation.design.tokens.InfiniteSize
import com.example.infinite_track.presentation.design.tokens.InfiniteSpacing

@Composable
fun WfaRequestReviewScreen(
    uiState: WfaRequestUiState,
    onEvent: (WfaRequestEvent) -> Unit,
    onBack: () -> Unit,
    onClose: () -> Unit = onBack,
    modifier: Modifier = Modifier
) {
    val isSubmitting = uiState.phase == WfaRequestPhase.Submitting
    val reason = uiState.config?.reasons?.firstOrNull { it.id == uiState.draft.reasonId }
    val location = uiState.draft.location ?: uiState.location
    val locationCoordinate = location?.takeIf { it.hasValidCoordinates }?.let {
        GeoCoordinate(it.latitude, it.longitude)
    }

    Column(modifier.fillMaxSize()) {
        InfiniteTopBar(
            title = stringResource(R.string.wfa_request_review_title),
            navigationContentDescription = stringResource(R.string.wfa_request_back),
            onNavigationClick = { if (!isSubmitting) onBack() },
            actionIcon = Icons.Default.Close,
            actionContentDescription = stringResource(R.string.wfa_request_close),
            onActionClick = { if (!isSubmitting) onClose() }
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize().testTag("wfaReviewContent"),
            contentPadding = PaddingValues(InfiniteSpacing.Default.lg),
            verticalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.lg)
        ) {
            item {
                InfiniteSectionHeader(
                    title = stringResource(R.string.wfa_request_review_guidance)
                )
            }
            if (location != null && uiState.config != null) {
                item {
                    InfiniteCard(
                        modifier = Modifier.fillMaxWidth().testTag("wfaReviewLocationCard")
                    ) {
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
                                radiusMeters = uiState.config.radiusMeters,
                                title = location.displayName,
                                address = location.formattedAddress,
                                modifier = Modifier.fillMaxWidth().testTag("wfaReviewLocationMap"),
                                contentDescription = stringResource(
                                    R.string.wfa_request_location_map_content_description,
                                    location.displayName,
                                    uiState.config.radiusMeters
                                )
                            )
                        }
                    }
                }
            }
            item {
                InfiniteCard(
                    modifier = Modifier.fillMaxWidth().testTag("wfaReviewDetailsCard")
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.md)) {
                        InfiniteSectionHeader(
                            title = stringResource(R.string.wfa_request_details),
                            leadingIcon = Icons.Outlined.Info
                        )
                        InfiniteInfoRow(
                            stringResource(R.string.wfa_request_date),
                            uiState.draft.scheduleDate?.toString().orEmpty()
                        )
                        InfiniteInfoRow(
                            stringResource(R.string.wfa_request_reason),
                            reason?.label.orEmpty()
                        )
                        if (reason?.isOther == true) {
                            InfiniteInfoRow(
                                stringResource(R.string.wfa_request_other_reason),
                                uiState.draft.otherReasonText
                            )
                        }
                        if (uiState.draft.notes.isNotBlank()) {
                            InfiniteInfoRow(
                                stringResource(R.string.wfa_request_notes),
                                uiState.draft.notes
                            )
                        }
                    }
                }
            }
            uiState.employee?.let { employee ->
                item {
                    InfiniteCard(
                        modifier = Modifier.fillMaxWidth().testTag("wfaReviewEmployeeCard")
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.md)) {
                            InfiniteSectionHeader(
                                title = stringResource(R.string.wfa_request_employee),
                                leadingIcon = Icons.Outlined.Person
                            )
                            InfiniteInfoRow(
                                stringResource(R.string.wfa_request_employee_name),
                                employee.fullName
                            )
                            InfiniteInfoRow(
                                stringResource(R.string.wfa_request_employee_division),
                                employee.division
                            )
                            InfiniteSupportingText(
                                text = stringResource(R.string.wfa_request_employee_profile_source)
                            )
                        }
                    }
                }
            }
            uiState.config?.let { config ->
                item {
                    InfiniteCard(
                        modifier = Modifier.fillMaxWidth().testTag("wfaReviewPolicyCard"),
                        semantic = InfiniteSemantic.Info,
                        showShadow = false
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.md)) {
                            InfiniteSectionHeader(
                                title = stringResource(R.string.wfa_request_policy_title),
                                subtitle = stringResource(
                                    R.string.wfa_request_policy_body,
                                    config.radiusMeters
                                ),
                                leadingIcon = Icons.Outlined.Info
                            )
                            InfiniteInfoRow(
                                label = stringResource(R.string.wfa_request_radius_policy_label),
                                value = stringResource(
                                    R.string.wfa_request_radius_policy,
                                    config.radiusMeters
                                ),
                                semantic = InfiniteSemantic.Primary
                            )
                            if (location?.hasValidCoordinates == true) {
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
                }
            }
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
