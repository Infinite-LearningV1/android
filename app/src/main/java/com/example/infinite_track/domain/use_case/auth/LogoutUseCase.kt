package com.example.infinite_track.domain.use_case.auth

import com.example.infinite_track.domain.repository.AuthRepository
import java.util.concurrent.CancellationException
import javax.inject.Inject

/**
 * User-initiated logout orchestration.
 *
 * Remote logout is best-effort and local authenticated runtime cleanup always runs.
 */
class LogoutUseCase private constructor(
    private val authRepository: AuthRepository,
    private val clearAuthenticatedRuntime: suspend () -> Unit
) {
    @Inject
    constructor(
        authRepository: AuthRepository,
        clearAuthenticatedRuntimeUseCase: ClearAuthenticatedRuntimeUseCase
    ) : this(authRepository, clearAuthenticatedRuntimeUseCase::invoke)

    internal constructor(authRepository: AuthRepository) : this(authRepository, {})

    suspend operator fun invoke(): Result<Unit> {
        authRepository.logoutRemote()
        return try {
            clearAuthenticatedRuntime()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
