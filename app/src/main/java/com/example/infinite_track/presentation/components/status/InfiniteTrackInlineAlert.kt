package com.example.infinite_track.presentation.components.status
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.infinite_track.presentation.design.components.status.InfiniteInlineAlert
@Composable fun InfiniteTrackInlineAlert(status: StatusStateSpec, message: String, modifier: Modifier = Modifier, title: String? = status.value, onDismiss: (() -> Unit)? = null) = InfiniteInlineAlert(title.orEmpty(), message, status.toInfiniteSemantic(), modifier, onDismiss)
