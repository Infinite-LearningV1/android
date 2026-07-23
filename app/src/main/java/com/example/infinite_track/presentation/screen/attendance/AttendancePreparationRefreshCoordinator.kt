package com.example.infinite_track.presentation.screen.attendance

import com.example.infinite_track.domain.model.auth.UserModel
import com.example.infinite_track.domain.repository.ProfileSyncResult

internal class AttendancePreparationRefreshCoordinator(
    private val fetchStatus: suspend (forceRefresh: Boolean) -> Unit,
    private val requestProfileRefresh: suspend () -> ProfileSyncResult,
    private val applyProfile: (UserModel) -> Unit,
    private val resolveWfh: () -> Unit,
    private val preserveProfileRecovery: () -> Unit
) {
    suspend fun refreshStatus() {
        fetchStatus(true)
    }

    suspend fun refreshProfile() {
        when (val result = requestProfileRefresh()) {
            is ProfileSyncResult.Success -> {
                applyProfile(result.user)
                resolveWfh()
            }

            is ProfileSyncResult.TemporaryFailure,
            is ProfileSyncResult.Unauthorized -> preserveProfileRecovery()
        }
    }
}
