package com.example.infinite_track.presentation.screen.profile.details.about.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.infinite_track.domain.model.about.AboutCreator
import com.example.infinite_track.presentation.core.headline3
import com.example.infinite_track.presentation.core.headline4
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
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CreatorAvatar()
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = creator.name,
                    style = headline3,
                    color = InfiniteColors.AccountHubTitle,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = creator.role,
                    style = headline4,
                    color = InfiniteColors.AboutPurple,
                    fontWeight = FontWeight.Bold
                )
                CreatorInfo(icon = Icons.Rounded.School, text = creator.university)
                CreatorInfo(icon = Icons.Rounded.Workspaces, text = creator.program)
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = InfiniteColors.Surface.copy(alpha = 0.46f),
            border = BorderStroke(1.dp, InfiniteColors.Surface.copy(alpha = 0.78f))
        ) {
            Text(
                modifier = Modifier.padding(18.dp),
                text = creator.contribution,
                style = headline4,
                color = InfiniteColors.AccountHubBodyText
            )
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
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
    Box(
        modifier = Modifier
            .size(112.dp)
            .background(
                Brush.sweepGradient(
                    colors = listOf(
                        InfiniteColors.AboutPurpleSoft.copy(alpha = 0.42f),
                        InfiniteColors.Surface.copy(alpha = 0.84f),
                        InfiniteColors.AboutCyan.copy(alpha = 0.28f),
                        InfiniteColors.AboutPurpleSoft.copy(alpha = 0.42f)
                    )
                ),
                CircleShape
            )
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.size(92.dp),
            shape = CircleShape,
            color = InfiniteColors.Surface.copy(alpha = 0.78f),
            border = BorderStroke(1.dp, InfiniteColors.Surface.copy(alpha = 0.92f)),
            shadowElevation = 8.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = null,
                    tint = InfiniteColors.AboutCreatorAvatar,
                    modifier = Modifier.size(58.dp)
                )
            }
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
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = text,
            style = headline4,
            color = InfiniteColors.AccountHubBodyText,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SkillChip(
    text: String,
    icon: ImageVector
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = InfiniteColors.Surface.copy(alpha = 0.58f),
        border = BorderStroke(1.dp, InfiniteColors.Surface.copy(alpha = 0.86f)),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (text.contains("Fuzzy")) InfiniteColors.AboutCyan else InfiniteColors.AboutPurple,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = text,
                style = headline4,
                color = InfiniteColors.AccountHubTitle,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
