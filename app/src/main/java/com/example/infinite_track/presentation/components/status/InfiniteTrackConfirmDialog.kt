package com.example.infinite_track.presentation.components.status
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.infinite_track.presentation.design.components.status.InfiniteConfirmDialogContent

@Composable
fun InfiniteTrackConfirmDialog(
    status: StatusStateSpec,
    title: String,
    message: String,
    showDialog: Boolean,
    modifier: Modifier = Modifier,
    confirmText: String = "Confirm",
    cancelText: String = "Cancel",
    isDestructive: Boolean = false,
    confirmLoading: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) = InfiniteConfirmDialogContent(
    title = title,
    message = message,
    semantic = status.toInfiniteSemantic(),
    showDialog = showDialog,
    modifier = modifier,
    confirmText = confirmText,
    cancelText = cancelText,
    onDismiss = onDismiss,
    onConfirm = onConfirm,
    isDestructive = isDestructive,
    confirmLoading = confirmLoading
)
