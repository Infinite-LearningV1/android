package com.example.infinite_track.domain.use_case.auth

import com.example.infinite_track.domain.model.auth.ReauthReason

sealed class SessionBootstrapFailure(message: String? = null, cause: Throwable? = null) : Exception(message, cause) {
    class ReAuthRequired(
        val reason: ReauthReason
    ) : SessionBootstrapFailure("Session requires re-authentication")

    class TemporaryFailure(
        cause: Throwable? = null,
        message: String? = cause?.message
    ) : SessionBootstrapFailure(message ?: "Temporary bootstrap failure", cause)
}
