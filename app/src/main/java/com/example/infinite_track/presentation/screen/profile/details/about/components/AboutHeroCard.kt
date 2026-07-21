package com.example.infinite_track.presentation.screen.profile.details.about.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.infinite_track.R
import com.example.infinite_track.domain.model.about.AboutHero
import com.example.infinite_track.presentation.design.tokens.InfiniteColors

@Composable
fun AboutHeroCard(
    hero: AboutHero,
    modifier: Modifier = Modifier
) {
    GlassSectionCard(
        modifier = modifier,
        cornerRadius = 30.dp,
        contentPadding = 20.dp,
        glowBrush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.78f),
                InfiniteColors.AboutPurpleSoft.copy(alpha = 0.10f),
                InfiniteColors.AboutCyanSoft.copy(alpha = 0.14f)
            )
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeroLogoBadge()
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = hero.appName,
                    style = MaterialTheme.typography.headlineSmall,
                    color = InfiniteColors.AccountHubTitle,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = hero.tagline,
                    style = MaterialTheme.typography.titleSmall,
                    color = InfiniteColors.AboutPurple,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = hero.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = InfiniteColors.AccountHubBodyText
                )
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = InfiniteColors.AboutPurple.copy(alpha = 0.10f),
                    border = BorderStroke(1.dp, InfiniteColors.AboutPurple.copy(alpha = 0.18f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WorkspacePremium,
                            contentDescription = null,
                            tint = InfiniteColors.AboutPurple,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = hero.badgeLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = InfiniteColors.AboutPurple,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroLogoBadge() {
    Box(
        modifier = Modifier
            .size(92.dp)
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = InfiniteColors.AboutPurpleSoft.copy(alpha = 0.28f),
                spotColor = InfiniteColors.AboutCyan.copy(alpha = 0.24f)
            )
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.96f),
                        InfiniteColors.AboutPurpleSoft.copy(alpha = 0.18f),
                        InfiniteColors.AboutCyanSoft.copy(alpha = 0.22f)
                    )
                )
            )
            .background(Color.White.copy(alpha = 0.82f)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(78.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Color.White.copy(alpha = 0.94f))
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.logo),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.Fit
            )
        }
    }
}
