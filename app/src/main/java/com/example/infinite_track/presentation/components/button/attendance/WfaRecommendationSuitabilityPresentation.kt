package com.example.infinite_track.presentation.components.button.attendance

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.infinite_track.R
import com.example.infinite_track.presentation.design.components.status.InfiniteStatusVariant
import com.example.infinite_track.presentation.design.tokens.InfiniteIcons
import com.example.infinite_track.presentation.design.tokens.InfiniteSemantic

internal data class WfaRecommendationSuitabilityPresentation(
    val variant: InfiniteStatusVariant,
    val icon: ImageVector,
    @StringRes val contentDescriptionRes: Int
)

internal object WfaRecommendationSuitabilityPresentationMapper {
    fun map(semantic: InfiniteSemantic): WfaRecommendationSuitabilityPresentation =
        when (semantic) {
            InfiniteSemantic.Success -> WfaRecommendationSuitabilityPresentation(
                variant = InfiniteStatusVariant.Excellent,
                icon = InfiniteIcons.Success,
                contentDescriptionRes = R.string.attendance_wfa_suitability_success_state
            )
            InfiniteSemantic.Warning -> WfaRecommendationSuitabilityPresentation(
                variant = InfiniteStatusVariant.NeedsReview,
                icon = InfiniteIcons.Warning,
                contentDescriptionRes = R.string.attendance_wfa_suitability_warning_state
            )
            InfiniteSemantic.Error -> WfaRecommendationSuitabilityPresentation(
                variant = InfiniteStatusVariant.Outside,
                icon = InfiniteIcons.Error,
                contentDescriptionRes = R.string.attendance_wfa_suitability_error_state
            )
            InfiniteSemantic.Info,
            InfiniteSemantic.Primary,
            InfiniteSemantic.Secondary,
            InfiniteSemantic.Neutral -> WfaRecommendationSuitabilityPresentation(
                variant = InfiniteStatusVariant.Recommended,
                icon = InfiniteIcons.Info,
                contentDescriptionRes = R.string.attendance_wfa_suitability_neutral_state
            )
        }
}
