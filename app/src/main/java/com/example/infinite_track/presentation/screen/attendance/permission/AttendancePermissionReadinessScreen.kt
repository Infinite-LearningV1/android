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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Scaffold
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
import com.example.infinite_track.presentation.design.components.data.InfiniteSectionHeader
import com.example.infinite_track.presentation.design.components.navigation.InfiniteTopBar
import com.example.infinite_track.presentation.design.components.state.InfiniteLoadingState
import com.example.infinite_track.presentation.design.tokens.InfiniteColors
import com.example.infinite_track.presentation.design.tokens.InfiniteFeedbackTypography
import com.example.infinite_track.presentation.design.tokens.InfiniteMotion
import com.example.infinite_track.presentation.design.tokens.InfiniteSpacing
import com.example.infinite_track.presentation.theme.Infinite_TrackTheme

@Composable
fun AttendancePermissionReadinessScreen(
    uiState: AttendancePermissionReadinessUiState,
    onEvent: (AttendancePermissionReadinessEvent) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var optionalExpanded by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("attendance-permission-screen"),
        containerColor = Color.Transparent,
        topBar = {
            InfiniteTopBar(
                title = "Attendance",
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                navigationContentDescription = "Kembali",
                onNavigationClick = onBackClick
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                InfiniteLoadingState(message = "Memeriksa kesiapan akses...")
            }
        } else {
            PermissionReadinessContent(
                uiState = uiState,
                optionalExpanded = optionalExpanded,
                onOptionalExpandedChange = { optionalExpanded = it },
                onEvent = onEvent,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }
    }
}

@Composable
private fun PermissionReadinessContent(
    uiState: AttendancePermissionReadinessUiState,
    optionalExpanded: Boolean,
    onOptionalExpandedChange: (Boolean) -> Unit,
    onEvent: (AttendancePermissionReadinessEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentRequiredIndex = uiState.requiredItems.indexOfFirst { !it.isReady }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.lg)
    ) {
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

        InfiniteSectionHeader(
            title = "Akses wajib",
            subtitle = "Lengkapi 3 akses utama sebelum lanjut",
            leadingIcon = Icons.Default.Shield
        )

        Column(verticalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.sm)) {
            uiState.requiredItems.forEachIndexed { index, item ->
                PermissionAccessCard(
                    item = item,
                    onActionClick = if (index == currentRequiredIndex && item.actionLabel != null) {
                        {
                            onEvent(
                                AttendancePermissionReadinessEvent.PermissionItemClicked(
                                    item.access
                                )
                            )
                        }
                    } else {
                        null
                    }
                )
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .sizeIn(minHeight = 48.dp)
                .testTag("permission-optional-toggle")
                .semantics(mergeDescendants = true) {
                    contentDescription = "Peringatan opsional"
                    stateDescription = if (optionalExpanded) "Diperluas" else "Diciutkan"
                    role = Role.Button
                }
                .clickable {
                    onOptionalExpandedChange(!optionalExpanded)
                },
            shape = RoundedCornerShape(16.dp),
            color = InfiniteColors.Surface,
            border = BorderStroke(
                1.dp,
                InfiniteColors.Neutral.copy(alpha = 0.18f)
                    .compositeOver(InfiniteColors.Surface)
            ),
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(InfiniteSpacing.Default.lg),
                horizontalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.xs)
                ) {
                    Text(
                        text = "Peringatan opsional",
                        style = InfiniteFeedbackTypography.snackbarTitle,
                        color = InfiniteColors.Text
                    )
                    Text(
                        text = "Boleh dilewati, absensi manual tetap bisa lanjut",
                        style = InfiniteFeedbackTypography.supportingBody,
                        color = InfiniteColors.Text.copy(alpha = 0.72f)
                    )
                }
                Text(
                    text = if (optionalExpanded) "Sembunyikan" else "Tampilkan",
                    style = InfiniteFeedbackTypography.actionLabel,
                    color = InfiniteColors.Primary
                )
            }
        }

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
                        onAction = if (
                            guidance.action == PermissionGuidanceAction.RETRY_REFRESH
                        ) {
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
            modifier = Modifier.fillMaxWidth(),
            variant = InfiniteButtonVariant.Primary,
            state = when {
                uiState.isRefreshing -> InfiniteButtonState.Loading
                uiState.primaryActionEnabled -> InfiniteButtonState.Enabled
                else -> InfiniteButtonState.Disabled
            },
            fullWidth = true
        )

        Spacer(modifier = Modifier.height(112.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun AttendancePermissionReadinessScreenPreview() {
    Infinite_TrackTheme {
        AttendancePermissionReadinessScreen(
            uiState = AttendancePermissionReadinessUiState(),
            onEvent = {},
            onBackClick = {}
        )
    }
}
