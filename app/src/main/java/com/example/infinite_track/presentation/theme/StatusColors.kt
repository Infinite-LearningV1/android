package com.example.infinite_track.presentation.theme

import androidx.compose.ui.graphics.Color
import com.example.infinite_track.presentation.design.components.status.InfiniteStatusVariant
import com.example.infinite_track.presentation.design.tokens.InfiniteColors
import java.util.Locale

/**
 * Shared status color for WFA request badges and other request-like statuses.
 */
fun requestStatusColor(statusKey: String?): Color {
    return when (statusKey.orEmpty().lowercase(Locale.getDefault())) {
        "approved" -> Status_Approved
        "rejected" -> Status_Rejected
        "pending" -> Status_Pending
        else -> Status_Default
    }
}

/**
 * Shared attendance mode colors for mode bars + timeline markers/icons.
 * Keep WFO/WFA/WFH mapping in one place only.
 */
fun attendanceModeColor(mode: String?): Color {
    val normalized = mode.orEmpty().lowercase(Locale.getDefault())
    return when {
        normalized.contains("wfh") || normalized.contains("home") -> InfiniteColors.Secondary
        normalized.contains("wfa") || normalized.contains("anywhere") -> InfiniteColors.Accent
        normalized.contains("wfo") || normalized.contains("office") -> InfiniteColors.Primary
        else -> InfiniteColors.Primary
    }
}

/**
 * Shared attendance status badge color, aligned with WFA request badge palette.
 */
fun attendanceStatusColor(statusKey: String?): Color {
    val normalized = statusKey.orEmpty().lowercase(Locale.getDefault())
    return when {
        normalized.contains("ontime") ||
            normalized.contains("on_time") ||
            normalized.contains("on-time") ||
            normalized.contains("on time") ||
            normalized.contains("completed") ||
            normalized.contains("approved") -> Status_Approved

        normalized.contains("late") ||
            normalized.contains("early") ||
            normalized.contains("pending") ||
            normalized.contains("needs") -> Status_Pending

        normalized.contains("alpha") ||
            normalized.contains("absent") ||
            normalized.contains("reject") ||
            normalized.contains("error") -> Status_Rejected

        normalized.contains("active") -> Status_Default
        else -> Status_Default
    }
}

/**
 * Shared badge color for InfiniteStatusVariant, reusing WFA request badge palette.
 */
fun InfiniteStatusVariant.toAttendanceBadgeColor(): Color = when (this) {
    InfiniteStatusVariant.OnTime,
    InfiniteStatusVariant.Completed,
    InfiniteStatusVariant.Approved,
    InfiniteStatusVariant.Excellent,
    InfiniteStatusVariant.PdfReady -> Status_Approved

    InfiniteStatusVariant.Late,
    InfiniteStatusVariant.Early,
    InfiniteStatusVariant.Pending,
    InfiniteStatusVariant.NeedsReview -> Status_Pending

    InfiniteStatusVariant.Alpha,
    InfiniteStatusVariant.Rejected,
    InfiniteStatusVariant.Outside -> Status_Rejected

    InfiniteStatusVariant.Active,
    InfiniteStatusVariant.Recommended,
    InfiniteStatusVariant.Good,
    InfiniteStatusVariant.Inside,
    InfiniteStatusVariant.NotStarted,
    InfiniteStatusVariant.Unavailable,
    InfiniteStatusVariant.Unknown,
    InfiniteStatusVariant.Neutral -> Status_Default
}
