package com.example.infinite_track.presentation.screen.history

import com.example.infinite_track.presentation.design.components.data.HistoryTimelineConnectorAccent
import kotlin.math.abs
import kotlin.math.min

internal const val HistoryKeyPrefix = "history-"
private const val HistoryFocusRangeFraction = 0.58f

internal data class VisibleHistoryItem(
    val recordIndex: Int,
    val center: Float
)

internal data class HistoryViewportSnapshot(
    val focusByKey: Map<String, Float> = emptyMap(),
    val timelineProgress: Float = 0f,
    val lastVisibleRecordIndex: Int? = null
)

internal fun historyItemKey(recordId: Int): String = "$HistoryKeyPrefix$recordId"

internal fun historyRecordIdFromKey(key: Any?): Int? =
    (key as? String)?.takeIf { it.startsWith(HistoryKeyPrefix) }
        ?.removePrefix(HistoryKeyPrefix)
        ?.toIntOrNull()

internal fun calculateHistoryFocusFraction(
    itemCenter: Float,
    viewportCenter: Float,
    viewportHeight: Float
): Float {
    val range = viewportHeight * HistoryFocusRangeFraction
    if (range <= 0f) return if (itemCenter == viewportCenter) 1f else 0f
    return (1f - min(abs(itemCenter - viewportCenter) / range, 1f)).coerceIn(0f, 1f)
}

internal fun calculateHistoryTimelineProgress(
    visibleItems: List<VisibleHistoryItem>,
    totalRecordCount: Int,
    viewportCenter: Float
): Float {
    if (totalRecordCount <= 1 || visibleItems.isEmpty()) return 0f
    val sorted = visibleItems.sortedBy { it.center }
    val lower = sorted.lastOrNull { it.center <= viewportCenter }
    val upper = sorted.firstOrNull { it.center >= viewportCenter }
    val fractionalIndex = when {
        lower == null -> upper!!.recordIndex.toFloat()
        upper == null -> lower.recordIndex.toFloat()
        lower.recordIndex == upper.recordIndex || lower.center == upper.center -> lower.recordIndex.toFloat()
        else -> {
            val fraction = ((viewportCenter - lower.center) / (upper.center - lower.center))
                .coerceIn(0f, 1f)
            lower.recordIndex + (upper.recordIndex - lower.recordIndex) * fraction
        }
    }
    return (fractionalIndex / (totalRecordCount - 1).toFloat()).coerceIn(0f, 1f)
}

internal fun shouldLoadMoreHistory(
    lastVisibleRecordIndex: Int?,
    recordCount: Int,
    canLoadMore: Boolean,
    loading: Boolean
): Boolean = lastVisibleRecordIndex != null &&
    recordCount > 0 &&
    lastVisibleRecordIndex >= recordCount - 3 &&
    canLoadMore &&
    !loading

internal fun resolveHistoryConnectorAccent(
    recordIndex: Int,
    recordCount: Int,
    timelineProgress: Float
): HistoryTimelineConnectorAccent {
    if (recordCount <= 0 || recordIndex !in 0 until recordCount) {
        return HistoryTimelineConnectorAccent(0f, false, 0f)
    }
    if (recordCount == 1) {
        return HistoryTimelineConnectorAccent(0f, true, 0f)
    }

    val progress = timelineProgress.coerceIn(0f, 1f)
    val lastIndex = recordCount - 1
    fun nodePosition(index: Int): Float = index / lastIndex.toFloat()
    fun segmentProgress(startIndex: Int, endIndex: Int): Float {
        val start = nodePosition(startIndex)
        val end = nodePosition(endIndex)
        return ((progress - start) / (end - start)).coerceIn(0f, 1f)
    }

    val top = if (recordIndex == 0) {
        0f
    } else {
        ((segmentProgress(recordIndex - 1, recordIndex) - 0.5f) * 2f)
            .coerceIn(0f, 1f)
    }
    val bottom = if (recordIndex == lastIndex) {
        0f
    } else {
        (segmentProgress(recordIndex, recordIndex + 1) * 2f)
            .coerceIn(0f, 1f)
    }

    return HistoryTimelineConnectorAccent(
        topFraction = top,
        nodeComplete = progress >= nodePosition(recordIndex),
        bottomFraction = bottom
    )
}
