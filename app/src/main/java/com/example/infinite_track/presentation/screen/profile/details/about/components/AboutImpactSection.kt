package com.example.infinite_track.presentation.screen.profile.details.about.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.PhoneIphone
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.infinite_track.domain.model.about.AboutImpactItem
import com.example.infinite_track.domain.model.about.AboutImpactSemantic
import com.example.infinite_track.presentation.design.components.data.InfiniteSectionHeader
import com.example.infinite_track.presentation.design.tokens.InfiniteColors

@Composable
fun AboutImpactSection(
    impacts: List<AboutImpactItem>,
    modifier: Modifier = Modifier
) {
    GlassSectionCard(modifier = modifier) {
        InfiniteSectionHeader(
            title = "Project Impact",
            subtitle = "Expected product value",
            leadingIcon = Icons.Rounded.Analytics
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            impacts.forEach { impact ->
                ImpactCard(impact = impact)
            }
        }
    }
}

@Composable
private fun ImpactCard(
    impact: AboutImpactItem
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = InfiniteColors.Surface.copy(alpha = 0.58f),
        border = BorderStroke(1.dp, InfiniteColors.Surface.copy(alpha = 0.84f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = impact.semantic.icon,
                contentDescription = null,
                tint = impact.semantic.tint,
                modifier = Modifier.size(20.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = impact.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = InfiniteColors.AccountHubTitle
                )
                Text(
                    text = impact.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = InfiniteColors.AccountHubBodyText
                )
            }
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
        AboutImpactSemantic.DecisionSupport -> InfiniteColors.AboutPurple
    }
