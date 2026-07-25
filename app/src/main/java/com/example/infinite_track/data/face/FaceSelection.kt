package com.example.infinite_track.data.face

/**
 * Pure face-selection logic shared by production and tests: pick the largest face by area
 * and report how many faces were present. Kept free of ML Kit types for JVM unit testing.
 */
object FaceSelection {
    data class Selection<T>(val primary: T, val totalFaces: Int)

    fun <T> select(faces: List<T>, area: (T) -> Int): Selection<T>? {
        val primary = faces.maxByOrNull(area) ?: return null
        return Selection(primary, faces.size)
    }
}
