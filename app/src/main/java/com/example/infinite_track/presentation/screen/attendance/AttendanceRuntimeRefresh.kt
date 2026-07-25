package com.example.infinite_track.presentation.screen.attendance

import kotlinx.coroutines.CancellationException

internal suspend fun runAttendanceRuntimeRefresh(
    operation: suspend () -> Unit,
    onUnexpectedFailure: (Exception) -> Unit
) {
    try {
        operation()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        onUnexpectedFailure(error)
    }
}
