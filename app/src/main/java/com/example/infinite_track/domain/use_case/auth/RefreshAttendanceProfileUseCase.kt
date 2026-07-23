package com.example.infinite_track.domain.use_case.auth

import com.example.infinite_track.domain.repository.AuthRepository
import com.example.infinite_track.domain.repository.ProfileSyncResult
import javax.inject.Inject

/** Fetches the authoritative profile from `/me` for attendance preparation recovery. */
class RefreshAttendanceProfileUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): ProfileSyncResult = authRepository.syncUserProfile()
}
