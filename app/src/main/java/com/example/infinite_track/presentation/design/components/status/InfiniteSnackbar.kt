package com.example.infinite_track.presentation.design.components.status

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.infinite_track.presentation.design.tokens.InfiniteFeedbackTypography
import com.example.infinite_track.presentation.design.tokens.InfiniteSemantic
import com.example.infinite_track.presentation.design.tokens.InfiniteSpacing
import com.example.infinite_track.presentation.design.tokens.infiniteFeedbackPalette
import com.example.infinite_track.presentation.theme.Infinite_TrackTheme
import kotlinx.coroutines.delay

@Immutable
data class InfiniteSnackbarVisuals(
    override val message: String,
    val semantic: InfiniteSemantic,
    override val actionLabel: String? = null,
    override val withDismissAction: Boolean = false,
    override val duration: SnackbarDuration = semantic.defaultSnackbarDuration(),
    val title: String? = null,
    val autoDismissTimeoutMillis: Long? = null
) : SnackbarVisuals

fun resolveSnackbarTimeoutMillis(
    baseTimeoutMillis: Long,
    recommendedTimeoutMillis: ((Long) -> Long)? = null
): Long {
    val recommendation = recommendedTimeoutMillis?.invoke(baseTimeoutMillis)
        ?: baseTimeoutMillis
    return recommendation.coerceAtLeast(baseTimeoutMillis)
}

fun InfiniteSemantic.defaultSnackbarDuration(): SnackbarDuration = when (this) {
    InfiniteSemantic.Warning,
    InfiniteSemantic.Error -> SnackbarDuration.Long
    InfiniteSemantic.Success,
    InfiniteSemantic.Info,
    InfiniteSemantic.Primary,
    InfiniteSemantic.Secondary,
    InfiniteSemantic.Neutral -> SnackbarDuration.Short
}

@Composable
fun InfiniteSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val accessibilityManager = LocalAccessibilityManager.current
    SnackbarHost(hostState = hostState, modifier = modifier) { data ->
        val visuals = data.visuals as? InfiniteSnackbarVisuals
        val baseTimeoutMillis = visuals?.autoDismissTimeoutMillis
        LaunchedEffect(data, baseTimeoutMillis, accessibilityManager) {
            if (baseTimeoutMillis != null) {
                val resolvedTimeoutMillis = resolveSnackbarTimeoutMillis(baseTimeoutMillis) { base ->
                    accessibilityManager?.calculateRecommendedTimeoutMillis(
                        originalTimeoutMillis = base,
                        containsIcons = false,
                        containsText = true,
                        containsControls = data.visuals.actionLabel != null ||
                            data.visuals.withDismissAction
                    ) ?: base
                }
                delay(resolvedTimeoutMillis)
                data.dismiss()
            }
        }
        InfiniteSnackbar(data = data)
    }
}

@Composable
fun InfiniteSnackbar(
    data: SnackbarData,
    modifier: Modifier = Modifier
) {
    val visuals = data.visuals as? InfiniteSnackbarVisuals
    val semantic = visuals?.semantic ?: InfiniteSemantic.Info
    val palette = infiniteFeedbackPalette(semantic)
    InfiniteFeedbackGlassSurface(
        semantic = semantic,
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }
    ) {
        Row(
            modifier = Modifier.padding(InfiniteSpacing.Default.lg),
            horizontalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.xs)
            ) {
                visuals?.title?.let { title ->
                    Text(
                        text = title,
                        color = palette.content,
                        style = InfiniteFeedbackTypography.snackbarTitle
                    )
                }
                Text(
                    text = data.visuals.message,
                    color = palette.content,
                    style = InfiniteFeedbackTypography.snackbarMessage
                )
                data.visuals.actionLabel?.let { actionLabel ->
                    TextButton(
                        onClick = data::performAction,
                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    ) {
                        Text(
                            text = actionLabel,
                            color = palette.content,
                            style = InfiniteFeedbackTypography.actionLabel
                        )
                    }
                }
            }
            if (data.visuals.withDismissAction) {
                IconButton(
                    onClick = data::dismiss,
                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup notifikasi",
                        tint = palette.content
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun InfiniteSnackbarPreview() {
    Infinite_TrackTheme {
        InfiniteSnackbar(
            data = PreviewSnackbarData(
                InfiniteSnackbarVisuals(
                    message = "Attendance saved successfully",
                    semantic = InfiniteSemantic.Success,
                    actionLabel = "Undo",
                    withDismissAction = true
                )
            )
        )
    }
}

private class PreviewSnackbarData(
    override val visuals: SnackbarVisuals
) : SnackbarData {
    override fun performAction() = Unit

    override fun dismiss() = Unit
}
