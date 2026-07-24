package com.example.infinite_track.presentation.components.maps

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.infinite_track.domain.model.location.GeoCoordinate
import com.example.infinite_track.domain.model.wfa.WfaRecommendation
import com.example.infinite_track.presentation.design.tokens.infiniteSemanticColors
import com.example.infinite_track.presentation.screen.attendance.preparation.WfaSuitabilityPresentationMapper

/**
 * Composable for displaying WFA (Work From Anywhere) recommendation markers
 */
@Composable
fun MarkerViewWfa(
    recommendation: WfaRecommendation,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    GlowingCard(
        isGlowing = isPressed,
        onClick = {
            isPressed = !isPressed
            onClick()
        },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header with name and score
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = recommendation.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                ScoreChip(
                    score = recommendation.suitabilityScore,
                    label = recommendation.suitabilityLabel
                )
            }

            // Category
            Text(
                text = recommendation.category,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )

            // Address with location icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Location",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = recommendation.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Distance
            Text(
                text = "${String.format("%.1f", recommendation.distanceMeters.value / 1_000.0)} km away",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ScoreChip(
    score: Double,
    label: String,
    modifier: Modifier = Modifier
) {
    val presentation = WfaSuitabilityPresentationMapper.map(score)
    val colors = infiniteSemanticColors(presentation.semantic)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = colors.container,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            colors.border
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Score",
                tint = colors.accent,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = presentation.percentage.toString(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = colors.accent
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = colors.accent,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun GlowingCard(
    isGlowing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val animatedElevation by animateDpAsState(
        targetValue = if (isGlowing) 12.dp else 4.dp,
        label = "elevation"
    )

    val animatedAlpha by animateFloatAsState(
        targetValue = if (isGlowing) 0.15f else 0f,
        label = "glow"
    )

    Box(modifier = modifier) {
        // Glow effect background
        if (isGlowing) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = animatedAlpha),
                                Color.Transparent
                            ),
                            radius = 100f
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
            )
        }

        // Main card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .shadow(
                    elevation = animatedElevation,
                    shape = RoundedCornerShape(16.dp)
                ),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            content()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MarkerViewWfaPreview() {
    MaterialTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // High score recommendation
                MarkerViewWfa(
                    recommendation = WfaRecommendation(
                        name = "Starbucks Coffee",
                        address = "Jl. Sudirman No. 123, Jakarta Pusat",
                        coordinate = GeoCoordinate(-6.2088, 106.8456),
                        category = "Coffee Shop",
                        suitabilityScore = 0.91,
                        suitabilityLabel = "Sangat sesuai",
                        distanceMeters = com.example.infinite_track.domain.model.location.DistanceMeters(800.0),
                        stableKey = WfaRecommendation.stableKeyFor("Starbucks Coffee", -6.2088, 106.8456)
                    ),
                    onClick = { }
                )

                // Medium score recommendation
                MarkerViewWfa(
                    recommendation = WfaRecommendation(
                        name = "Co-Working Space Central",
                        address = "Plaza Indonesia, Jl. M.H. Thamrin Kav. 28-30",
                        coordinate = GeoCoordinate(-6.1944, 106.8229),
                        category = "Co-Working Space",
                        suitabilityScore = 0.67,
                        suitabilityLabel = "Cukup sesuai",
                        distanceMeters = com.example.infinite_track.domain.model.location.DistanceMeters(1500.0),
                        stableKey = WfaRecommendation.stableKeyFor("Co-Working Space Central", -6.1944, 106.8229)
                    ),
                    onClick = { }
                )

                // Low score recommendation
                MarkerViewWfa(
                    recommendation = WfaRecommendation(
                        name = "Warung Kopi Tradisional",
                        address = "Jl. Kebon Sirih Raya No. 45",
                        coordinate = GeoCoordinate(-6.1751, 106.8650),
                        category = "Traditional Cafe",
                        suitabilityScore = 0.42,
                        suitabilityLabel = "Kurang sesuai",
                        distanceMeters = com.example.infinite_track.domain.model.location.DistanceMeters(2300.0),
                        stableKey = WfaRecommendation.stableKeyFor("Warung Kopi Tradisional", -6.1751, 106.8650)
                    ),
                    onClick = { }
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Single Card Preview")
@Composable
fun SingleMarkerViewWfaPreview() {
    MaterialTheme {
        Surface(
            modifier = Modifier.padding(16.dp)
        ) {
            MarkerViewWfa(
                recommendation = WfaRecommendation(
                    name = "WeWork Menara Astra",
                    address = "Jl. Jenderal Sudirman Kav. 5-6, Jakarta Pusat, DKI Jakarta 10220",
                    coordinate = GeoCoordinate(-6.2088, 106.8456),
                    category = "Premium Co-Working",
                    suitabilityScore = 0.96,
                    suitabilityLabel = "Sangat sesuai",
                    distanceMeters = com.example.infinite_track.domain.model.location.DistanceMeters(500.0),
                    stableKey = WfaRecommendation.stableKeyFor("WeWork Menara Astra", -6.2088, 106.8456)
                ),
                onClick = { }
            )
        }
    }
}
