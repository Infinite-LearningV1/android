package com.example.infinite_track.presentation.components.wfa

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.infinite_track.R
import com.example.infinite_track.domain.model.location.GeoCoordinate
import com.example.infinite_track.domain.model.wfa.WfaRecommendation
import com.example.infinite_track.domain.model.wfa.WfaRecommendationStatus
import com.example.infinite_track.presentation.core.body1
import com.example.infinite_track.presentation.core.body2
import com.example.infinite_track.presentation.design.components.button.InfiniteButton
import com.example.infinite_track.presentation.design.components.button.InfiniteButtonVariant
import com.example.infinite_track.presentation.design.components.data.InfiniteSectionHeader
import com.example.infinite_track.presentation.design.components.state.InfiniteEmptyState
import com.example.infinite_track.presentation.design.components.state.InfiniteErrorState
import com.example.infinite_track.presentation.design.components.state.InfiniteLoadingState
import com.example.infinite_track.presentation.design.components.status.InfiniteStatusPill
import com.example.infinite_track.presentation.design.components.status.InfiniteStatusVariant
import com.example.infinite_track.presentation.design.components.surface.InfiniteCard
import com.example.infinite_track.presentation.design.tokens.InfiniteColors
import com.example.infinite_track.presentation.design.tokens.InfiniteSemantic
import com.example.infinite_track.presentation.design.tokens.InfiniteSize
import com.example.infinite_track.presentation.design.tokens.InfiniteSpacing
import com.example.infinite_track.presentation.map.adapter.AttendanceMap
import com.example.infinite_track.presentation.map.model.AttendanceMapEvent
import com.example.infinite_track.presentation.map.model.MapCameraEffect
import com.example.infinite_track.presentation.screen.attendance.wfa_request.WfaRequestMapUiMapper
import com.example.infinite_track.presentation.screen.attendance.wfa_request.WfaRequestRecommendationState
import com.example.infinite_track.presentation.screen.attendance.wfa_request.WfaRequestUiMapper
import java.util.Locale

@Composable
fun WfaRecommendationPicker(
    state: WfaRequestRecommendationState,
    currentCoordinate: GeoCoordinate?,
    hasPreciseLocationPermission: Boolean,
    onRecommendationSelected: (String) -> Unit,
    onRetry: () -> Unit,
    onSearchLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.md)
    ) {
        InfiniteSectionHeader(
            title = stringResource(R.string.wfa_recommendation_title),
            subtitle = stringResource(R.string.wfa_recommendation_supporting),
            leadingIcon = Icons.Outlined.LocationOn
        )
        when (state) {
            WfaRequestRecommendationState.Initializing,
            WfaRequestRecommendationState.Loading -> InfiniteLoadingState(
                message = stringResource(R.string.wfa_recommendation_loading),
                modifier = Modifier.testTag("wfaRecommendationLoading")
            )
            WfaRequestRecommendationState.Empty -> InfiniteEmptyState(
                title = stringResource(R.string.wfa_recommendation_empty_title),
                message = stringResource(R.string.wfa_recommendation_empty_message),
                modifier = Modifier.testTag("wfaRecommendationEmpty")
            )
            is WfaRequestRecommendationState.Failure -> InfiniteErrorState(
                title = stringResource(R.string.wfa_recommendation_failure_title),
                message = stringResource(WfaRequestUiMapper.map(state.failure).messageRes),
                actionLabel = if (state.retryable) stringResource(R.string.wfa_request_retry) else null,
                onAction = if (state.retryable) onRetry else null,
                modifier = Modifier.testTag("wfaRecommendationFailure")
            )
            is WfaRequestRecommendationState.Content -> RecommendationContent(
                state = state,
                currentCoordinate = currentCoordinate,
                hasPreciseLocationPermission = hasPreciseLocationPermission,
                onRecommendationSelected = onRecommendationSelected
            )
        }
        InfiniteButton(
            text = stringResource(R.string.wfa_recommendation_search_fallback),
            onClick = onSearchLocation,
            variant = InfiniteButtonVariant.Outlined,
            leadingIcon = Icons.Outlined.Search,
            fullWidth = true,
            modifier = Modifier.fillMaxWidth().testTag("wfaSearchFallback")
        )
    }
}

@Composable
private fun RecommendationContent(
    state: WfaRequestRecommendationState.Content,
    currentCoordinate: GeoCoordinate?,
    hasPreciseLocationPermission: Boolean,
    onRecommendationSelected: (String) -> Unit
) {
    val mapState = remember(state, currentCoordinate, hasPreciseLocationPermission) {
        WfaRequestMapUiMapper.map(currentCoordinate, state, hasPreciseLocationPermission)
    }
    val coordinates = remember(mapState.markers) { mapState.markers.map { it.coordinate } }
    val cameraEffect = remember(coordinates) {
        MapCameraEffect.Fit(id = coordinates.hashCode().toLong(), coordinates = coordinates)
    }
    InfiniteCard(modifier = Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(220.dp).testTag("wfaRecommendationMap")) {
            AttendanceMap(
                state = mapState,
                cameraEffect = cameraEffect,
                onEvent = { _: AttendanceMapEvent -> Unit },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
    state.recommendations.forEach { recommendation ->
        RecommendationCard(
            recommendation = recommendation,
            selected = recommendation.stableKey == state.selectedKey,
            onClick = { onRecommendationSelected(recommendation.stableKey) }
        )
    }
}

@Composable
private fun RecommendationCard(
    recommendation: WfaRecommendation,
    selected: Boolean,
    onClick: () -> Unit
) {
    InfiniteCard(
        modifier = Modifier.fillMaxWidth().testTag("wfaRecommendationCard-${recommendation.stableKey}"),
        semantic = if (selected) InfiniteSemantic.Primary else InfiniteSemantic.Neutral,
        selected = selected,
        clickable = true,
        onClick = onClick
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.sm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.sm),
                verticalAlignment = Alignment.Top
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = recommendation.name,
                        style = body1,
                        fontWeight = FontWeight.Bold,
                        color = InfiniteColors.Text
                    )
                    Text(
                        text = recommendation.address,
                        style = body2,
                        color = InfiniteColors.Text.copy(alpha = 0.72f)
                    )
                }
                if (selected) {
                    InfiniteStatusPill(
                        label = stringResource(R.string.wfa_recommendation_selected),
                        variant = InfiniteStatusVariant.Active,
                        size = InfiniteSize.Small
                    )
                }
            }
            Text(
                text = stringResource(
                    R.string.wfa_recommendation_distance,
                    recommendation.distanceMeters.value
                ),
                style = body2,
                color = InfiniteColors.Text.copy(alpha = 0.72f)
            )
            RecommendationStatus(recommendation)
        }
    }
}

@Composable
private fun RecommendationStatus(recommendation: WfaRecommendation) {
    when (recommendation.status) {
        WfaRecommendationStatus.Ranked -> {
            val score = recommendation.finalScore
            val label = recommendation.finalLabel
            if (score != null && label != null) {
                InfiniteStatusPill(
                    label = stringResource(
                        R.string.wfa_recommendation_ranked_score,
                        String.format(Locale.US, "%.0f", score),
                        label
                    ),
                    variant = InfiniteStatusVariant.Recommended,
                    modifier = Modifier.testTag("wfaRecommendationScore-${recommendation.stableKey}")
                )
            } else {
                StatusText(stringResource(R.string.wfa_recommendation_score_unavailable))
            }
        }
        WfaRecommendationStatus.InsufficientFacilityData ->
            StatusText(stringResource(R.string.wfa_recommendation_insufficient_data))
        WfaRecommendationStatus.FacilityEnrichmentFailed ->
            StatusText(stringResource(R.string.wfa_recommendation_enrichment_failed))
        is WfaRecommendationStatus.Unsupported ->
            StatusText(stringResource(R.string.wfa_recommendation_score_unavailable))
    }
}

@Composable
private fun StatusText(text: String) {
    Text(
        text = text,
        style = body2,
        color = InfiniteColors.Text.copy(alpha = 0.72f),
        modifier = Modifier.padding(top = InfiniteSpacing.Default.xs)
    )
}
