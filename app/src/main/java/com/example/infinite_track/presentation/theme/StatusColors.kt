package com.example.infinite_track.presentation.theme

import androidx.compose.ui.graphics.Color
import java.util.Locale

fun requestStatusColor(statusKey: String?): Color {
    return when (statusKey.orEmpty().lowercase(Locale.getDefault())) {
        "approved" -> Status_Approved
        "rejected" -> Status_Rejected
        "pending" -> Status_Pending
        else -> Status_Default
    }
}
