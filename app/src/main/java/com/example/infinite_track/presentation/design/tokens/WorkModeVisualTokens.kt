package com.example.infinite_track.presentation.design.tokens

import androidx.compose.ui.graphics.Color
import com.example.infinite_track.domain.model.attendance.WorkMode
import com.example.infinite_track.presentation.theme.Blue_500
import com.example.infinite_track.presentation.theme.Blue_Accent_500
import com.example.infinite_track.presentation.theme.Orange_500

object WorkModeVisualTokens {
    fun color(mode: WorkMode): Color = when (mode) {
        WorkMode.WFO -> Blue_500
        WorkMode.WFH -> Blue_Accent_500
        WorkMode.WFA -> Orange_500
    }
}
