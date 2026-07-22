package com.example.infinite_track.presentation.components.status
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.infinite_track.presentation.design.components.status.InfiniteConfirmDialogContent
@Composable fun InfiniteTrackConfirmDialog(status: StatusStateSpec, title: String, message: String, showDialog: Boolean, modifier: Modifier = Modifier, confirmText: String = "Confirm", cancelText: String = "Cancel", isDestructive: Boolean = false, onDismiss: () -> Unit, onConfirm: () -> Unit) = InfiniteConfirmDialogContent(title, message, status.toInfiniteSemantic(), showDialog, modifier, confirmText, cancelText, onDismiss, onConfirm, isDestructive)
