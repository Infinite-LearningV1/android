package com.example.infinite_track.presentation.components.button.attendance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.example.infinite_track.domain.model.attendance.WorkMode
import com.example.infinite_track.presentation.design.components.button.InfiniteButton
import com.example.infinite_track.presentation.design.components.button.InfiniteButtonState
import com.example.infinite_track.presentation.design.components.button.InfiniteIconButton
import com.example.infinite_track.presentation.design.components.surface.InfiniteCard
import com.example.infinite_track.presentation.design.tokens.InfiniteColors
import com.example.infinite_track.presentation.design.tokens.InfiniteIcons
import com.example.infinite_track.presentation.design.tokens.InfiniteSemantic
import com.example.infinite_track.presentation.design.tokens.InfiniteSize
import com.example.infinite_track.presentation.design.tokens.InfiniteSpacing
import com.example.infinite_track.presentation.design.tokens.InfiniteSurfaceVariant
import com.example.infinite_track.presentation.screen.attendance.preparation.AttendancePreparationPrimaryAction
import com.example.infinite_track.presentation.screen.attendance.preparation.AttendancePreparationSecondaryAction
import com.example.infinite_track.presentation.screen.attendance.preparation.AttendancePreparationUiModel
import com.example.infinite_track.presentation.screen.attendance.preparation.TargetLocationSummaryUiModel
import com.example.infinite_track.presentation.screen.attendance.preparation.WfaDiscoveryUiModel
import com.example.infinite_track.presentation.screen.attendance.preparation.WorkModeOptionUiModel
import com.example.infinite_track.presentation.theme.Infinite_TrackTheme

sealed interface AttendancePreparationEvent {
    data class ModeSelected(val mode: WorkMode) : AttendancePreparationEvent
    data class RecommendationSelected(val stableKey: String) : AttendancePreparationEvent
    data object SearchWfaLocation : AttendancePreparationEvent
    data class PrimaryActionClicked(
        val action: AttendancePreparationPrimaryAction
    ) : AttendancePreparationEvent
}

@Composable
fun AttendanceBottomSheetContent(
    model: AttendancePreparationUiModel,
    onEvent: (AttendancePreparationEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    WorkModePreparationContent(
        model = model,
        onEvent = onEvent,
        modifier = modifier
    )
}

@Composable
fun WorkModePreparationContent(
    model: AttendancePreparationUiModel,
    onEvent: (AttendancePreparationEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(
                start = InfiniteSpacing.Default.lg,
                end = InfiniteSpacing.Default.lg,
                bottom = InfiniteSpacing.Default.xl
            ),
        verticalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.lg)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.xs)) {
            Text(
                text = "Pilih Mode Kerja",
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineSmall,
                color = InfiniteColors.Text
            )
            Text(
                text = "Pilih cara Anda bekerja hari ini sebelum check-in.",
                style = MaterialTheme.typography.bodyMedium,
                color = InfiniteColors.AttendanceReportBodyText
            )
        }

        WorkModeSelector(
            options = model.modeOptions,
            onModeSelected = { mode ->
                onEvent(AttendancePreparationEvent.ModeSelected(mode))
            }
        )

        model.targetSummary?.let { summary ->
            TargetLocationSummary(model = summary)
        }

        if (
            model.secondaryAction == AttendancePreparationSecondaryAction.SEARCH_WFA_LOCATION &&
            model.secondaryActionLabel != null
        ) {
            WfaSearchAction(
                label = model.secondaryActionLabel,
                onClick = { onEvent(AttendancePreparationEvent.SearchWfaLocation) }
            )
        }

        WfaRecommendationSection(
            model = model.wfaDiscovery,
            onRecommendationSelected = { stableKey ->
                onEvent(AttendancePreparationEvent.RecommendationSelected(stableKey))
            }
        )

        InfiniteCard(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { liveRegion = LiveRegionMode.Polite },
            variant = InfiniteSurfaceVariant.Outlined,
            semantic = InfiniteSemantic.Neutral,
            showShadow = false
        ) {
            Text(
                text = model.statusMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = InfiniteColors.Text
            )
        }

        InfiniteButton(
            text = model.primaryActionLabel,
            onClick = {
                onEvent(AttendancePreparationEvent.PrimaryActionClicked(model.primaryAction))
            },
            modifier = Modifier.testTag("attendancePrimaryAction"),
            size = InfiniteSize.Large,
            state = if (model.isPrimaryActionEnabled) {
                InfiniteButtonState.Enabled
            } else {
                InfiniteButtonState.Disabled
            },
            fullWidth = true,
            trailingIcon = InfiniteIcons.ChevronRight
        )
    }
}

@Composable
private fun WfaSearchAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    InfiniteCard(
        modifier = modifier.fillMaxWidth(),
        variant = InfiniteSurfaceVariant.Outlined,
        semantic = InfiniteSemantic.Secondary,
        showShadow = false
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.xs)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    color = InfiniteColors.Text
                )
                Text(
                    text = "Cari atau pilih lokasi untuk draf pengajuan WFA.",
                    style = MaterialTheme.typography.bodySmall,
                    color = InfiniteColors.AttendanceReportBodyText
                )
            }
            InfiniteIconButton(
                icon = InfiniteIcons.Search,
                contentDescription = label,
                onClick = onClick,
                size = InfiniteSize.Large
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 320)
@Composable
private fun WorkModePreparationContentPreview() {
    Infinite_TrackTheme {
        WorkModePreparationContent(
            model = AttendancePreparationUiModel(
                modeOptions = WorkMode.values().map { mode ->
                    WorkModeOptionUiModel(
                        mode = mode,
                        title = mode.displayLabel,
                        supportingText = when (mode) {
                            WorkMode.WFO -> "Lokasi kantor yang ditetapkan"
                            WorkMode.WFH -> "Lokasi rumah yang ditetapkan admin"
                            WorkMode.WFA -> "Memerlukan booking yang disetujui"
                        },
                        isSelected = mode == WorkMode.WFA
                    )
                },
                targetSummary = TargetLocationSummaryUiModel(
                    displayName = "WFA Disetujui",
                    sourceLabel = "Booking WFA disetujui",
                    radiusText = "Radius 100 m",
                    distanceText = "Jarak 25 m",
                    rangeText = "Di dalam jangkauan"
                ),
                statusMessage = "Lokasi target siap digunakan untuk kehadiran.",
                wfaDiscovery = WfaDiscoveryUiModel.Empty(
                    "Belum ada rekomendasi lokasi WFA."
                ),
                primaryAction = AttendancePreparationPrimaryAction.CONTINUE_TO_FACE_VERIFICATION,
                primaryActionLabel = "Lanjut ke Verifikasi Wajah",
                isPrimaryActionEnabled = true,
                secondaryAction = AttendancePreparationSecondaryAction.SEARCH_WFA_LOCATION,
                secondaryActionLabel = "Cari lokasi WFA"
            ),
            onEvent = {}
        )
    }
}
