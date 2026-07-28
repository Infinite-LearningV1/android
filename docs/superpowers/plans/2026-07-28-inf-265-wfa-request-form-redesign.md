# INF-265 WFA Request Form Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the legacy dialog-based WFA booking flow with a server-configured, typed Form → Review → Submit → Result transaction owned by one graph-scoped ViewModel.

**Architecture:** Preserve `Screen → ViewModel → UseCase → Repository Interface → RepositoryImpl → API`. Add typed WFA request configuration, draft, command, result, and failure contracts; keep `BookingRepository` as owner; move date/DTO/error mapping to Data; isolate Form, Review, and Result in one nested graph and transaction session.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Navigation Compose, Hilt, Retrofit/OkHttp, Kotlin coroutines/StateFlow, `java.time`, JUnit4, kotlinx-coroutines-test, hand-written fakes.

**Spec:** `docs/superpowers/specs/2026-07-28-inf-265-wfa-request-form-redesign.md`  
**Linear:** INF-265  
**Branch:** `djangosuryaa/inf-265-android-redesign-wfa-request-form-with-review-and-result`  
**Base:** `develop` at `2191a6e0d06b99f8fdfc3e67f9408102d220f802`

## Global Constraints

- Enter an isolated worktree for this branch before implementation; do not edit the operator's `develop` checkout.
- INF-265 is blocked by INF-270. Production API/DTO work begins only after the INF-270 OpenAPI contract is merged or otherwise locked with equivalent evidence.
- Backend remains final truth for radius, active reasons, date policy, conflicts, status, suitability, user identity, timestamps, and mutation success.
- Android must not send radius, user ID, status, suitability score/label, or created timestamp.
- Android sends schedule date only as ISO `YYYY-MM-DD`; remove all fallback-to-today behavior.
- Persistent transaction state uses `StateFlow`; one-time navigation/announcement effects use a separate stream.
- ViewModel must not hold `NavController`; screens render state and send typed events only.
- Keep existing booking-history contracts unchanged. Do not implement INF-214/INF-218 history/list work.
- Reuse Infinite Track Material 3 tokens/components; do not build a parallel design system.
- Preserve the existing `data/soucre` spelling; package cleanup is out of scope.
- Do not add MockK/Mockito. Follow the repository convention of JUnit4, coroutines-test, and hand-written fakes.
- One commit per task using `feat(wfa):`, `refactor(wfa):`, `test(wfa):`, or `docs(wfa):`.
- UI/navigation/attendance completion requires emulator/device evidence. Missing runtime evidence is `Needs Verification`, not Done.

---

## Current `develop` mapping

- `presentation/screen/attendance/booking/WfaBookingScreen.kt` owns `NavHostController`, local success/error dialogs, and direct Home navigation.
- `presentation/screen/attendance/booking/WfaBookingViewModel.kt` owns route parsing, employee prefill, reverse geocoding, primitive fields, date formatting/fallback, submit, generic errors, and success boolean.
- `presentation/components/dialog/WfaBookingDialog.kt` lets the employee edit radius and sends directly from the form.
- `domain/use_case/booking/SubmitWfaBookingUseCase.kt` forwards six primitives and returns `Result<Unit>`.
- `domain/repository/BookingRepository.kt` exposes primitive submit parameters including radius.
- `data/soucre/network/request/BookingRequest.kt` sends radius/description.
- `data/soucre/network/response/booking/BookingResponse.kt` is discarded on repository success.
- `data/repository/booking/BookingRepositoryImpl.kt` maps failures to generic `Exception` strings.
- `ApiService.kt` exposes `POST api/bookings` but not `GET api/wfa/request-config`.
- `Screen.WfaBooking` uses path coordinates and `MainContentNavGraph.kt` declares them as `NavType.FloatType`.
- The History nested graph already demonstrates the repository's graph-scoped ViewModel pattern; follow that ownership style.

---

### Task 1: Add typed WFA request domain contracts

**Files:**
- Create: `app/src/main/java/com/example/infinite_track/domain/model/booking/WfaCandidateLocation.kt`
- Create: `app/src/main/java/com/example/infinite_track/domain/model/booking/WfaRequestConfig.kt`
- Create: `app/src/main/java/com/example/infinite_track/domain/model/booking/WfaRequestDraft.kt`
- Create: `app/src/main/java/com/example/infinite_track/domain/model/booking/WfaRequestSubmission.kt`
- Test: `app/src/test/java/com/example/infinite_track/domain/model/booking/WfaRequestContractTest.kt`

**Interfaces:**
- Produces: `WfaCandidateLocation`, `WfaRequestConfig`, `WfaRequestReason`, `WfaRequestDraft`, `WfaRequestFieldErrors`, `WfaRequestValidationResult`, `SubmitWfaRequestCommand`, `SubmittedWfaRequest`, `WfaRequestStatus`, `WfaRequestConfigResult`, `WfaRequestResult`, and `WfaRequestFailure`.
- Consumed by: Tasks 2–10.

- [ ] **Step 1: Write the failing domain contract test**

```kotlin
package com.example.infinite_track.domain.model.booking

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class WfaRequestContractTest {

    private val location = WfaCandidateLocation(
        latitude = -0.9001,
        longitude = 119.877,
        displayName = "Infinity Creative Hub",
        formattedAddress = "Palu"
    )

    @Test
    fun `empty draft contains no server-authoritative fields`() {
        assertNull(WfaRequestDraft.Empty.scheduleDate)
        assertNull(WfaRequestDraft.Empty.reasonId)
        assertNull(WfaRequestDraft.Empty.location)
    }

    @Test
    fun `submit command has no editable radius`() {
        val properties = SubmitWfaRequestCommand::class.java.declaredFields.map { it.name }
        assertFalse(properties.contains("radius"))
        assertFalse(properties.contains("radiusMeters"))
    }

    @Test
    fun `success carries backend confirmed request`() {
        val request = SubmittedWfaRequest(
            bookingId = 42L,
            scheduleDate = LocalDate.of(2026, 8, 10),
            status = WfaRequestStatus.PENDING,
            location = location,
            reasonLabel = "Pertemuan dengan klien",
            radiusMeters = 100,
            submittedAt = null
        )
        assertEquals(request, WfaRequestResult.Success(request).request)
    }
}
```

- [ ] **Step 2: Run the test and verify failure**

```bash
./gradlew app:testDebugUnitTest --tests "com.example.infinite_track.domain.model.booking.WfaRequestContractTest" --console=plain
```

Expected: FAIL with unresolved WFA request contract references.

- [ ] **Step 3: Add the domain types**

Use focused files; keep DTO/Retrofit/Compose/Android imports out of this package.

```kotlin
package com.example.infinite_track.domain.model.booking

data class WfaCandidateLocation(
    val latitude: Double,
    val longitude: Double,
    val displayName: String,
    val formattedAddress: String
) {
    val hasValidCoordinates: Boolean
        get() = latitude in -90.0..90.0 &&
            longitude in -180.0..180.0 &&
            latitude != 0.0 && longitude != 0.0
}
```

```kotlin
package com.example.infinite_track.domain.model.booking

data class WfaRequestConfig(
    val radiusMeters: Int,
    val reasons: List<WfaRequestReason>
)

data class WfaRequestReason(
    val id: Long,
    val label: String,
    val isOther: Boolean
)
```

```kotlin
package com.example.infinite_track.domain.model.booking

import java.time.LocalDate

data class WfaRequestDraft(
    val scheduleDate: LocalDate?,
    val reasonId: Long?,
    val otherReasonText: String,
    val notes: String,
    val location: WfaCandidateLocation?
) {
    companion object {
        val Empty = WfaRequestDraft(null, null, "", "", null)
    }
}

enum class WfaRequestFieldError {
    REQUIRED,
    REASON_UNAVAILABLE,
    OTHER_REASON_REQUIRED,
    TOO_LONG,
    INVALID_LOCATION
}

data class WfaRequestFieldErrors(
    val scheduleDate: WfaRequestFieldError? = null,
    val reason: WfaRequestFieldError? = null,
    val otherReason: WfaRequestFieldError? = null,
    val notes: WfaRequestFieldError? = null,
    val location: WfaRequestFieldError? = null
) {
    val isEmpty: Boolean
        get() = scheduleDate == null && reason == null && otherReason == null &&
            notes == null && location == null
}
```

```kotlin
package com.example.infinite_track.domain.model.booking

import java.time.Instant
import java.time.LocalDate

data class SubmitWfaRequestCommand(
    val scheduleDate: LocalDate,
    val reasonId: Long,
    val otherReasonText: String?,
    val notes: String?,
    val location: WfaCandidateLocation
)

enum class WfaRequestStatus {
    PENDING,
    APPROVED,
    REJECTED,
    UNKNOWN
}

data class SubmittedWfaRequest(
    val bookingId: Long,
    val scheduleDate: LocalDate,
    val status: WfaRequestStatus,
    val location: WfaCandidateLocation,
    val reasonLabel: String,
    val radiusMeters: Int,
    val submittedAt: Instant?
)

sealed interface WfaRequestFailure {
    data object ConfigUnavailable : WfaRequestFailure
    data object InvalidDate : WfaRequestFailure
    data object ReasonUnavailable : WfaRequestFailure
    data object DuplicateRequest : WfaRequestFailure
    data object NetworkUnavailable : WfaRequestFailure
    data object ServerUnavailable : WfaRequestFailure
    data class ValidationRejected(val fieldErrors: Map<String, String>) : WfaRequestFailure
    data class BackendRejected(val safeMessage: String) : WfaRequestFailure
    data object Unknown : WfaRequestFailure
}

sealed interface WfaRequestConfigResult {
    data class Success(val config: WfaRequestConfig) : WfaRequestConfigResult
    data class Failure(val failure: WfaRequestFailure) : WfaRequestConfigResult
}

sealed interface WfaRequestResult {
    data class Success(val request: SubmittedWfaRequest) : WfaRequestResult
    data class Failure(val failure: WfaRequestFailure) : WfaRequestResult
}

sealed interface WfaRequestValidationResult {
    data class Valid(val command: SubmitWfaRequestCommand) : WfaRequestValidationResult
    data class Invalid(val errors: WfaRequestFieldErrors) : WfaRequestValidationResult
}
```

- [ ] **Step 4: Run the domain contract test**

Use the command from Step 2. Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/infinite_track/domain/model/booking app/src/test/java/com/example/infinite_track/domain/model/booking/WfaRequestContractTest.kt
git commit -m "feat(wfa): add typed request domain contracts"
```

---

### Task 2: Add pure draft validation and command construction

**Files:**
- Create: `app/src/main/java/com/example/infinite_track/domain/use_case/booking/ValidateWfaRequestDraftUseCase.kt`
- Test: `app/src/test/java/com/example/infinite_track/domain/use_case/booking/ValidateWfaRequestDraftUseCaseTest.kt`

**Interfaces:**
- Consumes: `WfaRequestDraft`, `WfaRequestConfig`.
- Produces: `operator fun invoke(draft, config): WfaRequestValidationResult`.

- [ ] **Step 1: Write failing validation tests**

```kotlin
package com.example.infinite_track.domain.use_case.booking

import com.example.infinite_track.domain.model.booking.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ValidateWfaRequestDraftUseCaseTest {

    private val useCase = ValidateWfaRequestDraftUseCase()
    private val location = WfaCandidateLocation(-0.9001, 119.877, "Hub", "Palu")
    private val config = WfaRequestConfig(
        radiusMeters = 100,
        reasons = listOf(
            WfaRequestReason(1L, "Client meeting", false),
            WfaRequestReason(2L, "Lainnya", true)
        )
    )

    @Test
    fun `valid draft becomes command and blank optional values become null`() {
        val result = useCase(
            WfaRequestDraft(LocalDate.of(2026, 8, 10), 1L, "", "   ", location),
            config
        ) as WfaRequestValidationResult.Valid

        assertEquals(1L, result.command.reasonId)
        assertNull(result.command.otherReasonText)
        assertNull(result.command.notes)
    }

    @Test
    fun `Other reason requires explanation`() {
        val result = useCase(
            WfaRequestDraft(LocalDate.of(2026, 8, 10), 2L, "  ", "", location),
            config
        ) as WfaRequestValidationResult.Invalid

        assertEquals(WfaRequestFieldError.OTHER_REASON_REQUIRED, result.errors.otherReason)
    }

    @Test
    fun `reason absent from server config is unavailable`() {
        val result = useCase(
            WfaRequestDraft(LocalDate.of(2026, 8, 10), 99L, "", "", location),
            config
        ) as WfaRequestValidationResult.Invalid

        assertEquals(WfaRequestFieldError.REASON_UNAVAILABLE, result.errors.reason)
    }

    @Test
    fun `invalid location blocks review`() {
        val invalid = location.copy(latitude = 0.0)
        val result = useCase(
            WfaRequestDraft(LocalDate.of(2026, 8, 10), 1L, "", "", invalid),
            config
        ) as WfaRequestValidationResult.Invalid

        assertEquals(WfaRequestFieldError.INVALID_LOCATION, result.errors.location)
        assertTrue(!result.errors.isEmpty)
    }
}
```

- [ ] **Step 2: Run the test and verify failure**

```bash
./gradlew app:testDebugUnitTest --tests "com.example.infinite_track.domain.use_case.booking.ValidateWfaRequestDraftUseCaseTest" --console=plain
```

Expected: FAIL because the use case does not exist.

- [ ] **Step 3: Implement the pure validator**

```kotlin
package com.example.infinite_track.domain.use_case.booking

import com.example.infinite_track.domain.model.booking.*
import javax.inject.Inject

class ValidateWfaRequestDraftUseCase @Inject constructor() {

    operator fun invoke(
        draft: WfaRequestDraft,
        config: WfaRequestConfig
    ): WfaRequestValidationResult {
        val reason = draft.reasonId?.let { id -> config.reasons.firstOrNull { it.id == id } }
        val errors = WfaRequestFieldErrors(
            scheduleDate = if (draft.scheduleDate == null) WfaRequestFieldError.REQUIRED else null,
            reason = when {
                draft.reasonId == null -> WfaRequestFieldError.REQUIRED
                reason == null -> WfaRequestFieldError.REASON_UNAVAILABLE
                else -> null
            },
            otherReason = if (reason?.isOther == true && draft.otherReasonText.isBlank()) {
                WfaRequestFieldError.OTHER_REASON_REQUIRED
            } else null,
            location = when {
                draft.location == null -> WfaRequestFieldError.REQUIRED
                !draft.location.hasValidCoordinates -> WfaRequestFieldError.INVALID_LOCATION
                else -> null
            }
        )

        if (!errors.isEmpty) return WfaRequestValidationResult.Invalid(errors)

        return WfaRequestValidationResult.Valid(
            SubmitWfaRequestCommand(
                scheduleDate = requireNotNull(draft.scheduleDate),
                reasonId = requireNotNull(draft.reasonId),
                otherReasonText = draft.otherReasonText.trim().ifBlank { null },
                notes = draft.notes.trim().ifBlank { null },
                location = requireNotNull(draft.location)
            )
        )
    }
}
```

Do not add past/same-day checks here unless INF-270 exposes the policy as config. Backend retains final date-policy authority.

- [ ] **Step 4: Run the validation tests**

Use Step 2 command. Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/infinite_track/domain/use_case/booking/ValidateWfaRequestDraftUseCase.kt app/src/test/java/com/example/infinite_track/domain/use_case/booking/ValidateWfaRequestDraftUseCaseTest.kt
git commit -m "feat(wfa): validate request drafts before review"
```

---

### Task 3: Add server WFA request-config DTO and mapping

**Files:**
- Create: `app/src/main/java/com/example/infinite_track/data/soucre/network/response/booking/WfaRequestConfigResponseDto.kt`
- Create: `app/src/main/java/com/example/infinite_track/data/mapper/booking/WfaRequestConfigMapper.kt`
- Modify: `app/src/main/java/com/example/infinite_track/data/soucre/network/retrofit/ApiService.kt`
- Test: `app/src/test/java/com/example/infinite_track/data/mapper/booking/WfaRequestConfigMapperTest.kt`

**Interfaces:**
- Produces: `ApiService.getWfaRequestConfig()` and `WfaRequestConfigResponseDto.toDomain()`.
- Exact endpoint: `GET api/wfa/request-config` from INF-270.

- [ ] **Step 1: Write failing mapper tests**

```kotlin
package com.example.infinite_track.data.mapper.booking

import com.example.infinite_track.data.soucre.network.response.booking.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WfaRequestConfigMapperTest {

    @Test
    fun `maps server radius and active reasons`() {
        val dto = WfaRequestConfigResponseDto(
            success = true,
            message = null,
            data = WfaRequestConfigDataDto(
                radiusMeters = 100,
                reasons = listOf(
                    WfaRequestReasonDto(1L, "Pertemuan dengan klien", false),
                    WfaRequestReasonDto(2L, "Lainnya", true)
                )
            )
        )

        val config = requireNotNull(dto.toDomainOrNull())
        assertEquals(100, config.radiusMeters)
        assertEquals(2, config.reasons.size)
        assertTrue(config.reasons.last().isOther)
    }

    @Test
    fun `non-positive radius produces unusable config`() {
        val dto = WfaRequestConfigResponseDto(
            success = true,
            message = null,
            data = WfaRequestConfigDataDto(0, emptyList())
        )
        assertEquals(null, dto.toDomainOrNull())
    }
}
```

- [ ] **Step 2: Run the test and verify failure**

```bash
./gradlew app:testDebugUnitTest --tests "com.example.infinite_track.data.mapper.booking.WfaRequestConfigMapperTest" --console=plain
```

Expected: FAIL with missing DTO/mapper references.

- [ ] **Step 3: Implement DTOs and mapper**

```kotlin
package com.example.infinite_track.data.soucre.network.response.booking

import com.google.gson.annotations.SerializedName

data class WfaRequestConfigResponseDto(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: WfaRequestConfigDataDto?
)

data class WfaRequestConfigDataDto(
    @SerializedName("radius_meters") val radiusMeters: Int?,
    @SerializedName("reasons") val reasons: List<WfaRequestReasonDto> = emptyList()
)

data class WfaRequestReasonDto(
    @SerializedName("id") val id: Long?,
    @SerializedName("label") val label: String?,
    @SerializedName("is_other") val isOther: Boolean = false
)
```

```kotlin
package com.example.infinite_track.data.mapper.booking

import com.example.infinite_track.data.soucre.network.response.booking.WfaRequestConfigResponseDto
import com.example.infinite_track.domain.model.booking.WfaRequestConfig
import com.example.infinite_track.domain.model.booking.WfaRequestReason

fun WfaRequestConfigResponseDto.toDomainOrNull(): WfaRequestConfig? {
    val body = data ?: return null
    val radius = body.radiusMeters ?: return null
    if (!success || radius <= 0) return null

    val mappedReasons = body.reasons.mapNotNull { reason ->
        val id = reason.id ?: return@mapNotNull null
        val label = reason.label?.trim().orEmpty()
        if (label.isBlank()) null else WfaRequestReason(id, label, reason.isOther)
    }.distinctBy { it.id }

    if (mappedReasons.isEmpty() || mappedReasons.count { it.isOther } > 1) return null
    return WfaRequestConfig(radius, mappedReasons)
}
```

- [ ] **Step 4: Extend Retrofit**

Add to `ApiService.kt`:

```kotlin
@GET("api/wfa/request-config")
suspend fun getWfaRequestConfig(): WfaRequestConfigResponseDto
```

Add the DTO import and leave booking-history methods unchanged.

- [ ] **Step 5: Run mapper tests and compile**

```bash
./gradlew app:testDebugUnitTest --tests "com.example.infinite_track.data.mapper.booking.WfaRequestConfigMapperTest" app:compileDebugKotlin --console=plain
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/example/infinite_track/data/soucre/network/response/booking/WfaRequestConfigResponseDto.kt app/src/main/java/com/example/infinite_track/data/mapper/booking/WfaRequestConfigMapper.kt app/src/main/java/com/example/infinite_track/data/soucre/network/retrofit/ApiService.kt app/src/test/java/com/example/infinite_track/data/mapper/booking/WfaRequestConfigMapperTest.kt
git commit -m "feat(wfa): consume server request configuration"
```

---

### Task 4: Add typed submit DTO, response mapping, and failure classification

**Files:**
- Create: `app/src/main/java/com/example/infinite_track/data/soucre/network/request/WfaRequestDto.kt`
- Create: `app/src/main/java/com/example/infinite_track/data/soucre/network/response/booking/WfaRequestResponseDto.kt`
- Create: `app/src/main/java/com/example/infinite_track/data/mapper/booking/WfaRequestDtoMapper.kt`
- Create: `app/src/main/java/com/example/infinite_track/data/mapper/booking/WfaRequestFailureMapper.kt`
- Modify: `app/src/main/java/com/example/infinite_track/data/soucre/network/retrofit/ApiService.kt`
- Test: `app/src/test/java/com/example/infinite_track/data/mapper/booking/WfaRequestDtoMapperTest.kt`
- Test: `app/src/test/java/com/example/infinite_track/data/mapper/booking/WfaRequestFailureMapperTest.kt`

**Interfaces:**
- Produces: `SubmitWfaRequestCommand.toDto()`, `WfaRequestResponseDto.toDomainOrNull()`, and `WfaRequestFailureMapper`.
- Request payload excludes radius and suitability by type construction.

- [ ] **Step 1: Write failing DTO mapper tests**

```kotlin
package com.example.infinite_track.data.mapper.booking

import com.example.infinite_track.domain.model.booking.SubmitWfaRequestCommand
import com.example.infinite_track.domain.model.booking.WfaCandidateLocation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.LocalDate

class WfaRequestDtoMapperTest {

    @Test
    fun `maps date to ISO and excludes radius`() {
        val dto = SubmitWfaRequestCommand(
            scheduleDate = LocalDate.of(2026, 8, 10),
            reasonId = 4L,
            otherReasonText = null,
            notes = null,
            location = WfaCandidateLocation(-0.9001, 119.877, "Hub", "Palu")
        ).toDto()

        assertEquals("2026-08-10", dto.scheduleDate)
        assertEquals(4L, dto.reasonId)
        assertFalse(dto::class.java.declaredFields.map { it.name }.contains("radius"))
    }
}
```

- [ ] **Step 2: Write failing failure-mapper tests**

```kotlin
package com.example.infinite_track.data.mapper.booking

import com.example.infinite_track.domain.model.booking.WfaRequestFailure
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class WfaRequestFailureMapperTest {

    @Test
    fun `duplicate code maps to DuplicateRequest`() {
        assertEquals(
            WfaRequestFailure.DuplicateRequest,
            WfaRequestFailureMapper.mapHttp(409, "DUPLICATE_BOOKING", null, emptyMap())
        )
    }

    @Test
    fun `inactive reason maps to ReasonUnavailable`() {
        assertEquals(
            WfaRequestFailure.ReasonUnavailable,
            WfaRequestFailureMapper.mapHttp(400, "REQUEST_REASON_INACTIVE", null, emptyMap())
        )
    }

    @Test
    fun `io failure maps to NetworkUnavailable`() {
        assertEquals(
            WfaRequestFailure.NetworkUnavailable,
            WfaRequestFailureMapper.mapThrowable(IOException("offline"))
        )
    }

    @Test
    fun `server error maps to ServerUnavailable`() {
        assertEquals(
            WfaRequestFailure.ServerUnavailable,
            WfaRequestFailureMapper.mapHttp(503, null, null, emptyMap())
        )
    }
}
```

- [ ] **Step 3: Run both tests and verify failure**

```bash
./gradlew app:testDebugUnitTest --tests "com.example.infinite_track.data.mapper.booking.WfaRequestDtoMapperTest" --tests "com.example.infinite_track.data.mapper.booking.WfaRequestFailureMapperTest" --console=plain
```

Expected: FAIL with missing DTO/mapper types.

- [ ] **Step 4: Implement request/response DTOs and mappers**

```kotlin
package com.example.infinite_track.data.soucre.network.request

import com.google.gson.annotations.SerializedName

data class WfaRequestDto(
    @SerializedName("schedule_date") val scheduleDate: String,
    @SerializedName("request_reason_id") val reasonId: Long,
    @SerializedName("request_other_reason") val otherReasonText: String?,
    @SerializedName("notes") val notes: String?,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double
)
```

```kotlin
package com.example.infinite_track.data.mapper.booking

import com.example.infinite_track.data.soucre.network.request.WfaRequestDto
import com.example.infinite_track.domain.model.booking.SubmitWfaRequestCommand

fun SubmitWfaRequestCommand.toDto() = WfaRequestDto(
    scheduleDate = scheduleDate.toString(),
    reasonId = reasonId,
    otherReasonText = otherReasonText,
    notes = notes,
    latitude = location.latitude,
    longitude = location.longitude
)
```

Define nullable-safe response DTOs matching the merged INF-270 response. Map `booking_id`, `schedule_date`, status, location, request reason label, radius snapshot, and submitted timestamp to `SubmittedWfaRequest`. Use `LocalDate.parse` and `Instant.parse` only inside Data; invalid required fields return null and become `Unknown` rather than crashing.

- [ ] **Step 5: Implement the failure mapper**

```kotlin
package com.example.infinite_track.data.mapper.booking

import com.example.infinite_track.domain.model.booking.WfaRequestFailure
import java.io.IOException

object WfaRequestFailureMapper {
    fun mapHttp(
        statusCode: Int,
        code: String?,
        safeMessage: String?,
        fieldErrors: Map<String, String>
    ): WfaRequestFailure = when {
        code == "DUPLICATE_BOOKING" -> WfaRequestFailure.DuplicateRequest
        code in setOf(
            "INVALID_DATE_FORMAT",
            "INVALID_DATE_VALUE",
            "PAST_DATE_NOT_ALLOWED",
            "SAME_DAY_NOT_ALLOWED"
        ) -> WfaRequestFailure.InvalidDate
        code in setOf("REQUEST_REASON_NOT_FOUND", "REQUEST_REASON_INACTIVE") ->
            WfaRequestFailure.ReasonUnavailable
        fieldErrors.isNotEmpty() -> WfaRequestFailure.ValidationRejected(fieldErrors)
        statusCode >= 500 -> WfaRequestFailure.ServerUnavailable
        statusCode in 400..499 && !safeMessage.isNullOrBlank() ->
            WfaRequestFailure.BackendRejected(safeMessage)
        else -> WfaRequestFailure.Unknown
    }

    fun mapThrowable(throwable: Throwable): WfaRequestFailure =
        if (throwable is IOException) WfaRequestFailure.NetworkUnavailable
        else WfaRequestFailure.Unknown
}
```

- [ ] **Step 6: Update Retrofit submit method**

Replace `BookingRequest`/`BookingResponse` types for `submitWfaBooking`:

```kotlin
@POST("api/bookings")
suspend fun submitWfaRequest(
    @Body request: WfaRequestDto
): WfaRequestResponseDto
```

Do not change booking-history endpoints.

- [ ] **Step 7: Run mapper tests and compile**

Use Step 3 command plus `app:compileDebugKotlin`. Expected: BUILD SUCCESSFUL.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/example/infinite_track/data/soucre/network/request/WfaRequestDto.kt app/src/main/java/com/example/infinite_track/data/soucre/network/response/booking/WfaRequestResponseDto.kt app/src/main/java/com/example/infinite_track/data/mapper/booking/WfaRequestDtoMapper.kt app/src/main/java/com/example/infinite_track/data/mapper/booking/WfaRequestFailureMapper.kt app/src/main/java/com/example/infinite_track/data/soucre/network/retrofit/ApiService.kt app/src/test/java/com/example/infinite_track/data/mapper/booking
git commit -m "feat(wfa): add typed request transport mapping"
```

---

### Task 5: Replace primitive repository submission with typed contracts

**Files:**
- Modify: `app/src/main/java/com/example/infinite_track/domain/repository/BookingRepository.kt`
- Modify: `app/src/main/java/com/example/infinite_track/data/repository/booking/BookingRepositoryImpl.kt`
- Test: `app/src/test/java/com/example/infinite_track/data/repository/booking/BookingRepositoryImplWfaRequestTest.kt`

**Interfaces:**
- Produces: `getWfaRequestConfig(): WfaRequestConfigResult` and `submitWfaRequest(command): WfaRequestResult`.
- Preserves: existing `getBookingHistory(...)` signature and behavior.

- [ ] **Step 1: Write failing repository tests with a hand fake `ApiService`**

Cover these cases:

```text
config success → typed WfaRequestConfigResult.Success
config malformed → ConfigUnavailable
submit success → backend-confirmed SubmittedWfaRequest
HTTP duplicate → DuplicateRequest
IOException → NetworkUnavailable
success=false or malformed success body → Unknown/BackendRejected without throwing raw exception
```

Use the same fake-service approach as existing repository tests. Do not introduce a mocking dependency.

- [ ] **Step 2: Run the repository test and verify failure**

```bash
./gradlew app:testDebugUnitTest --tests "com.example.infinite_track.data.repository.booking.BookingRepositoryImplWfaRequestTest" --console=plain
```

Expected: FAIL because repository methods do not exist.

- [ ] **Step 3: Update the repository interface**

```kotlin
interface BookingRepository {
    suspend fun getBookingHistory(
        status: String? = null,
        page: Int = 1,
        limit: Int = 10,
        sortBy: String = "created_at",
        sortOrder: String = "DESC"
    ): Result<BookingHistoryPage>

    suspend fun getWfaRequestConfig(): WfaRequestConfigResult

    suspend fun submitWfaRequest(
        command: SubmitWfaRequestCommand
    ): WfaRequestResult
}
```

Delete the primitive `submitBooking(...)` declaration after all compile errors are resolved within this task series.

- [ ] **Step 4: Implement repository methods**

Implementation rules:

- call Retrofit inside `withContext(Dispatchers.IO)` as current repository convention;
- map config through `toDomainOrNull()`;
- map command through `toDto()`;
- map submit response through the dedicated response mapper;
- catch `HttpException`, parse the locked backend error DTO once, and call `WfaRequestFailureMapper.mapHttp(...)`;
- catch other throwables and call `mapThrowable`;
- never return raw `Exception(message)` for these new methods;
- never log request body, employee identity, or auth-bearing response.

- [ ] **Step 5: Run repository and booking-history tests**

```bash
./gradlew app:testDebugUnitTest --tests "com.example.infinite_track.data.repository.booking.*" --console=plain
```

Expected: PASS, including existing booking-history coverage.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/example/infinite_track/domain/repository/BookingRepository.kt app/src/main/java/com/example/infinite_track/data/repository/booking/BookingRepositoryImpl.kt app/src/test/java/com/example/infinite_track/data/repository/booking/BookingRepositoryImplWfaRequestTest.kt
git commit -m "refactor(wfa): expose typed booking repository contracts"
```

---

### Task 6: Add bounded config/submit use cases and Hilt wiring

**Files:**
- Create: `app/src/main/java/com/example/infinite_track/domain/use_case/booking/LoadWfaRequestConfigUseCase.kt`
- Create: `app/src/main/java/com/example/infinite_track/domain/use_case/booking/SubmitWfaRequestUseCase.kt`
- Delete after migration: `app/src/main/java/com/example/infinite_track/domain/use_case/booking/SubmitWfaBookingUseCase.kt`
- Modify only if required by current constructor wiring: `app/src/main/java/com/example/infinite_track/di/UseCaseModule.kt`
- Test: `app/src/test/java/com/example/infinite_track/domain/use_case/booking/LoadWfaRequestConfigUseCaseTest.kt`
- Test: `app/src/test/java/com/example/infinite_track/domain/use_case/booking/SubmitWfaRequestUseCaseTest.kt`

**Interfaces:**
- `LoadWfaRequestConfigUseCase(): WfaRequestConfigResult`.
- `SubmitWfaRequestUseCase(command): WfaRequestResult`.

- [ ] **Step 1: Write failing use-case tests**

Use a hand-written `BookingRepository` fake that records commands and returns configurable results. Assert:

- valid config is returned unchanged;
- non-positive radius/empty reasons is converted to `ConfigUnavailable`;
- submit delegates exactly once with the same command;
- submit returns typed failure unchanged.

- [ ] **Step 2: Run the tests and verify failure**

```bash
./gradlew app:testDebugUnitTest --tests "com.example.infinite_track.domain.use_case.booking.LoadWfaRequestConfigUseCaseTest" --tests "com.example.infinite_track.domain.use_case.booking.SubmitWfaRequestUseCaseTest" --console=plain
```

Expected: FAIL because use cases do not exist.

- [ ] **Step 3: Implement the use cases**

```kotlin
class LoadWfaRequestConfigUseCase @Inject constructor(
    private val repository: BookingRepository
) {
    suspend operator fun invoke(): WfaRequestConfigResult = when (val result = repository.getWfaRequestConfig()) {
        is WfaRequestConfigResult.Success -> {
            val config = result.config
            if (config.radiusMeters > 0 && config.reasons.isNotEmpty()) result
            else WfaRequestConfigResult.Failure(WfaRequestFailure.ConfigUnavailable)
        }
        is WfaRequestConfigResult.Failure -> result
    }
}
```

```kotlin
class SubmitWfaRequestUseCase @Inject constructor(
    private val repository: BookingRepository
) {
    suspend operator fun invoke(command: SubmitWfaRequestCommand): WfaRequestResult =
        repository.submitWfaRequest(command)
}
```

Because constructors use `@Inject`, do not add redundant Hilt providers. Remove obsolete provider code only if current `UseCaseModule` explicitly provides `SubmitWfaBookingUseCase`.

- [ ] **Step 4: Run tests and compile**

Use Step 2 command plus `app:compileDebugKotlin`. Expected: BUILD SUCCESSFUL after temporary call-site compile updates are made in Task 8; before then, limit this task's command to the unit tests if old production call sites still reference the old use case.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/infinite_track/domain/use_case/booking app/src/main/java/com/example/infinite_track/di/UseCaseModule.kt app/src/test/java/com/example/infinite_track/domain/use_case/booking
git commit -m "feat(wfa): add config and typed submit use cases"
```

---

### Task 7: Add presentation state, events, effects, and failure copy mapper

**Files:**
- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/wfa_request/WfaRequestUiState.kt`
- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/wfa_request/WfaRequestEvent.kt`
- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/wfa_request/WfaRequestEffect.kt`
- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/wfa_request/WfaRequestUiMapper.kt`
- Test: `app/src/test/java/com/example/infinite_track/presentation/screen/attendance/wfa_request/WfaRequestUiMapperTest.kt`

**Interfaces:**
- Produces the stable presentation contracts consumed by ViewModel and screens.

- [ ] **Step 1: Write failing UI mapper tests**

```kotlin
class WfaRequestUiMapperTest {
    @Test
    fun `duplicate request has correction copy`() {
        val copy = WfaRequestUiMapper.map(WfaRequestFailure.DuplicateRequest)
        assertEquals("Permintaan sudah ada", copy.title)
        assertEquals("Anda sudah memiliki permintaan WFA aktif pada tanggal tersebut.", copy.message)
        assertEquals(WfaRequestFailureAction.EDIT, copy.primaryAction)
    }

    @Test
    fun `network failure is retryable`() {
        val copy = WfaRequestUiMapper.map(WfaRequestFailure.NetworkUnavailable)
        assertEquals(WfaRequestFailureAction.RETRY, copy.primaryAction)
    }
}
```

- [ ] **Step 2: Run and verify failure**

```bash
./gradlew app:testDebugUnitTest --tests "com.example.infinite_track.presentation.screen.attendance.wfa_request.WfaRequestUiMapperTest" --console=plain
```

- [ ] **Step 3: Add phase/state/event/effect types**

```kotlin
sealed interface WfaRequestPhase {
    data object Loading : WfaRequestPhase
    data object Editing : WfaRequestPhase
    data object ReadyForReview : WfaRequestPhase
    data object Reviewing : WfaRequestPhase
    data object Submitting : WfaRequestPhase
    data object Success : WfaRequestPhase
    data object Failure : WfaRequestPhase
}

data class WfaEmployeeSummary(val fullName: String, val division: String)

data class WfaRequestUiState(
    val phase: WfaRequestPhase = WfaRequestPhase.Loading,
    val employee: WfaEmployeeSummary? = null,
    val config: WfaRequestConfig? = null,
    val draft: WfaRequestDraft = WfaRequestDraft.Empty,
    val fieldErrors: WfaRequestFieldErrors = WfaRequestFieldErrors(),
    val submitResult: SubmittedWfaRequest? = null,
    val failure: WfaRequestFailure? = null
)
```

Use the event list from the spec, including separate retry-config and retry-submit events. Define effects as navigation semantics, not route strings.

- [ ] **Step 4: Implement localized failure mapping**

Follow `AttendanceSubmitUiMapper` style: presentation-only strings, typed input, no Retrofit types. Define `WfaRequestFailureAction` as `EDIT`, `RETRY`, or `BACK` and return `WfaRequestUiFailure(title, message, primaryAction)`.

- [ ] **Step 5: Run tests**

Use Step 2 command. Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/example/infinite_track/presentation/screen/attendance/wfa_request app/src/test/java/com/example/infinite_track/presentation/screen/attendance/wfa_request/WfaRequestUiMapperTest.kt
git commit -m "feat(wfa): define request presentation contracts"
```

---

### Task 8: Build the graph-scoped `WfaRequestViewModel`

**Files:**
- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/wfa_request/WfaRequestViewModel.kt`
- Test: `app/src/test/java/com/example/infinite_track/presentation/screen/attendance/wfa_request/WfaRequestViewModelTest.kt`

**Interfaces:**
- Consumes: config/validation/submit use cases, `GetLoggedInUserUseCase`, `ReverseGeocodeUseCase`, typed route bootstrap.
- Produces: `StateFlow<WfaRequestUiState>`, `Flow<WfaRequestEffect>`, `onEvent(event)`.

- [ ] **Step 1: Write failing ViewModel tests**

Use `StandardTestDispatcher`, `runTest`, and hand fakes. Cover:

```text
initial load reads employee once
initial load resolves candidate coordinate and config
config failure blocks editing and supports RetryConfigClicked
field events mutate only in Editing
ReviewClicked invalid → field errors, no effect
ReviewClicked valid → ReadyForReview + OpenReview once
EditClicked → Editing + ReturnToForm
SubmitConfirmed → Submitting before fake repository resumes
repeated SubmitConfirmed while in flight → one submit call
success → Success with backend result
failure → Failure with original draft preserved
RetrySubmitClicked → one new attempt
no raw exception message enters state
```

For the in-flight test, make the fake submit use case wait on a `CompletableDeferred` and assert call count before completing it.

- [ ] **Step 2: Run and verify failure**

```bash
./gradlew app:testDebugUnitTest --tests "com.example.infinite_track.presentation.screen.attendance.wfa_request.WfaRequestViewModelTest" --console=plain
```

Expected: FAIL because ViewModel does not exist.

- [ ] **Step 3: Implement initialization**

Constructor:

```kotlin
@HiltViewModel
class WfaRequestViewModel @Inject constructor(
    private val loadConfig: LoadWfaRequestConfigUseCase,
    private val validateDraft: ValidateWfaRequestDraftUseCase,
    private val submitRequest: SubmitWfaRequestUseCase,
    private val getLoggedInUser: GetLoggedInUserUseCase,
    private val reverseGeocode: ReverseGeocodeUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel()
```

Parse route latitude/longitude as strings to `Double`, reject invalid/missing values into a safe blocked failure, and create a `WfaCandidateLocation` after address resolution. Read the user with `filterNotNull().first()` exactly once. Load config once. Do not nest long-lived collectors.

- [ ] **Step 4: Implement event reducer and submit guard**

Core rule:

```kotlin
private suspend fun submit(command: SubmitWfaRequestCommand) {
    if (_uiState.value.phase == WfaRequestPhase.Submitting) return
    _uiState.update { it.copy(phase = WfaRequestPhase.Submitting, failure = null) }
    when (val result = submitRequest(command)) {
        is WfaRequestResult.Success -> _uiState.update {
            it.copy(
                phase = WfaRequestPhase.Success,
                submitResult = result.request,
                failure = null
            )
        }
        is WfaRequestResult.Failure -> _uiState.update {
            it.copy(
                phase = WfaRequestPhase.Failure,
                failure = result.failure,
                submitResult = null
            )
        }
    }
}
```

Store the last validated command separately from editable draft only while Review/Submit is active. `EditClicked` returns to the draft and clears field errors without losing values.

- [ ] **Step 5: Implement effect delivery**

Use a buffered `Channel<WfaRequestEffect>` exposed with `receiveAsFlow()`. Emit navigation effects only after the corresponding state transition. Do not store route strings or `NavController`.

- [ ] **Step 6: Run ViewModel tests**

Use Step 2 command. Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/example/infinite_track/presentation/screen/attendance/wfa_request/WfaRequestViewModel.kt app/src/test/java/com/example/infinite_track/presentation/screen/attendance/wfa_request/WfaRequestViewModelTest.kt
git commit -m "feat(wfa): orchestrate one request transaction session"
```

---

### Task 9: Build Form, Review, Submitting, and Result UI surfaces

**Files:**
- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/wfa_request/WfaRequestFormScreen.kt`
- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/wfa_request/WfaRequestReviewScreen.kt`
- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/wfa_request/WfaRequestResultScreen.kt`
- Create focused components under: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/wfa_request/components/`
- Add/modify string resources under: `app/src/main/res/values/strings.xml`
- Test: `app/src/androidTest/java/com/example/infinite_track/presentation/screen/attendance/wfa_request/WfaRequestScreensTest.kt`

**Interfaces:**
- Screens receive immutable state and callbacks; no screen receives ViewModel or NavController in its previewable content function.

- [ ] **Step 1: Write failing Compose tests**

Cover stable semantics with test tags/content descriptions:

```text
form shows employee and location summaries
radius value is visible and no editable radius field exists
server reasons render as selectable options
Other selection shows explanation field
field errors render next to the owning control
Review CTA dispatches one callback
review renders exact draft values
Submitting disables confirm
success renders backend booking ID
failure renders mapped safe copy and correct action
```

Use synthetic identity/location values only.

- [ ] **Step 2: Compile instrumentation tests and verify failure**

```bash
./gradlew app:compileDebugAndroidTestKotlin --console=plain
```

Expected: FAIL because screens/components do not exist.

- [ ] **Step 3: Build feature-local reusable components**

Create:

```text
WfaSelectedLocationCard
WfaEmployeeSummaryCard
WfaRequestPolicyInfo
WfaRequestDateField
WfaRequestReasonField
WfaRequestNotesField
WfaRequestReviewSummary
WfaRequestSubmittingSurface
WfaRequestResultSurface
```

Use existing theme tokens and Material 3 primitives. No hardcoded business policy. Keep the app root background visible and screen surfaces transparent unless a component needs its own card surface.

- [ ] **Step 4: Build Form screen**

Form signature:

```kotlin
@Composable
fun WfaRequestFormScreen(
    state: WfaRequestUiState,
    onEvent: (WfaRequestEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
)
```

Render loading/config-error/editing states from `state.phase`. The only primary CTA is `Review Permintaan`.

- [ ] **Step 5: Build Review and Result screens**

Review is read-only and emits `EditClicked` / `SubmitConfirmed`. Result renders `SubmittedWfaRequest` for success and `WfaRequestUiMapper` output for failure. Do not link to WFA history/list.

- [ ] **Step 6: Run instrumentation compile and unit tests**

```bash
./gradlew app:compileDebugAndroidTestKotlin app:testDebugUnitTest --console=plain
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/example/infinite_track/presentation/screen/attendance/wfa_request app/src/main/res/values/strings.xml app/src/androidTest/java/com/example/infinite_track/presentation/screen/attendance/wfa_request
git commit -m "feat(wfa): add form review and result surfaces"
```

---

### Task 10: Add the nested WFA request graph and migrate route ownership

**Files:**
- Modify: `app/src/main/java/com/example/infinite_track/presentation/navigation/Screen.kt`
- Create: `app/src/main/java/com/example/infinite_track/presentation/navigation/WfaRequestNavGraph.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/navigation/MainContentNavGraph.kt`
- Modify all current callers of: `Screen.WfaBooking.createRoute(...)`
- Test: `app/src/androidTest/java/com/example/infinite_track/presentation/navigation/WfaRequestNavigationTest.kt`

**Interfaces:**
- Produces one `WfaRequestFlow` graph with Form/Review/Result destinations and one shared ViewModel.

- [ ] **Step 1: Search and record all old route call sites**

```bash
git grep -n "WfaBooking\|wfa_booking"
```

Expected current owners include `Screen.kt`, `MainContentNavGraph.kt`, and Attendance/location-selection call sites. Add every found production call site to this task's migration diff; do not leave mixed route ownership.

- [ ] **Step 2: Add route definitions with precision-safe arguments**

```kotlin
data object WfaRequestFlow : Screen(
    "wfa_request?latitude={latitude}&longitude={longitude}"
) {
    fun createRoute(latitude: Double, longitude: Double): String =
        "wfa_request?latitude=$latitude&longitude=$longitude"
}

data object WfaRequestForm : Screen("wfa_request/form")
data object WfaRequestReview : Screen("wfa_request/review")
data object WfaRequestResult : Screen("wfa_request/result")
```

Declare graph arguments as `NavType.StringType`; parse them to `Double` in ViewModel bootstrap. This avoids the existing `FloatType` precision loss.

- [ ] **Step 3: Add the nested graph**

Follow the existing History graph pattern:

```kotlin
fun NavGraphBuilder.wfaRequestNavGraph(navController: NavHostController) {
    navigation(
        startDestination = Screen.WfaRequestForm.route,
        route = Screen.WfaRequestFlow.route,
        arguments = listOf(
            navArgument("latitude") { type = NavType.StringType },
            navArgument("longitude") { type = NavType.StringType }
        )
    ) {
        composable(Screen.WfaRequestForm.route) { entry ->
            val parentEntry = remember(entry) {
                navController.getBackStackEntry(Screen.WfaRequestFlow.route)
            }
            val viewModel: WfaRequestViewModel = hiltViewModel(parentEntry)
            // collect state/effects and render Form
        }
        // Review and Result use the same parentEntry/ViewModel pattern.
    }
}
```

Each active destination collects effects and performs semantic navigation. ViewModel remains navigation-free.

- [ ] **Step 4: Migrate entry call sites**

Replace every old `Screen.WfaBooking.createRoute(latitude, longitude)` call with `Screen.WfaRequestFlow.createRoute(latitude, longitude)`. Remove the old standalone booking composable from `MainContentNavGraph.kt` and call `wfaRequestNavGraph(navController)` once.

- [ ] **Step 5: Add navigation tests**

Verify:

```text
Form/Review/Result are registered once
all three resolve the same graph-scoped ViewModel
Form → Review and Review → Result navigation do not duplicate destinations
Edit returns to Form without rebuilding the transaction
success return to Attendance/Home pops the WFA graph
```

- [ ] **Step 6: Run navigation/instrumentation compile and debug compile**

```bash
./gradlew app:compileDebugKotlin app:compileDebugAndroidTestKotlin --console=plain
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/example/infinite_track/presentation/navigation app/src/main/java/com/example/infinite_track/presentation/screen/attendance app/src/androidTest/java/com/example/infinite_track/presentation/navigation
git commit -m "feat(wfa): own request flow in a nested navigation graph"
```

---

### Task 11: Remove the legacy dialog-based booking path

**Files:**
- Delete: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/booking/WfaBookingScreen.kt`
- Delete: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/booking/WfaBookingViewModel.kt`
- Delete: `app/src/main/java/com/example/infinite_track/presentation/components/dialog/WfaBookingDialog.kt`
- Delete: `app/src/main/java/com/example/infinite_track/data/soucre/network/request/BookingRequest.kt`
- Delete or replace if no remaining references: `app/src/main/java/com/example/infinite_track/data/soucre/network/response/booking/BookingResponse.kt`
- Delete: `app/src/main/java/com/example/infinite_track/domain/use_case/booking/SubmitWfaBookingUseCase.kt`
- Modify imports/wiring found by `git grep`.

**Interfaces:**
- Removes editable radius, silent date fallback, generic `Result<Unit>`, success boolean, and duplicate UI implementation.

- [ ] **Step 1: Prove the old path has no legitimate callers**

```bash
git grep -n "WfaBookingScreen\|WfaBookingViewModel\|WfaBookingDialog\|SubmitWfaBookingUseCase\|BookingRequest\|BookingResponse\|submitBooking(" -- app/src
```

Expected before deletion: only legacy definitions/imports that this task removes. Any non-history production caller must be migrated rather than ignored.

- [ ] **Step 2: Delete legacy files and stale DI/imports**

Do not leave a deprecated bridge that still sends radius or formats date in Presentation.

- [ ] **Step 3: Verify removed behaviors by source search**

```bash
git grep -n "onRadiusChanged\|SimpleDateFormat\|isBookingSuccessful\|wfa_booking/{latitude}/{longitude}" -- app/src || true
```

Expected: no INF-265 legacy occurrences.

- [ ] **Step 4: Run the full unit suite and compile**

```bash
./gradlew app:testDebugUnitTest app:compileDebugKotlin --console=plain
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "refactor(wfa): remove legacy dialog booking flow"
```

---

### Task 12: Final verification, runtime evidence, and issue/PR handoff

**Files:**
- Update only when source-of-truth behavior changed: focused docs/ADR under `docs/`.
- Add runtime evidence links/screenshots to Linear/PR, not raw secrets or auth-bearing logs.

**Interfaces:**
- Produces release-readiness evidence for INF-265 without marking backend-dependent gaps as complete.

- [ ] **Step 1: Run source and diff hygiene checks**

```bash
git status --short
git diff --check develop...HEAD
git grep -n "request_reason_id\|request_other_reason" -- app/src/main
git grep -n "SerializedName(\"radius\")" -- app/src/main/java/com/example/infinite_track/data/soucre/network/request || true
```

Expected: clean working tree after commits; no whitespace errors; new request fields present; no radius in WFA submit request DTO.

- [ ] **Step 2: Run required Gradle verification**

```bash
./gradlew app:testDebugUnitTest --console=plain
./gradlew app:compileDebugAndroidTestKotlin --console=plain
./gradlew app:lintDebug --console=plain
./gradlew app:assembleDebug --console=plain
```

Expected: BUILD SUCCESSFUL for each command. Document any pre-existing unrelated blocker with exact command and scoped evidence; do not hide it.

- [ ] **Step 3: Run focused emulator/device flow**

Verify with the INF-270 backend contract available:

```text
Attendance → WFA candidate → Request Form
server radius visible and read-only
active server reasons visible
Other requires explanation
invalid draft cannot open Review
Review exactly matches draft
confirm enters Submitting and sends once
success shows backend booking ID/status
network/server failure preserves draft
return to Attendance/Home does not replay submit
rotation/recomposition does not duplicate effects
```

Capture screenshots/recording for Form, validation, Review, Submitting, Success, and Failure. Redact tokens, email, identifiers, and private employee data.

- [ ] **Step 4: Run verification-before-completion review**

Check:

```text
Presentation: screens render state/send events only
Domain: no Android/Compose/Retrofit/DTO imports
Data: DTO/date/error mapping contained here
Navigation: one graph owner, one graph-scoped ViewModel
Source of truth: radius/reasons/backend success remain server-authoritative
Scope: no WFA history/list or Contact redesign
Tests: duplicate submit and duplicate effect coverage present
```

- [ ] **Step 5: Prepare PR body**

```markdown
## Linear
- INF-265
- Blocked contract: INF-270

## Architecture
- Presentation: Form/Review/Result use one graph-scoped WfaRequestViewModel.
- Domain: typed config/draft/command/result/failure and pure validation.
- Data: server config, ISO date mapping, response mapping, typed failure classification.
- Navigation: one nested WFA request graph; legacy standalone destination removed.
- Source of truth: Backend owns radius, reasons, policy, and final success.

## Scope exclusions
- No WFA history/list/filter work.
- No Contact-tab changes.
- No backend or Management Web implementation.

## Verification
- app:testDebugUnitTest: PASS
- app:compileDebugAndroidTestKotlin: PASS
- app:lintDebug: PASS
- app:assembleDebug: PASS
- Device flow: PASS or Needs Verification with reason
```

- [ ] **Step 6: Commit any final documentation-only correction**

```bash
git add docs
git commit -m "docs(wfa): record INF-265 verification contract"
```

Skip this commit when no documentation file changed.

---

## Plan self-review

### Spec coverage

- Server configuration and read-only radius: Tasks 3, 5, 6, 8, 9.
- Typed draft/command/result/failure: Tasks 1, 2, 4, 5.
- Form/Review/Result transaction state: Tasks 7–10.
- Graph-scoped ViewModel and navigation ownership: Tasks 8 and 10.
- Removal of fallback date/editable radius/legacy dialog: Tasks 2, 4, 11.
- Duplicate submit/effect handling: Tasks 8, 10, 12.
- Backend-confirmed result: Tasks 4, 5, 8, 9.
- No history/list scope creep: Global Constraints, Tasks 5, 9, 12.
- Tests/build/runtime evidence: every task plus Task 12.

### Type consistency

- `SubmitWfaRequestCommand` is created only by `ValidateWfaRequestDraftUseCase` and consumed by `SubmitWfaRequestUseCase`/`BookingRepository`.
- `WfaRequestResult.Success` always carries `SubmittedWfaRequest`.
- `WfaRequestConfigResult` and `WfaRequestResult` share `WfaRequestFailure` classification.
- Presentation state uses Domain values but no Data/Retrofit types.
- Request DTO has no radius field by construction.

### Execution handoff

Plan execution must start only after entering an isolated worktree for this branch and confirming the INF-270 production contract. Recommended execution mode: `superpowers:subagent-driven-development`, one task and two-stage review at a time.
