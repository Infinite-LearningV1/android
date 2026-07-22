package com.example.infinite_track.presentation.design.components.state

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.infinite_track.presentation.components.loading.LoadingAnimation
import com.example.infinite_track.presentation.design.components.status.InfiniteInlineAlert
import com.example.infinite_track.presentation.design.components.status.InfiniteFeedbackGlassSurface
import com.example.infinite_track.presentation.design.tokens.InfiniteFeedbackTypography
import com.example.infinite_track.presentation.design.tokens.InfiniteSemantic

@Composable
fun InfiniteEmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    StateColumn(modifier = modifier) {
        Text(text = title, style = InfiniteFeedbackTypography.snackbarTitle, textAlign = TextAlign.Center)
        Text(text = message, style = InfiniteFeedbackTypography.supportingBody, textAlign = TextAlign.Center)
        StateAction(actionLabel, onAction)
    }
}

@Composable
fun InfiniteLoadingState(
    modifier: Modifier = Modifier,
    message: String = "Loading...",
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    StateColumn(modifier = modifier) {
        LoadingAnimation()
        Text(text = message, style = InfiniteFeedbackTypography.supportingBody, textAlign = TextAlign.Center)
        StateAction(actionLabel, onAction)
    }
}

@Composable
fun InfiniteErrorState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    if (actionLabel != null && onAction != null) InfiniteInlineAlert(title, message, InfiniteSemantic.Error, actionLabel, onAction, modifier)
    else InfiniteInlineAlert(title, message, InfiniteSemantic.Error, modifier)
}

@Composable
private fun StateColumn(
    modifier: Modifier,
    content: @Composable () -> Unit
) {
    InfiniteFeedbackGlassSurface(
        semantic = InfiniteSemantic.Neutral,
        modifier = modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = "Embedded state" }
    ) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        content()
    }
    }
}

@Composable
private fun StateAction(label: String?, onAction: (() -> Unit)?) {
    if (label != null && onAction != null) androidx.compose.material3.TextButton(onAction, Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) {
        Text(label, style = InfiniteFeedbackTypography.actionLabel)
    }
}
