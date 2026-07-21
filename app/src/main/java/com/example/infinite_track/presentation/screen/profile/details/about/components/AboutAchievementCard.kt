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
import androidx.compose.material.icons.outlined.MilitaryTech
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Star
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.infinite_track.domain.model.about.AboutAchievement
import com.example.infinite_track.presentation.design.tokens.InfiniteColors

@Composable
fun AboutAchievementCard(
    achievement: AboutAchievement,
    modifier: Modifier = Modifier
) {
    GlassSectionCard(
        modifier = modifier,
        cornerRadius = 30.dp,
        surfaceColor = InfiniteColors.AboutGoldSurface.copy(alpha = 0.72f),
        borderColor = InfiniteColors.AboutGoldBorder.copy(alpha = 0.70f),
        contentPadding = 20.dp,
        glowBrush = Brush.linearGradient(
            colors = listOf(
                InfiniteColors.AboutGoldPale.copy(alpha = 0.92f),
                InfiniteColors.AboutGoldSoft.copy(alpha = 0.48f),
                Color.White.copy(alpha = 0.28f)
            )
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TrophyBadge()
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = achievement.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = InfiniteColors.AboutGoldStrong,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MilitaryTech,
                        contentDescription = null,
                        tint = InfiniteColors.AboutGold,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = achievement.subtitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = InfiniteColors.AboutGoldStrong,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = achievement.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = InfiniteColors.AccountHubBodyText
                )
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.42f),
                    border = BorderStroke(1.dp, InfiniteColors.AboutGoldBorder.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Verified,
                            contentDescription = null,
                            tint = InfiniteColors.AboutGoldStrong,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = achievement.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = InfiniteColors.AboutGoldMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrophyBadge() {
    Box(
        modifier = Modifier
            .size(88.dp)
            .shadow(
                elevation = 14.dp,
                shape = CircleShape,
                ambientColor = InfiniteColors.AboutGoldGlow.copy(alpha = 0.45f),
                spotColor = InfiniteColors.AboutGold.copy(alpha = 0.35f)
            )
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.98f),
                        InfiniteColors.AboutGoldGlow.copy(alpha = 0.55f),
                        InfiniteColors.AboutGoldSoft.copy(alpha = 0.28f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.size(68.dp),
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.72f),
            border = BorderStroke(1.dp, InfiniteColors.AboutGoldBorder.copy(alpha = 0.55f)),
            shadowElevation = 0.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.EmojiEvents,
                    contentDescription = null,
                    tint = InfiniteColors.AboutGold,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
        Icon(
            imageVector = Icons.Rounded.Star,
            contentDescription = null,
            tint = InfiniteColors.AboutGoldGlow.copy(alpha = 0.85f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(16.dp)
        )
    }
}
