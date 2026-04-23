package com.example.infinite_track.di.auth

import com.example.infinite_track.data.soucre.network.request.LoginRequest
import com.example.infinite_track.domain.model.auth.UserModel
import com.example.infinite_track.domain.repository.AuthRepository
import com.example.infinite_track.domain.repository.RefreshSessionResult
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Provider

class RefreshSingleFlightCoordinatorTest {

    @Test
    fun `concurrent refresh attempts share a single in flight refresh`() = runBlocking {
        val refreshCalls = AtomicInteger(0)

        val repository = object : AuthRepository {
            override suspend fun refreshSession(): RefreshSessionResult {
                refreshCalls.incrementAndGet()
                delay(100)
                return RefreshSessionResult.Success
            }

            override suspend fun login(loginRequest: LoginRequest): Result<UserModel> = Result.failure(NotImplementedError())
            override suspend fun syncUserProfile(): Result<UserModel> = Result.failure(NotImplementedError())
            override suspend fun logout(): Result<Unit> = Result.failure(NotImplementedError())
            override fun getLoggedInUser(): Flow<UserModel?> = flowOf(null)
            override suspend fun saveFaceEmbedding(userId: Int, embedding: ByteArray): Result<Unit> = Result.failure(NotImplementedError())
        }

        val coordinator = RefreshSingleFlightCoordinator(
            authRepositoryProvider = Provider { repository }
        )

        val results = coroutineScope {
            (1..10).map {
                async { coordinator.refreshOrJoin() }
            }.awaitAll()
        }

        assertEquals(1, refreshCalls.get())
        assertTrue(results.all { it is RefreshSessionResult.Success })
    }
}
