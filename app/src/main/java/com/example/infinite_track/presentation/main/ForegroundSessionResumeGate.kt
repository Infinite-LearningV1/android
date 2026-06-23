package com.example.infinite_track.presentation.main

internal class ForegroundSessionResumeGate(
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
    private val debounceWindowMs: Long = 2_000L
) {
    private var initialStartObserved: Boolean = false
    private var validationRunning: Boolean = false
    private var lastStartedAtMillis: Long = Long.MIN_VALUE

    @Synchronized
    fun tryAcquire(bootstrapInProgress: Boolean): Boolean {
        if (!initialStartObserved) {
            initialStartObserved = true
            return false
        }
        if (bootstrapInProgress) {
            return false
        }
        if (validationRunning) {
            return false
        }
        val now = nowMillis()
        if (lastStartedAtMillis != Long.MIN_VALUE && now - lastStartedAtMillis < debounceWindowMs) {
            return false
        }
        validationRunning = true
        lastStartedAtMillis = now
        return true
    }

    @Synchronized
    fun release() {
        validationRunning = false
    }
}
