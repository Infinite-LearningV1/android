package com.example.infinite_track.di.auth

import com.example.infinite_track.domain.repository.AuthRefreshResult
import com.example.infinite_track.domain.repository.AuthRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class RefreshSingleFlightCoordinator @Inject constructor(
    private val authRepositoryProvider: Provider<AuthRepository>
) {
    enum class Status { IDLE, REFRESHING }

    private val lock = Mutex()
    private val _status = MutableStateFlow(Status.IDLE)
    private var inFlight: CompletableDeferred<Result<AuthRefreshResult>>? = null

    val status: StateFlow<Status> = _status.asStateFlow()

    suspend fun refreshOrJoin(): Result<AuthRefreshResult> {
        var createdByThisCaller: CompletableDeferred<Result<AuthRefreshResult>>? = null

        val deferred = lock.withLock {
            inFlight ?: CompletableDeferred<Result<AuthRefreshResult>>().also {
                inFlight = it
                createdByThisCaller = it
                _status.value = Status.REFRESHING
            }
        }

        val ownedDeferred = createdByThisCaller
        if (ownedDeferred != null) {
            try {
                ownedDeferred.complete(authRepositoryProvider.get().refreshSession())
            } catch (t: Throwable) {
                ownedDeferred.completeExceptionally(t)
                throw t
            } finally {
                lock.withLock {
                    if (inFlight === ownedDeferred) {
                        inFlight = null
                        _status.value = Status.IDLE
                    }
                }
            }
        }

        return deferred.await()
    }

    suspend fun runRefresh(): Result<Unit> {
        return refreshOrJoin().map { Unit }
    }
}
