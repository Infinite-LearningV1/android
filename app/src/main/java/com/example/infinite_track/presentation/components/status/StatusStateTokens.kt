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
import com.example.infinite_track.presentation.theme.Blue_50
import com.example.infinite_track.presentation.theme.Blue_500
import com.example.infinite_track.presentation.theme.Blue_700
import com.example.infinite_track.presentation.theme.Blue_Info
import com.example.infinite_track.presentation.theme.Green_Success
import com.example.infinite_track.presentation.theme.Orange_50
import com.example.infinite_track.presentation.theme.Purple_500
import com.example.infinite_track.presentation.theme.Red_Error
import com.example.infinite_track.presentation.theme.Yellow_Warning
import com.example.infinite_track.presentation.design.tokens.InfiniteSemantic

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
    return when (status.id.trim().lowercase()) {
        "success" -> StatusStateTokens(
            containerColor = Green_Success.copy(alpha = 0.14f),
            contentColor = Purple_500,
            borderColor = Green_Success,
            iconTint = Green_Success,
            icon = Icons.Filled.CheckCircle
        )

        "error" -> StatusStateTokens(
            containerColor = Red_Error.copy(alpha = 0.12f),
            contentColor = Purple_500,
            borderColor = Red_Error,
            iconTint = Red_Error,
            icon = Icons.Filled.Error
        )

        "warning" -> StatusStateTokens(
            containerColor = Orange_50,
            contentColor = Purple_500,
            borderColor = Yellow_Warning,
            iconTint = Yellow_Warning,
            icon = Icons.Filled.Warning
        )

        "info" -> infoTokens()
        else -> infoTokens()
    }
}

private fun infoTokens(): StatusStateTokens {
    return StatusStateTokens(
        containerColor = Blue_50,
        contentColor = Blue_700,
        borderColor = Blue_500.copy(alpha = 0.35f),
        iconTint = Blue_Info,
        icon = Icons.Filled.Info
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
