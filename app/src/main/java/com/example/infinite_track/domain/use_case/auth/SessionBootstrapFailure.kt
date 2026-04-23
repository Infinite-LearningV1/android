package com.example.infinite_track.domain.use_case.auth

import com.example.infinite_track.domain.repository.RefreshSessionResult

sealed class SessionBootstrapFailure(message: String? = null, cause: Throwable? = null) : Exception(message, cause) {
    class ReAuthRequired(
        val reason: RefreshSessionResult.ReAuthRequired
    ) : SessionBootstrapFailure("Session requires re-authentication")

    class TemporaryFailure(
        cause: Throwable? = null,
        message: String? = cause?.message
    ) : SessionBootstrapFailure(message ?: "Temporary bootstrap failure", cause)
}
