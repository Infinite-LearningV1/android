# INF-264 Layer 5 Attendance Submit Architecture Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete the Layer 5 attendance submit architecture with typed `AttendanceSubmitCommand` / `AttendanceSubmitResult` / `AttendanceSubmitFailure` contracts, one `SubmitAttendanceUseCase` orchestrator, repository failure mapping, and a thinned `AttendanceViewModel` — preserving all existing submit semantics.

**Architecture:** Move business request construction out of Presentation into a single domain use case; replace `Result<ActiveAttendanceSession>` with typed sealed contracts; classify backend/provider failures in the data layer via `AttendanceApiFailureMapper`; map typed failures to localized copy in presentation via `AttendanceSubmitUiMapper`.

**Tech Stack:** Kotlin, Hilt, Retrofit/OkHttp, kotlinx-coroutines-test, JUnit4, hand-rolled fakes (repo convention — no mockk/mockito).

**Spec:** Linear INF-264 (issue description is the locked spec). Baseline: `develop` @ `497988e`.

## Global Constraints

- Every Gradle command on this machine MUST be prefixed with the JVM workaround (see `$GRADLE_ENV` below); without it the build fails with `Unable to establish loopback connection` or kapt `IllegalAccessError`.
- Preserve existing Layer 5 semantics exactly: face success → `Submitting` → one backend mutation → `Success`/`RetryableFailure`; refresh + geofence reconcile ONLY after backend success.
- Domain must not import Retrofit, `HttpException`, DTOs, Android Context, or Compose.
- Do not display raw JSON/stack traces to users; `BackendRejected.safeReason` carries only the backend `message` string.
- Backend remains the final attendance authority; no client-side final validation.
- Copy is Indonesian, matching existing tone (`"Check-in gagal"`, `"Silakan coba lagi."`).
- Scope exclusions (do NOT touch): face verification internals, geofence runtime internals, auth/session flow, offline queueing, automatic retry.
- One commit per task, message format `feat(attendance): ...` / `refactor(attendance): ...` / `test(attendance): ...`.

**`$GRADLE_ENV` (PowerShell, prepend to every gradlew call):**

```powershell
$env:JAVA_TOOL_OPTIONS='-Djdk.net.unixdomain.tmpdir=C:\Users\Public --add-opens=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED --add-opens=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED --add-opens=jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED --add-opens=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED --add-opens=jdk.compiler/com.sun.tools.javac.jvm=ALL-UNNAMED --add-opens=jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED --add-opens=jdk.compiler/com.sun.tools.javac.model=ALL-UNNAMED --add-opens=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED --add-opens=jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED --add-opens=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED --add-opens=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED'
```

---

### Task 1: Move `AttendanceActionIntent` to domain

The spec's `AttendanceSubmitCommand` (domain) references `AttendanceActionIntent`, which currently lives in `presentation/screen/attendance/AttendanceActionState.kt:3`. Domain must not depend on presentation, so the enum moves to domain with a deprecated typealias bridge so no call site breaks.

**Files:**
- Create: `app/src/main/java/com/example/infinite_track/domain/model/attendance/AttendanceActionIntent.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceActionState.kt` (delete enum declaration at lines 3-6, add typealias)

**Interfaces:**
- Produces: `com.example.infinite_track.domain.model.attendance.AttendanceActionIntent` enum with entries `CHECK_IN`, `CHECK_OUT` — consumed by Tasks 2, 4, 5, 6, 7.

- [ ] **Step 1: Create the domain enum**

```kotlin
// app/src/main/java/com/example/infinite_track/domain/model/attendance/AttendanceActionIntent.kt
package com.example.infinite_track.domain.model.attendance

/** Canonical attendance mutation intent shared by presentation and Layer 5 domain contracts. */
enum class AttendanceActionIntent {
    CHECK_IN,
    CHECK_OUT
}
```

- [ ] **Step 2: Replace the presentation enum with a typealias**

In `AttendanceActionState.kt`, delete the `enum class AttendanceActionIntent { CHECK_IN, CHECK_OUT }` declaration (top of file) and put in its place:

```kotlin
typealias AttendanceActionIntent =
    com.example.infinite_track.domain.model.attendance.AttendanceActionIntent
```

Keep the existing `readyLabel()` / `submittingMessage()` extension functions in this file unchanged — they are presentation copy and stay in presentation.

- [ ] **Step 3: Compile and run the attendance test packages**

```powershell
<$GRADLE_ENV>; .\gradlew.bat app:compileDebugKotlin app:testDebugUnitTest --console=plain
```

Expected: BUILD SUCCESSFUL (typealias keeps every existing import/usage source-compatible).

- [ ] **Step 4: Commit**

```bash
git add -A && git commit -m "refactor(attendance): move AttendanceActionIntent to domain with presentation typealias"
```

---

### Task 2: Domain submit contracts (`Command`, `Result`, `Failure`)

**Files:**
- Create: `app/src/main/java/com/example/infinite_track/domain/model/attendance/AttendanceSubmitCommand.kt`
- Create: `app/src/main/java/com/example/infinite_track/domain/model/attendance/AttendanceSubmitResult.kt`
- Create: `app/src/main/java/com/example/infinite_track/domain/model/attendance/AttendanceSubmitFailure.kt`
- Test: `app/src/test/java/com/example/infinite_track/domain/model/attendance/AttendanceSubmitContractTest.kt`

**Interfaces:**
- Consumes: `AttendanceActionIntent` (Task 1), `AuthoritativeTargetLocation`, `WorkMode`, `ActiveAttendanceSession` (existing).
- Produces (exact shapes below): `AttendanceSubmitCommand.CheckIn(workMode, authoritativeTarget)`, `AttendanceSubmitCommand.CheckOut(activeAttendanceId)`, `AttendanceSubmitResult.Success(intent, session)`, `AttendanceSubmitResult.Failure(intent, failure)`, `AttendanceSubmitFailure` variants listed below.

- [ ] **Step 1: Write the failing contract test**

```kotlin
// app/src/test/java/com/example/infinite_track/domain/model/attendance/AttendanceSubmitContractTest.kt
package com.example.infinite_track.domain.model.attendance

import com.example.infinite_track.domain.model.location.DistanceMeters
import com.example.infinite_track.domain.model.location.GeoCoordinate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AttendanceSubmitContractTest {

    private fun target(mode: WorkMode, wfa: ApprovedWfaTargetContext? = null) =
        AuthoritativeTargetLocation(
            targetId = TargetLocationId("t-1"),
            mode = mode,
            source = when (mode) {
                WorkMode.WFO -> TargetLocationSource.STATUS_TODAY
                WorkMode.WFH -> TargetLocationSource.ADMIN_PROFILE
                WorkMode.WFA -> TargetLocationSource.APPROVED_WFA_BOOKING
            },
            coordinate = GeoCoordinate(-0.89, 119.87),
            radius = DistanceMeters(100.0),
            displayName = "Target",
            approvedWfaContext = wfa
        )

    @Test
    fun `check-in command exposes CHECK_IN intent`() {
        val command = AttendanceSubmitCommand.CheckIn(
            workMode = WorkMode.WFO,
            authoritativeTarget = target(WorkMode.WFO)
        )
        assertEquals(AttendanceActionIntent.CHECK_IN, command.intent)
    }

    @Test
    fun `check-out command exposes CHECK_OUT intent and nullable id`() {
        val command = AttendanceSubmitCommand.CheckOut(activeAttendanceId = null)
        assertEquals(AttendanceActionIntent.CHECK_OUT, command.intent)
        assertNull(command.activeAttendanceId)
    }

    @Test
    fun `backend rejected failure carries safe reason only`() {
        val failure = AttendanceSubmitFailure.BackendRejected(safeReason = "Absensi ditolak")
        assertEquals("Absensi ditolak", failure.safeReason)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```powershell
<$GRADLE_ENV>; .\gradlew.bat app:testDebugUnitTest --tests "com.example.infinite_track.domain.model.attendance.AttendanceSubmitContractTest" --console=plain
```

Expected: FAIL — unresolved references `AttendanceSubmitCommand`, `AttendanceSubmitFailure`.

- [ ] **Step 3: Write the three contract files**

```kotlin
// app/src/main/java/com/example/infinite_track/domain/model/attendance/AttendanceSubmitCommand.kt
package com.example.infinite_track.domain.model.attendance

/**
 * Canonical Layer 5 submit command. Presentation builds this from already-resolved
 * preparation state; all business validation happens in SubmitAttendanceUseCase.
 */
sealed interface AttendanceSubmitCommand {
    val intent: AttendanceActionIntent

    data class CheckIn(
        val workMode: WorkMode,
        val authoritativeTarget: AuthoritativeTargetLocation
    ) : AttendanceSubmitCommand {
        override val intent: AttendanceActionIntent = AttendanceActionIntent.CHECK_IN
    }

    data class CheckOut(
        val activeAttendanceId: Int?
    ) : AttendanceSubmitCommand {
        override val intent: AttendanceActionIntent = AttendanceActionIntent.CHECK_OUT
    }
}
```

```kotlin
// app/src/main/java/com/example/infinite_track/domain/model/attendance/AttendanceSubmitResult.kt
package com.example.infinite_track.domain.model.attendance

/** Typed outcome of one Layer 5 backend mutation attempt. */
sealed interface AttendanceSubmitResult {
    data class Success(
        val intent: AttendanceActionIntent,
        val session: ActiveAttendanceSession
    ) : AttendanceSubmitResult

    data class Failure(
        val intent: AttendanceActionIntent,
        val failure: AttendanceSubmitFailure
    ) : AttendanceSubmitResult
}
```

```kotlin
// app/src/main/java/com/example/infinite_track/domain/model/attendance/AttendanceSubmitFailure.kt
package com.example.infinite_track.domain.model.attendance

/**
 * Typed Layer 5 submit failure classification. Transport/provider types
 * (HttpException, DTOs) must never cross this boundary.
 */
sealed interface AttendanceSubmitFailure {
    // Local precondition failures (before any backend call)
    data object CurrentLocationUnavailable : AttendanceSubmitFailure
    data object SessionUnavailable : AttendanceSubmitFailure
    data object ActiveAttendanceUnavailable : AttendanceSubmitFailure
    data object TargetModeMismatch : AttendanceSubmitFailure
    data object WfaBookingRequired : AttendanceSubmitFailure

    // Backend business rejections
    data object DuplicateAttendance : AttendanceSubmitFailure
    data object OutsideAllowedRadius : AttendanceSubmitFailure
    data object WfaBookingRejected : AttendanceSubmitFailure
    data object AlreadyCheckedOut : AttendanceSubmitFailure

    // Transport failures
    data object NetworkUnavailable : AttendanceSubmitFailure
    data object ServerUnavailable : AttendanceSubmitFailure

    /** Recognized backend rejection whose category is unknown; [safeReason] is the backend `message` string only. */
    data class BackendRejected(val safeReason: String) : AttendanceSubmitFailure
    data object Unknown : AttendanceSubmitFailure
}
```

Note: `TargetModeMismatch` is added beyond the spec's minimum list because the spec requires "Target mode mismatch fails before repository call" and no listed variant represents it. `WfaBookingRequired` is grouped as a local precondition because the domain guard fires before the backend call.

- [ ] **Step 4: Run test to verify it passes**

Same command as Step 2. Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(attendance): add typed Layer 5 submit command, result, and failure contracts"
```

---

### Task 3: `AttendanceApiFailureMapper` in data layer

**Files:**
- Create: `app/src/main/java/com/example/infinite_track/data/mapper/attendance/AttendanceApiFailureMapper.kt`
- Test: `app/src/test/java/com/example/infinite_track/data/mapper/attendance/AttendanceApiFailureMapperTest.kt`

**Interfaces:**
- Consumes: `AttendanceSubmitFailure` (Task 2).
- Produces: `object AttendanceApiFailureMapper` with:
  - `fun mapHttp(code: Int, backendMessage: String?): AttendanceSubmitFailure`
  - `fun mapThrowable(throwable: Throwable): AttendanceSubmitFailure` (handles `IOException` → `NetworkUnavailable`, else `Unknown`; the repository catches `HttpException` itself and calls `mapHttp`).

- [ ] **Step 1: Write the failing tests**

```kotlin
// app/src/test/java/com/example/infinite_track/data/mapper/attendance/AttendanceApiFailureMapperTest.kt
package com.example.infinite_track.data.mapper.attendance

import com.example.infinite_track.domain.model.attendance.AttendanceSubmitFailure
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException

class AttendanceApiFailureMapperTest {

    @Test
    fun `already checked out message maps to AlreadyCheckedOut`() {
        assertEquals(
            AttendanceSubmitFailure.AlreadyCheckedOut,
            AttendanceApiFailureMapper.mapHttp(400, "Anda sudah melakukan check-out hari ini")
        )
    }

    @Test
    fun `duplicate check-in message maps to DuplicateAttendance`() {
        assertEquals(
            AttendanceSubmitFailure.DuplicateAttendance,
            AttendanceApiFailureMapper.mapHttp(400, "Anda sudah melakukan check-in hari ini")
        )
    }

    @Test
    fun `outside radius message maps to OutsideAllowedRadius`() {
        assertEquals(
            AttendanceSubmitFailure.OutsideAllowedRadius,
            AttendanceApiFailureMapper.mapHttp(
                400,
                "Anda berada di luar radius lokasi yang diizinkan untuk check-out"
            )
        )
    }

    @Test
    fun `booking rejection message maps to WfaBookingRejected`() {
        assertEquals(
            AttendanceSubmitFailure.WfaBookingRejected,
            AttendanceApiFailureMapper.mapHttp(400, "Booking WFA Anda belum disetujui")
        )
    }

    @Test
    fun `5xx maps to ServerUnavailable regardless of message`() {
        assertEquals(
            AttendanceSubmitFailure.ServerUnavailable,
            AttendanceApiFailureMapper.mapHttp(503, "Internal error detail that must not leak")
        )
    }

    @Test
    fun `unrecognized 4xx with message maps to BackendRejected carrying only the message`() {
        val failure = AttendanceApiFailureMapper.mapHttp(422, "Absensi ditolak kebijakan kantor")
        assertEquals(
            AttendanceSubmitFailure.BackendRejected("Absensi ditolak kebijakan kantor"),
            failure
        )
    }

    @Test
    fun `unrecognized 4xx without message maps to Unknown`() {
        assertEquals(AttendanceSubmitFailure.Unknown, AttendanceApiFailureMapper.mapHttp(400, null))
        assertEquals(AttendanceSubmitFailure.Unknown, AttendanceApiFailureMapper.mapHttp(400, "  "))
    }

    @Test
    fun `IOException maps to NetworkUnavailable`() {
        assertEquals(
            AttendanceSubmitFailure.NetworkUnavailable,
            AttendanceApiFailureMapper.mapThrowable(SocketTimeoutException("timeout"))
        )
        assertEquals(
            AttendanceSubmitFailure.NetworkUnavailable,
            AttendanceApiFailureMapper.mapThrowable(IOException("no route"))
        )
    }

    @Test
    fun `non-IO throwable maps to Unknown`() {
        assertEquals(
            AttendanceSubmitFailure.Unknown,
            AttendanceApiFailureMapper.mapThrowable(IllegalStateException("boom"))
        )
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```powershell
<$GRADLE_ENV>; .\gradlew.bat app:testDebugUnitTest --tests "com.example.infinite_track.data.mapper.attendance.AttendanceApiFailureMapperTest" --console=plain
```

Expected: FAIL — `AttendanceApiFailureMapper` unresolved.

- [ ] **Step 3: Implement the mapper**

```kotlin
// app/src/main/java/com/example/infinite_track/data/mapper/attendance/AttendanceApiFailureMapper.kt
package com.example.infinite_track.data.mapper.attendance

import com.example.infinite_track.domain.model.attendance.AttendanceSubmitFailure
import java.io.IOException
import java.util.Locale

/**
 * Classifies transport/backend failures into typed domain failures.
 * Message-pattern matching is a compatibility bridge until backend exposes
 * machine-readable error codes; unrecognized safe messages fall back to
 * BackendRejected(message) so backend copy is preserved for the user.
 */
object AttendanceApiFailureMapper {

    fun mapHttp(code: Int, backendMessage: String?): AttendanceSubmitFailure {
        if (code >= 500) return AttendanceSubmitFailure.ServerUnavailable

        val message = backendMessage?.trim().orEmpty()
        if (message.isEmpty()) return AttendanceSubmitFailure.Unknown

        val normalized = message.lowercase(Locale.ROOT)
        return when {
            "check-out" in normalized && "sudah" in normalized ->
                AttendanceSubmitFailure.AlreadyCheckedOut

            "sudah" in normalized && ("check-in" in normalized || "absen" in normalized) ->
                AttendanceSubmitFailure.DuplicateAttendance

            "luar radius" in normalized ->
                AttendanceSubmitFailure.OutsideAllowedRadius

            "booking" in normalized && (
                "belum disetujui" in normalized ||
                    "ditolak" in normalized ||
                    "tidak valid" in normalized
                ) ->
                AttendanceSubmitFailure.WfaBookingRejected

            "booking" in normalized && ("wajib" in normalized || "diperlukan" in normalized) ->
                AttendanceSubmitFailure.WfaBookingRequired

            else -> AttendanceSubmitFailure.BackendRejected(safeReason = message)
        }
    }

    fun mapThrowable(throwable: Throwable): AttendanceSubmitFailure = when (throwable) {
        is IOException -> AttendanceSubmitFailure.NetworkUnavailable
        else -> AttendanceSubmitFailure.Unknown
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Same command as Step 2. Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(attendance): add AttendanceApiFailureMapper for typed backend failure classification"
```

---

### Task 4: Typed repository contract and implementation

**Files:**
- Modify: `app/src/main/java/com/example/infinite_track/domain/repository/AttendanceRepository.kt` (checkIn at line 28, checkOut at lines 36-40)
- Modify: `app/src/main/java/com/example/infinite_track/data/repository/attendance/AttendanceRepositoryImpl.kt` (checkIn lines 112-137, checkOut lines 143-175)
- Test: `app/src/test/java/com/example/infinite_track/data/repository/attendance/AttendanceRepositoryImplSubmitTest.kt`

**Interfaces:**
- Consumes: contracts from Task 2, mapper from Task 3.
- Produces (later tasks call these exact signatures):
  - `suspend fun checkIn(request: AttendanceRequestModel): AttendanceSubmitResult`
  - `suspend fun checkOut(attendanceId: Int, latitude: Double, longitude: Double): AttendanceSubmitResult`
- `getTodayStatus`, `getActiveAttendanceId`, `sendLocationEvent` keep their current signatures.

- [ ] **Step 1: Write the failing repository submit tests**

Follow the exact fake pattern of `AttendanceRepositoryImplTodayStatusCacheTest.kt` (temp-file DataStores + a hand-rolled `ApiService` fake; copy its `setUp`/`tearDown` and unimplemented-member stubs). Test class skeleton and cases:

```kotlin
// app/src/test/java/com/example/infinite_track/data/repository/attendance/AttendanceRepositoryImplSubmitTest.kt
package com.example.infinite_track.data.repository.attendance

// imports: mirror AttendanceRepositoryImplTodayStatusCacheTest.kt plus:
import com.example.infinite_track.domain.model.attendance.AttendanceActionIntent
import com.example.infinite_track.domain.model.attendance.AttendanceRequestModel
import com.example.infinite_track.domain.model.attendance.AttendanceSubmitFailure
import com.example.infinite_track.domain.model.attendance.AttendanceSubmitResult
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class AttendanceRepositoryImplSubmitTest {
    // setUp/tearDown identical to AttendanceRepositoryImplTodayStatusCacheTest

    private fun httpException(code: Int, message: String): HttpException {
        val body = """{"success":false,"message":"$message"}"""
            .toResponseBody("application/json".toMediaType())
        return HttpException(Response.error<Any>(code, body))
    }

    private fun checkInRequest() = AttendanceRequestModel(
        categoryId = 1,
        latitude = -0.89,
        longitude = 119.87,
        notes = "Check-in via mobile app",
        bookingId = null,
        type = "checkin"
    )

    // Case 1: successful check-in returns Success(CHECK_IN), stores active
    //   attendance id in AttendancePreference, clears today-status cache.
    // Case 2: successful checkout returns Success(CHECK_OUT), clears active
    //   attendance id and today-status cache.
    // Case 3: check-in HttpException 400 "Anda sudah melakukan check-in hari ini"
    //   -> Failure(CHECK_IN, DuplicateAttendance); preference untouched.
    // Case 4: checkout HttpException 400 "Anda berada di luar radius lokasi yang
    //   diizinkan untuk check-out" -> Failure(CHECK_OUT, OutsideAllowedRadius);
    //   active attendance id NOT cleared.
    // Case 5: checkout HttpException 400 "Anda sudah melakukan check-out hari ini"
    //   -> Failure(CHECK_OUT, AlreadyCheckedOut).
    // Case 6: check-in HttpException 500 -> Failure(CHECK_IN, ServerUnavailable).
    // Case 7: fake ApiService throws IOException -> Failure(intent, NetworkUnavailable).
    // Case 8: response.success == false with message -> Failure(intent,
    //   BackendRejected(message)) — no exception path.
    // Each case asserts the returned type is AttendanceSubmitResult (compile-time
    //   proof no Result<T>/HttpException leaks to the caller).
}
```

Write all 8 cases fully (assert preference state via `attendancePreference.getActiveAttendanceId().first()` and cache via `todayStatusPreference.getTodayStatusCache().first()`).

The fake `ApiService` for these tests exposes settable behavior:

```kotlin
private class SubmitFakeApiService : ApiService {
    var checkInBehavior: () -> AttendanceResponse = { error("unused") }
    var checkOutBehavior: () -> AttendanceResponse = { error("unused") }
    override suspend fun checkIn(request: AttendanceRequest): AttendanceResponse = checkInBehavior()
    override suspend fun checkOut(attendanceId: Int, request: CheckOutRequestDto): AttendanceResponse = checkOutBehavior()
    // every other ApiService member: TODO("not used") — copy list from
    // AttendanceRepositoryImplTodayStatusCacheTest's fake.
}
```

- [ ] **Step 2: Run test to verify it fails**

```powershell
<$GRADLE_ENV>; .\gradlew.bat app:testDebugUnitTest --tests "com.example.infinite_track.data.repository.attendance.AttendanceRepositoryImplSubmitTest" --console=plain
```

Expected: FAIL — compile error (checkIn still returns `Result<ActiveAttendanceSession>`).

- [ ] **Step 3: Change the repository interface**

In `AttendanceRepository.kt`, replace the two signatures:

```kotlin
import com.example.infinite_track.domain.model.attendance.AttendanceSubmitResult

/**
 * Performs check-in mutation. Returns a typed submit outcome; transport
 * failures are classified in the data layer and never leak upward.
 */
suspend fun checkIn(request: AttendanceRequestModel): AttendanceSubmitResult

/**
 * Performs check-out mutation for [attendanceId] with fresh coordinates.
 */
suspend fun checkOut(
    attendanceId: Int,
    latitude: Double,
    longitude: Double
): AttendanceSubmitResult
```

- [ ] **Step 4: Rewrite `AttendanceRepositoryImpl.checkIn` / `checkOut`**

```kotlin
override suspend fun checkIn(request: AttendanceRequestModel): AttendanceSubmitResult {
    val intent = AttendanceActionIntent.CHECK_IN
    return try {
        val response = apiService.checkIn(request.toDto())
        if (response.success) {
            attendancePreference.saveActiveAttendanceId(response.data.idAttendance)
            todayStatusPreference.clearTodayStatusCache()
            AttendanceSubmitResult.Success(intent, response.data.toActiveSession())
        } else {
            AttendanceSubmitResult.Failure(
                intent,
                AttendanceApiFailureMapper.mapHttp(code = 200, backendMessage = response.message)
            )
        }
    } catch (e: HttpException) {
        Log.e(TAG, "HTTP Error during check-in", e)
        AttendanceSubmitResult.Failure(
            intent,
            AttendanceApiFailureMapper.mapHttp(e.code(), extractErrorMessage(e))
        )
    } catch (e: Exception) {
        Log.e(TAG, "Error during check-in", e)
        AttendanceSubmitResult.Failure(intent, AttendanceApiFailureMapper.mapThrowable(e))
    }
}
```

`checkOut` follows the same shape with `intent = AttendanceActionIntent.CHECK_OUT`, existing DTO construction, and on success `attendancePreference.clearActiveAttendanceId()` + `todayStatusPreference.clearTodayStatusCache()`.

Note: `extractErrorMessage` currently falls back to `"HTTP <code> <message>"`; change its fallback to return `null` (signature `HttpException -> String?`) so raw HTTP status text is never shown as a "safe reason" — `mapHttp` turns null message into `Unknown` (or `ServerUnavailable` for 5xx). `getTodayStatus` also calls `extractErrorMessage`; keep its behavior by substituting `?: "HTTP ${e.code()}"` at that call site.

Imports to add in the impl: `AttendanceActionIntent`, `AttendanceSubmitResult`, `AttendanceApiFailureMapper`.

This will temporarily break `CheckInUseCase`/`CheckOutUseCase` (they expect `Result<T>`); fix minimally in this task by updating them to adapt (they are deleted in Task 5):

```kotlin
// CheckInUseCase - temporary adapter body until Task 5 removes it
val submitResult = attendanceRepository.checkIn(updatedRequest)
return when (submitResult) {
    is AttendanceSubmitResult.Success -> Result.success(submitResult.session)
    is AttendanceSubmitResult.Failure -> Result.failure(Exception(submitResult.failure.toString()))
}
```

(same adapter shape in `CheckOutUseCase`).

- [ ] **Step 5: Run the new tests plus the existing cache test**

```powershell
<$GRADLE_ENV>; .\gradlew.bat app:testDebugUnitTest --tests "com.example.infinite_track.data.repository.attendance.*" --console=plain
```

Expected: PASS.

- [ ] **Step 6: Full unit test run to catch signature fallout**

```powershell
<$GRADLE_ENV>; .\gradlew.bat app:testDebugUnitTest --console=plain
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add -A && git commit -m "feat(attendance): return typed AttendanceSubmitResult from repository submit mutations"
```

---

### Task 5: `SubmitAttendanceUseCase` replaces `CheckInUseCase`/`CheckOutUseCase`

**Files:**
- Create: `app/src/main/java/com/example/infinite_track/domain/use_case/attendance/SubmitAttendanceUseCase.kt`
- Delete: `app/src/main/java/com/example/infinite_track/domain/use_case/attendance/CheckInUseCase.kt`
- Delete: `app/src/main/java/com/example/infinite_track/domain/use_case/attendance/CheckOutUseCase.kt`
- Modify: `app/src/main/java/com/example/infinite_track/di/UseCaseModule.kt` (providers at lines ~272-286)
- Modify: `app/src/test/java/com/example/infinite_track/domain/use_case/attendance/AttendanceSubmissionBoundaryTest.kt` (point the source-scan at `SubmitAttendanceUseCase.kt`)
- Test: `app/src/test/java/com/example/infinite_track/domain/use_case/attendance/SubmitAttendanceUseCaseTest.kt`
- Modify (temporarily broken by deletion): `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceViewModel.kt` — in THIS task only swap the two constructor deps for `submitAttendanceUseCase` and make `proceedWithCheckIn`/`proceedWithCheckOut` call it with a locally built command, keeping their surrounding state handling; the real thinning happens in Task 7.

**Interfaces:**
- Consumes: Task 2 contracts, Task 4 repository signatures, `GetCurrentLocationUseCase` (`suspend operator fun invoke(): CurrentLocationResult`), `GetLoggedInUserUseCase` (`operator fun invoke(): Flow<UserModel>`).
- Produces: `class SubmitAttendanceUseCase @Inject constructor(attendanceRepository, getCurrentLocationUseCase, getLoggedInUserUseCase)` with `suspend operator fun invoke(command: AttendanceSubmitCommand): AttendanceSubmitResult`.

- [ ] **Step 1: Write the failing use case tests**

Hand-rolled fakes (repo convention):

```kotlin
// app/src/test/java/com/example/infinite_track/domain/use_case/attendance/SubmitAttendanceUseCaseTest.kt
package com.example.infinite_track.domain.use_case.attendance

// Fakes:
// - RecordingAttendanceRepository : AttendanceRepository — records every call
//   (checkInCalls: MutableList<AttendanceRequestModel>, checkOutCalls:
//   MutableList<Triple<Int, Double, Double>>), returns configurable
//   AttendanceSubmitResult; getTodayStatus returns configurable Result<TodayStatus>;
//   getActiveAttendanceId returns configurable Int?.
// - FakeCurrentLocationRepository behind a real GetCurrentLocationUseCase, or a
//   direct fake of the use case's repository interface CurrentLocationRepository.
// - GetLoggedInUserUseCase built over a fake AuthRepository? — simpler: the use
//   case takes GetLoggedInUserUseCase; construct it with a fake of its repository
//   interface returning flowOf(userModel) / emptyFlow().
```

Test cases (each with full arrange/act/assert):

1. `WFO check-in maps category_id 1 and repository invoked exactly once` — command CheckIn(WFO, wfoTarget); assert `checkInCalls.size == 1`, `checkInCalls[0].categoryId == 1`, `bookingId == null`, `type == "checkin"`, latitude/longitude equal the fresh GPS fake coordinates (NOT the target's).
2. `WFH check-in maps category_id 2`.
3. `WFA check-in maps category_id 3 with approved booking id` — target has `approvedWfaContext = ApprovedWfaTargetContext(bookingId = 42, scheduleDate = "2026-07-27")`; assert `bookingId == 42`.
4. `WFA without approved booking fails before repository call` — WFA target with `approvedWfaContext = null` → `Failure(CHECK_IN, WfaBookingRequired)`, `checkInCalls.isEmpty()`.
5. `target mode mismatch fails before repository call` — CheckIn(WFO, target(mode=WFH)) → `Failure(CHECK_IN, TargetModeMismatch)`, no repo call.
6. `location failure returns CurrentLocationUnavailable` — location fake returns `CurrentLocationResult.Failure(...)`; assert failure and no repo call.
7. `missing session returns SessionUnavailable` — logged-in-user flow empty → `Failure(intent, SessionUnavailable)`, no repo call (both intents).
8. `checkout uses caller attendance id first` — CheckOut(activeAttendanceId = 7) → `checkOutCalls[0].first == 7`; `getTodayStatus` fake must record it was NOT called.
9. `checkout falls back to force-refreshed status-today id` — CheckOut(null), status fake returns TodayStatus with activeAttendanceId = 9 → checkout with 9; assert `getTodayStatus` called with `forceRefresh = true`.
10. `checkout falls back to preference only after status lookup` — CheckOut(null), status returns activeAttendanceId = null, preference returns 11 → checkout with 11.
11. `missing checkout id returns ActiveAttendanceUnavailable` — all three sources null → typed failure, `checkOutCalls.isEmpty()`.
12. `checkout repository invoked exactly once on success`.

- [ ] **Step 2: Run test to verify it fails**

```powershell
<$GRADLE_ENV>; .\gradlew.bat app:testDebugUnitTest --tests "com.example.infinite_track.domain.use_case.attendance.SubmitAttendanceUseCaseTest" --console=plain
```

Expected: FAIL — `SubmitAttendanceUseCase` unresolved.

- [ ] **Step 3: Implement the use case**

```kotlin
// app/src/main/java/com/example/infinite_track/domain/use_case/attendance/SubmitAttendanceUseCase.kt
package com.example.infinite_track.domain.use_case.attendance

import com.example.infinite_track.domain.model.attendance.AttendanceRequestModel
import com.example.infinite_track.domain.model.attendance.AttendanceSubmitCommand
import com.example.infinite_track.domain.model.attendance.AttendanceSubmitFailure
import com.example.infinite_track.domain.model.attendance.AttendanceSubmitResult
import com.example.infinite_track.domain.model.attendance.WorkMode
import com.example.infinite_track.domain.model.location.CurrentLocationResult
import com.example.infinite_track.domain.model.location.GeoCoordinate
import com.example.infinite_track.domain.repository.AttendanceRepository
import com.example.infinite_track.domain.use_case.auth.GetLoggedInUserUseCase
import com.example.infinite_track.domain.use_case.location.GetCurrentLocationUseCase
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

/**
 * Single Layer 5 orchestrator: validates the submit command, captures fresh GPS
 * once, resolves checkout attendance id per fallback policy, and performs
 * exactly one repository mutation per attempt.
 */
class SubmitAttendanceUseCase @Inject constructor(
    private val attendanceRepository: AttendanceRepository,
    private val getCurrentLocationUseCase: GetCurrentLocationUseCase,
    private val getLoggedInUserUseCase: GetLoggedInUserUseCase
) {
    suspend operator fun invoke(command: AttendanceSubmitCommand): AttendanceSubmitResult {
        getLoggedInUserUseCase().firstOrNull()
            ?: return AttendanceSubmitResult.Failure(
                command.intent,
                AttendanceSubmitFailure.SessionUnavailable
            )

        return when (command) {
            is AttendanceSubmitCommand.CheckIn -> submitCheckIn(command)
            is AttendanceSubmitCommand.CheckOut -> submitCheckOut(command)
        }
    }

    private suspend fun submitCheckIn(
        command: AttendanceSubmitCommand.CheckIn
    ): AttendanceSubmitResult {
        if (command.authoritativeTarget.mode != command.workMode) {
            return AttendanceSubmitResult.Failure(
                command.intent,
                AttendanceSubmitFailure.TargetModeMismatch
            )
        }
        val bookingId = command.authoritativeTarget.approvedWfaContext?.bookingId
        if (command.workMode == WorkMode.WFA && bookingId == null) {
            return AttendanceSubmitResult.Failure(
                command.intent,
                AttendanceSubmitFailure.WfaBookingRequired
            )
        }

        val coordinate = freshCoordinate()
            ?: return AttendanceSubmitResult.Failure(
                command.intent,
                AttendanceSubmitFailure.CurrentLocationUnavailable
            )

        val request = AttendanceRequestModel(
            categoryId = command.workMode.categoryId,
            latitude = coordinate.latitude,
            longitude = coordinate.longitude,
            notes = CHECK_IN_NOTES,
            bookingId = bookingId.takeIf { command.workMode == WorkMode.WFA },
            type = REQUEST_TYPE_CHECK_IN
        )
        return attendanceRepository.checkIn(request)
    }

    private suspend fun submitCheckOut(
        command: AttendanceSubmitCommand.CheckOut
    ): AttendanceSubmitResult {
        val attendanceId = command.activeAttendanceId?.takeIf { it > 0 }
            ?: attendanceRepository.getTodayStatus(forceRefresh = true)
                .getOrNull()?.activeAttendanceId?.takeIf { it > 0 }
            ?: attendanceRepository.getActiveAttendanceId()?.takeIf { it > 0 }
            ?: return AttendanceSubmitResult.Failure(
                command.intent,
                AttendanceSubmitFailure.ActiveAttendanceUnavailable
            )

        val coordinate = freshCoordinate()
            ?: return AttendanceSubmitResult.Failure(
                command.intent,
                AttendanceSubmitFailure.CurrentLocationUnavailable
            )

        return attendanceRepository.checkOut(
            attendanceId = attendanceId,
            latitude = coordinate.latitude,
            longitude = coordinate.longitude
        )
    }

    private suspend fun freshCoordinate(): GeoCoordinate? =
        (getCurrentLocationUseCase() as? CurrentLocationResult.Success)
            ?.location?.coordinate

    private companion object {
        const val CHECK_IN_NOTES = "Check-in via mobile app"
        const val REQUEST_TYPE_CHECK_IN = "checkin"
    }
}
```

- [ ] **Step 4: Delete old use cases, update DI, ViewModel call sites, and boundary test**

- Delete `CheckInUseCase.kt` and `CheckOutUseCase.kt`.
- In `UseCaseModule.kt`, replace `provideCheckInUseCase`/`provideCheckOutUseCase` with:

```kotlin
@Provides
@Singleton
fun provideSubmitAttendanceUseCase(
    attendanceRepository: AttendanceRepository,
    getCurrentLocationUseCase: GetCurrentLocationUseCase,
    getLoggedInUserUseCase: GetLoggedInUserUseCase
): SubmitAttendanceUseCase {
    return SubmitAttendanceUseCase(
        attendanceRepository,
        getCurrentLocationUseCase,
        getLoggedInUserUseCase
    )
}
```

(match the module's existing provider style and import list; `GetLoggedInUserUseCase` already has a provider in this module — reuse its type.)

- In `AttendanceViewModel`, replace constructor params `checkInUseCase`/`checkOutUseCase` with `private val submitAttendanceUseCase: SubmitAttendanceUseCase`; inside `proceedWithCheckIn` replace the factory + `checkInUseCase(attendanceRequest)` block with:

```kotlin
val submitResult = submitAttendanceUseCase(
    AttendanceSubmitCommand.CheckIn(
        workMode = selectedMode,
        authoritativeTarget = resolvedTarget
    )
)
```

and inside `proceedWithCheckOut` replace `checkOutUseCase(attendanceId)` with:

```kotlin
val submitResult = submitAttendanceUseCase(
    AttendanceSubmitCommand.CheckOut(activeAttendanceId = attendanceId)
)
```

then branch `when (submitResult) { is AttendanceSubmitResult.Success -> ...existing success block... is AttendanceSubmitResult.Failure -> ...existing failure block (keep generic copy for now; Task 6/7 wires the mapper)... }`. Remove the now-dead `AttendanceCheckInRequestFactory` try/catch block and the `getLoggedInUserUseCase().firstOrNull()` session guard from `proceedWithCheckIn` (the use case owns both now).

- Update `AttendanceSubmissionBoundaryTest` to scan `SubmitAttendanceUseCase.kt` instead of the two deleted files (keep its assertion: no geofence/preference imports in domain submission use cases).

- [ ] **Step 5: Run new tests + full suite**

```powershell
<$GRADLE_ENV>; .\gradlew.bat app:testDebugUnitTest --console=plain
```

Expected: BUILD SUCCESSFUL, `SubmitAttendanceUseCaseTest` all green.

- [ ] **Step 6: Commit**

```bash
git add -A && git commit -m "feat(attendance): add SubmitAttendanceUseCase as single Layer 5 mutation orchestrator"
```

---

### Task 6: `AttendanceSubmitUiMapper` in presentation

**Files:**
- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceSubmitUiMapper.kt`
- Test: `app/src/test/java/com/example/infinite_track/presentation/screen/attendance/AttendanceSubmitUiMapperTest.kt`

**Interfaces:**
- Consumes: `AttendanceSubmitFailure`, `AttendanceActionIntent` (Tasks 1-2).
- Produces:
  - `data class AttendanceSubmitUiFailure(val title: String, val message: String)`
  - `object AttendanceSubmitUiMapper { fun map(intent: AttendanceActionIntent, failure: AttendanceSubmitFailure): AttendanceSubmitUiFailure }`

- [ ] **Step 1: Write the failing tests**

```kotlin
// app/src/test/java/com/example/infinite_track/presentation/screen/attendance/AttendanceSubmitUiMapperTest.kt
package com.example.infinite_track.presentation.screen.attendance

import com.example.infinite_track.domain.model.attendance.AttendanceActionIntent
import com.example.infinite_track.domain.model.attendance.AttendanceSubmitFailure
import org.junit.Assert.assertEquals
import org.junit.Test

class AttendanceSubmitUiMapperTest {

    @Test
    fun `title follows intent`() {
        assertEquals(
            "Check-in gagal",
            AttendanceSubmitUiMapper.map(
                AttendanceActionIntent.CHECK_IN,
                AttendanceSubmitFailure.Unknown
            ).title
        )
        assertEquals(
            "Check-out gagal",
            AttendanceSubmitUiMapper.map(
                AttendanceActionIntent.CHECK_OUT,
                AttendanceSubmitFailure.Unknown
            ).title
        )
    }

    @Test
    fun `backend rejected shows safe reason verbatim`() {
        val ui = AttendanceSubmitUiMapper.map(
            AttendanceActionIntent.CHECK_IN,
            AttendanceSubmitFailure.BackendRejected("Absensi ditolak kebijakan kantor")
        )
        assertEquals("Absensi ditolak kebijakan kantor", ui.message)
    }

    @Test
    fun `every failure type maps to non-blank distinct-enough copy`() {
        val failures = listOf(
            AttendanceSubmitFailure.CurrentLocationUnavailable,
            AttendanceSubmitFailure.SessionUnavailable,
            AttendanceSubmitFailure.ActiveAttendanceUnavailable,
            AttendanceSubmitFailure.TargetModeMismatch,
            AttendanceSubmitFailure.WfaBookingRequired,
            AttendanceSubmitFailure.DuplicateAttendance,
            AttendanceSubmitFailure.OutsideAllowedRadius,
            AttendanceSubmitFailure.WfaBookingRejected,
            AttendanceSubmitFailure.AlreadyCheckedOut,
            AttendanceSubmitFailure.NetworkUnavailable,
            AttendanceSubmitFailure.ServerUnavailable,
            AttendanceSubmitFailure.Unknown
        )
        val messages = failures.map {
            AttendanceSubmitUiMapper.map(AttendanceActionIntent.CHECK_IN, it).message
        }
        messages.forEach { message -> assert(message.isNotBlank()) }
        // Business rejections must not collapse into the generic message.
        assert(
            messages.count { it == AttendanceSubmitUiMapper.map(
                AttendanceActionIntent.CHECK_IN, AttendanceSubmitFailure.Unknown
            ).message } == 1
        )
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```powershell
<$GRADLE_ENV>; .\gradlew.bat app:testDebugUnitTest --tests "com.example.infinite_track.presentation.screen.attendance.AttendanceSubmitUiMapperTest" --console=plain
```

Expected: FAIL — unresolved `AttendanceSubmitUiMapper`.

- [ ] **Step 3: Implement the mapper**

```kotlin
// app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceSubmitUiMapper.kt
package com.example.infinite_track.presentation.screen.attendance

import com.example.infinite_track.domain.model.attendance.AttendanceActionIntent
import com.example.infinite_track.domain.model.attendance.AttendanceSubmitFailure

data class AttendanceSubmitUiFailure(
    val title: String,
    val message: String
)

/** Maps typed Layer 5 failures to localized user copy. Owns UI wording only. */
object AttendanceSubmitUiMapper {

    fun map(
        intent: AttendanceActionIntent,
        failure: AttendanceSubmitFailure
    ): AttendanceSubmitUiFailure {
        val title = when (intent) {
            AttendanceActionIntent.CHECK_IN -> "Check-in gagal"
            AttendanceActionIntent.CHECK_OUT -> "Check-out gagal"
        }
        val message = when (failure) {
            AttendanceSubmitFailure.CurrentLocationUnavailable ->
                "Lokasi belum dapat dibaca. Periksa GPS lalu coba lagi."
            AttendanceSubmitFailure.SessionUnavailable ->
                "Sesi Anda tidak tersedia. Silakan masuk ulang lalu coba lagi."
            AttendanceSubmitFailure.ActiveAttendanceUnavailable ->
                "Sesi absensi aktif tidak ditemukan. Muat ulang status absensi lalu coba lagi."
            AttendanceSubmitFailure.TargetModeMismatch ->
                "Target lokasi belum sesuai dengan mode kerja terpilih. Muat ulang lalu coba lagi."
            AttendanceSubmitFailure.WfaBookingRequired ->
                "Check-in WFA membutuhkan booking yang sudah disetujui."
            AttendanceSubmitFailure.DuplicateAttendance ->
                "Anda sudah melakukan check-in hari ini."
            AttendanceSubmitFailure.OutsideAllowedRadius ->
                "Anda berada di luar radius lokasi yang diizinkan."
            AttendanceSubmitFailure.WfaBookingRejected ->
                "Booking WFA Anda belum disetujui untuk hari ini."
            AttendanceSubmitFailure.AlreadyCheckedOut ->
                "Anda sudah melakukan check-out hari ini."
            AttendanceSubmitFailure.NetworkUnavailable ->
                "Koneksi internet bermasalah. Periksa jaringan lalu coba lagi."
            AttendanceSubmitFailure.ServerUnavailable ->
                "Server sedang bermasalah. Silakan coba lagi beberapa saat lagi."
            is AttendanceSubmitFailure.BackendRejected -> failure.safeReason
            AttendanceSubmitFailure.Unknown ->
                "Absensi belum berhasil. Silakan coba lagi."
        }
        return AttendanceSubmitUiFailure(title = title, message = message)
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Same command as Step 2. Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(attendance): map typed submit failures to localized copy via AttendanceSubmitUiMapper"
```

---

### Task 7: Thin `AttendanceViewModel` to one `submitAttendance(command)` entry point

**Files:**
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceViewModel.kt` (lines ~711-970: `onFaceVerificationResult`, `proceedWithCheckIn`, `proceedWithCheckOut`)
- Delete: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceCheckInRequestFactory.kt`
- Delete: `app/src/test/java/com/example/infinite_track/presentation/screen/attendance/AttendanceCheckInRequestFactoryTest.kt`
- Test: `app/src/test/java/com/example/infinite_track/presentation/screen/attendance/AttendanceSubmitCommandBuilderTest.kt`
- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceSubmitCommandBuilder.kt`

**Interfaces:**
- Consumes: `SubmitAttendanceUseCase` (Task 5), `AttendanceSubmitUiMapper` (Task 6), contracts (Task 2).
- Produces: `object AttendanceSubmitCommandBuilder { fun build(intent: AttendanceActionIntent, state: AttendanceScreenState): AttendanceSubmitCommand? }` — pure, unit-testable command construction from screen state (returns null when check-in preparation is not `Resolved` + `Ready`, mirroring the current guard at `proceedWithCheckIn` lines 807-818).

- [ ] **Step 1: Write the failing command-builder tests**

```kotlin
// app/src/test/java/com/example/infinite_track/presentation/screen/attendance/AttendanceSubmitCommandBuilderTest.kt
package com.example.infinite_track.presentation.screen.attendance

// Cases (arrange AttendanceScreenState fixtures the same way
// AttendancePreparationReducerTest builds preparation state):
// 1. CHECK_IN with Resolved target + eligibility Ready
//    -> AttendanceSubmitCommand.CheckIn(selectedMode, resolvedTarget)
// 2. CHECK_IN with unresolved target -> null
// 3. CHECK_IN with eligibility != Ready -> null
// 4. CHECK_OUT with todayStatus.activeAttendanceId = 7
//    -> AttendanceSubmitCommand.CheckOut(7)
// 5. CHECK_OUT with todayStatus null -> AttendanceSubmitCommand.CheckOut(null)
```

Write the five cases fully with real `AttendanceScreenState` fixtures.

- [ ] **Step 2: Run test to verify it fails**

```powershell
<$GRADLE_ENV>; .\gradlew.bat app:testDebugUnitTest --tests "com.example.infinite_track.presentation.screen.attendance.AttendanceSubmitCommandBuilderTest" --console=plain
```

Expected: FAIL — unresolved `AttendanceSubmitCommandBuilder`.

- [ ] **Step 3: Implement the builder**

```kotlin
// app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceSubmitCommandBuilder.kt
package com.example.infinite_track.presentation.screen.attendance

import com.example.infinite_track.domain.model.attendance.AttendanceActionIntent
import com.example.infinite_track.domain.model.attendance.AttendancePreparationEligibility
import com.example.infinite_track.domain.model.attendance.AttendanceSubmitCommand
import com.example.infinite_track.domain.model.attendance.TargetLocationResolution

/**
 * Builds the typed submit command from already-resolved screen state.
 * Pure state projection — no business rule lives here; SubmitAttendanceUseCase
 * revalidates everything it needs.
 */
object AttendanceSubmitCommandBuilder {

    fun build(
        intent: AttendanceActionIntent,
        state: AttendanceScreenState
    ): AttendanceSubmitCommand? = when (intent) {
        AttendanceActionIntent.CHECK_IN -> {
            val preparation = state.preparation
            val resolvedTarget =
                (preparation.targetResolution as? TargetLocationResolution.Resolved)?.target
            if (resolvedTarget == null ||
                preparation.eligibility !is AttendancePreparationEligibility.Ready
            ) {
                null
            } else {
                AttendanceSubmitCommand.CheckIn(
                    workMode = preparation.selectedMode,
                    authoritativeTarget = resolvedTarget
                )
            }
        }

        AttendanceActionIntent.CHECK_OUT ->
            AttendanceSubmitCommand.CheckOut(
                activeAttendanceId = state.todayStatus?.activeAttendanceId
            )
    }
}
```

(Adjust `AttendancePreparationEligibility.Ready` reference to the actual sealed shape used at `AttendanceViewModel.kt:813`.)

- [ ] **Step 4: Replace `proceedWithCheckIn`/`proceedWithCheckOut` with one `submitAttendance`**

In `AttendanceViewModel`:

- `onFaceVerificationResult` keeps its current guards (ignore while `Submitting` at line 714, intent resolution at 719-735, non-success handling at 750-775). In the `result.submitsAttendance` branch replace the `when (intent)` dispatch with `submitAttendance(intent)`.
- Delete `proceedWithCheckIn` and `proceedWithCheckOut`; add:

```kotlin
private fun submitAttendance(intent: AttendanceActionIntent) {
    viewModelScope.launch {
        val submittingState = _uiState.value.actionState as? AttendanceActionState.Submitting
        if (submittingState?.intent != intent) {
            Log.w(TAG, "Submit blocked because action state is not Submitting($intent): ${_uiState.value.actionState}")
            return@launch
        }

        val command = AttendanceSubmitCommandBuilder.build(intent, _uiState.value)
        if (command == null) {
            // Preparation regressed while verifying face; fall back to resolved state.
            _uiState.value = _uiState.value.withActionState(
                AttendanceActionResolver.resolve(_uiState.value)
            )
            return@launch
        }

        val result = try {
            submitAttendanceUseCase(command)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during attendance submit", e)
            AttendanceSubmitResult.Failure(intent, AttendanceSubmitFailure.Unknown)
        }

        when (result) {
            is AttendanceSubmitResult.Success -> {
                val successMessage = publishTransientFeedback(
                    when (intent) {
                        AttendanceActionIntent.CHECK_IN -> AttendanceTransientFeedbackKind.CHECK_IN_SUCCESS
                        AttendanceActionIntent.CHECK_OUT -> AttendanceTransientFeedbackKind.CHECK_OUT_SUCCESS
                    }
                ).message
                _uiState.value = _uiState.value.withActionState(
                    AttendanceActionState.Success(intent = intent, message = successMessage)
                )
                refreshAttendanceAndRuntime(
                    when (intent) {
                        AttendanceActionIntent.CHECK_IN -> GeofenceReconcileReason.CHECK_IN_SUCCEEDED
                        AttendanceActionIntent.CHECK_OUT -> GeofenceReconcileReason.CHECK_OUT_SUCCEEDED
                    }
                )
            }

            is AttendanceSubmitResult.Failure -> {
                val uiFailure = AttendanceSubmitUiMapper.map(intent, result.failure)
                publishSubmitFailureFeedback(uiFailure)
                _uiState.value = _uiState.value.withActionState(
                    AttendanceActionState.RetryableFailure(
                        intent = intent,
                        title = uiFailure.title,
                        message = uiFailure.message
                    )
                )
                // No refresh and no geofence reconciliation on failure.
            }
        }
    }
}

private fun publishSubmitFailureFeedback(uiFailure: AttendanceSubmitUiFailure) {
    nextTransientFeedbackId += 1
    val feedback = AttendanceTransientFeedback(
        id = nextTransientFeedbackId,
        message = uiFailure.message,
        semantic = InfiniteSemantic.Error,
        duration = AttendanceTransientFeedbackDuration.LONG
    )
    if (!_transientFeedback.tryEmit(feedback)) {
        viewModelScope.launch { _transientFeedback.emit(feedback) }
    }
}
```

Add imports (`AttendanceSubmitCommand`, `AttendanceSubmitFailure`, `AttendanceSubmitResult`, `InfiniteSemantic`) and remove now-unused imports (`AttendanceCheckInRequestFactory`, `AttendanceRequestModel` if present, `firstOrNull` if unused elsewhere — `observeProfileForPreparation` still uses `collect`, keep whatever it needs).

- Delete `AttendanceCheckInRequestFactory.kt` and its test (its two rules — mode match and WFA booking guard — now live in `SubmitAttendanceUseCase` with tests from Task 5).

- [ ] **Step 5: Full unit test run**

```powershell
<$GRADLE_ENV>; .\gradlew.bat app:testDebugUnitTest --console=plain
```

Expected: BUILD SUCCESSFUL; no references to `AttendanceCheckInRequestFactory`, `proceedWithCheckIn`, `proceedWithCheckOut` remain (`grep` to confirm).

- [ ] **Step 6: Commit**

```bash
git add -A && git commit -m "refactor(attendance): thin AttendanceViewModel to single typed submitAttendance entry point"
```

---

### Task 8: Quality gate, docs note, and PR

**Files:**
- Modify: `docs/superpowers/plans/2026-07-27-inf-264-layer5-attendance-submit-contracts.md` (check off completed tasks)

- [ ] **Step 1: Run the full quality gate**

```powershell
<$GRADLE_ENV>; .\gradlew.bat app:testDebugUnitTest app:lintDebug app:assembleDebug --console=plain
```

Expected: BUILD SUCCESSFUL (document any pre-existing lint blockers instead of fixing unrelated code).

```bash
git diff develop --check
```

Expected: no whitespace errors.

- [ ] **Step 2: Verify acceptance greps**

```bash
# Must return no results:
grep -rn "AttendanceCheckInRequestFactory" app/src/main
grep -rn "Result<ActiveAttendanceSession>" app/src/main/java/com/example/infinite_track/domain
grep -rn "HttpException" app/src/main/java/com/example/infinite_track/domain
grep -rn "buttonText.contains" app/src/main
```

- [ ] **Step 3: Push branch and open PR to `develop`**

PR body must include: scope summary, file list, test evidence (task-by-task green runs), the machine-level Gradle workaround note, and a `Needs Verification` section for device/emulator runtime evidence (check-in/checkout end-to-end, backend rejections, WFA paths, duplicate-tap guard) — runtime evidence is REQUIRED before the issue is Done per CLAUDE.md; the PR must say so explicitly.

- [ ] **Step 4: Docs/ADR note**

This work changes attendance capture semantics' internal contracts but not the backend API contract. Add note to PR: `DOCS/ADR UPDATE REQUIRED: none — mobile-internal refactor; backend payloads and endpoints unchanged. Failure-mapping message patterns documented in AttendanceApiFailureMapper KDoc; recommend backend error-code contract as follow-up.`

---

## Self-Review Notes

- Spec coverage: contracts (Task 2), use case + checkout fallback + WFA guard (Task 5), repository mapping (Tasks 3-4), ViewModel thinning + factory removal (Task 7), UI copy mapping (Task 6), quality gate (Task 8). Runtime/device evidence is intentionally `Needs Verification` — cannot be produced in this environment.
- Spec's ViewModel test list is covered by: duplicate-guard + intent gating (existing `onFaceVerificationResult` logic, unchanged, plus Submitting-guard retained in `submitAttendance`), command construction (`AttendanceSubmitCommandBuilderTest`), one-mutation-per-attempt + no-submit-on-face-failure (SubmitAttendanceUseCaseTest + unchanged `submitsAttendance` gating). Full ViewModel-instantiation tests are impractical in this repo (no mocking library, 13 constructor deps); this follows the repo's established pure-transition test pattern.
- Type consistency verified: `AttendanceSubmitResult` produced by repository (Task 4) and consumed by use case (Task 5) and ViewModel (Task 7); `AttendanceSubmitUiFailure(title, message)` produced Task 6, consumed Task 7.
