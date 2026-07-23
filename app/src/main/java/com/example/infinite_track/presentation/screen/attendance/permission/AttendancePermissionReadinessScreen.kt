package com.example.infinite_track.presentation.screen.attendance.permission

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.infinite_track.presentation.design.components.button.InfiniteButton
import com.example.infinite_track.presentation.design.components.button.InfiniteButtonState
import com.example.infinite_track.presentation.design.components.button.InfiniteButtonVariant
import com.example.infinite_track.presentation.design.components.state.InfiniteLoadingState
import com.example.infinite_track.presentation.design.tokens.InfiniteColors
import com.example.infinite_track.presentation.design.tokens.InfiniteFeedbackTypography
import com.example.infinite_track.presentation.design.tokens.InfiniteMotion
import com.example.infinite_track.presentation.design.tokens.InfiniteSpacing
import com.example.infinite_track.presentation.theme.Infinite_TrackTheme

@Composable
fun AttendancePermissionPanelContent(
    uiState: AttendancePermissionReadinessUiState,
    onEvent: (AttendancePermissionReadinessEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    var optionalExpanded by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 760.dp)
            .testTag("attendance-permission-panel")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(width = 40.dp, height = 4.dp)
                    .testTag("permission-panel-drag-handle")
            ) {
                Surface(
                    modifier = Modifier.matchParentSize(),
                    shape = RoundedCornerShape(999.dp),
                    color = InfiniteColors.Neutral.copy(alpha = 0.25f)
                ) {}
            }
        }

        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                contentAlignment = Alignment.Center
            ) {
                InfiniteLoadingState(message = "Memeriksa kesiapan akses...")
            }
        } else {
            PermissionPanelBody(
                uiState = uiState,
                optionalExpanded = optionalExpanded,
                onOptionalExpandedChange = { optionalExpanded = it },
                onEvent = onEvent,
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(
                        start = InfiniteSpacing.Default.lg,
                        end = InfiniteSpacing.Default.lg,
                        bottom = InfiniteSpacing.Default.xl
                    )
            )
        }
    }
}

@Composable
private fun PermissionPanelBody(
    uiState: AttendancePermissionReadinessUiState,
    optionalExpanded: Boolean,
    onOptionalExpandedChange: (Boolean) -> Unit,
    onEvent: (AttendancePermissionReadinessEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (uiState.requiredTotalCount == 0) {
        0f
    } else {
        uiState.requiredReadyCount.toFloat() / uiState.requiredTotalCount
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.md)
    ) {
        PermissionPanelHero(
            canContinue = uiState.canContinue,
            readyCount = uiState.requiredReadyCount,
            totalCount = uiState.requiredTotalCount,
            progress = progress
        )

        uiState.recoverableFailure?.let { failure ->
            PermissionReadinessInfoBox(
                guidance = failure,
                onAction = if (failure.action == PermissionGuidanceAction.RETRY_REFRESH) {
                    { onEvent(AttendancePermissionReadinessEvent.RetryRefresh) }
                } else {
                    null
                }
            )
        }

        PermissionSectionLabel(
            title = "Akses wajib",
            subtitle = "Diperlukan untuk presensi dan verifikasi"
        )

        Column(verticalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.sm)) {
            uiState.requiredItems.forEach { item ->
                PermissionAccessCard(
                    item = item,
                    onActionClick = item.actionLabel?.let {
                        {
                            onEvent(
                                AttendancePermissionReadinessEvent.PermissionItemClicked(
                                    item.access
                                )
                            )
                        }
                    }
                )
            }
        }

        OptionalPermissionHeader(
            expanded = optionalExpanded,
            readyCount = uiState.optionalItems.count { it.isReady },
            totalCount = uiState.optionalItems.size,
            onClick = { onOptionalExpandedChange(!optionalExpanded) }
        )

        AnimatedVisibility(
            visible = optionalExpanded,
            enter = fadeIn(InfiniteMotion.normalTween()) +
                expandVertically(InfiniteMotion.normalTween()),
            exit = fadeOut(InfiniteMotion.normalTween()) +
                shrinkVertically(InfiniteMotion.normalTween())
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.sm)) {
                uiState.optionalItems.forEach { item ->
                    PermissionAccessCard(
                        item = item,
                        onActionClick = item.actionLabel?.let {
                            {
                                onEvent(
                                    AttendancePermissionReadinessEvent.PermissionItemClicked(
                                        item.access
                                    )
                                )
                            }
                        }
                    )
                }
                uiState.contextualGuidance?.let { guidance ->
                    PermissionReadinessInfoBox(
                        guidance = guidance,
                        onAction = if (guidance.action == PermissionGuidanceAction.RETRY_REFRESH) {
                            { onEvent(AttendancePermissionReadinessEvent.RetryRefresh) }
                        } else {
                            null
                        }
                    )
                }
            }
        }

        InfiniteButton(
            text = uiState.primaryActionLabel,
            onClick = { onEvent(AttendancePermissionReadinessEvent.PrimaryActionClicked) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("permission-panel-primary-action"),
            variant = InfiniteButtonVariant.Primary,
            state = when {
                uiState.isRefreshing -> InfiniteButtonState.Loading
                uiState.primaryActionEnabled -> InfiniteButtonState.Enabled
                else -> InfiniteButtonState.Disabled
            },
            fullWidth = true
        )

        Spacer(modifier = Modifier.height(InfiniteSpacing.Default.sm))
    }
}

@Composable
private fun PermissionPanelHero(
    canContinue: Boolean,
    readyCount: Int,
    totalCount: Int,
    progress: Float
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = InfiniteColors.Primary.copy(alpha = 0.08f)
            .compositeOver(InfiniteColors.Surface),
        border = BorderStroke(1.dp, InfiniteColors.Primary.copy(alpha = 0.18f))
    ) {
        Column(
            modifier = Modifier.padding(InfiniteSpacing.Default.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.sm)
        ) {
            Surface(
                shape = CircleShape,
                color = if (canContinue) {
                    InfiniteColors.Success.copy(alpha = 0.16f)
                        .compositeOver(InfiniteColors.Surface)
                } else {
                    InfiniteColors.Primary.copy(alpha = 0.14f)
                        .compositeOver(InfiniteColors.Surface)
                }
            ) {
                Icon(
                    imageVector = if (canContinue) Icons.Default.CheckCircle else Icons.Default.Shield,
                    contentDescription = null,
                    tint = if (canContinue) InfiniteColors.Success else InfiniteColors.Primary,
                    modifier = Modifier
                        .padding(14.dp)
                        .size(30.dp)
                )
            }
            Text(
                text = if (canContinue) "Akses attendance siap" else "Izin akses diperlukan",
                style = InfiniteFeedbackTypography.dialogTitle,
                color = InfiniteColors.Text
            )
            Text(
                text = if (canContinue) {
                    "Semua akses wajib aktif. Anda tetap dapat memeriksa akses opsional kapan saja."
                } else {
                    "Lengkapi akses wajib sebelum menjalankan presensi dan verifikasi wajah."
                },
                style = InfiniteFeedbackTypography.dialogBody,
                color = InfiniteColors.Text.copy(alpha = 0.70f)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$readyCount/$totalCount akses wajib siap",
                    style = InfiniteFeedbackTypography.actionLabel,
                    color = InfiniteColors.Primary
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = InfiniteFeedbackTypography.pillLabel,
                    color = InfiniteColors.Text.copy(alpha = 0.64f)
                )
            }
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = if (canContinue) InfiniteColors.Success else InfiniteColors.Primary,
                trackColor = InfiniteColors.Neutral.copy(alpha = 0.14f)
            )
        }
    }
}

@Composable
private fun PermissionSectionLabel(
    title: String,
    subtitle: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            style = InfiniteFeedbackTypography.snackbarTitle,
            color = InfiniteColors.Text
        )
        Text(
            text = subtitle,
            style = InfiniteFeedbackTypography.supportingBody,
            color = InfiniteColors.Text.copy(alpha = 0.64f)
        )
    }
}

@Composable
private fun OptionalPermissionHeader(
    expanded: Boolean,
    readyCount: Int,
    totalCount: Int,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("permission-optional-toggle")
            .semantics(mergeDescendants = true) {
                contentDescription = "Akses opsional"
                stateDescription = if (expanded) "Diperluas" else "Diciutkan"
                role = Role.Button
            }
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = InfiniteColors.Surface,
        border = BorderStroke(
            1.dp,
            InfiniteColors.Neutral.copy(alpha = 0.16f)
                .compositeOver(InfiniteColors.Surface)
        ),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(InfiniteSpacing.Default.md),
            horizontalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "Akses opsional",
                    style = InfiniteFeedbackTypography.snackbarTitle,
                    color = InfiniteColors.Text
                )
                Text(
                    text = "$readyCount/$totalCount aktif · tidak menghambat absensi manual",
                    style = InfiniteFeedbackTypography.pillLabel,
                    color = InfiniteColors.Text.copy(alpha = 0.64f)
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = null,
                tint = InfiniteColors.Primary
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AttendancePermissionPanelContentPreview() {
    Infinite_TrackTheme {
        AttendancePermissionPanelContent(
            uiState = AttendancePermissionReadinessUiState(isLoading = false),
            onEvent = {}
        )
    }
}
