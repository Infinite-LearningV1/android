package com.example.infinite_track.domain.repository

sealed interface RefreshSessionResult {
    data object Success : RefreshSessionResult

    sealed interface ReAuthRequired : RefreshSessionResult {
        data object InvalidOrRevoked : ReAuthRequired
        data object InactivityExceeded : ReAuthRequired
    }

    data class TemporaryFailure(val reason: String? = null) : RefreshSessionResult
}
