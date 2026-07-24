package com.example.infinite_track.presentation.components.button.attendance

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.infinite_track.R
import com.example.infinite_track.domain.model.attendance.WorkMode
import com.example.infinite_track.presentation.design.components.status.InfiniteStatusPill
import com.example.infinite_track.presentation.design.components.status.InfiniteStatusVariant
import com.example.infinite_track.presentation.design.components.surface.InfiniteCard
import com.example.infinite_track.presentation.design.tokens.InfiniteColors
import com.example.infinite_track.presentation.design.tokens.InfiniteIcons
import com.example.infinite_track.presentation.design.tokens.InfiniteMotion
import com.example.infinite_track.presentation.design.tokens.InfiniteRadius
import com.example.infinite_track.presentation.design.tokens.InfiniteSemantic
import com.example.infinite_track.presentation.design.tokens.InfiniteSize
import com.example.infinite_track.presentation.design.tokens.InfiniteSpacing
import com.example.infinite_track.presentation.design.tokens.InfiniteSurfaceVariant
import com.example.infinite_track.presentation.design.tokens.infiniteSemanticColors
import com.example.infinite_track.presentation.screen.attendance.preparation.WorkModeOptionUiModel
import com.example.infinite_track.presentation.theme.Infinite_TrackTheme

@Composable
fun WorkModeSelector(
    options: List<WorkModeOptionUiModel>,
    onModeSelected: (WorkMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.sm)
    ) {
        options.forEach { option ->
            WorkModeOptionCard(
                option = option,
                onClick = { onModeSelected(option.mode) }
            )
        }
    }
}

@Composable
private fun WorkModeOptionCard(
    option: WorkModeOptionUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val presentation = option.mode.presentation()
    val selectedStateDescription = stringResource(R.string.attendance_work_mode_selected)
    val semanticColors = infiniteSemanticColors(presentation.semantic)
    val selectedTint by animateColorAsState(
        targetValue = if (option.isSelected) {
            semanticColors.container
        } else {
            InfiniteColors.Transparent
        },
        animationSpec = InfiniteMotion.normalTween(),
        label = "work-mode-selection-tint"
    )
    val shape = InfiniteRadius.shape(InfiniteSize.Medium)

    InfiniteCard(
        modifier = modifier
            .fillMaxWidth()
            .sizeIn(minHeight = 96.dp)
            .clip(shape)
            .background(selectedTint, shape)
            .semantics(mergeDescendants = true) {
                selected = option.isSelected
                role = Role.RadioButton
                if (option.isSelected) stateDescription = selectedStateDescription
            },
        variant = InfiniteSurfaceVariant.Outlined,
        semantic = presentation.semantic,
        selected = option.isSelected,
        clickable = true,
        showShadow = false,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(semanticColors.container),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = presentation.icon,
                    contentDescription = null,
                    modifier = Modifier.size(InfiniteSpacing.Default.xl),
                    tint = semanticColors.accent
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.xs)
            ) {
                InfiniteStatusPill(
                    label = option.mode.shortLabel,
                    variant = InfiniteStatusVariant.Neutral,
                    size = InfiniteSize.Small,
                    colorOverride = semanticColors.accent
                )
                Text(
                    text = option.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = InfiniteColors.Text
                )
                Text(
                    text = option.supportingText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = InfiniteColors.AttendanceReportBodyText
                )
                InfiniteStatusPill(
                    label = presentation.evidenceLabel,
                    variant = InfiniteStatusVariant.Neutral,
                    size = InfiniteSize.Small,
                    leadingIcon = presentation.evidenceIcon,
                    colorOverride = semanticColors.accent
                )
            }

            if (option.isSelected) {
                Icon(
                    imageVector = InfiniteIcons.Success,
                    contentDescription = null,
                    modifier = Modifier.size(InfiniteSpacing.Default.xl),
                    tint = semanticColors.accent
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(InfiniteSpacing.Default.xl)
                        .border(1.dp, InfiniteColors.Neutral.copy(alpha = 0.42f), CircleShape)
                )
            }
        }
    }
}

private data class WorkModePresentation(
    val semantic: InfiniteSemantic,
    val icon: ImageVector,
    val evidenceLabel: String,
    val evidenceIcon: ImageVector
)

@Composable
private fun WorkMode.presentation(): WorkModePresentation = when (this) {
    WorkMode.WFO -> WorkModePresentation(
        semantic = InfiniteSemantic.Primary,
        icon = InfiniteIcons.Work,
        evidenceLabel = stringResource(R.string.attendance_wfo_evidence),
        evidenceIcon = InfiniteIcons.Location
    )
    WorkMode.WFH -> WorkModePresentation(
        semantic = InfiniteSemantic.Info,
        icon = Icons.Default.Home,
        evidenceLabel = stringResource(R.string.attendance_wfh_evidence),
        evidenceIcon = InfiniteIcons.Shield
    )
    WorkMode.WFA -> WorkModePresentation(
        semantic = InfiniteSemantic.Secondary,
        icon = InfiniteIcons.Location,
        evidenceLabel = stringResource(R.string.attendance_wfa_evidence),
        evidenceIcon = InfiniteIcons.Calendar
    )
}

@Preview(showBackground = true, widthDp = 320)
@Composable
private fun WorkModeSelectorPreview() {
    Infinite_TrackTheme {
        WorkModeSelector(
            options = WorkMode.values().map { mode ->
                WorkModeOptionUiModel(
                    mode = mode,
                    title = mode.displayLabel,
                    supportingText = when (mode) {
                        WorkMode.WFO -> "Lokasi kantor yang ditetapkan"
                        WorkMode.WFH -> "Lokasi rumah yang ditetapkan admin"
                        WorkMode.WFA -> "Memerlukan booking yang disetujui"
                    },
                    isSelected = mode == WorkMode.WFH
                )
            },
            onModeSelected = {}
        )
    }
}
