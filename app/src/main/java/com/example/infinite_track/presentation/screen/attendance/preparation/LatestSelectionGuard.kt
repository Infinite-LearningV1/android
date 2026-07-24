package com.example.infinite_track.presentation.screen.attendance.preparation

import com.example.infinite_track.domain.model.attendance.WorkMode

class SelectionRequestToken internal constructor(
    internal val generation: Long,
    internal val mode: WorkMode
)

class LatestSelectionGuard {
    private var generation = 0L
    private var latest: SelectionRequestToken? = null

    @Synchronized
    fun next(mode: WorkMode): SelectionRequestToken = SelectionRequestToken(
        generation = ++generation,
        mode = mode
    ).also { latest = it }

    @Synchronized
    fun isCurrent(request: SelectionRequestToken, mode: WorkMode): Boolean =
        request.mode == mode && request === latest
}
