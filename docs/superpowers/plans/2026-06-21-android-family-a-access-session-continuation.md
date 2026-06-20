# Android Family A Access Session Continuation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Harden the existing Android Family A auth/session consumer so it prioritizes the backend primary `data.auth.*` token payload, keeps legacy token fallback, sends canonical `X-Client-Type: mobile`, and preserves existing silent-refresh orchestration.

**Architecture:** Continue the existing `INF-147` Android chain. Keep token parsing in DTO/repository code, keep refresh orchestration in `AuthRefreshInterceptor` + `RefreshSingleFlightCoordinator`, keep bootstrap validation in `CheckSessionUseCase`, and keep forced re-auth state in `SessionManager`.

**Tech Stack:** Kotlin, Android Gradle Plugin, Retrofit/Gson, OkHttp interceptors, DataStore, Room, Hilt, JUnit 4 unit tests.

## Global Constraints

- Work only in the isolated worktree branch `fix/android-family-a-access-session-continuation`.
- Android is the Family A consumer; backend `INF-145` is the source of truth for auth/session validity.
- Scope is Android-only; do not work on Web FE or backend issues except as contract context.
- Canonical Android mobile auth header is `X-Client-Type: mobile`.
- Token payload priority is `data.auth.access_token` then legacy `data.token`, and `data.auth.refresh_token` then legacy `data.refresh_token`.
- Temporary refresh failures such as offline, timeout, server-down, malformed temporary response, or unknown 5xx must not clear local session.
- Terminal auth failures `AUTH_REFRESH_TOKEN_INVALID`, `AUTH_REFRESH_TOKEN_REVOKED`, and `AUTH_SESSION_INACTIVE` must force full re-auth.
- Runtime-sensitive Family A closure needs emulator/device evidence against a reachable backend; without it, status remains `Needs Verification`.
- Do not print or commit token values, email, full name, identifiers, Firebase config, keystore material, or auth-bearing logs.
- Existing Gradle baseline in this environment can fail before tests with `java.io.IOException: Unable to establish loopback connection`; if it appears, record it as environment-blocked verification, not test failure.

---

## File Structure

- Create `app/src/main/java/com/example/infinite_track/data/soucre/network/response/AuthPayload.kt`
  - Holds the primary backend auth token payload DTO used by login and refresh responses.
- Modify `app/src/main/java/com/example/infinite_track/data/soucre/network/response/LoginResponse.kt`
  - Add optional `auth` payload to `UserData`.
  - Make legacy `token` nullable with a default.
  - Add `resolvedAccessToken()` and `resolvedRefreshToken()` methods.
- Modify `app/src/main/java/com/example/infinite_track/data/soucre/network/response/RefreshSessionResponse.kt`
  - Add optional `auth` payload to `AuthData`.
  - Make legacy `token` nullable with a default.
  - Add `resolvedAccessToken()` and `resolvedRefreshToken()` methods.
- Modify `app/src/main/java/com/example/infinite_track/data/repository/auth/AuthRepositoryImpl.kt`
  - Use token resolver methods for login and refresh.
  - Call `AuthSessionApiService.refreshSession()` for refresh rather than the protected `ApiService.refresh()` lane.
- Modify `app/src/main/java/com/example/infinite_track/data/soucre/network/retrofit/AuthSessionApiService.kt`
  - Change hardcoded header to `X-Client-Type: mobile`.
- Modify `app/src/test/java/com/example/infinite_track/data/soucre/network/AuthApiContractTest.kt`
  - Add primary `data.auth.*` parsing tests, legacy fallback tests, and header annotation test.
- Modify `app/src/test/java/com/example/infinite_track/data/repository/auth/AuthRepositoryImplRefreshSessionTest.kt`
  - Add repository tests for primary login/refresh payloads and auth-session refresh lane.
  - Update helper constructors for nullable legacy token plus optional auth payload.
- Modify `docs/adr/ADR-XXX-android-refresh-session-compat.md`
  - Document primary `data.auth.*` priority and legacy fallback.
  - Document canonical `mobile` client type.
- Modify `docs/auth-runtime-evidence/RUN_2026-05-30.md`
  - Add a contract-hardening note if runtime verification remains blocked.

---

### Task 1: Add primary auth payload DTO and contract parsing tests

**Files:**
- Create: `app/src/main/java/com/example/infinite_track/data/soucre/network/response/AuthPayload.kt`
- Modify: `app/src/main/java/com/example/infinite_track/data/soucre/network/response/LoginResponse.kt`
- Modify: `app/src/main/java/com/example/infinite_track/data/soucre/network/response/RefreshSessionResponse.kt`
- Test: `app/src/test/java/com/example/infinite_track/data/soucre/network/AuthApiContractTest.kt`

**Interfaces:**
- Consumes: Gson parsing of `LoginResponse`, `RefreshResponse`, and `RefreshErrorResponse`.
- Produces:
  - `data class AuthPayload(val accessToken: String? = null, val refreshToken: String? = null)`
  - `fun UserData.resolvedAccessToken(): String`
  - `fun UserData.resolvedRefreshToken(): String?`
  - `fun AuthData.resolvedAccessToken(): String`
  - `fun AuthData.resolvedRefreshToken(): String?`

- [ ] **Step 1: Write failing contract tests for primary and legacy token payloads**

Replace `app/src/test/java/com/example/infinite_track/data/soucre/network/AuthApiContractTest.kt` with:

```kotlin
package com.example.infinite_track.data.soucre.network

import com.example.infinite_track.data.soucre.network.request.RefreshRequest
import com.example.infinite_track.data.soucre.network.response.LoginResponse
import com.example.infinite_track.data.soucre.network.response.RefreshErrorResponse
import com.example.infinite_track.data.soucre.network.response.RefreshResponse
import com.example.infinite_track.data.soucre.network.retrofit.AuthSessionApiService
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.http.Headers

class AuthApiContractTest {
    private val gson = Gson()

    @Test
    fun `mobile login response parses primary auth token payload`() {
        val json = """
            {
              "success": true,
              "message": "Login success",
              "data": {
                "id": 147,
                "full_name": "Redacted User",
                "email": "redacted@example.test",
                "role_name": "Student",
                "position_name": "Learner",
                "program_name": "Infinite Learning",
                "division_name": "Mobile",
                "nip_nim": "NIM-REDACTED",
                "phone": "0000000000",
                "photo": "https://example.test/avatar.png",
                "photo_updated_at": "2026-05-30T00:00:00Z",
                "location": {
                  "latitude": -6.2,
                  "longitude": 106.8,
                  "radius": 100,
                  "description": "Redacted office",
                  "category_name": "Office"
                },
                "auth": {
                  "access_token": "primary-access-token-redacted",
                  "refresh_token": "primary-refresh-token-redacted"
                }
              }
            }
        """.trimIndent()

        val response = gson.fromJson(json, LoginResponse::class.java)

        assertTrue(response.success)
        assertEquals("primary-access-token-redacted", response.data.resolvedAccessToken())
        assertEquals("primary-refresh-token-redacted", response.data.resolvedRefreshToken())
    }

    @Test
    fun `mobile login response falls back to legacy direct token fields`() {
        val json = """
            {
              "success": true,
              "message": "Login success",
              "data": {
                "id": 147,
                "full_name": "Redacted User",
                "email": "redacted@example.test",
                "role_name": "Student",
                "position_name": "Learner",
                "program_name": "Infinite Learning",
                "division_name": "Mobile",
                "nip_nim": "NIM-REDACTED",
                "phone": "0000000000",
                "photo": "https://example.test/avatar.png",
                "photo_updated_at": "2026-05-30T00:00:00Z",
                "location": {
                  "latitude": -6.2,
                  "longitude": 106.8,
                  "radius": 100,
                  "description": "Redacted office",
                  "category_name": "Office"
                },
                "token": "legacy-access-token-redacted",
                "refresh_token": "legacy-refresh-token-redacted"
              }
            }
        """.trimIndent()

        val response = gson.fromJson(json, LoginResponse::class.java)

        assertTrue(response.success)
        assertEquals("legacy-access-token-redacted", response.data.resolvedAccessToken())
        assertEquals("legacy-refresh-token-redacted", response.data.resolvedRefreshToken())
    }

    @Test
    fun `refresh request serializes refresh token for backend json contract`() {
        val json = gson.toJson(RefreshRequest(refreshToken = "refresh-token-redacted"))

        assertEquals("{\"refresh_token\":\"refresh-token-redacted\"}", json)
    }

    @Test
    fun `refresh success response parses primary auth token payload`() {
        val json = """
            {
              "success": true,
              "code": null,
              "message": "Refresh success",
              "data": {
                "id": 147,
                "auth": {
                  "access_token": "primary-new-access-token-redacted",
                  "refresh_token": "primary-new-refresh-token-redacted"
                }
              }
            }
        """.trimIndent()

        val response = gson.fromJson(json, RefreshResponse::class.java)

        assertTrue(response.success)
        assertEquals(147, response.data.id)
        assertEquals("primary-new-access-token-redacted", response.data.resolvedAccessToken())
        assertEquals("primary-new-refresh-token-redacted", response.data.resolvedRefreshToken())
        assertEquals("Refresh success", response.message)
    }

    @Test
    fun `refresh success response falls back to legacy direct token fields`() {
        val json = """
            {
              "success": true,
              "code": null,
              "message": "Refresh success",
              "data": {
                "id": 147,
                "token": "legacy-new-access-token-redacted",
                "refresh_token": "legacy-new-refresh-token-redacted"
              }
            }
        """.trimIndent()

        val response = gson.fromJson(json, RefreshResponse::class.java)

        assertTrue(response.success)
        assertEquals(147, response.data.id)
        assertEquals("legacy-new-access-token-redacted", response.data.resolvedAccessToken())
        assertEquals("legacy-new-refresh-token-redacted", response.data.resolvedRefreshToken())
    }

    @Test
    fun `auth session refresh endpoint uses canonical mobile client type header`() {
        val method = AuthSessionApiService::class.java.getMethod(
            "refreshSession",
            com.example.infinite_track.data.soucre.network.request.RefreshSessionRequest::class.java
        )
        val headers = method.getAnnotation(Headers::class.java)?.value?.toList().orEmpty()

        assertTrue(headers.contains("X-Client-Type: mobile"))
        assertFalse(headers.any { it.equals("X-Client-Type: android", ignoreCase = true) })
    }

    @Test
    fun `refresh error response parses auth code for 401 handling`() {
        val json = """
            {
              "success": false,
              "code": "AUTH_SESSION_INACTIVE",
              "message": "Session inactive for more than 48 hours"
            }
        """.trimIndent()

        val response = gson.fromJson(json, RefreshErrorResponse::class.java)

        assertFalse(response.success)
        assertEquals("AUTH_SESSION_INACTIVE", response.code)
        assertEquals("Session inactive for more than 48 hours", response.message)
    }
}
```

- [ ] **Step 2: Run contract tests and verify they fail before implementation**

Run:

```bash
./gradlew app:testDebugUnitTest --tests "com.example.infinite_track.data.soucre.network.AuthApiContractTest"
```

Expected before implementation: FAIL with unresolved `resolvedAccessToken` / `resolvedRefreshToken`, missing `AuthPayload`, or header assertion failure. If Gradle exits before test execution with `Unable to establish loopback connection`, record environment-blocked verification and continue with the code changes.

- [ ] **Step 3: Add shared auth payload DTO**

Create `app/src/main/java/com/example/infinite_track/data/soucre/network/response/AuthPayload.kt`:

```kotlin
package com.example.infinite_track.data.soucre.network.response

import com.google.gson.annotations.SerializedName

data class AuthPayload(
    @SerializedName("access_token") val accessToken: String? = null,
    @SerializedName("refresh_token") val refreshToken: String? = null
)
```

- [ ] **Step 4: Update login response DTO with primary-token resolver methods**

Replace `UserData` in `app/src/main/java/com/example/infinite_track/data/soucre/network/response/LoginResponse.kt` with:

```kotlin
data class UserData(
    @SerializedName("id") val id: Int,
    @SerializedName("full_name") val fullName: String,
    @SerializedName("email") val email: String,
    @SerializedName("role_name") val roleName: String,
    @SerializedName("position_name") val positionName: String,
    @SerializedName("program_name") val programName: String,
    @SerializedName("division_name") val divisionName: String,
    @SerializedName("nip_nim") val nipNim: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("photo") val photo: String,
    @SerializedName("photo_updated_at") val photoUpdatedAt: String,
    @SerializedName("location") val location: LocationData,
    @SerializedName("token") val token: String? = null,
    @SerializedName("refresh_token") val refreshToken: String? = null,
    @SerializedName("auth") val auth: AuthPayload? = null
) {
    fun resolvedAccessToken(): String {
        return auth?.accessToken?.takeIf { it.isNotBlank() }
            ?: token.orEmpty()
    }

    fun resolvedRefreshToken(): String? {
        return auth?.refreshToken?.takeIf { it.isNotBlank() }
            ?: refreshToken?.takeIf { it.isNotBlank() }
    }
}
```

Keep `LoginResponse` and `LocationData` unchanged except for the new `UserData` body.

- [ ] **Step 5: Update refresh response DTO with primary-token resolver methods**

Replace `AuthData` in `app/src/main/java/com/example/infinite_track/data/soucre/network/response/RefreshSessionResponse.kt` with:

```kotlin
data class AuthData(
    @SerializedName("id") val id: Int,
    @SerializedName("token") val token: String? = null,
    @SerializedName("refresh_token") val refreshToken: String? = null,
    @SerializedName("auth") val auth: AuthPayload? = null
) {
    fun resolvedAccessToken(): String {
        return auth?.accessToken?.takeIf { it.isNotBlank() }
            ?: token.orEmpty()
    }

    fun resolvedRefreshToken(): String? {
        return auth?.refreshToken?.takeIf { it.isNotBlank() }
            ?: refreshToken?.takeIf { it.isNotBlank() }
    }
}
```

Keep `RefreshResponse`, `RefreshErrorResponse`, `RefreshSessionResponse`, and `RefreshSessionData` aliases unchanged.

- [ ] **Step 6: Run contract tests and verify DTO behavior passes except header if not changed yet**

Run:

```bash
./gradlew app:testDebugUnitTest --tests "com.example.infinite_track.data.soucre.network.AuthApiContractTest"
```

Expected after DTO changes and before Task 3: token parsing tests PASS; header test FAIL while `AuthSessionApiService` still uses `android`. If the header is already corrected during this task, all tests PASS.

- [ ] **Step 7: Commit DTO and contract tests**

Run:

```bash
git add app/src/main/java/com/example/infinite_track/data/soucre/network/response/AuthPayload.kt \
  app/src/main/java/com/example/infinite_track/data/soucre/network/response/LoginResponse.kt \
  app/src/main/java/com/example/infinite_track/data/soucre/network/response/RefreshSessionResponse.kt \
  app/src/test/java/com/example/infinite_track/data/soucre/network/AuthApiContractTest.kt

git commit -m "test: cover Android auth payload contract" \
  -m "Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 2: Use primary token resolvers in repository login and refresh

**Files:**
- Modify: `app/src/main/java/com/example/infinite_track/data/repository/auth/AuthRepositoryImpl.kt`
- Test: `app/src/test/java/com/example/infinite_track/data/repository/auth/AuthRepositoryImplRefreshSessionTest.kt`

**Interfaces:**
- Consumes:
  - `UserData.resolvedAccessToken(): String`
  - `UserData.resolvedRefreshToken(): String?`
  - `AuthData.resolvedAccessToken(): String`
  - `AuthData.resolvedRefreshToken(): String?`
  - `AuthSessionApiService.refreshSession(request: RefreshSessionRequest): RefreshSessionResponse`
- Produces:
  - `AuthRepositoryImpl.login()` persists tokens resolved from primary `data.auth.*` first.
  - `AuthRepositoryImpl.refreshSession()` calls the auth-session lane and persists tokens resolved from primary `data.auth.*` first.

- [ ] **Step 1: Add repository tests for primary login and primary refresh payloads**

In `app/src/test/java/com/example/infinite_track/data/repository/auth/AuthRepositoryImplRefreshSessionTest.kt`, add import:

```kotlin
import com.example.infinite_track.data.soucre.network.response.AuthPayload
```

Add these tests near the existing login and refresh tests:

```kotlin
@Test
fun `login persists primary auth token payload before legacy direct fields`() = runBlocking {
    val userPreference = createUserPreference()
    val userDao = CapturingUserDao()
    val repository = createLoginRepository(
        userPreference = userPreference,
        userDao = userDao,
        userData = createUserData(refreshToken = "legacy-refresh").copy(
            token = "legacy-access",
            auth = AuthPayload(
                accessToken = "primary-access",
                refreshToken = "primary-refresh"
            )
        )
    )

    val result = repository.login(LoginRequest(email = "user@example.com", password = "secret"))

    assertTrue(result.isSuccess)
    assertEquals("primary-access", userPreference.getAuthToken().first())
    assertEquals("primary-refresh", userPreference.getRefreshToken().first())
    assertEquals("10", userPreference.getUserId().first())
}

@Test
fun `refresh session uses auth session api and stores primary auth token payload`() = runBlocking {
    val userPreference = createUserPreference()
    userPreference.saveSession(token = "old-access", userId = "10", refreshToken = "old-refresh")
    val fakeAuthSessionApi = FakeAuthSessionApiService(
        refreshSessionBlock = {
            RefreshSessionResponse(
                success = true,
                message = "ok",
                data = RefreshSessionData(
                    id = 10,
                    token = "legacy-new-access",
                    refreshToken = "legacy-new-refresh",
                    auth = AuthPayload(
                        accessToken = "primary-new-access",
                        refreshToken = "primary-new-refresh"
                    )
                )
            )
        }
    )

    val repository = AuthRepositoryImpl(
        userPreference = userPreference,
        apiService = FakeApiService(
            refreshBlock = {
                throw AssertionError("refresh must use AuthSessionApiService, not protected ApiService")
            }
        ),
        authSessionApiService = fakeAuthSessionApi,
        userDao = FakeUserDao()
    )

    val result = repository.refreshSession()

    assertTrue(result.isSuccess)
    assertEquals("primary-new-access", userPreference.getAuthToken().first())
    assertEquals("primary-new-refresh", userPreference.getRefreshToken().first())
    assertEquals("old-refresh", fakeAuthSessionApi.lastRefreshRequest?.refreshToken)
}
```

- [ ] **Step 2: Update test helper to allow auth payload values**

Replace the helper `createUserData(refreshToken: String?): UserData` with:

```kotlin
private fun createUserData(refreshToken: String?): UserData {
    return UserData(
        id = 10,
        fullName = "Test User",
        email = "user@example.com",
        roleName = "staff",
        positionName = "Engineer",
        programName = "Program",
        divisionName = "Division",
        nipNim = "12345",
        phone = "08123456789",
        photo = "photo.jpg",
        photoUpdatedAt = "2026-01-01T00:00:00Z",
        location = LocationData(
            latitude = -0.9,
            longitude = 119.8,
            radius = 100,
            description = "Office",
            categoryName = "WFO"
        ),
        token = "access-token",
        refreshToken = refreshToken,
        auth = null
    )
}
```

- [ ] **Step 3: Run the targeted repository tests and verify failure before implementation**

Run:

```bash
./gradlew app:testDebugUnitTest --tests "com.example.infinite_track.data.repository.auth.AuthRepositoryImplRefreshSessionTest"
```

Expected before repository changes: the new primary login test stores legacy values, and the new refresh-lane test throws `AssertionError("refresh must use AuthSessionApiService, not protected ApiService")`.

- [ ] **Step 4: Update repository imports**

In `app/src/main/java/com/example/infinite_track/data/repository/auth/AuthRepositoryImpl.kt`, replace:

```kotlin
import com.example.infinite_track.data.soucre.network.request.RefreshRequest
```

with:

```kotlin
import com.example.infinite_track.data.soucre.network.request.RefreshSessionRequest
```

- [ ] **Step 5: Update refreshSession implementation to use auth-session lane and resolver methods**

Inside `AuthRepositoryImpl.refreshSession()`, replace:

```kotlin
val refreshData = apiService.refresh(
    RefreshRequest(refreshToken = existingRefreshToken)
).data

if (refreshData.token.isBlank() || refreshData.id <= 0) {
    val error = IllegalStateException("Invalid refresh session payload")
    safeLogError("Refresh session returned invalid payload", error)
    return Result.failure(
        AuthRefreshException(
            kind = AuthRefreshFailureKind.TRANSIENT,
            reason = AuthRefreshFailureReason.INVALID_PAYLOAD,
            message = "Invalid refresh session payload",
            cause = error
        )
    )
}

val refreshedUserId = refreshData.id.toString()
val refreshTokenToStore = refreshData.refreshToken?.takeIf { it.isNotBlank() } ?: existingRefreshToken

userPreference.saveSession(
    token = refreshData.token,
    userId = refreshedUserId,
    refreshToken = refreshTokenToStore,
    lastRefreshAt = System.currentTimeMillis()
)
Result.success(
    AuthRefreshResult(
        token = refreshData.token,
        refreshToken = refreshTokenToStore,
        userId = refreshedUserId
    )
)
```

with:

```kotlin
val refreshData = authSessionApiService.refreshSession(
    RefreshSessionRequest(refreshToken = existingRefreshToken)
).data
val accessToken = refreshData.resolvedAccessToken()

if (accessToken.isBlank() || refreshData.id <= 0) {
    val error = IllegalStateException("Invalid refresh session payload")
    safeLogError("Refresh session returned invalid payload", error)
    return Result.failure(
        AuthRefreshException(
            kind = AuthRefreshFailureKind.TRANSIENT,
            reason = AuthRefreshFailureReason.INVALID_PAYLOAD,
            message = "Invalid refresh session payload",
            cause = error
        )
    )
}

val refreshedUserId = refreshData.id.toString()
val refreshTokenToStore = refreshData.resolvedRefreshToken() ?: existingRefreshToken

userPreference.saveSession(
    token = accessToken,
    userId = refreshedUserId,
    refreshToken = refreshTokenToStore,
    lastRefreshAt = System.currentTimeMillis()
)
Result.success(
    AuthRefreshResult(
        token = accessToken,
        refreshToken = refreshTokenToStore,
        userId = refreshedUserId
    )
)
```

- [ ] **Step 6: Update login implementation to use resolver methods**

Inside `AuthRepositoryImpl.login()`, replace:

```kotlin
val accessToken = loginData.token.takeIf { it.isNotBlank() }
    ?: return Result.failure(IllegalStateException("Login response missing usable access token"))
val refreshToken = loginData.refreshToken?.takeIf { it.isNotBlank() }
```

with:

```kotlin
val accessToken = loginData.resolvedAccessToken().takeIf { it.isNotBlank() }
    ?: return Result.failure(IllegalStateException("Login response missing usable access token"))
val refreshToken = loginData.resolvedRefreshToken()
```

- [ ] **Step 7: Run repository tests and verify pass**

Run:

```bash
./gradlew app:testDebugUnitTest --tests "com.example.infinite_track.data.repository.auth.AuthRepositoryImplRefreshSessionTest"
```

Expected after implementation: PASS. If Gradle loopback blocks execution, record the exact Gradle error and continue to static review and remaining tasks.

- [ ] **Step 8: Commit repository token resolver integration**

Run:

```bash
git add app/src/main/java/com/example/infinite_track/data/repository/auth/AuthRepositoryImpl.kt \
  app/src/test/java/com/example/infinite_track/data/repository/auth/AuthRepositoryImplRefreshSessionTest.kt

git commit -m "fix: prefer backend auth payload in Android session flow" \
  -m "Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 3: Normalize auth-session client type to mobile and preserve refresh orchestration

**Files:**
- Modify: `app/src/main/java/com/example/infinite_track/data/soucre/network/retrofit/AuthSessionApiService.kt`
- Test: `app/src/test/java/com/example/infinite_track/data/soucre/network/AuthApiContractTest.kt`
- Test: `app/src/test/java/com/example/infinite_track/di/auth/AuthRefreshInterceptorTest.kt`
- Test: `app/src/test/java/com/example/infinite_track/di/auth/RefreshSingleFlightCoordinatorTest.kt`

**Interfaces:**
- Consumes: `AuthApiContractTest.auth session refresh endpoint uses canonical mobile client type header` from Task 1.
- Produces: `AuthSessionApiService.refreshSession()` annotation uses `@Headers("X-Client-Type: mobile")`.

- [ ] **Step 1: Run the header contract test and verify failure before header change**

Run:

```bash
./gradlew app:testDebugUnitTest --tests "com.example.infinite_track.data.soucre.network.AuthApiContractTest.auth session refresh endpoint uses canonical mobile client type header"
```

Expected before header change: FAIL because `AuthSessionApiService.refreshSession()` contains `X-Client-Type: android`.

- [ ] **Step 2: Change auth-session client type header**

In `app/src/main/java/com/example/infinite_track/data/soucre/network/retrofit/AuthSessionApiService.kt`, replace:

```kotlin
@Headers("X-Client-Type: android")
```

with:

```kotlin
@Headers("X-Client-Type: mobile")
```

- [ ] **Step 3: Run header and orchestration tests**

Run:

```bash
./gradlew app:testDebugUnitTest \
  --tests "com.example.infinite_track.data.soucre.network.AuthApiContractTest" \
  --tests "com.example.infinite_track.di.auth.AuthRefreshInterceptorTest" \
  --tests "com.example.infinite_track.di.auth.RefreshSingleFlightCoordinatorTest"
```

Expected after header change: PASS. These tests confirm canonical client type, protected request retry behavior, temporary refresh failure preservation, forced re-auth behavior, and single-flight refresh dedupe.

- [ ] **Step 4: Commit client-type normalization**

Run:

```bash
git add app/src/main/java/com/example/infinite_track/data/soucre/network/retrofit/AuthSessionApiService.kt \
  app/src/test/java/com/example/infinite_track/data/soucre/network/AuthApiContractTest.kt

git commit -m "fix: use mobile client type for auth session refresh" \
  -m "Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 4: Update ADR and runtime evidence notes

**Files:**
- Modify: `docs/adr/ADR-XXX-android-refresh-session-compat.md`
- Modify: `docs/auth-runtime-evidence/RUN_2026-05-30.md`

**Interfaces:**
- Consumes: implementation from Tasks 1-3.
- Produces: docs that state Android's final Family A consumer contract and honest verification status.

- [ ] **Step 1: Update ADR decision section**

In `docs/adr/ADR-XXX-android-refresh-session-compat.md`, replace decision bullets 2 and 3:

```markdown
2. Login reads `refresh_token` from the JSON response body.
3. Refresh uses `POST /api/auth/refresh` with body `{ "refresh_token": "..." }`.
```

with:

```markdown
2. Login and refresh prefer the backend primary native/mobile token payload: `data.auth.access_token` and `data.auth.refresh_token`.
3. Legacy direct fields `data.token` and `data.refresh_token` remain compatibility fallbacks during backend cutover.
4. Refresh uses `POST /api/auth/refresh` with body `{ "refresh_token": "..." }` and canonical `X-Client-Type: mobile`.
```

Then renumber the following bullets so the list remains sequential.

- [ ] **Step 2: Update ADR consequences section**

In the same ADR, add this bullet under `## Consequences`:

```markdown
- Android tests now treat `data.auth.*` as the primary mobile contract and legacy direct token fields as fallback compatibility only.
```

- [ ] **Step 3: Update runtime evidence document with contract-hardening note**

In `docs/auth-runtime-evidence/RUN_2026-05-30.md`, add this section after the status paragraph:

```markdown
## Contract Hardening Note — 2026-06-21

Android continuation work now targets the final `INF-145` native/mobile payload priority:

- primary access token: `data.auth.access_token`
- primary refresh token: `data.auth.refresh_token`
- compatibility fallback access token: `data.token`
- compatibility fallback refresh token: `data.refresh_token`
- canonical mobile client header: `X-Client-Type: mobile`

This note does not claim end-to-end runtime success. Scenarios below remain `Observed: _pending_` until run against a reachable backend environment with redacted evidence.
```

- [ ] **Step 4: Run docs diff review**

Run:

```bash
git diff -- docs/adr/ADR-XXX-android-refresh-session-compat.md docs/auth-runtime-evidence/RUN_2026-05-30.md
```

Expected: diff only mentions token payload priority, legacy fallback, canonical mobile header, and verification status. No secret values appear.

- [ ] **Step 5: Commit docs updates**

Run:

```bash
git add docs/adr/ADR-XXX-android-refresh-session-compat.md \
  docs/auth-runtime-evidence/RUN_2026-05-30.md

git commit -m "docs: clarify Android auth payload contract" \
  -m "Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 5: Run verification and prepare handoff evidence

**Files:**
- Read: `app/src/main/java/com/example/infinite_track/data/soucre/network/response/AuthPayload.kt`
- Read: `app/src/main/java/com/example/infinite_track/data/soucre/network/response/LoginResponse.kt`
- Read: `app/src/main/java/com/example/infinite_track/data/soucre/network/response/RefreshSessionResponse.kt`
- Read: `app/src/main/java/com/example/infinite_track/data/repository/auth/AuthRepositoryImpl.kt`
- Read: `app/src/main/java/com/example/infinite_track/data/soucre/network/retrofit/AuthSessionApiService.kt`
- Read: `docs/adr/ADR-XXX-android-refresh-session-compat.md`
- Read: `docs/auth-runtime-evidence/RUN_2026-05-30.md`

**Interfaces:**
- Consumes: all code and docs from Tasks 1-4.
- Produces: final verification summary and PR-ready notes.

- [ ] **Step 1: Run targeted Family A unit tests**

Run:

```bash
./gradlew app:testDebugUnitTest \
  --tests "com.example.infinite_track.data.soucre.network.AuthApiContractTest" \
  --tests "com.example.infinite_track.data.repository.auth.AuthRepositoryImplRefreshSessionTest" \
  --tests "com.example.infinite_track.di.auth.AuthRefreshInterceptorTest" \
  --tests "com.example.infinite_track.di.auth.RefreshSingleFlightCoordinatorTest" \
  --tests "com.example.infinite_track.domain.use_case.auth.CheckSessionUseCaseTest" \
  --tests "com.example.infinite_track.domain.manager.SessionManagerTest" \
  --tests "com.example.infinite_track.presentation.screen.auth.LoginViewModelReauthTest" \
  --tests "com.example.infinite_track.presentation.screen.splash.SplashViewModelTest"
```

Expected in healthy Gradle environment: PASS. If Gradle fails with loopback before executing tests, record the exact failure as environment-blocked verification.

- [ ] **Step 2: Run broader local gates**

Run:

```bash
./gradlew app:testDebugUnitTest
./gradlew app:assembleDebug
./gradlew app:lint
```

Expected in healthy Gradle environment: PASS. If any command cannot start because of the loopback issue, do not claim test failure; report the Gradle startup blocker.

- [ ] **Step 3: Check code diff for scope discipline**

Run:

```bash
git diff --stat origin/develop...HEAD
git diff --name-only origin/develop...HEAD
```

Expected changed paths are limited to auth/session DTOs, repository, auth-session service, tests, ADR/evidence docs, and the spec/plan docs.

- [ ] **Step 4: Inspect for accidental secret exposure**

Run:

```bash
git diff origin/develop...HEAD -- docs app/src/main/java app/src/test/java
```

Expected: no access token values, refresh token values, email, full name, phone, NIP/NIM, Firebase config, keystore, Mapbox token, or auth-bearing logs. Test fixtures must keep redacted values such as `primary-access-token-redacted`.

- [ ] **Step 5: Prepare final notes**

Use this final summary shape:

```markdown
Fact
- Continued Android Family A from existing INF-147 chain.
- Android now prioritizes `data.auth.access_token` / `data.auth.refresh_token` with legacy fallback.
- Auth-session refresh uses canonical `X-Client-Type: mobile`.

Mismatch
- Linear INF-147 still shows Backlog despite merged Android PR history and this continuation work.

Risk
- Runtime Family A behavior still needs emulator/device/backend evidence before Done.

Needs Verification
- Include exact Gradle command results.
- If Gradle loopback blocked tests, include the exact `Unable to establish loopback connection` failure.
- Include missing runtime scenarios: expired access -> silent refresh success, invalid/revoked refresh -> full re-auth, inactivity > 48h -> full re-auth, offline/server down -> no false logout, concurrent refresh storm prevention.

Recommendation
- Open PR from `fix/android-family-a-access-session-continuation` to `develop` after local Gradle environment can run targeted tests, or mark PR as requiring human runtime verification if environment remains blocked.
```

- [ ] **Step 6: Commit final evidence changes only if a file changed**

If Task 5 updates no files, do not create an empty commit. If an evidence file is updated with fresh redacted runtime or command evidence, run:

```bash
git add docs/auth-runtime-evidence/RUN_2026-05-30.md

git commit -m "docs: update Android auth runtime evidence" \
  -m "Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```
