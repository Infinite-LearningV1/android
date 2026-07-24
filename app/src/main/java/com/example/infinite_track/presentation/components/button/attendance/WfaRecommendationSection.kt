package com.example.infinite_track.presentation.components.button.attendance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.infinite_track.R
import com.example.infinite_track.presentation.design.components.status.InfiniteStatusPill
import com.example.infinite_track.presentation.design.components.status.InfiniteStatusVariant
import com.example.infinite_track.presentation.design.components.surface.InfiniteCard
import com.example.infinite_track.presentation.design.tokens.InfiniteColors
import com.example.infinite_track.presentation.design.tokens.InfiniteIcons
import com.example.infinite_track.presentation.design.tokens.InfiniteSemantic
import com.example.infinite_track.presentation.design.tokens.InfiniteSize
import com.example.infinite_track.presentation.design.tokens.InfiniteSpacing
import com.example.infinite_track.presentation.design.tokens.InfiniteSurfaceVariant
import com.example.infinite_track.presentation.screen.attendance.preparation.WfaDiscoveryUiModel
import com.example.infinite_track.presentation.screen.attendance.preparation.WfaRecommendationUiModel
import com.example.infinite_track.presentation.screen.attendance.preparation.isRecommendationSelected
import com.example.infinite_track.presentation.theme.Infinite_TrackTheme

@Composable
fun WfaRecommendationSection(
    model: WfaDiscoveryUiModel,
    onRecommendationSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    when (model) {
        WfaDiscoveryUiModel.Hidden -> Unit
        WfaDiscoveryUiModel.Loading -> WfaDiscoveryStateCard(
            message = stringResource(R.string.attendance_wfa_recommendation_loading),
            semantic = InfiniteSemantic.Info,
            modifier = modifier,
            loading = true
        )
        is WfaDiscoveryUiModel.Empty -> WfaDiscoveryStateCard(
            message = model.message,
            semantic = InfiniteSemantic.Neutral,
            modifier = modifier
        )
        is WfaDiscoveryUiModel.Failure -> WfaDiscoveryStateCard(
            message = model.message,
            semantic = InfiniteSemantic.Error,
            modifier = modifier
        )
        is WfaDiscoveryUiModel.Content -> WfaRecommendationContent(
            model = model,
            onRecommendationSelected = onRecommendationSelected,
            modifier = modifier
        )
    }
}

@Composable
private fun WfaRecommendationContent(
    model: WfaDiscoveryUiModel.Content,
    onRecommendationSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.sm)
    ) {
        if (model.rows.isNotEmpty()) {
            Text(
                text = stringResource(R.string.attendance_wfa_recommendation_heading),
                style = MaterialTheme.typography.titleMedium,
                color = InfiniteColors.Text
            )
            Text(
                text = stringResource(R.string.attendance_wfa_recommendation_supporting),
                style = MaterialTheme.typography.bodySmall,
                color = InfiniteColors.AttendanceReportBodyText
            )
            model.rows.forEach { recommendation ->
                key(recommendation.stableKey) {
                    WfaRecommendationOption(
                        model = recommendation,
                        selected = model.isRecommendationSelected(recommendation),
                        onSelect = {
                            onRecommendationSelected(recommendation.stableKey)
                        }
                    )
                }
            }
        }

        model.searchPreviewName?.let { previewName ->
            InfiniteCard(
                modifier = Modifier.fillMaxWidth(),
                variant = InfiniteSurfaceVariant.Outlined,
                semantic = InfiniteSemantic.Info,
                showShadow = false
            ) {
                Text(
                    text = stringResource(R.string.attendance_wfa_preview_selected),
                    style = MaterialTheme.typography.labelMedium,
                    color = InfiniteColors.AttendanceReportMutedText
                )
                Text(
                    text = previewName,
                    style = MaterialTheme.typography.titleSmall,
                    color = InfiniteColors.Text
                )
                InfiniteStatusPill(
                    label = stringResource(R.string.attendance_wfa_preview_draft),
                    variant = InfiniteStatusVariant.Recommended,
                    size = InfiniteSize.Small,
                    leadingIcon = InfiniteIcons.Location
                )
            }
        }
    }
}

@Composable
private fun WfaDiscoveryStateCard(
    message: String,
    semantic: InfiniteSemantic,
    modifier: Modifier = Modifier,
    loading: Boolean = false
) {
    InfiniteCard(
        modifier = modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
        variant = InfiniteSurfaceVariant.Outlined,
        semantic = semantic,
        showShadow = false
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(InfiniteSpacing.Default.xl),
                    strokeWidth = 2.dp,
                    color = InfiniteColors.Primary
                )
            } else {
                Icon(
                    imageVector = when (semantic) {
                        InfiniteSemantic.Error -> InfiniteIcons.Error
                        InfiniteSemantic.Info -> InfiniteIcons.Info
                        else -> InfiniteIcons.Location
                    },
                    contentDescription = null,
                    modifier = Modifier.size(InfiniteSpacing.Default.xl),
                    tint = when (semantic) {
                        InfiniteSemantic.Error -> InfiniteColors.Error
                        InfiniteSemantic.Info -> InfiniteColors.Info
                        else -> InfiniteColors.Neutral
                    }
                )
            }
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = InfiniteColors.Text
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 320)
@Composable
private fun WfaRecommendationSectionPreview() {
    Infinite_TrackTheme {
        WfaRecommendationSection(
            model = WfaDiscoveryUiModel.Content(
                rows = listOf(
                    WfaRecommendationUiModel(
                        stableKey = "cafe-palu",
                        name = "Cafe Palu",
                        supportingText = "Cafe • 1,25 km",
                        suitabilityText = "Skor WFA 91 • Sangat sesuai",
                    )
                ),
                selectedKey = "cafe-palu",
                searchPreviewName = null
            ),
            onRecommendationSelected = {}
        )
    }
}
