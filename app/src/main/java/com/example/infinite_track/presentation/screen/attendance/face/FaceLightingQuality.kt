package com.example.infinite_track.presentation.screen.attendance.face

internal enum class LightingQuality {
    ACCEPTABLE,
    LOW_LIGHT
}

internal object FaceLightingQuality {
    private const val MIN_AVERAGE_LUMINANCE = 55.0
    private const val MIN_BRIGHT_PIXEL_RATIO = 0.12
    private const val BRIGHT_PIXEL_LUMINANCE = 72.0

    fun evaluate(pixels: IntArray): LightingQuality {
        if (pixels.isEmpty()) return LightingQuality.ACCEPTABLE

        var totalLuminance = 0.0
        var brightPixels = 0

        pixels.forEach { color ->
            val luminance = color.luminance()
            totalLuminance += luminance
            if (luminance >= BRIGHT_PIXEL_LUMINANCE) {
                brightPixels += 1
            }
        }

        val averageLuminance = totalLuminance / pixels.size
        val brightPixelRatio = brightPixels.toDouble() / pixels.size

        return if (
            averageLuminance < MIN_AVERAGE_LUMINANCE &&
            brightPixelRatio < MIN_BRIGHT_PIXEL_RATIO
        ) {
            LightingQuality.LOW_LIGHT
        } else {
            LightingQuality.ACCEPTABLE
        }
    }

    private fun Int.luminance(): Double {
        val red = this shr 16 and 0xFF
        val green = this shr 8 and 0xFF
        val blue = this and 0xFF
        return 0.299 * red + 0.587 * green + 0.114 * blue
    }
}
