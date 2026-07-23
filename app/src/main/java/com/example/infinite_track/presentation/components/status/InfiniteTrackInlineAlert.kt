package com.example.infinite_track.presentation.components.status
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.infinite_track.presentation.design.components.status.InfiniteInlineAlertDuration
import com.example.infinite_track.presentation.design.components.status.InfiniteInlineAlert
import com.example.infinite_track.presentation.design.components.status.defaultInlineAlertDuration

@Composable
fun InfiniteTrackInlineAlert(
    status: StatusStateSpec,
    message: String,
    modifier: Modifier = Modifier,
    title: String? = status.value,
    onDismiss: (() -> Unit)? = null,
    duration: InfiniteInlineAlertDuration = status.toInfiniteSemantic().defaultInlineAlertDuration()
) = InfiniteInlineAlert(
    title = title.orEmpty(),
    message = message,
    semantic = status.toInfiniteSemantic(),
    modifier = modifier,
    onDismiss = onDismiss,
    duration = duration
)
