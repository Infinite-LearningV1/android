package com.example.infinite_track.di.auth

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.infinite_track.data.soucre.local.preferences.UserPreference
import com.example.infinite_track.data.soucre.network.request.LoginRequest
import com.example.infinite_track.domain.manager.SessionManager
import com.example.infinite_track.domain.model.auth.UserModel
import com.example.infinite_track.domain.repository.AuthRepository
import com.example.infinite_track.domain.repository.RefreshSessionResult
import com.example.infinite_track.domain.use_case.auth.LogoutUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import okhttp3.Call
import okhttp3.Connection
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Provider

class AuthRefreshInterceptorTest {

    @Test
    fun `non 401 response returns normally without refresh`() = runBlocking {
        val fixture = TestFixture(refreshResult = RefreshSessionResult.Success)
        fixture.userPreference.saveSession("token-a", "10", "refresh-a")

        val interceptor = fixture.createInterceptor()
        val chain = FakeChain(
            request = request("https://example.com/api/attendance/status-today"),
            proceedBlock = { req -> okResponse(req, 200) }
        )

        val response = interceptor.intercept(chain)

        assertEquals(200, response.code)
        assertEquals(0, fixture.refreshCalls.get())
        assertEquals(0, fixture.logoutCalls.get())
        assertFalse(fixture.sessionManager.sessionExpired.value)
        response.close()
    }

    @Test
    fun `401 on protected request refreshes once and retries with latest token`() = runBlocking {
        val fixture = TestFixture(refreshResult = RefreshSessionResult.Success)
        fixture.userPreference.saveSession("old-access", "10", "old-refresh")
        fixture.onRefresh = {
            fixture.userPreference.saveSession("new-access", "10", "new-refresh")
        }

        val interceptor = fixture.createInterceptor()
        val proceedCalls = AtomicInteger(0)
        val observedAuthHeaders = mutableListOf<String?>()
        val chain = FakeChain(
            request = request("https://example.com/api/attendance/status-today"),
            proceedBlock = { req ->
                observedAuthHeaders += req.header("Authorization")
                if (proceedCalls.incrementAndGet() == 1) {
                    okResponse(req, 401)
                } else {
                    okResponse(req, 200)
                }
            }
        )

        val response = interceptor.intercept(chain)

        assertEquals(200, response.code)
        assertEquals(2, proceedCalls.get())
        assertEquals(1, fixture.refreshCalls.get())
        assertEquals(listOf("Bearer old-access", "Bearer new-access"), observedAuthHeaders)
        assertEquals(0, fixture.logoutCalls.get())
        assertFalse(fixture.sessionManager.sessionExpired.value)
        response.close()
    }

    @Test
    fun `auth endpoints do not get authorization bearer injected`() = runBlocking {
        val fixture = TestFixture(refreshResult = RefreshSessionResult.Success)
        fixture.userPreference.saveSession("token-a", "10", "refresh-a")
        val interceptor = fixture.createInterceptor()

        val requests = listOf(
            request("https://example.com/api/auth/login"),
            request("https://example.com/api/auth/refresh"),
            request("https://example.com/api/auth/logout")
        )

        requests.forEach { original ->
            var observedAuth: String? = null
            val chain = FakeChain(
                request = original,
                proceedBlock = { req ->
                    observedAuth = req.header("Authorization")
                    okResponse(req, 401)
                }
            )

            val response = interceptor.intercept(chain)
            assertEquals(401, response.code)
            assertEquals(null, observedAuth)
            response.close()
        }

        assertEquals(0, fixture.refreshCalls.get())
        assertEquals(0, fixture.logoutCalls.get())
        assertFalse(fixture.sessionManager.sessionExpired.value)
    }

    @Test
    fun `401 on protected request with reauth required clears session and triggers forced reauth`() = runBlocking {
        val fixture = TestFixture(refreshResult = RefreshSessionResult.ReAuthRequired.InvalidOrRevoked)
        fixture.userPreference.saveSession("token-a", "10", "refresh-a")

        val interceptor = fixture.createInterceptor()
        val chain = FakeChain(
            request = request("https://example.com/api/attendance/status-today"),
            proceedBlock = { req -> okResponse(req, 401) }
        )

        val response = interceptor.intercept(chain)
        delay(100)

        assertEquals(401, response.code)
        assertEquals(1, fixture.refreshCalls.get())
        assertEquals(1, fixture.logoutCalls.get())
        assertTrue(fixture.sessionManager.sessionExpired.value)
        response.close()
    }

    @Test
    fun `401 on protected request with temporary refresh failure keeps auth state and does not force reauth`() = runBlocking {
        val fixture = TestFixture(refreshResult = RefreshSessionResult.TemporaryFailure("timeout"))
        fixture.userPreference.saveSession("token-a", "10", "refresh-a")

        val interceptor = fixture.createInterceptor()
        val chain = FakeChain(
            request = request("https://example.com/api/attendance/status-today"),
            proceedBlock = { req -> okResponse(req, 401) }
        )

        val response = interceptor.intercept(chain)
        delay(100)

        assertEquals(401, response.code)
        assertEquals(1, fixture.refreshCalls.get())
        assertEquals(0, fixture.logoutCalls.get())
        assertFalse(fixture.sessionManager.sessionExpired.value)
        assertEquals("token-a", fixture.userPreference.getAuthToken().first())
        assertEquals("refresh-a", fixture.userPreference.getRefreshToken().first())
        response.close()
    }

    @Test
    fun `401 on protected request with unexpected refresh exception returns original 401 without logout`() = runBlocking {
        val fixture = TestFixture(refreshResult = RefreshSessionResult.Success)
        fixture.userPreference.saveSession("token-a", "10", "refresh-a")
        fixture.onRefresh = {
            throw IllegalStateException("boom")
        }

        val interceptor = fixture.createInterceptor()
        val chain = FakeChain(
            request = request("https://example.com/api/attendance/status-today"),
            proceedBlock = { req -> okResponse(req, 401) }
        )

        val response = interceptor.intercept(chain)

        assertEquals(401, response.code)
        assertEquals(1, fixture.refreshCalls.get())
        assertEquals(0, fixture.logoutCalls.get())
        assertFalse(fixture.sessionManager.sessionExpired.value)
        assertEquals("token-a", fixture.userPreference.getAuthToken().first())
        assertEquals("refresh-a", fixture.userPreference.getRefreshToken().first())
        response.close()
    }

    @Test
    fun `request retried at most once when retry response is still 401`() = runBlocking {
        val fixture = TestFixture(refreshResult = RefreshSessionResult.Success)
        fixture.userPreference.saveSession("old-access", "10", "old-refresh")
        fixture.onRefresh = {
            fixture.userPreference.saveSession("new-access", "10", "new-refresh")
        }

        val interceptor = fixture.createInterceptor()
        val proceedCalls = AtomicInteger(0)
        val chain = FakeChain(
            request = request("https://example.com/api/attendance/status-today"),
            proceedBlock = { req ->
                proceedCalls.incrementAndGet()
                okResponse(req, 401)
            }
        )

        val response = interceptor.intercept(chain)

        assertEquals(401, response.code)
        assertEquals(2, proceedCalls.get())
        assertEquals(1, fixture.refreshCalls.get())
        response.close()
    }

    @Test
    fun `concurrent 401 protected requests use single refresh call and all retry`() = runBlocking {
        val fixture = TestFixture(refreshResult = RefreshSessionResult.Success)
        fixture.userPreference.saveSession("old-access", "10", "old-refresh")
        fixture.onRefresh = {
            delay(120)
            fixture.userPreference.saveSession("new-access", "10", "new-refresh")
        }

        val interceptor = fixture.createInterceptor()
        val firstAttemptSeen = java.util.concurrent.ConcurrentHashMap.newKeySet<Int>()
        val proceedCounts = java.util.concurrent.ConcurrentHashMap<Int, AtomicInteger>()
        val threadPool = Executors.newFixedThreadPool(5)
        val done = CountDownLatch(5)
        val responses = java.util.Collections.synchronizedList(mutableListOf<Response>())
        val failures = java.util.Collections.synchronizedList(mutableListOf<Throwable>())

        try {
            for (idx in 1..5) {
                threadPool.execute {
                    try {
                        val chain = FakeChain(
                            request = request("https://example.com/api/attendance/status-today?req=$idx"),
                            proceedBlock = { req ->
                                val count = proceedCounts.computeIfAbsent(idx) { AtomicInteger(0) }.incrementAndGet()
                                if (count == 1) {
                                    firstAttemptSeen.add(idx)
                                    okResponse(req, 401)
                                } else {
                                    okResponse(req, 200)
                                }
                            }
                        )
                        responses += interceptor.intercept(chain)
                    } catch (t: Throwable) {
                        failures += t
                    } finally {
                        done.countDown()
                    }
                }
            }

            assertTrue(done.await(3, TimeUnit.SECONDS))
            assertTrue(failures.isEmpty())
            assertEquals(5, responses.size)
            responses.forEach { response ->
                assertEquals(200, response.code)
                response.close()
            }

            assertEquals(5, firstAttemptSeen.size)
            assertTrue(proceedCounts.values.all { it.get() == 2 })
            assertEquals(1, fixture.refreshCalls.get())
        } finally {
            threadPool.shutdownNow()
        }
    }

    @Test
    fun `concurrent 401 protected requests with shared reauth required trigger logout once`() = runBlocking {
        val fixture = TestFixture(refreshResult = RefreshSessionResult.ReAuthRequired.InvalidOrRevoked)
        fixture.userPreference.saveSession("token-a", "10", "refresh-a")
        fixture.onRefresh = {
            delay(120)
        }

        val interceptor = fixture.createInterceptor()
        val threadPool = Executors.newFixedThreadPool(5)
        val done = CountDownLatch(5)
        val responses = java.util.Collections.synchronizedList(mutableListOf<Response>())
        val failures = java.util.Collections.synchronizedList(mutableListOf<Throwable>())

        try {
            for (idx in 1..5) {
                threadPool.execute {
                    try {
                        val chain = FakeChain(
                            request = request("https://example.com/api/attendance/status-today?req=$idx"),
                            proceedBlock = { req -> okResponse(req, 401) }
                        )
                        responses += interceptor.intercept(chain)
                    } catch (t: Throwable) {
                        failures += t
                    } finally {
                        done.countDown()
                    }
                }
            }

            assertTrue(done.await(3, TimeUnit.SECONDS))
            assertTrue(failures.isEmpty())
            assertEquals(5, responses.size)
            responses.forEach { response ->
                assertEquals(401, response.code)
                response.close()
            }

            assertEquals(1, fixture.refreshCalls.get())
            assertEquals(1, fixture.logoutCalls.get())
            assertTrue(fixture.sessionManager.sessionExpired.value)
        } finally {
            threadPool.shutdownNow()
        }
    }

    @Test
    fun `after session-expired reset later reauth wave can trigger logout again`() = runBlocking {
        val fixture = TestFixture(refreshResult = RefreshSessionResult.ReAuthRequired.InvalidOrRevoked)
        fixture.userPreference.saveSession("token-a", "10", "refresh-a")
        fixture.onRefresh = {
            delay(120)
        }

        val interceptor = fixture.createInterceptor()
        val firstWavePool = Executors.newFixedThreadPool(3)
        val firstWaveDone = CountDownLatch(3)

        try {
            repeat(3) {
                firstWavePool.execute {
                    try {
                        val chain = FakeChain(
                            request = request("https://example.com/api/attendance/status-today?wave=1&req=$it"),
                            proceedBlock = { req -> okResponse(req, 401) }
                        )
                        interceptor.intercept(chain).close()
                    } finally {
                        firstWaveDone.countDown()
                    }
                }
            }
            assertTrue(firstWaveDone.await(3, TimeUnit.SECONDS))
        } finally {
            firstWavePool.shutdownNow()
        }

        assertEquals(1, fixture.logoutCalls.get())
        assertTrue(fixture.sessionManager.sessionExpired.value)

        fixture.sessionManager.resetSessionExpired()
        assertFalse(fixture.sessionManager.sessionExpired.value)

        fixture.userPreference.saveSession("token-b", "10", "refresh-b")

        val secondWaveResponse = interceptor.intercept(
            FakeChain(
                request = request("https://example.com/api/attendance/status-today?wave=2"),
                proceedBlock = { req -> okResponse(req, 401) }
            )
        )
        secondWaveResponse.close()

        assertEquals(2, fixture.logoutCalls.get())
        assertTrue(fixture.sessionManager.sessionExpired.value)
    }

    private fun request(url: String): Request = Request.Builder().url(url).build()

    private fun okResponse(request: Request, code: Int): Response {
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .message("test")
            .code(code)
            .body("{}".toResponseBody("application/json".toMediaType()))
            .build()
    }
}

private class TestFixture(
    private val refreshResult: RefreshSessionResult
) {
    val refreshCalls = AtomicInteger(0)
    val logoutCalls = AtomicInteger(0)
    var onRefresh: (suspend () -> Unit)? = null

    val userPreference: UserPreference = run {
        val testFile = File.createTempFile("user_prefs", ".preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(produceFile = { testFile })
        UserPreference(dataStore)
    }

    val sessionManager = SessionManager()

    private val authRepository = object : AuthRepository {
        override suspend fun refreshSession(): RefreshSessionResult {
            refreshCalls.incrementAndGet()
            onRefresh?.invoke()
            return refreshResult
        }

        override suspend fun logout(): Result<Unit> {
            logoutCalls.incrementAndGet()
            userPreference.clearAuthData()
            return Result.success(Unit)
        }

        override suspend fun login(loginRequest: LoginRequest): Result<UserModel> = Result.failure(NotImplementedError())
        override suspend fun syncUserProfile(): Result<UserModel> = Result.failure(NotImplementedError())
        override fun getLoggedInUser(): Flow<UserModel?> = flowOf(null)
        override suspend fun saveFaceEmbedding(userId: Int, embedding: ByteArray): Result<Unit> = Result.failure(NotImplementedError())
    }

    fun createInterceptor(): AuthRefreshInterceptor {
        val coordinator = RefreshSingleFlightCoordinator(authRepositoryProvider = Provider { authRepository })
        return AuthRefreshInterceptor(
            userPreference = userPreference,
            refreshSingleFlightCoordinator = coordinator,
            logoutUseCaseProvider = Provider { LogoutUseCase(authRepository) },
            sessionManagerProvider = Provider { sessionManager }
        )
    }
}

private class FakeChain(
    private val request: Request,
    private val proceedBlock: (Request) -> Response
) : Interceptor.Chain {
    override fun request(): Request = request

    override fun proceed(request: Request): Response = proceedBlock(request)

    override fun call(): Call {
        throw NotImplementedError("Not used in tests")
    }

    override fun connection(): Connection? = null

    override fun connectTimeoutMillis(): Int = 30_000

    override fun withConnectTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this

    override fun readTimeoutMillis(): Int = 30_000

    override fun withReadTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this

    override fun writeTimeoutMillis(): Int = 30_000

    override fun withWriteTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
}
