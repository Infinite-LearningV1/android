package com.example.infinite_track.domain.model.auth

enum class ReauthReason {
    INACTIVITY_EXPIRED,
    REFRESH_INVALID,
    REFRESH_REVOKED,
    NETWORK_OFFLINE_AT_REFRESH,
    UNKNOWN
}
