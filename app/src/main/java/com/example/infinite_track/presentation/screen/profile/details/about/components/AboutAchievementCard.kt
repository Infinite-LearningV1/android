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
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Star
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
import com.example.infinite_track.domain.model.about.AboutAchievement
import com.example.infinite_track.domain.model.about.VerificationStatus
import com.example.infinite_track.presentation.core.headline3
import com.example.infinite_track.presentation.core.headline4
import com.example.infinite_track.presentation.design.components.status.InfiniteStatusPill
import com.example.infinite_track.presentation.design.components.status.InfiniteStatusVariant
import com.example.infinite_track.presentation.design.tokens.InfiniteColors
import com.example.infinite_track.presentation.design.tokens.InfiniteSize

@Composable
fun AboutAchievementCard(
    achievement: AboutAchievement,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = InfiniteColors.AboutGoldSurface.copy(alpha = 0.62f),
        border = BorderStroke(1.dp, InfiniteColors.AboutGoldBorder.copy(alpha = 0.62f)),
        shadowElevation = 10.dp
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            InfiniteColors.AboutGoldPale.copy(alpha = 0.82f),
                            InfiniteColors.AboutGoldSoft.copy(alpha = 0.42f),
                            InfiniteColors.Surface.copy(alpha = 0.30f)
                        )
                    )
                )
                .padding(22.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Star,
                contentDescription = null,
                tint = InfiniteColors.AboutGoldGlow.copy(alpha = 0.72f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(34.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TrophyBadge()
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = achievement.title,
                        style = headline4,
                        color = InfiniteColors.AboutGoldStrong,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = achievement.subtitle,
                        style = headline3,
                        color = InfiniteColors.AboutGoldStrong,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = achievement.description,
                        style = headline4,
                        color = InfiniteColors.AccountHubBodyText
                    )
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = InfiniteColors.Surface.copy(alpha = 0.48f),
                        border = BorderStroke(1.dp, InfiniteColors.AboutGoldBorder.copy(alpha = 0.32f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Shield,
                                contentDescription = null,
                                tint = InfiniteColors.AboutGoldStrong,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = achievement.note,
                                style = headline4,
                                color = InfiniteColors.AboutGoldMuted,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    InfiniteStatusPill(
                        label = achievement.verificationStatus.label,
                        variant = achievement.verificationStatus.statusVariant,
                        size = InfiniteSize.Small
                    )
                }
            }
        }
    }
}

@Composable
private fun TrophyBadge() {
    Box(
        modifier = Modifier
            .size(112.dp)
            .clip(CircleShape)
            .background(
                Brush.sweepGradient(
                    colors = listOf(
                        InfiniteColors.AboutGoldGlow.copy(alpha = 0.92f),
                        InfiniteColors.Surface.copy(alpha = 0.92f),
                        InfiniteColors.AboutGoldLight.copy(alpha = 0.68f),
                        InfiniteColors.AboutGoldGlow.copy(alpha = 0.92f)
                    )
                )
            )
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.size(88.dp),
            shape = CircleShape,
            color = InfiniteColors.Surface.copy(alpha = 0.70f),
            border = BorderStroke(1.dp, InfiniteColors.AboutGoldBorder.copy(alpha = 0.50f)),
            shadowElevation = 8.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.EmojiEvents,
                    contentDescription = null,
                    tint = InfiniteColors.AboutGold,
                    modifier = Modifier.size(58.dp)
                )
            }
        }
    }
}

private val VerificationStatus.label: String
    get() = when (this) {
        VerificationStatus.PreviewOnly -> "Preview only"
        VerificationStatus.NeedsVerification -> "Needs verification"
        VerificationStatus.Verified -> "Verified"
    }

private val VerificationStatus.statusVariant: InfiniteStatusVariant
    get() = when (this) {
        VerificationStatus.PreviewOnly -> InfiniteStatusVariant.Pending
        VerificationStatus.NeedsVerification -> InfiniteStatusVariant.NeedsReview
        VerificationStatus.Verified -> InfiniteStatusVariant.Approved
    }
