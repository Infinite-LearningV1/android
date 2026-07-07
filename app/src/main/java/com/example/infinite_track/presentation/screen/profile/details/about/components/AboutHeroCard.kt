package com.example.infinite_track.presentation.screen.profile.details.about.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.infinite_track.domain.model.about.AboutHero
import com.example.infinite_track.presentation.core.headline2
import com.example.infinite_track.presentation.core.headline4
import com.example.infinite_track.presentation.design.components.status.InfiniteStatusPill
import com.example.infinite_track.presentation.design.components.status.InfiniteStatusVariant
import com.example.infinite_track.presentation.design.tokens.InfiniteColors
import com.example.infinite_track.presentation.design.tokens.InfiniteSize

@Composable
fun AboutHeroCard(
    hero: AboutHero,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        color = InfiniteColors.Surface.copy(alpha = 0.60f),
        border = BorderStroke(1.dp, InfiniteColors.Surface.copy(alpha = 0.88f)),
        shadowElevation = 10.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(InfiniteColors.GlassGradient)
                .padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(148.dp)
                    .clip(RoundedCornerShape(topStart = 90.dp, topEnd = 40.dp, bottomStart = 48.dp, bottomEnd = 30.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                InfiniteColors.AboutCyanSoft.copy(alpha = 0.56f),
                                InfiniteColors.AboutCyanSoft.copy(alpha = 0.20f),
                                InfiniteColors.Surface.copy(alpha = 0.06f)
                            )
                        )
                    )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(22.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HeroIconBadge()
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = hero.appName,
                        style = headline2,
                        color = InfiniteColors.AccountHubTitle,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = hero.tagline,
                        style = headline4,
                        color = InfiniteColors.AboutPurple,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = hero.description,
                        style = headline4,
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
}

@Composable
private fun HeroIconBadge() {
    Box(
        modifier = Modifier
            .size(128.dp)
            .clip(CircleShape)
            .background(
                Brush.sweepGradient(
                    colors = listOf(
                        InfiniteColors.AboutPurpleSoft.copy(alpha = 0.72f),
                        InfiniteColors.AboutCyan.copy(alpha = 0.62f),
                        InfiniteColors.Surface.copy(alpha = 0.92f),
                        InfiniteColors.AboutPurpleSoft.copy(alpha = 0.72f)
                    )
                )
            )
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.size(104.dp),
            shape = CircleShape,
            color = InfiniteColors.Surface.copy(alpha = 0.76f),
            border = BorderStroke(1.dp, InfiniteColors.Surface.copy(alpha = 0.92f)),
            shadowElevation = 10.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Surface(
                    modifier = Modifier.size(74.dp),
                    shape = RoundedCornerShape(22.dp),
                    color = InfiniteColors.Surface.copy(alpha = 0.92f),
                    shadowElevation = 8.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Key,
                            contentDescription = null,
                            tint = InfiniteColors.AboutPurple,
                            modifier = Modifier.size(42.dp)
                        )
                    }
                }
            }
        }
    }
}
