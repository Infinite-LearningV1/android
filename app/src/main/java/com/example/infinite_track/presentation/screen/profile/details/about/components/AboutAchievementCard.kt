package com.example.infinite_track.presentation.screen.profile.details.about.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.infinite_track.domain.model.about.AboutAchievement
import com.example.infinite_track.domain.model.about.VerificationStatus
import com.example.infinite_track.presentation.design.components.data.InfiniteSectionHeader
import com.example.infinite_track.presentation.design.components.status.InfiniteStatusPill
import com.example.infinite_track.presentation.design.components.status.InfiniteStatusVariant
import com.example.infinite_track.presentation.design.tokens.InfiniteColors
import com.example.infinite_track.presentation.design.tokens.InfiniteSize

@Composable
fun AboutAchievementCard(
    achievement: AboutAchievement,
    modifier: Modifier = Modifier
) {
    GlassSectionCard(modifier = modifier) {
        InfiniteSectionHeader(
            title = achievement.title,
            subtitle = achievement.subtitle,
            leadingIcon = Icons.Rounded.EmojiEvents
        )
        Text(
            text = achievement.description,
            style = MaterialTheme.typography.bodyMedium,
            color = InfiniteColors.AccountHubBodyText
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = InfiniteColors.Surface.copy(alpha = 0.48f),
            border = BorderStroke(1.dp, InfiniteColors.AboutGoldBorder.copy(alpha = 0.32f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Rounded.Shield,
                    contentDescription = null,
                    tint = InfiniteColors.AboutGoldStrong,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = achievement.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = InfiniteColors.AboutGoldMuted
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
