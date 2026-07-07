package com.example.infinite_track.presentation.design.components.state

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.infinite_track.presentation.components.loading.LoadingAnimation
import com.example.infinite_track.presentation.design.components.status.InfiniteInlineAlert
import com.example.infinite_track.presentation.design.tokens.InfiniteSemantic

@Composable
fun InfiniteEmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier
) {
    StateColumn(modifier = modifier) {
        Text(text = title, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Text(text = message, textAlign = TextAlign.Center)
    }
}

@Composable
fun InfiniteLoadingState(
    modifier: Modifier = Modifier,
    message: String = "Loading..."
) {
    StateColumn(modifier = modifier) {
        LoadingAnimation()
        Text(text = message, textAlign = TextAlign.Center)
    }
}

@Composable
fun InfiniteErrorState(
    title: String,
    message: String,
    modifier: Modifier = Modifier
) {
    InfiniteInlineAlert(
        title = title,
        message = message,
        semantic = InfiniteSemantic.Error,
        modifier = modifier
    )
}

@Composable
private fun StateColumn(
    modifier: Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        content()
    }
}
