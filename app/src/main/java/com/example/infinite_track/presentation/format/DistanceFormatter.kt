package com.example.infinite_track.presentation.format

import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

fun formatDistanceMeters(value: Double, locale: Locale): String {
    val formatter = NumberFormat.getNumberInstance(locale).apply {
        roundingMode = RoundingMode.HALF_UP
    }
    return if (value < 1_000.0) {
        formatter.minimumFractionDigits = 0
        formatter.maximumFractionDigits = 0
        "${formatter.format(value)} m"
    } else {
        formatter.minimumFractionDigits = 1
        formatter.maximumFractionDigits = 1
        "${formatter.format(value / 1_000.0)} km"
    }
}
