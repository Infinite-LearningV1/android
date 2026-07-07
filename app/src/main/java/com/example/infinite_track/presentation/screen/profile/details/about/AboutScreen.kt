package com.example.infinite_track.presentation.screen.profile.details.about

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.infinite_track.presentation.design.tokens.InfiniteColors
import com.example.infinite_track.presentation.screen.profile.details.about.components.AboutAchievementCard
import com.example.infinite_track.presentation.screen.profile.details.about.components.AboutCreatorCard
import com.example.infinite_track.presentation.screen.profile.details.about.components.AboutFooter
import com.example.infinite_track.presentation.screen.profile.details.about.components.AboutHeroCard
import com.example.infinite_track.presentation.screen.profile.details.about.components.AboutImpactSection
import com.example.infinite_track.presentation.screen.profile.details.about.components.AboutOverviewCard
import com.example.infinite_track.presentation.screen.profile.details.about.components.AboutTimelineSection
import com.example.infinite_track.presentation.screen.profile.details.about.components.AboutTopBar

@Composable
fun AboutScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AboutViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(InfiniteColors.AccountHubBackgroundGradient)
    ) {
        LiquidBackgroundDecor()
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = InfiniteColors.Transparent,
            topBar = { AboutTopBar(onBackClick = onBackClick) }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                uiState.content?.let { content ->
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 30.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item { AboutHeroCard(hero = content.hero) }
                        item { AboutOverviewCard(overview = content.overview) }
                        item { AboutTimelineSection(timeline = content.timeline) }
                        item { AboutCreatorCard(creator = content.creator) }
                        item { AboutAchievementCard(achievement = content.achievement) }
                        item { AboutImpactSection(impacts = content.impacts) }
                        item { AboutFooter(note = content.footerNote) }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiquidBackgroundDecor() {
    Box(
        modifier = Modifier
            .offset(x = 280.dp, y = (-22).dp)
            .size(150.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        InfiniteColors.Surface.copy(alpha = 0.82f),
                        InfiniteColors.AboutLavender.copy(alpha = 0.28f),
                        InfiniteColors.Transparent
                    )
                )
            )
    )
    Box(
        modifier = Modifier
            .offset(x = (-42).dp, y = 640.dp)
            .size(150.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        InfiniteColors.Surface.copy(alpha = 0.72f),
                        InfiniteColors.AboutPurpleSoft.copy(alpha = 0.22f),
                        InfiniteColors.Transparent
                    )
                )
            )
    )
}
