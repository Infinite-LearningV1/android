package com.example.infinite_track.presentation.screen.attendance.permission

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.infinite_track.presentation.design.components.button.InfiniteButton
import com.example.infinite_track.presentation.design.components.button.InfiniteButtonState
import com.example.infinite_track.presentation.design.components.button.InfiniteButtonVariant
import com.example.infinite_track.presentation.design.components.data.InfiniteSectionHeader
import com.example.infinite_track.presentation.design.components.navigation.InfiniteTopBar
import com.example.infinite_track.presentation.design.tokens.InfiniteColors
import com.example.infinite_track.presentation.design.tokens.InfiniteSemantic
import com.example.infinite_track.presentation.theme.Infinite_TrackTheme

@Composable
fun AttendancePermissionReadinessScreen(
    uiState: AttendancePermissionReadinessUiState,
    onEvent: (AttendancePermissionReadinessEvent) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(InfiniteColors.AttendanceReportBackground)
    ) {
        PermissionBackdrop()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            InfiniteTopBar(
                title = "Attendance",
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                navigationContentDescription = "Kembali",
                onNavigationClick = onBackClick
            )
            PermissionHeroCard(isLoading = uiState.isLoading)
            PermissionProgressHeader(uiState = uiState)

            PermissionSection(
                title = "Akses wajib",
                subtitle = "Lengkapi 3 akses utama sebelum lanjut",
                icon = Icons.Default.Shield,
                items = uiState.requiredItems,
                onEvent = onEvent
            )

            PermissionSection(
                title = "Pengingat opsional",
                subtitle = "Boleh dilewati, absensi manual tetap bisa lanjut",
                icon = Icons.Default.Notifications,
                trailingText = "Opsional",
                items = uiState.optionalItems,
                onEvent = onEvent
            )

            val guidance = uiState.recoverableFailure
                ?: uiState.contextualGuidance
                ?: defaultGuidance(uiState.canContinue)
            PermissionReadinessInfoBox(
                guidance = guidance,
                onAction = when (guidance.action) {
                    PermissionGuidanceAction.RETRY_REFRESH -> {
                        { onEvent(AttendancePermissionReadinessEvent.RetryRefresh) }
                    }
                    null -> null
                }
            )

            InfiniteButton(
                text = uiState.primaryActionLabel,
                onClick = { onEvent(AttendancePermissionReadinessEvent.PrimaryActionClicked) },
                modifier = Modifier.fillMaxWidth(),
                variant = InfiniteButtonVariant.Primary,
                state = when {
                    uiState.isLoading || uiState.isRefreshing -> InfiniteButtonState.Loading
                    uiState.primaryActionEnabled -> InfiniteButtonState.Enabled
                    else -> InfiniteButtonState.Disabled
                },
                fullWidth = true
            )

            Spacer(modifier = Modifier.height(112.dp))
        }
    }
}

@Composable
private fun PermissionBackdrop() {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .size(280.dp)
                .align(Alignment.TopEnd)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            InfiniteColors.Primary.copy(alpha = 0.20f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(260.dp)
                .align(Alignment.BottomStart)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            InfiniteColors.Accent.copy(alpha = 0.16f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
    }
}

@Composable
private fun PermissionSection(
    title: String,
    subtitle: String,
    icon: ImageVector,
    items: List<PermissionItemUiModel>,
    onEvent: (AttendancePermissionReadinessEvent) -> Unit,
    modifier: Modifier = Modifier,
    trailingText: String? = null
) {
    PermissionGlassCard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            InfiniteSectionHeader(
                title = title,
                subtitle = subtitle,
                leadingIcon = icon,
                trailingText = trailingText
            )
            items.forEach { item ->
                PermissionMissionRow(
                    item = item,
                    icon = item.iconKey.toImageVector(),
                    onActionClick = item.actionLabel?.let {
                        {
                            onEvent(
                                AttendancePermissionReadinessEvent.PermissionItemClicked(item.access)
                            )
                        }
                    }
                )
            }
        }
    }
}

private fun PermissionIconKey.toImageVector(): ImageVector = when (this) {
    PermissionIconKey.LOCATION -> Icons.Default.LocationOn
    PermissionIconKey.CAMERA -> Icons.Default.PhotoCamera
    PermissionIconKey.DEVICE_LOCATION -> Icons.Default.Settings
    PermissionIconKey.NOTIFICATION -> Icons.Default.Notifications
    PermissionIconKey.BACKGROUND_LOCATION -> Icons.Default.Tune
}

private fun defaultGuidance(canContinue: Boolean): PermissionGuidanceUiModel =
    PermissionGuidanceUiModel(
        title = if (canContinue) "Akses wajib sudah siap" else "Selesaikan akses wajib",
        message = "Notifikasi dan lokasi latar belakang membantu pengingat, tetapi tidak memblokir absensi manual.",
        semantic = if (canContinue) InfiniteSemantic.Info else InfiniteSemantic.Warning
    )

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
