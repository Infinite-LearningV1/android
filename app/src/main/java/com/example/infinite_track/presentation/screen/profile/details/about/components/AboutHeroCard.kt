package com.example.infinite_track.presentation.screen.profile.details.about.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.infinite_track.domain.model.about.AboutHero
import com.example.infinite_track.presentation.design.components.data.InfiniteSectionHeader
import com.example.infinite_track.presentation.design.components.status.InfiniteStatusPill
import com.example.infinite_track.presentation.design.components.status.InfiniteStatusVariant
import com.example.infinite_track.presentation.design.tokens.InfiniteColors
import com.example.infinite_track.presentation.design.tokens.InfiniteSize

@Composable
fun AboutHeroCard(
    hero: AboutHero,
    modifier: Modifier = Modifier
) {
    GlassSectionCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Rounded.Key,
                contentDescription = null,
                tint = InfiniteColors.Primary,
                modifier = Modifier.size(26.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InfiniteSectionHeader(
                    title = hero.appName,
                    subtitle = hero.tagline,
                    leadingIcon = null
                )
                Text(
                    text = hero.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = InfiniteColors.AccountHubBodyText
                )
                InfiniteStatusPill(
                    label = hero.badgeLabel,
                    variant = InfiniteStatusVariant.Recommended,
                    size = InfiniteSize.Small,
                    leadingIcon = Icons.Rounded.AutoAwesome
                )
            }
        }
    }
}
