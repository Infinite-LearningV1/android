package com.example.infinite_track.presentation.design.components.status

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.infinite_track.presentation.design.tokens.InfiniteFeedbackTypography
import com.example.infinite_track.presentation.design.tokens.InfiniteSemantic
import com.example.infinite_track.presentation.design.tokens.InfiniteSpacing
import com.example.infinite_track.presentation.design.tokens.infiniteFeedbackPalette
import com.example.infinite_track.presentation.theme.White

@Composable
fun InfiniteInlineAlert(title: String, message: String, semantic: InfiniteSemantic, modifier: Modifier = Modifier, onDismiss: (() -> Unit)? = null) {
    InfiniteInlineAlertContent(title, message, semantic, null, null, modifier, onDismiss)
}

@Composable
fun InfiniteInlineAlert(title: String, message: String, semantic: InfiniteSemantic, actionLabel: String, onAction: () -> Unit, modifier: Modifier = Modifier, onDismiss: (() -> Unit)? = null) {
    InfiniteInlineAlertContent(title, message, semantic, actionLabel, onAction, modifier, onDismiss)
}

@Composable
private fun InfiniteInlineAlertContent(title: String, message: String, semantic: InfiniteSemantic, actionLabel: String?, onAction: (() -> Unit)?, modifier: Modifier, onDismiss: (() -> Unit)?) {
    val palette = infiniteFeedbackPalette(semantic)
    InfiniteFeedbackGlassSurface(
        semantic = semantic,
        modifier = modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = "$title. $message" }
    ) {
        Row(Modifier.padding(InfiniteSpacing.Default.lg), horizontalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.sm), verticalAlignment = Alignment.Top) {
            Icon(Icons.Default.Error, null, tint = palette.accent, modifier = Modifier.size(22.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, color = palette.content, style = InfiniteFeedbackTypography.snackbarTitle)
                Text(message, color = palette.supportingContent, style = InfiniteFeedbackTypography.supportingBody)
                if (actionLabel != null && onAction != null) TextButton(actionLabel, onAction)
            }
            if (onDismiss != null) IconButton(onDismiss, Modifier.sizeIn(48.dp, 48.dp)) {
                Icon(Icons.Default.Close, "Dismiss alert", tint = palette.content)
            }
        }
    }
}

@Composable
private fun TextButton(label: String, onClick: () -> Unit) = androidx.compose.material3.TextButton(onClick, Modifier.sizeIn(48.dp, 48.dp)) {
    Text(label, style = InfiniteFeedbackTypography.actionLabel)
}

@Composable
fun InfiniteStatusDialog(title: String, message: String, semantic: InfiniteSemantic, showDialog: Boolean, modifier: Modifier = Modifier, confirmText: String = "OK", onDismiss: () -> Unit, onConfirm: () -> Unit = onDismiss) {
    InfiniteStatusDialogContent(title, message, semantic, showDialog, modifier, confirmText, onDismiss, onConfirm, null)
}

@Composable
internal fun InfiniteStatusDialogContent(title: String, message: String, semantic: InfiniteSemantic, showDialog: Boolean, modifier: Modifier, confirmText: String, onDismiss: () -> Unit, onConfirm: () -> Unit, @DrawableRes imageRes: Int?) {
    if (!showDialog) return
    val palette = infiniteFeedbackPalette(semantic)
    Dialog(onDismissRequest = onDismiss) { InfiniteFeedbackGlassSurface(semantic, modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (imageRes != null) Image(painterResource(imageRes), null, Modifier.size(112.dp)) else Icon(Icons.Default.Error, null, tint = palette.accent, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(18.dp)); Text(title, style = InfiniteFeedbackTypography.dialogTitle, color = palette.content, textAlign = TextAlign.Center)
            Spacer(Modifier.height(10.dp)); Text(message, style = InfiniteFeedbackTypography.dialogBody, color = palette.supportingContent, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp)); Button(onConfirm, Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp)) { Text(confirmText) }
        }
    }}
}

@Composable
fun InfiniteConfirmDialog(title: String, message: String, semantic: InfiniteSemantic, showDialog: Boolean, modifier: Modifier = Modifier, confirmText: String = "Confirm", cancelText: String = "Cancel", onDismiss: () -> Unit, onConfirm: () -> Unit) {
    InfiniteConfirmDialogContent(title, message, semantic, showDialog, modifier, confirmText, cancelText, onDismiss, onConfirm, semantic == InfiniteSemantic.Error)
}

@Composable
internal fun InfiniteConfirmDialogContent(title: String, message: String, semantic: InfiniteSemantic, showDialog: Boolean, modifier: Modifier, confirmText: String, cancelText: String, onDismiss: () -> Unit, onConfirm: () -> Unit, isDestructive: Boolean) {
    if (!showDialog) return
    val palette = infiniteFeedbackPalette(semantic)
    Dialog(onDismissRequest = onDismiss) { InfiniteFeedbackGlassSurface(semantic, modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Error, null, tint = palette.accent, modifier = Modifier.size(58.dp)); Spacer(Modifier.height(18.dp))
            Text(title, style = InfiniteFeedbackTypography.dialogTitle, color = palette.content, textAlign = TextAlign.Center); Spacer(Modifier.height(10.dp))
            Text(message, style = InfiniteFeedbackTypography.dialogBody, color = palette.supportingContent, textAlign = TextAlign.Center); Spacer(Modifier.height(24.dp))
            BoxWithConstraints { val vertical = maxWidth < 360.dp
                if (vertical) Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { DialogButtons(cancelText, confirmText, onDismiss, onConfirm, isDestructive, false) }
                else Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { DialogButtons(cancelText, confirmText, onDismiss, onConfirm, isDestructive, true) }
            }
        }
    }}
}

@Composable
private fun DialogButtons(cancelText: String, confirmText: String, onDismiss: () -> Unit, onConfirm: () -> Unit, destructive: Boolean, weighted: Boolean) {
    val modifier = if (weighted) Modifier.sizeIn(minHeight = 48.dp) else Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp)
    OutlinedButton(onDismiss, modifier) { Text(cancelText) }
    Button(onConfirm, modifier, colors = ButtonDefaults.buttonColors(containerColor = if (destructive) InfiniteSemantic.Error.let { infiniteFeedbackPalette(it).accent } else White, contentColor = if (destructive) White else infiniteFeedbackPalette(InfiniteSemantic.Primary).content)) { Text(confirmText) }
}
