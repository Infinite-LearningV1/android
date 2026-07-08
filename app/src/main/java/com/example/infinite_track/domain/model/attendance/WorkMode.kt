package com.example.infinite_track.domain.model.attendance

enum class WorkMode(
    val categoryId: Int,
    val shortLabel: String,
    val displayLabel: String
) {
    WFO(
        categoryId = 1,
        shortLabel = "WFO",
        displayLabel = "Work From Office"
    ),
    WFH(
        categoryId = 2,
        shortLabel = "WFH",
        displayLabel = "Work From Home"
    ),
    WFA(
        categoryId = 3,
        shortLabel = "WFA",
        displayLabel = "Work From Anywhere"
    );

    companion object {
        fun fromRaw(value: String?): WorkMode? {
            val normalized = value?.trim().orEmpty()
            return values().firstOrNull { mode ->
                normalized.equals(mode.shortLabel, ignoreCase = true) ||
                    normalized.equals(mode.displayLabel, ignoreCase = true)
            }
        }
    }
}
