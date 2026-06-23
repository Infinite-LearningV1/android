# Android Family A Foreground / Resume Session Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an explicit foreground/resume session validation owner so Android revalidates backend session truth when the app returns to the foreground, without false logout on temporary transport failures.

**Architecture:** Keep the existing Family A auth stack intact. Add a narrow resume-validation use case in the auth domain, a pure foreground/debounce gate in `presentation/main`, and a process-level lifecycle observer registered from `InfiniteTrackApplication`. The observer delegates contract decisions to the use case and routes terminal outcomes into the existing `SessionManager` forced re-auth lane.

**Tech Stack:** Kotlin, Hilt, AndroidX Lifecycle `ProcessLifecycleOwner`, coroutines, DataStore, JUnit 4, kotlinx-coroutines-test.

## Global Constraints

- Work only in the isolated worktree branch `fix/android-family-a-access-session-continuation`.
- Android is the Family A consumer; backend `INF-145` is the source of truth for auth/session validity.
- Scope is Android-only; do not work on Web FE or backend issues except as contract context.
- Preserve the existing `AuthRefreshInterceptor`, `RefreshSingleFlightCoordinator`, `AuthRepositoryImpl`, `SessionManager`, and cold-start `CheckSessionUseCase` architecture.
- Do not move auth/session decisions into screen-specific UI code.
- Temporary transport failures such as offline, timeout, or server-down must not clear local session state.
- Terminal auth outcomes `AUTH_REFRESH_TOKEN_INVALID`, `AUTH_REFRESH_TOKEN_REVOKED`, and `AUTH_SESSION_INACTIVE` must force full re-auth.
- Keep the solution narrow: foreground lifecycle owner + resume validator + tests + evidence/docs updates.
- Baseline verification commands remain `./gradlew app:testDebugUnitTest`, `./gradlew app:lint`, and `./gradlew app:assembleDebug`.
- If the local environment cannot execute verification successfully, the work must remain `Needs Verification` rather than being overstated as complete.

---

## File Structure

- Create: `app/src/main/java/com/example/infinite_track/domain/use_case/auth/ValidateForegroundSessionUseCase.kt`
  - Contract-aware foreground/resume validation using existing repository/session semantics.
- Create: `app/src/main/java/com/example/infinite_track/presentation/main/ForegroundSessionResumeGate.kt`
  - Pure in-flight + debounce guard for meaningful background -> foreground transitions.
- Create: `app/src/main/java/com/example/infinite_track/di/ApplicationCoroutineScope.kt`
  - Hilt qualifier for the app-wide coroutine scope used by the lifecycle observer.
- Create: `app/src/main/java/com/example/infinite_track/presentation/main/ForegroundSessionLifecycleObserver.kt`
  - Process-level lifecycle observer that triggers foreground validation and routes terminal outcomes into `SessionManager`.
- Modify: `app/src/main/java/com/example/infinite_track/InfiniteTrackApplication.kt`
  - Registers the observer with `ProcessLifecycleOwner`.
- Modify: `app/src/main/java/com/example/infinite_track/di/AppModule.kt`
  - Provides the qualified application coroutine scope.
- Modify: `app/src/main/java/com/example/infinite_track/di/UseCaseModule.kt`
  - Provides `ValidateForegroundSessionUseCase` to match existing use-case provisioning.
- Modify: `docs/adr/ADR-XXX-android-refresh-session-compat.md`
  - Records explicit foreground/resume validation ownership.
- Modify: `docs/auth-runtime-evidence/RUN_2026-05-30.md`
  - Adds the new foreground/resume runtime scenarios.
- Test: `app/src/test/java/com/example/infinite_track/domain/use_case/auth/ValidateForegroundSessionUseCaseTest.kt`
- Test: `app/src/test/java/com/example/infinite_track/presentation/main/ForegroundSessionResumeGateTest.kt`
- Test: `app/src/test/java/com/example/infinite_track/presentation/main/ForegroundSessionLifecycleObserverTest.kt`

## Task 1: Add contract-aware foreground session validation use case

**Files:**
- Create: `app/src/main/java/com/example/infinite_track/domain/use_case/auth/ValidateForegroundSessionUseCase.kt`
- Modify: `app/src/main/java/com/example/infinite_track/di/UseCaseModule.kt`
- Test: `app/src/test/java/com/example/infinite_track/domain/use_case/auth/ValidateForegroundSessionUseCaseTest.kt`

**Interfaces:**
- Consumes:
  - `AuthRepository.syncUserProfile(): ProfileSyncResult`
  - `AuthRepository.refreshSession(): Result<AuthRefreshResult>`
  - `UserPreference.getAuthToken(): Flow<String>`
  - `UserPreference.getRefreshToken(): Flow<String>`
  - `SessionManager.isBootstrapSessionInProgress: Boolean`
- Produces:
  - `sealed class ForegroundSessionValidationResult`
  - `suspend operator fun ValidateForegroundSessionUseCase.invoke(): ForegroundSessionValidationResult`

- [ ] **Step 1: Write the failing use-case test file**

Create `app/src/test/java/com/example/infinite_track/domain/use_case/auth/ValidateForegroundSessionUseCaseTest.kt` with:

```kotlin
package com.example.infinite_track.domain.use_case.auth

import android.content.ContextWrapper
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.infinite_track.data.soucre.local.preferences.UserPreference
import com.example.infinite_track.data.soucre.network.request.LoginRequest
import com.example.infinite_track.domain.manager.SessionManager
import com.example.infinite_track.domain.model.auth.UserModel
import com.example.infinite_track.domain.repository.AuthRefreshException
import com.example.infinite_track.domain.repository.AuthRefreshFailureKind
import com.example.infinite_track.domain.repository.AuthRefreshFailureReason
import com.example.infinite_track.domain.repository.AuthRefreshResult
import com.example.infinite_track.domain.repository.AuthRepository
import com.example.infinite_track.domain.repository.ProfileSyncResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ValidateForegroundSessionUseCaseTest {

    @Test
    fun `skips when auth token or refresh token is missing`() = runBlocking {
        val sessionManager = SessionManager()
        val userPreference = createUserPreference()
        val repository = FakeAuthRepository()

        val result = ValidateForegroundSessionUseCase(repository, userPreference, sessionManager)()

        assertEquals(ForegroundSessionValidationResult.Skipped, result)
        assertEquals(0, repository.syncCallCount)
        assertEquals(0, repository.refreshCallCount)
    }

    @Test
    fun `skips while bootstrap session is already in progress`() = runBlocking {
        val sessionManager = SessionManager().also { it.beginBootstrapSession() }
        val userPreference = createUserPreference().also {
            it.saveSession("access", userId = "1", refreshToken = "refresh", lastRefreshAt = 1L)
        }
        val repository = FakeAuthRepository(syncResults = mutableListOf(ProfileSyncResult.Success(sampleUser())))

        val result = ValidateForegroundSessionUseCase(repository, userPreference, sessionManager)()

        assertEquals(ForegroundSessionValidationResult.Skipped, result)
        assertEquals(0, repository.syncCallCount)
        assertEquals(0, repository.refreshCallCount)
    }

    @Test
    fun `returns valid when profile sync succeeds without refresh`() = runBlocking {
        val sessionManager = SessionManager()
        val userPreference = createUserPreference().also {
            it.saveSession("access", userId = "1", refreshToken = "refresh", lastRefreshAt = 1L)
        }
        val repository = FakeAuthRepository(syncResults = mutableListOf(ProfileSyncResult.Success(sampleUser())))

        val result = ValidateForegroundSessionUseCase(repository, userPreference, sessionManager)()

        assertEquals(ForegroundSessionValidationResult.Valid, result)
        assertEquals(1, repository.syncCallCount)
        assertEquals(0, repository.refreshCallCount)
    }

    @Test
    fun `returns refreshed when sync is unauthorized but refresh and retry succeed`() = runBlocking {
        val sessionManager = SessionManager()
        val userPreference = createUserPreference().also {
            it.saveSession("expired-access", userId = "1", refreshToken = "refresh", lastRefreshAt = 1L)
        }
        val repository = FakeAuthRepository(
            syncResults = mutableListOf(
                ProfileSyncResult.Unauthorized(),
                ProfileSyncResult.Success(sampleUser())
            ),
            refreshSessionResult = Result.success(AuthRefreshResult("new-access", "new-refresh", "1"))
        )

        val result = ValidateForegroundSessionUseCase(repository, userPreference, sessionManager)()

        assertEquals(ForegroundSessionValidationResult.Refreshed, result)
        assertEquals(2, repository.syncCallCount)
        assertEquals(1, repository.refreshCallCount)
    }

    @Test
    fun `returns reauth required when refresh fails with non refreshable reason`() = runBlocking {
        val sessionManager = SessionManager()
        val userPreference = createUserPreference().also {
            it.saveSession("expired-access", userId = "1", refreshToken = "refresh", lastRefreshAt = 1L)
        }
        val repository = FakeAuthRepository(
            syncResults = mutableListOf(ProfileSyncResult.Unauthorized()),
            refreshSessionResult = Result.failure(
                AuthRefreshException(
                    kind = AuthRefreshFailureKind.NON_REFRESHABLE,
                    reason = AuthRefreshFailureReason.REFRESH_REVOKED,
                    message = "revoked"
                )
            )
        )

        val result = ValidateForegroundSessionUseCase(repository, userPreference, sessionManager)()

        assertEquals(
            ForegroundSessionValidationResult.ReauthRequired(SessionManager.ReauthReason.REFRESH_REVOKED),
            result
        )
        assertEquals(1, repository.syncCallCount)
        assertEquals(1, repository.refreshCallCount)
    }

    @Test
    fun `returns temporary failure when refresh transport error occurs`() = runBlocking {
        val sessionManager = SessionManager()
        val userPreference = createUserPreference().also {
            it.saveSession("expired-access", userId = "1", refreshToken = "refresh", lastRefreshAt = 1L)
        }
        val repository = FakeAuthRepository(
            syncResults = mutableListOf(ProfileSyncResult.Unauthorized()),
            refreshSessionResult = Result.failure(
                AuthRefreshException(
                    kind = AuthRefreshFailureKind.TRANSPORT,
                    reason = AuthRefreshFailureReason.TRANSPORT_ERROR,
                    message = "offline"
                )
            )
        )

        val result = ValidateForegroundSessionUseCase(repository, userPreference, sessionManager)()

        assertTrue(result is ForegroundSessionValidationResult.TemporaryFailure)
        assertEquals(1, repository.syncCallCount)
        assertEquals(1, repository.refreshCallCount)
        assertEquals(false, sessionManager.sessionExpired.value)
    }

    private fun createUserPreference(): UserPreference {
        val tempDir = createTempDir(prefix = "foreground-session-")
        val appContext = object : ContextWrapper(null) {
            override fun getFilesDir(): File = tempDir
        }
        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = { File(appContext.filesDir, "user.preferences_pb") }
        )
        return UserPreference(dataStore)
    }

    private fun sampleUser(): UserModel = UserModel(
        id = 1,
        fullName = "Redacted User",
        email = "redacted@example.test",
        roleName = "Employee",
        positionName = "Engineer",
        divisionName = "Mobile",
        nipNim = "EMP-001",
        phone = "08123456789",
        photoUrl = "https://example.test/photo.png",
        workLocation = null,
        programName = null,
        employeeType = null,
        photoUpdatedAt = "2026-06-21T00:00:00Z",
        faceEmbedding = null
    )

    private class FakeAuthRepository(
        private val syncResults: MutableList<ProfileSyncResult> = mutableListOf(),
        private val refreshSessionResult: Result<AuthRefreshResult> = Result.success(
            AuthRefreshResult("token", "refresh", "1")
        )
    ) : AuthRepository {
        var syncCallCount: Int = 0
        var refreshCallCount: Int = 0

        override suspend fun login(loginRequest: LoginRequest): Result<UserModel> {
            error("Not used in this test")
        }

        override suspend fun logout(): Result<Unit> {
            return Result.success(Unit)
        }

        override suspend fun refreshSession(): Result<AuthRefreshResult> {
            refreshCallCount += 1
            return refreshSessionResult
        }

        override suspend fun syncUserProfile(): ProfileSyncResult {
            syncCallCount += 1
            return syncResults.removeAt(0)
        }

        override suspend fun syncUserProfileForBootstrap(): ProfileSyncResult {
            error("Bootstrap sync is not used by foreground validation")
        }

        override fun getLoggedInUser(): Flow<UserModel?> = flowOf(null)
    }
}
```

- [ ] **Step 2: Run the targeted test to verify it fails**

Run:

```bash
./gradlew app:testDebugUnitTest --tests "com.example.infinite_track.domain.use_case.auth.ValidateForegroundSessionUseCaseTest"
```

Expected:

- FAIL because `ValidateForegroundSessionUseCase` and `ForegroundSessionValidationResult` do not exist yet.

- [ ] **Step 3: Write the minimal foreground validation use case**

Create `app/src/main/java/com/example/infinite_track/domain/use_case/auth/ValidateForegroundSessionUseCase.kt` with:

```kotlin
package com.example.infinite_track.domain.use_case.auth

import com.example.infinite_track.data.soucre.local.preferences.UserPreference
import com.example.infinite_track.domain.manager.SessionManager
import com.example.infinite_track.domain.repository.AuthRefreshException
import com.example.infinite_track.domain.repository.AuthRefreshFailureKind
import com.example.infinite_track.domain.repository.AuthRefreshFailureReason
import com.example.infinite_track.domain.repository.AuthRepository
import com.example.infinite_track.domain.repository.ProfileSyncResult
import kotlinx.coroutines.flow.first

sealed class ForegroundSessionValidationResult {
    data object Skipped : ForegroundSessionValidationResult()
    data object Valid : ForegroundSessionValidationResult()
    data object Refreshed : ForegroundSessionValidationResult()
    data class ReauthRequired(val reason: SessionManager.ReauthReason) : ForegroundSessionValidationResult()
    data class TemporaryFailure(val message: String?, val cause: Throwable?) : ForegroundSessionValidationResult()
}

class ValidateForegroundSessionUseCase(
    private val authRepository: AuthRepository,
    private val userPreference: UserPreference,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke(): ForegroundSessionValidationResult {
        val accessToken = userPreference.getAuthToken().first()
        val refreshToken = userPreference.getRefreshToken().first()

        if (accessToken.isBlank() || refreshToken.isBlank()) {
            return ForegroundSessionValidationResult.Skipped
        }

        if (sessionManager.isBootstrapSessionInProgress) {
            return ForegroundSessionValidationResult.Skipped
        }

        return when (val syncResult = authRepository.syncUserProfile()) {
            is ProfileSyncResult.Success -> ForegroundSessionValidationResult.Valid
            is ProfileSyncResult.TemporaryFailure -> ForegroundSessionValidationResult.TemporaryFailure(
                message = syncResult.message,
                cause = syncResult.cause
            )
            is ProfileSyncResult.Unauthorized -> handleUnauthorized(syncResult.reason)
        }
    }

    private suspend fun handleUnauthorized(
        initialReason: AuthRefreshFailureReason?
    ): ForegroundSessionValidationResult {
        val refreshResult = authRepository.refreshSession()
        if (refreshResult.isSuccess) {
            return when (val retrySync = authRepository.syncUserProfile()) {
                is ProfileSyncResult.Success -> ForegroundSessionValidationResult.Refreshed
                is ProfileSyncResult.TemporaryFailure -> ForegroundSessionValidationResult.TemporaryFailure(
                    message = retrySync.message,
                    cause = retrySync.cause
                )
                is ProfileSyncResult.Unauthorized -> ForegroundSessionValidationResult.ReauthRequired(
                    reason = reauthReasonFor(preferredUnauthorizedReason(retrySync.reason, initialReason))
                )
            }
        }

        val refreshException = refreshResult.exceptionOrNull() as? AuthRefreshException
        return when (refreshException?.kind) {
            AuthRefreshFailureKind.NON_REFRESHABLE -> ForegroundSessionValidationResult.ReauthRequired(
                reason = reauthReasonFor(preferredUnauthorizedReason(refreshException.reason, initialReason))
            )
            AuthRefreshFailureKind.TRANSPORT,
            AuthRefreshFailureKind.TRANSIENT,
            null -> ForegroundSessionValidationResult.TemporaryFailure(
                message = refreshException?.message,
                cause = refreshException
            )
        }
    }

    private fun preferredUnauthorizedReason(
        laterReason: AuthRefreshFailureReason?,
        initialReason: AuthRefreshFailureReason?
    ): AuthRefreshFailureReason {
        val fallbackReason = laterReason ?: AuthRefreshFailureReason.REFRESH_INVALID
        return when {
            initialReason == null -> fallbackReason
            initialReason.isTerminalReason() && fallbackReason.isGenericReason() -> initialReason
            else -> fallbackReason
        }
    }

    private fun AuthRefreshFailureReason.isTerminalReason(): Boolean {
        return this == AuthRefreshFailureReason.INACTIVITY_EXPIRED ||
            this == AuthRefreshFailureReason.REFRESH_REVOKED
    }

    private fun AuthRefreshFailureReason.isGenericReason(): Boolean {
        return this == AuthRefreshFailureReason.REFRESH_INVALID ||
            this == AuthRefreshFailureReason.UNKNOWN
    }

    private fun reauthReasonFor(reason: AuthRefreshFailureReason): SessionManager.ReauthReason {
        return when (reason) {
            AuthRefreshFailureReason.INACTIVITY_EXPIRED -> SessionManager.ReauthReason.INACTIVITY_EXPIRED
            AuthRefreshFailureReason.REFRESH_INVALID,
            AuthRefreshFailureReason.MISSING_REFRESH_TOKEN -> SessionManager.ReauthReason.REFRESH_INVALID
            AuthRefreshFailureReason.REFRESH_REVOKED -> SessionManager.ReauthReason.REFRESH_REVOKED
            else -> SessionManager.ReauthReason.UNKNOWN
        }
    }
}
```

Modify `app/src/main/java/com/example/infinite_track/di/UseCaseModule.kt` by adding:

```kotlin
import com.example.infinite_track.domain.use_case.auth.ValidateForegroundSessionUseCase
```

and:

```kotlin
@Provides
fun provideValidateForegroundSessionUseCase(
    authRepository: AuthRepository,
    userPreference: UserPreference,
    sessionManager: SessionManager
): ValidateForegroundSessionUseCase {
    return ValidateForegroundSessionUseCase(authRepository, userPreference, sessionManager)
}
```

- [ ] **Step 4: Run the targeted test to verify it passes**

Run:

```bash
./gradlew app:testDebugUnitTest --tests "com.example.infinite_track.domain.use_case.auth.ValidateForegroundSessionUseCaseTest"
```

Expected:

- PASS for all six tests in `ValidateForegroundSessionUseCaseTest`.

- [ ] **Step 5: Commit**

```bash
git add \
  app/src/main/java/com/example/infinite_track/domain/use_case/auth/ValidateForegroundSessionUseCase.kt \
  app/src/main/java/com/example/infinite_track/di/UseCaseModule.kt \
  app/src/test/java/com/example/infinite_track/domain/use_case/auth/ValidateForegroundSessionUseCaseTest.kt

git commit -m "feat: add foreground session validation use case"
```

## Task 2: Add foreground debounce and single-flight gate

**Files:**
- Create: `app/src/main/java/com/example/infinite_track/presentation/main/ForegroundSessionResumeGate.kt`
- Test: `app/src/test/java/com/example/infinite_track/presentation/main/ForegroundSessionResumeGateTest.kt`

**Interfaces:**
- Consumes: `SessionManager.isBootstrapSessionInProgress: Boolean`
- Produces:
  - `class ForegroundSessionResumeGate`
  - `fun tryAcquire(bootstrapInProgress: Boolean): Boolean`
  - `fun release()`

- [ ] **Step 1: Write the failing gate test file**

Create `app/src/test/java/com/example/infinite_track/presentation/main/ForegroundSessionResumeGateTest.kt` with:

```kotlin
package com.example.infinite_track.presentation.main

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ForegroundSessionResumeGateTest {

    @Test
    fun `acquires once and blocks while validation is still running`() {
        val gate = ForegroundSessionResumeGate(nowMillis = { 10_000L }, debounceWindowMs = 2_000L)

        assertTrue(gate.tryAcquire(bootstrapInProgress = false))
        assertFalse(gate.tryAcquire(bootstrapInProgress = false))
    }

    @Test
    fun `skips while bootstrap is in progress`() {
        val gate = ForegroundSessionResumeGate(nowMillis = { 10_000L }, debounceWindowMs = 2_000L)

        assertFalse(gate.tryAcquire(bootstrapInProgress = true))
    }

    @Test
    fun `debounces fast foreground bounce after release`() {
        var now = 10_000L
        val gate = ForegroundSessionResumeGate(nowMillis = { now }, debounceWindowMs = 2_000L)

        assertTrue(gate.tryAcquire(bootstrapInProgress = false))
        gate.release()

        now = 11_000L
        assertFalse(gate.tryAcquire(bootstrapInProgress = false))

        now = 12_100L
        assertTrue(gate.tryAcquire(bootstrapInProgress = false))
    }
}
```

- [ ] **Step 2: Run the targeted test to verify it fails**

Run:

```bash
./gradlew app:testDebugUnitTest --tests "com.example.infinite_track.presentation.main.ForegroundSessionResumeGateTest"
```

Expected:

- FAIL because `ForegroundSessionResumeGate` does not exist yet.

- [ ] **Step 3: Write the minimal gate implementation**

Create `app/src/main/java/com/example/infinite_track/presentation/main/ForegroundSessionResumeGate.kt` with:

```kotlin
package com.example.infinite_track.presentation.main

internal class ForegroundSessionResumeGate(
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
    private val debounceWindowMs: Long = 2_000L
) {
    private var validationRunning: Boolean = false
    private var lastStartedAtMillis: Long = Long.MIN_VALUE

    @Synchronized
    fun tryAcquire(bootstrapInProgress: Boolean): Boolean {
        if (bootstrapInProgress) {
            return false
        }
        if (validationRunning) {
            return false
        }
        val now = nowMillis()
        if (lastStartedAtMillis != Long.MIN_VALUE && now - lastStartedAtMillis < debounceWindowMs) {
            return false
        }
        validationRunning = true
        lastStartedAtMillis = now
        return true
    }

    @Synchronized
    fun release() {
        validationRunning = false
    }
}
```

- [ ] **Step 4: Run the targeted test to verify it passes**

Run:

```bash
./gradlew app:testDebugUnitTest --tests "com.example.infinite_track.presentation.main.ForegroundSessionResumeGateTest"
```

Expected:

- PASS for all three gate tests.

- [ ] **Step 5: Commit**

```bash
git add \
  app/src/main/java/com/example/infinite_track/presentation/main/ForegroundSessionResumeGate.kt \
  app/src/test/java/com/example/infinite_track/presentation/main/ForegroundSessionResumeGateTest.kt

git commit -m "feat: add foreground session debounce gate"
```

## Task 3: Wire process-level foreground observer into the app

**Files:**
- Create: `app/src/main/java/com/example/infinite_track/di/ApplicationCoroutineScope.kt`
- Modify: `app/src/main/java/com/example/infinite_track/di/AppModule.kt`
- Create: `app/src/main/java/com/example/infinite_track/presentation/main/ForegroundSessionLifecycleObserver.kt`
- Modify: `app/src/main/java/com/example/infinite_track/InfiniteTrackApplication.kt`
- Test: `app/src/test/java/com/example/infinite_track/presentation/main/ForegroundSessionLifecycleObserverTest.kt`

**Interfaces:**
- Consumes:
  - `ValidateForegroundSessionUseCase.invoke(): ForegroundSessionValidationResult`
  - `SessionManager.beginSessionExpiryHandling(): Boolean`
  - `SessionManager.triggerForcedReauth(reason: SessionManager.ReauthReason)`
  - `LogoutUseCase.invoke(): Result<Unit>`
  - `ForegroundSessionResumeGate.tryAcquire(bootstrapInProgress: Boolean): Boolean`
- Produces:
  - `@Qualifier annotation class ApplicationCoroutineScope`
  - `class ForegroundSessionLifecycleObserver : DefaultLifecycleObserver`

- [ ] **Step 1: Write the failing lifecycle observer test file**

Create `app/src/test/java/com/example/infinite_track/presentation/main/ForegroundSessionLifecycleObserverTest.kt` with:

```kotlin
package com.example.infinite_track.presentation.main

import android.content.ContextWrapper
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.example.infinite_track.data.soucre.local.preferences.UserPreference
import com.example.infinite_track.data.soucre.network.request.LoginRequest
import com.example.infinite_track.domain.manager.SessionManager
import com.example.infinite_track.domain.model.auth.UserModel
import com.example.infinite_track.domain.repository.AuthRefreshException
import com.example.infinite_track.domain.repository.AuthRefreshFailureKind
import com.example.infinite_track.domain.repository.AuthRefreshFailureReason
import com.example.infinite_track.domain.repository.AuthRefreshResult
import com.example.infinite_track.domain.repository.AuthRepository
import com.example.infinite_track.domain.repository.ProfileSyncResult
import com.example.infinite_track.domain.use_case.auth.LogoutUseCase
import com.example.infinite_track.domain.use_case.auth.ValidateForegroundSessionUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.inject.Provider

class ForegroundSessionLifecycleObserverTest {

    @Test
    fun `onStart validates once and ignores re-entry while running`() = runTest {
        val scope = TestScope(StandardTestDispatcher(testScheduler) + Job())
        val sessionManager = SessionManager()
        val userPreference = createUserPreference().also {
            it.saveSession("access", userId = "1", refreshToken = "refresh", lastRefreshAt = 1L)
        }
        val validationRepository = FakeValidationRepository(
            syncResults = mutableListOf(ProfileSyncResult.Success(sampleUser()))
        )
        val logoutRepository = FakeLogoutRepository()
        val observer = ForegroundSessionLifecycleObserver(
            validateForegroundSessionUseCase = ValidateForegroundSessionUseCase(
                authRepository = validationRepository,
                userPreference = userPreference,
                sessionManager = sessionManager
            ),
            sessionManager = sessionManager,
            logoutUseCaseProvider = Provider { LogoutUseCase(logoutRepository) },
            applicationScope = scope,
            gate = ForegroundSessionResumeGate(nowMillis = { 10_000L }, debounceWindowMs = 2_000L)
        )
        val owner = TestLifecycleOwner()

        observer.onStart(owner)
        observer.onStart(owner)
        scope.advanceUntilIdle()

        assertEquals(1, observer.validationCount)
        assertEquals(1, validationRepository.syncCallCount)
        assertFalse(sessionManager.sessionExpired.value)
    }

    @Test
    fun `reauth required triggers logout and forced reauth state`() = runTest {
        val scope = TestScope(StandardTestDispatcher(testScheduler) + Job())
        val sessionManager = SessionManager()
        val userPreference = createUserPreference().also {
            it.saveSession("access", userId = "1", refreshToken = "refresh", lastRefreshAt = 1L)
        }
        val validationRepository = FakeValidationRepository(
            syncResults = mutableListOf(
                ProfileSyncResult.Unauthorized(AuthRefreshFailureReason.REFRESH_INVALID)
            ),
            refreshFailureReason = AuthRefreshFailureReason.REFRESH_INVALID
        )
        val logoutRepository = FakeLogoutRepository()
        val observer = ForegroundSessionLifecycleObserver(
            validateForegroundSessionUseCase = ValidateForegroundSessionUseCase(
                authRepository = validationRepository,
                userPreference = userPreference,
                sessionManager = sessionManager
            ),
            sessionManager = sessionManager,
            logoutUseCaseProvider = Provider { LogoutUseCase(logoutRepository) },
            applicationScope = scope,
            gate = ForegroundSessionResumeGate(nowMillis = { 10_000L }, debounceWindowMs = 2_000L)
        )
        val owner = TestLifecycleOwner()

        observer.onStart(owner)
        scope.advanceUntilIdle()

        assertEquals(1, logoutRepository.logoutCallCount)
        assertTrue(sessionManager.sessionExpired.value)
        assertEquals(SessionManager.ReauthReason.REFRESH_INVALID, sessionManager.reauthReason.value)
    }

    @Test
    fun `temporary failure does not trigger logout or forced reauth`() = runTest {
        val scope = TestScope(StandardTestDispatcher(testScheduler) + Job())
        val sessionManager = SessionManager()
        val userPreference = createUserPreference().also {
            it.saveSession("access", userId = "1", refreshToken = "refresh", lastRefreshAt = 1L)
        }
        val validationRepository = FakeValidationRepository(
            syncResults = mutableListOf(ProfileSyncResult.TemporaryFailure(message = "offline"))
        )
        val logoutRepository = FakeLogoutRepository()
        val observer = ForegroundSessionLifecycleObserver(
            validateForegroundSessionUseCase = ValidateForegroundSessionUseCase(
                authRepository = validationRepository,
                userPreference = userPreference,
                sessionManager = sessionManager
            ),
            sessionManager = sessionManager,
            logoutUseCaseProvider = Provider { LogoutUseCase(logoutRepository) },
            applicationScope = scope,
            gate = ForegroundSessionResumeGate(nowMillis = { 10_000L }, debounceWindowMs = 2_000L)
        )
        val owner = TestLifecycleOwner()

        observer.onStart(owner)
        scope.advanceUntilIdle()

        assertEquals(0, logoutRepository.logoutCallCount)
        assertFalse(sessionManager.sessionExpired.value)
        assertEquals(null, sessionManager.reauthReason.value)
    }

    private fun createUserPreference(): UserPreference {
        val tempDir = createTempDir(prefix = "foreground-observer-")
        val appContext = object : ContextWrapper(null) {
            override fun getFilesDir(): File = tempDir
        }
        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = { File(appContext.filesDir, "user.preferences_pb") }
        )
        return UserPreference(dataStore)
    }

    private fun sampleUser(): UserModel = UserModel(
        id = 1,
        fullName = "Redacted User",
        email = "redacted@example.test",
        roleName = "Employee",
        positionName = "Engineer",
        divisionName = "Mobile",
        nipNim = "EMP-001",
        phone = "08123456789",
        photoUrl = "https://example.test/photo.png",
        workLocation = null,
        programName = null,
        employeeType = null,
        photoUpdatedAt = "2026-06-21T00:00:00Z",
        faceEmbedding = null
    )
}

private class TestLifecycleOwner : LifecycleOwner {
    override val lifecycle: Lifecycle = LifecycleRegistry(this)
}

private class FakeValidationRepository(
    private val syncResults: MutableList<ProfileSyncResult>,
    private val refreshFailureReason: AuthRefreshFailureReason? = null
) : AuthRepository {
    var syncCallCount: Int = 0
        private set

    override suspend fun login(loginRequest: LoginRequest): Result<UserModel> {
        error("Not used")
    }

    override suspend fun logout(): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun refreshSession(): Result<AuthRefreshResult> {
        return if (refreshFailureReason == null) {
            Result.success(AuthRefreshResult("new-access", "new-refresh", "1"))
        } else {
            Result.failure(
                AuthRefreshException(
                    kind = AuthRefreshFailureKind.NON_REFRESHABLE,
                    reason = refreshFailureReason,
                    message = "reauth"
                )
            )
        }
    }

    override suspend fun syncUserProfile(): ProfileSyncResult {
        syncCallCount += 1
        return syncResults.removeAt(0)
    }

    override suspend fun syncUserProfileForBootstrap(): ProfileSyncResult {
        error("Not used")
    }

    override fun getLoggedInUser(): Flow<UserModel?> = flowOf(null)
}

private class FakeLogoutRepository : AuthRepository {
    var logoutCallCount: Int = 0
        private set

    override suspend fun login(loginRequest: LoginRequest): Result<UserModel> {
        error("Not used")
    }

    override suspend fun logout(): Result<Unit> {
        logoutCallCount += 1
        return Result.success(Unit)
    }

    override suspend fun refreshSession(): Result<AuthRefreshResult> {
        error("Not used")
    }

    override suspend fun syncUserProfile(): ProfileSyncResult {
        error("Not used")
    }

    override suspend fun syncUserProfileForBootstrap(): ProfileSyncResult {
        error("Not used")
    }

    override fun getLoggedInUser(): Flow<UserModel?> = flowOf(null)
}
```

- [ ] **Step 2: Run the targeted test to verify it fails**

Run:

```bash
./gradlew app:testDebugUnitTest --tests "com.example.infinite_track.presentation.main.ForegroundSessionLifecycleObserverTest"
```

Expected:

- FAIL because `ForegroundSessionLifecycleObserver` and `ApplicationCoroutineScope` do not exist yet.

- [ ] **Step 3: Write the observer, scope qualifier, and application wiring**

Create `app/src/main/java/com/example/infinite_track/di/ApplicationCoroutineScope.kt` with:

```kotlin
package com.example.infinite_track.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationCoroutineScope
```

Modify `app/src/main/java/com/example/infinite_track/di/AppModule.kt` by adding imports:

```kotlin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
```

and provider:

```kotlin
@Singleton
@Provides
@ApplicationCoroutineScope
fun provideApplicationCoroutineScope(): CoroutineScope {
    return CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
}
```

Create `app/src/main/java/com/example/infinite_track/presentation/main/ForegroundSessionLifecycleObserver.kt` with:

```kotlin
package com.example.infinite_track.presentation.main

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.infinite_track.di.ApplicationCoroutineScope
import com.example.infinite_track.domain.manager.SessionManager
import com.example.infinite_track.domain.use_case.auth.ForegroundSessionValidationResult
import com.example.infinite_track.domain.use_case.auth.LogoutUseCase
import com.example.infinite_track.domain.use_case.auth.ValidateForegroundSessionUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class ForegroundSessionLifecycleObserver @Inject constructor(
    private val validateForegroundSessionUseCase: ValidateForegroundSessionUseCase,
    private val sessionManager: SessionManager,
    private val logoutUseCaseProvider: Provider<LogoutUseCase>,
    @ApplicationCoroutineScope private val applicationScope: CoroutineScope,
    private val gate: ForegroundSessionResumeGate = ForegroundSessionResumeGate()
) : DefaultLifecycleObserver {

    internal var validationCount: Int = 0
        private set

    override fun onStart(owner: LifecycleOwner) {
        if (!gate.tryAcquire(sessionManager.isBootstrapSessionInProgress)) {
            return
        }

        applicationScope.launch {
            try {
                validationCount += 1
                when (val result = validateForegroundSessionUseCase()) {
                    ForegroundSessionValidationResult.Skipped -> Unit
                    ForegroundSessionValidationResult.Valid -> Unit
                    ForegroundSessionValidationResult.Refreshed -> Unit
                    is ForegroundSessionValidationResult.TemporaryFailure -> Unit
                    is ForegroundSessionValidationResult.ReauthRequired -> {
                        if (sessionManager.beginSessionExpiryHandling()) {
                            try {
                                logoutUseCaseProvider.get().invoke()
                            } finally {
                                sessionManager.triggerForcedReauth(result.reason)
                            }
                        }
                    }
                }
            } finally {
                gate.release()
            }
        }
    }
}
```

Modify `app/src/main/java/com/example/infinite_track/InfiniteTrackApplication.kt` by adding imports:

```kotlin
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.infinite_track.presentation.main.ForegroundSessionLifecycleObserver
```

and fields / registration:

```kotlin
@Inject
lateinit var foregroundSessionLifecycleObserver: ForegroundSessionLifecycleObserver

override fun onCreate() {
    super.onCreate()

    NotificationHelper.createNotificationChannel(this)
    ProcessLifecycleOwner.get().lifecycle.addObserver(foregroundSessionLifecycleObserver)
}
```

- [ ] **Step 4: Run the targeted lifecycle observer test to verify it passes**

Run:

```bash
./gradlew app:testDebugUnitTest --tests "com.example.infinite_track.presentation.main.ForegroundSessionLifecycleObserverTest"
```

Expected:

- PASS for the three lifecycle observer tests.

- [ ] **Step 5: Commit**

```bash
git add \
  app/src/main/java/com/example/infinite_track/di/ApplicationCoroutineScope.kt \
  app/src/main/java/com/example/infinite_track/di/AppModule.kt \
  app/src/main/java/com/example/infinite_track/presentation/main/ForegroundSessionLifecycleObserver.kt \
  app/src/main/java/com/example/infinite_track/InfiniteTrackApplication.kt \
  app/src/test/java/com/example/infinite_track/presentation/main/ForegroundSessionLifecycleObserverTest.kt

git commit -m "feat: validate session on app foreground"
```

## Task 4: Update docs and run bounded verification

**Files:**
- Modify: `docs/adr/ADR-XXX-android-refresh-session-compat.md`
- Modify: `docs/auth-runtime-evidence/RUN_2026-05-30.md`

**Interfaces:**
- Consumes:
  - `ForegroundSessionLifecycleObserver`
  - `ValidateForegroundSessionUseCase`
  - existing runtime evidence format in `RUN_2026-05-30.md`
- Produces:
  - ADR note stating Android now owns explicit foreground/resume validation
  - runtime evidence checklist expanded with foreground/resume scenarios

- [ ] **Step 1: Add the ADR note for foreground/resume ownership**

Append this section to `docs/adr/ADR-XXX-android-refresh-session-compat.md`:

```markdown
## Foreground / Resume Validation Ownership

Android now owns an explicit foreground/resume validation lane for Family A.

This lane exists in addition to:

- cold-start splash bootstrap validation; and
- reactive protected-request refresh on `401`.

When the app transitions from background to foreground, Android attempts a bounded contract-aware session validation.

Terminal backend auth/session outcomes still route to forced re-auth.
Temporary transport failures such as offline or server-down do not trigger false logout and do not clear local session state.
```

- [ ] **Step 2: Update runtime evidence doc with the new scenario wording**

In `docs/auth-runtime-evidence/RUN_2026-05-30.md`, replace the Scenario 2 section with:

````markdown
## Scenario 2 — Foreground / resume with expired access token triggers validation and refresh

Logcat template:

```bash
adb logcat | grep -E "ForegroundSessionLifecycleObserver|ValidateForegroundSessionUseCase|AuthRefreshInterceptor|RefreshSingleFlight|SessionManager|AUTH_ACCESS_TOKEN_EXPIRED"
```

Expected:

- App returns from background to foreground.
- Android foreground observer triggers one resume validation attempt.
- If the access token is expired and refresh is valid, Android refreshes the session once.
- The app remains usable without false forced logout.

Observed: _pending_
````

Then add this new section after the inactivity scenario:

````markdown
## Scenario 5 — Foreground / resume while offline or backend is down does not trigger false logout

Expected:

- App returns from background to foreground.
- Android foreground observer triggers one resume validation attempt.
- Validation fails temporarily due to offline / server-down transport failure.
- Android does not clear local session state.
- Android does not trigger misleading forced re-auth cleanup.

Observed: _pending_
````

Renumber the old offline refresh scenario to Scenario 6 and keep its expectations aligned with the same non-terminal semantics.

- [ ] **Step 3: Run the targeted test suite for the new feature**

Run:

```bash
./gradlew app:testDebugUnitTest --tests "com.example.infinite_track.domain.use_case.auth.ValidateForegroundSessionUseCaseTest" --tests "com.example.infinite_track.presentation.main.ForegroundSessionResumeGateTest" --tests "com.example.infinite_track.presentation.main.ForegroundSessionLifecycleObserverTest"
```

Expected:

- PASS for all new foreground/resume tests.

- [ ] **Step 4: Run the repo baseline verification**

Run:

```bash
./gradlew app:testDebugUnitTest
./gradlew app:lint
./gradlew app:assembleDebug
```

Expected:

- PASS, or
- if the environment still blocks KAPT / local toolchain execution, capture the exact failure and mark the work as `Needs Verification` rather than claiming success.

- [ ] **Step 5: Commit**

```bash
git add \
  docs/adr/ADR-XXX-android-refresh-session-compat.md \
  docs/auth-runtime-evidence/RUN_2026-05-30.md

git commit -m "docs: record foreground session validation contract"
```
