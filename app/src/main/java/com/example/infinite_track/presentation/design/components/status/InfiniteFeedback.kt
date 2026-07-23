package com.example.infinite_track.presentation.design.components.status

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.infinite_track.presentation.design.tokens.InfiniteFeedbackTypography
import com.example.infinite_track.presentation.design.tokens.InfiniteMotion
import com.example.infinite_track.presentation.design.tokens.InfiniteSemantic
import com.example.infinite_track.presentation.design.tokens.InfiniteSpacing
import com.example.infinite_track.presentation.design.tokens.infiniteFeedbackPalette
import com.example.infinite_track.presentation.theme.White
import kotlinx.coroutines.delay

internal const val INLINE_ALERT_TIMER_TAG = "inlineAlertTimer"

enum class InfiniteInlineAlertDuration(internal val timeoutMillis: Int?) {
    Short(4_000),
    Long(8_000),
    Persistent(null)
}

fun InfiniteSemantic.defaultInlineAlertDuration(
    hasAction: Boolean = false
): InfiniteInlineAlertDuration {
    if (hasAction) return InfiniteInlineAlertDuration.Persistent
    return when (this) {
        InfiniteSemantic.Success,
        InfiniteSemantic.Info -> InfiniteInlineAlertDuration.Short
        InfiniteSemantic.Primary,
        InfiniteSemantic.Secondary,
        InfiniteSemantic.Warning,
        InfiniteSemantic.Error,
        InfiniteSemantic.Neutral -> InfiniteInlineAlertDuration.Persistent
    }
}

@Composable
fun InfiniteInlineAlert(
    title: String,
    message: String,
    semantic: InfiniteSemantic,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null,
    duration: InfiniteInlineAlertDuration = semantic.defaultInlineAlertDuration()
) {
    InfiniteInlineAlertContent(title, message, semantic, null, null, modifier, onDismiss, duration)
}

@Composable
fun InfiniteInlineAlert(
    title: String,
    message: String,
    semantic: InfiniteSemantic,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null,
    duration: InfiniteInlineAlertDuration = semantic.defaultInlineAlertDuration(hasAction = true)
) {
    InfiniteInlineAlertContent(title, message, semantic, actionLabel, onAction, modifier, onDismiss, duration)
}

@Composable
private fun InfiniteInlineAlertContent(
    title: String,
    message: String,
    semantic: InfiniteSemantic,
    actionLabel: String?,
    onAction: (() -> Unit)?,
    modifier: Modifier,
    onDismiss: (() -> Unit)?,
    duration: InfiniteInlineAlertDuration
) {
    val palette = infiniteFeedbackPalette(semantic)
    var visible by remember(title, message, semantic, duration) { mutableStateOf(true) }
    var dismissalRequested by remember(title, message, semantic, duration) { mutableStateOf(false) }
    val timerProgress = remember(title, message, semantic, duration) { Animatable(1f) }
    val currentOnDismiss by rememberUpdatedState(onDismiss)

    LaunchedEffect(title, message, semantic, duration) {
        timerProgress.snapTo(1f)
        duration.timeoutMillis?.let { timeout ->
            timerProgress.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = timeout,
                    easing = LinearEasing
                )
            )
            if (!dismissalRequested) {
                dismissalRequested = true
                visible = false
                delay(InfiniteMotion.ExitDurationMillis.toLong())
                currentOnDismiss?.invoke()
            }
        }
    }

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(InfiniteMotion.enterTween()) +
            expandVertically(InfiniteMotion.enterTween(), expandFrom = Alignment.Top),
        exit = fadeOut(InfiniteMotion.exitTween()) +
            shrinkVertically(InfiniteMotion.exitTween(), shrinkTowards = Alignment.Top)
    ) {
        InfiniteFeedbackGlassSurface(
            semantic = semantic,
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) { contentDescription = "$title. $message" }
        ) {
            Row(
                Modifier.padding(
                    horizontal = InfiniteSpacing.Default.md,
                    vertical = InfiniteSpacing.Default.sm
                ),
                horizontalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.sm),
                verticalAlignment = Alignment.Top
            ) {
                FeedbackIcon(semantic = semantic, size = 36.dp, iconSize = 20.dp)
                Column(
                    Modifier
                        .weight(1f)
                        .padding(top = 1.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(title, color = palette.content, style = InfiniteFeedbackTypography.inlineTitle)
                    Text(message, color = palette.supportingContent, style = InfiniteFeedbackTypography.inlineBody)
                    if (actionLabel != null && onAction != null) TextButton(actionLabel, onAction)
                }
                if (onDismiss != null || duration != InfiniteInlineAlertDuration.Persistent) {
                    IconButton(
                        onClick = {
                            if (!dismissalRequested) {
                                dismissalRequested = true
                                visible = false
                                currentOnDismiss?.invoke()
                            }
                        },
                        modifier = Modifier.sizeIn(48.dp, 48.dp)
                    ) {
                        Icon(Icons.Default.Close, "Dismiss alert", tint = palette.supportingContent)
                    }
                }
            }
            if (duration.timeoutMillis != null) {
                InlineAlertTimer(
                    progress = { timerProgress.value },
                    trackColor = palette.stateContainer,
                    progressColor = palette.accent
                )
            }
        }
    }
}

@Composable
private fun BoxScope.InlineAlertTimer(
    progress: () -> Float,
    trackColor: androidx.compose.ui.graphics.Color,
    progressColor: androidx.compose.ui.graphics.Color
) {
    Canvas(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .fillMaxWidth()
            .height(3.dp)
            .testTag(INLINE_ALERT_TIMER_TAG)
    ) {
        val radius = size.height / 2f
        drawRoundRect(
            color = trackColor,
            cornerRadius = CornerRadius(radius, radius)
        )
        drawRoundRect(
            color = progressColor,
            size = Size(
                width = size.width * progress().coerceIn(0f, 1f),
                height = size.height
            ),
            cornerRadius = CornerRadius(radius, radius)
        )
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
            if (imageRes != null) Image(painterResource(imageRes), null, Modifier.size(112.dp)) else FeedbackIcon(semantic)
            Spacer(Modifier.height(18.dp)); Text(title, style = InfiniteFeedbackTypography.dialogTitle, color = palette.content, textAlign = TextAlign.Center)
            Spacer(Modifier.height(10.dp)); Text(message, style = InfiniteFeedbackTypography.dialogBody, color = palette.supportingContent, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp)); Button(onConfirm, Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp), colors = ButtonDefaults.buttonColors(containerColor = infiniteFeedbackPalette(InfiniteSemantic.Primary).accent, contentColor = White)) { Text(confirmText, style = InfiniteFeedbackTypography.actionLabel) }
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
    Dialog(onDismissRequest = onDismiss) { InfiniteConfirmDialogBody(title, message, semantic, modifier, confirmText, cancelText, onDismiss, onConfirm, isDestructive) }
}

@Composable
internal fun InfiniteConfirmDialogBody(title: String, message: String, semantic: InfiniteSemantic, modifier: Modifier = Modifier, confirmText: String = "Confirm", cancelText: String = "Cancel", onDismiss: () -> Unit, onConfirm: () -> Unit, isDestructive: Boolean = false) {
    val palette = infiniteFeedbackPalette(semantic)
    InfiniteFeedbackGlassSurface(semantic, modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            FeedbackIcon(semantic); Spacer(Modifier.height(18.dp))
            Text(title, style = InfiniteFeedbackTypography.dialogTitle, color = palette.content, textAlign = TextAlign.Center); Spacer(Modifier.height(10.dp))
            Text(message, style = InfiniteFeedbackTypography.dialogBody, color = palette.supportingContent, textAlign = TextAlign.Center); Spacer(Modifier.height(24.dp))
            BoxWithConstraints {
                val fontScale = LocalDensity.current.fontScale
                val vertical = maxWidth < 360.dp || fontScale >= 1.5f
                if (vertical) Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { DialogButtons(cancelText, confirmText, onDismiss, onConfirm, isDestructive, false) }
                else Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { DialogButtons(cancelText, confirmText, onDismiss, onConfirm, isDestructive, true) }
            }
        }
    }
}

@Composable
private fun DialogButtons(cancelText: String, confirmText: String, onDismiss: () -> Unit, onConfirm: () -> Unit, destructive: Boolean, weighted: Boolean) {
    val modifier = if (weighted) Modifier.sizeIn(minHeight = 48.dp) else Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp)
    OutlinedButton(onDismiss, modifier) { Text(cancelText, style = InfiniteFeedbackTypography.actionLabel) }
    Button(onConfirm, modifier, colors = ButtonDefaults.buttonColors(containerColor = if (destructive) InfiniteSemantic.Error.let { infiniteFeedbackPalette(it).accent } else infiniteFeedbackPalette(InfiniteSemantic.Primary).accent, contentColor = White)) { Text(confirmText, style = InfiniteFeedbackTypography.actionLabel) }
}

@Composable
private fun FeedbackIcon(
    semantic: InfiniteSemantic,
    size: androidx.compose.ui.unit.Dp = 64.dp,
    iconSize: androidx.compose.ui.unit.Dp = 32.dp
) {
    val palette = infiniteFeedbackPalette(semantic)
    Surface(
        modifier = Modifier.size(size),
        shape = CircleShape,
        color = palette.stateContainer,
        contentColor = palette.accent
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = semanticIcon(semantic),
                contentDescription = null,
                modifier = Modifier.size(iconSize),
                tint = palette.accent
            )
        }
    }
}

internal fun semanticIcon(semantic: InfiniteSemantic) = when (semantic) {
    InfiniteSemantic.Success -> Icons.Default.CheckCircle
    InfiniteSemantic.Warning -> Icons.Default.Warning
    InfiniteSemantic.Error -> Icons.Default.Error
    InfiniteSemantic.Info, InfiniteSemantic.Primary, InfiniteSemantic.Secondary, InfiniteSemantic.Neutral -> Icons.Default.Info
}

internal fun statusDialogUsesIllustration(@DrawableRes imageRes: Int?): Boolean = imageRes != null
