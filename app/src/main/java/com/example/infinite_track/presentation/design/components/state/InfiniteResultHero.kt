package com.example.infinite_track.presentation.design.components.state

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.example.infinite_track.presentation.core.body1
import com.example.infinite_track.presentation.core.headline3
import com.example.infinite_track.presentation.design.components.status.InfiniteFeedbackGlassSurface
import com.example.infinite_track.presentation.design.components.status.semanticIcon
import com.example.infinite_track.presentation.design.tokens.InfiniteRadius
import com.example.infinite_track.presentation.design.tokens.InfiniteSemantic
import com.example.infinite_track.presentation.design.tokens.InfiniteSize
import com.example.infinite_track.presentation.design.tokens.InfiniteSpacing
import com.example.infinite_track.presentation.design.tokens.infiniteFeedbackPalette

@Composable
fun InfiniteResultHero(
    title: String,
    message: String,
    semantic: InfiniteSemantic,
    modifier: Modifier = Modifier
) {
    val palette = infiniteFeedbackPalette(semantic)
    val iconSurfaceSize = InfiniteSpacing.Default.xxl * 3
    val iconSize = InfiniteSpacing.Default.xl + InfiniteSpacing.Default.lg

    InfiniteFeedbackGlassSurface(
        semantic = semantic,
        modifier = modifier.fillMaxWidth(),
        shape = InfiniteRadius.shape(InfiniteSize.Large),
        showTopHighlight = true
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(InfiniteSpacing.Default.xxl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.md)
        ) {
            Surface(
                modifier = Modifier.size(iconSurfaceSize),
                shape = InfiniteRadius.pillShape(),
                color = palette.stateContainer,
                contentColor = palette.accent
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = semanticIcon(semantic),
                        contentDescription = null,
                        modifier = Modifier.size(iconSize),
                        tint = palette.accent
                    )
                }
            }
            Text(
                text = title,
                color = palette.content,
                style = headline3,
                textAlign = TextAlign.Center
            )
            Text(
                text = message,
                color = palette.supportingContent,
                style = body1,
                textAlign = TextAlign.Center
            )
        }
    }
}
