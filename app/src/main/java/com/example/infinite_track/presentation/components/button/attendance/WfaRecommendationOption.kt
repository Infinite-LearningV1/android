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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.infinite_track.R
import com.example.infinite_track.presentation.design.components.status.InfiniteStatusPill
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
import com.example.infinite_track.presentation.screen.attendance.preparation.WfaRecommendationUiModel
import com.example.infinite_track.presentation.screen.attendance.preparation.WfaRecommendationCategoryIcon
import com.example.infinite_track.presentation.theme.Infinite_TrackTheme

@Composable
fun WfaRecommendationOption(
    model: WfaRecommendationUiModel,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = infiniteSemanticColors(InfiniteSemantic.Secondary)
    val suitabilityPresentation = WfaRecommendationSuitabilityPresentationMapper.map(
        model.suitabilitySemantic
    )
    val suitabilityContentDescription = stringResource(
        suitabilityPresentation.contentDescriptionRes
    )
    val selectedStateDescription = stringResource(R.string.attendance_work_mode_selected)
    val selectedTint by animateColorAsState(
        targetValue = if (selected) colors.container else InfiniteColors.Transparent,
        animationSpec = InfiniteMotion.normalTween(),
        label = "wfa-recommendation-selection-tint"
    )
    val shape = InfiniteRadius.shape(InfiniteSize.Medium)

    InfiniteCard(
        modifier = modifier
            .fillMaxWidth()
            .sizeIn(minHeight = 88.dp)
            .clip(shape)
            .background(selectedTint, shape)
            .semantics(mergeDescendants = true) {
                this.selected = selected
                role = Role.RadioButton
                if (selected) stateDescription = selectedStateDescription
            },
        variant = InfiniteSurfaceVariant.Outlined,
        semantic = InfiniteSemantic.Secondary,
        selected = selected,
        clickable = true,
        showShadow = false,
        onClick = onSelect
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
                    .background(colors.container),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (model.categoryIcon) {
                        WfaRecommendationCategoryIcon.CAFE -> InfiniteIcons.Cafe
                        WfaRecommendationCategoryIcon.COWORKING -> InfiniteIcons.Coworking
                        WfaRecommendationCategoryIcon.LIBRARY -> InfiniteIcons.Library
                        WfaRecommendationCategoryIcon.PARK -> InfiniteIcons.Park
                        WfaRecommendationCategoryIcon.WORK -> InfiniteIcons.Work
                    },
                    contentDescription = null,
                    modifier = Modifier.size(InfiniteSpacing.Default.xl),
                    tint = colors.accent
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.xs)
            ) {
                Text(
                    text = model.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = InfiniteColors.Text
                )
                Text(
                    text = model.supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = InfiniteColors.AttendanceReportBodyText
                )
                InfiniteStatusPill(
                    label = model.suitabilityText,
                    variant = suitabilityPresentation.variant,
                    contentDescription = suitabilityContentDescription,
                    size = InfiniteSize.Small,
                    leadingIcon = suitabilityPresentation.icon,
                    selected = selected
                )
            }

            if (selected) {
                Icon(
                    imageVector = InfiniteIcons.Success,
                    contentDescription = null,
                    modifier = Modifier.size(InfiniteSpacing.Default.xl),
                    tint = colors.accent
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

@Preview(showBackground = true, widthDp = 320)
@Composable
private fun WfaRecommendationOptionPreview() {
    Infinite_TrackTheme {
        WfaRecommendationOption(
            model = WfaRecommendationUiModel(
                stableKey = "cafe-palu",
                name = "Cafe Palu",
                supportingText = "Cafe • 1.25 km",
                suitabilityText = "WFA score 91 • Excellent",
            ),
            selected = true,
            onSelect = {}
        )
    }
}
