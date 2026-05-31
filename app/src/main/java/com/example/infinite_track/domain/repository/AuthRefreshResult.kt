package com.example.infinite_track.domain.repository

data class AuthRefreshResult(
    val token: String,
    val refreshToken: String,
    val userId: String
)

enum class AuthRefreshFailureKind(val value: String) {
    TRANSIENT("transient"),
    NON_REFRESHABLE("non_refreshable"),
    TRANSPORT("transport")
}

enum class AuthRefreshFailureReason(val value: String) {
    ACCESS_EXPIRED("access_expired"),
    REFRESH_INVALID("refresh_invalid"),
    REFRESH_REVOKED("refresh_revoked"),
    INACTIVITY_EXPIRED("inactivity_expired"),
    MISSING_REFRESH_TOKEN("missing_refresh_token"),
    INVALID_PAYLOAD("invalid_payload"),
    TRANSPORT_ERROR("transport_error"),
    UNKNOWN("unknown")
}

class AuthRefreshException(
    val kind: AuthRefreshFailureKind,
    val reason: AuthRefreshFailureReason,
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
