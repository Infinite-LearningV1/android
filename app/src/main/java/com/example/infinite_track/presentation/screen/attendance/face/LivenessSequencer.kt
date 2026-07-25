package com.example.infinite_track.presentation.screen.attendance.face

/**
 * Fixed order of the four liveness challenges shown in the numbered rail and
 * the "Challenge N of 4" pill.
 */
val CHALLENGE_ORDER: List<LivenessChallenge> = listOf(
    LivenessChallenge.BLINK,
    LivenessChallenge.SMILE,
    LivenessChallenge.TURN_LEFT,
    LivenessChallenge.TURN_RIGHT
)

/**
 * Deterministic, pure sequencer for the multi-step liveness flow. Kept free of Android
 * types so it can be unit-tested on the JVM.
 */
class LivenessSequencer(private val order: List<LivenessChallenge> = CHALLENGE_ORDER) {
    var passedCount: Int = 0
        private set

    val current: LivenessChallenge
        get() = order[passedCount.coerceIn(0, order.lastIndex)]

    /** 1-based index of the active challenge, clamped to the last index when complete. */
    val index: Int get() = (passedCount + 1).coerceAtMost(order.size)

    val total: Int get() = order.size

    val isComplete: Boolean get() = passedCount >= order.size

    fun pass() {
        if (passedCount < order.size) passedCount++
    }

    fun reset() {
        passedCount = 0
    }
}
