package com.example.infinite_track.domain.repository

import com.example.infinite_track.domain.model.auth.UserModel

sealed interface ProfileSyncResult {
    data class Success(val user: UserModel) : ProfileSyncResult
    data object Unauthorized : ProfileSyncResult
    data class TemporaryFailure(
        val cause: Throwable? = null,
        val message: String? = cause?.message
    ) : ProfileSyncResult
}
