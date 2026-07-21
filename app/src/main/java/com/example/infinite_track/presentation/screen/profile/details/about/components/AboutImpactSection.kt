package com.example.infinite_track.presentation.screen.profile.details.about.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.PhoneIphone
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.infinite_track.domain.model.about.AboutImpactItem
import com.example.infinite_track.domain.model.about.AboutImpactSemantic
import com.example.infinite_track.presentation.design.tokens.InfiniteColors

@Composable
fun AboutImpactSection(
    impacts: List<AboutImpactItem>,
    modifier: Modifier = Modifier
) {
    GlassSectionCard(modifier = modifier) {
        AboutSectionTitle(
            title = "Project Impact",
            icon = Icons.Rounded.Analytics,
            tint = InfiniteColors.AboutPurple
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            impacts.forEach { impact ->
                ImpactTile(
                    impact = impact,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ImpactTile(
    impact: AboutImpactItem,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = InfiniteColors.AboutGlassSurfaceStrong.copy(alpha = 0.70f),
        border = BorderStroke(1.dp, InfiniteColors.AboutGlassBorder),
        shadowElevation = 3.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AboutSoftIconBadge(
                icon = impact.semantic.icon,
                tint = impact.semantic.tint,
                size = 42.dp,
                iconSize = 22.dp,
                background = Color.White.copy(alpha = 0.78f)
            )
            Text(
                text = impact.title,
                style = MaterialTheme.typography.labelLarge,
                color = impact.semantic.tint,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Text(
                text = impact.description,
                style = MaterialTheme.typography.bodySmall,
                color = InfiniteColors.AccountHubBodyText,
                textAlign = TextAlign.Center
            )
        }
    }
}

private val AboutImpactSemantic.icon: ImageVector
    get() = when (this) {
        AboutImpactSemantic.Attendance -> Icons.Rounded.PhoneIphone
        AboutImpactSemantic.Visibility -> Icons.Rounded.Groups
        AboutImpactSemantic.DecisionSupport -> Icons.Rounded.TrackChanges
    }

private val AboutImpactSemantic.tint: Color
    get() = when (this) {
        AboutImpactSemantic.Attendance -> InfiniteColors.AboutPurple
        AboutImpactSemantic.Visibility -> InfiniteColors.AboutCyan
        AboutImpactSemantic.DecisionSupport -> InfiniteColors.AboutPurpleSoft
    }
