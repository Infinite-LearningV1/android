package com.example.infinite_track.presentation.screen.profile.details.about.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DesignServices
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Workspaces
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.infinite_track.domain.model.about.AboutCreator
import com.example.infinite_track.presentation.design.components.data.InfiniteSectionHeader
import com.example.infinite_track.presentation.design.tokens.InfiniteColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AboutCreatorCard(
    creator: AboutCreator,
    modifier: Modifier = Modifier
) {
    GlassSectionCard(modifier = modifier) {
        InfiniteSectionHeader(
            title = "Created By",
            subtitle = "Creator and contribution",
            leadingIcon = Icons.Rounded.Person
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            CreatorAvatar()
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = creator.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = InfiniteColors.AccountHubTitle
                )
                Text(
                    text = creator.role,
                    style = MaterialTheme.typography.bodyMedium,
                    color = InfiniteColors.Primary
                )
                CreatorInfo(icon = Icons.Rounded.School, text = creator.university)
                CreatorInfo(icon = Icons.Rounded.Workspaces, text = creator.program)
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = InfiniteColors.Surface.copy(alpha = 0.46f),
            border = BorderStroke(1.dp, InfiniteColors.Surface.copy(alpha = 0.78f))
        ) {
            Text(
                modifier = Modifier.padding(16.dp),
                text = creator.contribution,
                style = MaterialTheme.typography.bodyMedium,
                color = InfiniteColors.AccountHubBodyText
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            creator.skillTags.forEachIndexed { index, tag ->
                SkillChip(
                    text = tag,
                    icon = when (index % 3) {
                        0 -> Icons.Rounded.Code
                        1 -> Icons.Rounded.Storage
                        else -> Icons.Rounded.DesignServices
                    }
                )
            }
        }
    }
}

@Composable
private fun CreatorAvatar() {
    Surface(
        modifier = Modifier.size(64.dp),
        shape = CircleShape,
        color = InfiniteColors.Surface.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, InfiniteColors.AttendanceReportGlassBorder)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Person,
                contentDescription = null,
                tint = InfiniteColors.Primary,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
private fun CreatorInfo(
    icon: ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = InfiniteColors.AccountHubMutedText,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = InfiniteColors.AccountHubBodyText
        )
    }
}

@Composable
private fun SkillChip(
    text: String,
    icon: ImageVector
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = InfiniteColors.Surface.copy(alpha = 0.58f),
        border = BorderStroke(1.dp, InfiniteColors.Surface.copy(alpha = 0.86f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (text.contains("Fuzzy")) InfiniteColors.AboutCyan else InfiniteColors.Primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = InfiniteColors.AccountHubTitle
            )
        }
    }
}
