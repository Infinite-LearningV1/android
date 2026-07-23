package com.example.infinite_track.presentation.components.status

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.infinite_track.presentation.design.tokens.InfiniteSemantic
import com.example.infinite_track.presentation.design.tokens.infiniteFeedbackPalette

@Immutable
data class StatusStateTokens(
    val containerColor: Color,
    val contentColor: Color,
    val borderColor: Color,
    val iconTint: Color,
    val icon: ImageVector
)

@Composable
fun rememberStatusStateTokens(status: StatusStateSpec): StatusStateTokens {
    val semantic = status.toInfiniteSemantic()
    val palette = infiniteFeedbackPalette(semantic)
    return StatusStateTokens(
        containerColor = palette.surface,
        contentColor = palette.content,
        borderColor = palette.border,
        iconTint = palette.accent,
        icon = when (semantic) {
            InfiniteSemantic.Success -> Icons.Filled.CheckCircle
            InfiniteSemantic.Warning -> Icons.Filled.Warning
            InfiniteSemantic.Error -> Icons.Filled.Error
            else -> Icons.Filled.Info
        }
    )
}

object StatusStates {
    val Success = StatusStateSpec(id = "success", value = "Success")
    val Error = StatusStateSpec(id = "error", value = "Error")
    val Warning = StatusStateSpec(id = "warning", value = "Warning")
    val Info = StatusStateSpec(id = "info", value = "Info")
}

fun StatusStateSpec.toInfiniteSemantic(): InfiniteSemantic = when (id.trim().lowercase()) {
    "success" -> InfiniteSemantic.Success
    "warning" -> InfiniteSemantic.Warning
    "error" -> InfiniteSemantic.Error
    else -> InfiniteSemantic.Info
}
