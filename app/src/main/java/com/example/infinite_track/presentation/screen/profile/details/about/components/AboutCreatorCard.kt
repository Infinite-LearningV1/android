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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DesignServices
import androidx.compose.material.icons.rounded.Functions
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.infinite_track.domain.model.about.AboutCreator
import com.example.infinite_track.presentation.design.tokens.InfiniteColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AboutCreatorCard(
    creator: AboutCreator,
    modifier: Modifier = Modifier
) {
    GlassSectionCard(modifier = modifier) {
        AboutSectionTitle(
            title = "Created By",
            icon = Icons.Rounded.Person,
            tint = InfiniteColors.AboutPurple
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1.05f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CreatorAvatar()
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = creator.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = InfiniteColors.AccountHubTitle,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = creator.role,
                            style = MaterialTheme.typography.bodySmall,
                            color = InfiniteColors.AboutPurple,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                CreatorInfo(icon = Icons.Rounded.School, text = creator.university)
                CreatorInfo(icon = Icons.Rounded.Workspaces, text = creator.program)
            }
            Text(
                modifier = Modifier.weight(1f),
                text = creator.contribution,
                style = MaterialTheme.typography.bodySmall,
                color = InfiniteColors.AccountHubBodyText
            )
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            creator.skillTags.forEachIndexed { index, tag ->
                val icon = when {
                    tag.contains("Android", ignoreCase = true) -> Icons.Rounded.Android
                    tag.contains("Backend", ignoreCase = true) || tag.contains("API", ignoreCase = true) -> Icons.Rounded.Storage
                    tag.contains("UI", ignoreCase = true) || tag.contains("Design", ignoreCase = true) -> Icons.Rounded.DesignServices
                    tag.contains("Fuzzy", ignoreCase = true) || tag.contains("AHP", ignoreCase = true) -> Icons.Rounded.Functions
                    tag.contains("Attendance", ignoreCase = true) -> Icons.Rounded.CalendarMonth
                    else -> Icons.Rounded.Code
                }
                val tint = if (index % 2 == 0) InfiniteColors.AboutPurple else InfiniteColors.AboutCyan
                AboutPillChip(label = tag, icon = icon, tint = tint)
            }
        }
    }
}

@Composable
private fun CreatorAvatar() {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.98f),
                        InfiniteColors.AboutPurpleSoft.copy(alpha = 0.22f),
                        InfiniteColors.AboutCyanSoft.copy(alpha = 0.18f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            color = InfiniteColors.AboutGlassSurfaceStrong,
            border = BorderStroke(1.dp, InfiniteColors.AboutGlassBorderStrong),
            shadowElevation = 4.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = null,
                    tint = InfiniteColors.AboutCreatorAvatar,
                    modifier = Modifier.size(28.dp)
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
            modifier = Modifier.size(15.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = InfiniteColors.AccountHubBodyText
        )
    }
}
