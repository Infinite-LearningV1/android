package com.example.infinite_track.presentation.screen.profile.details.about.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.infinite_track.presentation.core.headline3
import com.example.infinite_track.presentation.design.tokens.InfiniteColors

@Composable
fun AboutTopBar(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        shape = RoundedCornerShape(28.dp),
        color = InfiniteColors.Surface.copy(alpha = 0.62f),
        border = BorderStroke(1.dp, InfiniteColors.Surface.copy(alpha = 0.86f)),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .background(InfiniteColors.GlassGradient)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.size(52.dp),
                shape = CircleShape,
                color = InfiniteColors.Surface.copy(alpha = 0.82f),
                border = BorderStroke(1.dp, InfiniteColors.AccountHubStrongOutline),
                shadowElevation = 6.dp
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowBackIosNew,
                        contentDescription = "Back",
                        tint = InfiniteColors.AccountHubTitle,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "About Infinite Track",
                    style = headline3,
                    color = InfiniteColors.AccountHubTitle,
                    fontWeight = FontWeight.Bold
                )
            }
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(InfiniteColors.AccountHubDecorativeTint)
            )
        }
    }
}
