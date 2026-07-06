package com.example.infinite_track.domain.use_case.auth

import com.example.infinite_track.domain.repository.AuthRuntimeCleaner
import javax.inject.Inject

class ClearAuthenticatedRuntimeUseCase @Inject constructor(
    private val authRuntimeCleaner: AuthRuntimeCleaner
) {
    suspend operator fun invoke() {
        authRuntimeCleaner.clearAuthenticatedRuntime()
    }
}
