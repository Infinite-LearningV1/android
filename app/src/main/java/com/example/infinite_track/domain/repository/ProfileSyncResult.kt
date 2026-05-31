package com.example.infinite_track.domain.repository

import com.example.infinite_track.domain.model.auth.UserModel

sealed interface ProfileSyncResult {
    data class Success(val user: UserModel) : ProfileSyncResult
    data class Unauthorized(val reason: AuthRefreshFailureReason? = null) : ProfileSyncResult
    data class TemporaryFailure(
        val cause: Throwable? = null,
        val message: String? = cause?.message
    ) : ProfileSyncResult
}
