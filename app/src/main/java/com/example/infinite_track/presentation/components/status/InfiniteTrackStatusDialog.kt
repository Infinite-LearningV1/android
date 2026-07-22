package com.example.infinite_track.presentation.components.status
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.infinite_track.presentation.design.components.status.InfiniteStatusDialogContent
@Composable fun InfiniteTrackStatusDialog(status: StatusStateSpec, title: String, message: String, showDialog: Boolean, modifier: Modifier = Modifier, confirmText: String = "OK", @DrawableRes imageRes: Int? = null, onDismiss: () -> Unit, onConfirm: () -> Unit = onDismiss) = InfiniteStatusDialogContent(title, message, status.toInfiniteSemantic(), showDialog, modifier, confirmText, onDismiss, onConfirm, imageRes)
