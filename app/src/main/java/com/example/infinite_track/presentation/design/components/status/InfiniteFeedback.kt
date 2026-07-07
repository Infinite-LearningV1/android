package com.example.infinite_track.presentation.design.components.status

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.infinite_track.presentation.components.status.InfiniteTrackConfirmDialog
import com.example.infinite_track.presentation.components.status.InfiniteTrackInlineAlert
import com.example.infinite_track.presentation.components.status.InfiniteTrackStatusDialog
import com.example.infinite_track.presentation.components.status.StatusStateSpec
import com.example.infinite_track.presentation.design.tokens.InfiniteSemantic

@Composable
fun InfiniteInlineAlert(
    title: String,
    message: String,
    semantic: InfiniteSemantic,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null
) {
    InfiniteTrackInlineAlert(
        status = semantic.toStatusStateSpec(),
        title = title,
        message = message,
        modifier = modifier,
        onDismiss = onDismiss
    )
}

@Composable
fun InfiniteStatusDialog(
    title: String,
    message: String,
    semantic: InfiniteSemantic,
    showDialog: Boolean,
    modifier: Modifier = Modifier,
    confirmText: String = "OK",
    onDismiss: () -> Unit,
    onConfirm: () -> Unit = onDismiss
) {
    InfiniteTrackStatusDialog(
        status = semantic.toStatusStateSpec(),
        title = title,
        message = message,
        showDialog = showDialog,
        modifier = modifier,
        confirmText = confirmText,
        onDismiss = onDismiss,
        onConfirm = onConfirm
    )
}

@Composable
fun InfiniteConfirmDialog(
    title: String,
    message: String,
    semantic: InfiniteSemantic,
    showDialog: Boolean,
    modifier: Modifier = Modifier,
    confirmText: String = "Confirm",
    cancelText: String = "Cancel",
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    InfiniteTrackConfirmDialog(
        status = semantic.toStatusStateSpec(),
        title = title,
        message = message,
        showDialog = showDialog,
        modifier = modifier,
        confirmText = confirmText,
        cancelText = cancelText,
        isDestructive = semantic == InfiniteSemantic.Error,
        onDismiss = onDismiss,
        onConfirm = onConfirm
    )
}

private fun InfiniteSemantic.toStatusStateSpec(): StatusStateSpec = when (this) {
    InfiniteSemantic.Success -> StatusStateSpec("success", "Success")
    InfiniteSemantic.Warning -> StatusStateSpec("warning", "Warning")
    InfiniteSemantic.Error -> StatusStateSpec("error", "Error")
    InfiniteSemantic.Info,
    InfiniteSemantic.Primary,
    InfiniteSemantic.Secondary,
    InfiniteSemantic.Neutral -> StatusStateSpec("info", "Info")
}
