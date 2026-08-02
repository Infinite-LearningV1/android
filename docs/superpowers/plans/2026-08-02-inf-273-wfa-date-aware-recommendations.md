# INF-273 Date-Aware WFA Recommendations Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move date-aware WFA recommendation discovery into the existing graph-scoped WFA Request transaction, default the request date to tomorrow in Jakarta, and automatically reload truthful INF-272 recommendations whenever the date changes.

**Architecture:** Attendance keeps ownership of today's backend-authoritative WFA state and approved attendance target. `WfaRequestViewModel` becomes the single owner of future schedule date, current-coordinate snapshot, recommendation lifecycle, selected candidate, form draft, review, and submission. The existing Clean Architecture path remains `Screen → ViewModel → UseCase → Repository → RepositoryImpl → ApiService`.

**Tech Stack:** Kotlin, Android 14 / minSdk 26, Jetpack Compose, Material 3, Navigation Compose, Kotlin Coroutines/StateFlow, Hilt, Retrofit/Gson, JUnit4, Compose UI Test.

## Global Constraints

- Work only on `feature/inf-273-wfa-date-aware-recommendations` in an isolated worktree.
- Integration target remains `develop`.
- Backend dependency is INF-272; do not modify Backend from this branch.
- Calendar policy is `Asia/Jakarta`; today and past are invalid, tomorrow is the default minimum.
- No same-day booking support.
- Approved WFA booking remains the only authoritative WFA attendance target.
- `AttendanceViewModel` must not own new-request recommendations after migration.
- One graph-scoped `WfaRequestViewModel` owns form, review, result, and recommendation state.
- No DTO, Retrofit exception, `Context`, Compose type, or `NavController` enters Domain.
- Missing final score is `null`, never `0`.
- Missing facility evidence is `UNKNOWN`, never inferred false.
- Do not restore wifi/noise/crowd/generic-amenity score details.
- Reuse the existing provider-neutral map adapter; do not perform a broad map rewrite.
- Preserve INF-265 form → review → submit → result semantics.
- Every production change starts with a failing focused test and ends with a bounded commit.

---

## Target File Map

### New files

- `app/src/main/java/com/example/infinite_track/domain/validation/WfaScheduleDatePolicy.kt`
- `app/src/main/java/com/example/infinite_track/domain/model/wfa/WfaRecommendationContract.kt`
- `app/src/main/java/com/example/infinite_track/data/mapper/wfa/WfaRecommendationFailureMapper.kt`
- `app/src/main/java/com/example/infinite_track/presentation/navigation/LocationSearchResultContract.kt`
- `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/wfa_request/WfaRequestRecommendationState.kt`
- `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/wfa_request/WfaRequestMapUiMapper.kt`
- `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/wfa_request/components/WfaRecommendationPicker.kt`
- corresponding focused test files described in each task.

### Main modified files

- `domain/model/booking/WfaRequestDraft.kt`
- `domain/use_case/booking/ValidateWfaRequestDraftUseCase.kt`
- `domain/model/wfa/WfaModels.kt`
- `domain/repository/WfaRepository.kt`
- `domain/use_case/wfa/GetWfaRecommendationsUseCase.kt`
- `data/soucre/network/response/WfaRecommendationResponse.kt`
- `data/soucre/network/retrofit/ApiService.kt`
- `data/mapper/wfa/WfaMapper.kt`
- `data/repository/wfa/WfaRepositoryImpl.kt`
- `presentation/navigation/Screen.kt`
- `presentation/navigation/WfaRequestNavGraph.kt`
- `presentation/screen/attendance/wfa_request/WfaRequestEvent.kt`
- `presentation/screen/attendance/wfa_request/WfaRequestUiState.kt`
- `presentation/screen/attendance/wfa_request/WfaRequestViewModel.kt`
- `presentation/screen/attendance/wfa_request/WfaRequestFormScreen.kt`
- `presentation/screen/attendance/AttendanceViewModel.kt`
- `presentation/screen/attendance/AttendanceScreen.kt`
- `presentation/screen/attendance/preparation/AttendancePreparationState.kt`
- `presentation/screen/attendance/preparation/AttendancePreparationUiMapper.kt`
- `presentation/map/mapper/AttendanceMapUiMapper.kt`
- `app/src/main/res/values/strings.xml`

---

### Task 1: Add Jakarta future-date policy and enforce it in Domain

**Files:**
- Create: `app/src/main/java/com/example/infinite_track/domain/validation/WfaScheduleDatePolicy.kt`
- Create: `app/src/test/java/com/example/infinite_track/domain/validation/WfaScheduleDatePolicyTest.kt`
- Modify: `app/src/main/java/com/example/infinite_track/domain/model/booking/WfaRequestDraft.kt`
- Modify: `app/src/main/java/com/example/infinite_track/domain/use_case/booking/ValidateWfaRequestDraftUseCase.kt`
- Modify: `app/src/test/java/com/example/infinite_track/domain/use_case/booking/ValidateWfaRequestDraftUseCaseTest.kt`

**Interfaces:**
- Produces: `WfaScheduleDatePolicy.today(clock)`, `minimumDate(today)`, and `isSelectable(date, today)`.
- Produces: `WfaRequestFieldError.FUTURE_DATE_REQUIRED`.
- Consumed later by: `WfaRequestViewModel` and `WfaRequestFormScreen` state.

- [ ] **Step 1: Write the failing date-policy tests**

```kotlin
class WfaScheduleDatePolicyTest {
    private val policy = WfaScheduleDatePolicy()
    private val today = LocalDate.of(2026, 8, 2)

    @Test
    fun `tomorrow is the minimum selectable date`() {
        assertEquals(LocalDate.of(2026, 8, 3), policy.minimumDate(today))
    }

    @Test
    fun `today and past are rejected while future is accepted`() {
        assertFalse(policy.isSelectable(today.minusDays(1), today))
        assertFalse(policy.isSelectable(today, today))
        assertTrue(policy.isSelectable(today.plusDays(1), today))
        assertTrue(policy.isSelectable(today.plusDays(20), today))
    }

    @Test
    fun `today uses Asia Jakarta`() {
        val instant = Instant.parse("2026-08-02T16:30:00Z")
        val clock = Clock.fixed(instant, ZoneOffset.UTC)

        assertEquals(LocalDate.of(2026, 8, 2), policy.today(clock))
    }
}
```

- [ ] **Step 2: Add failing validator coverage**

```kotlin
@Test
fun `same day and past return FUTURE_DATE_REQUIRED`() {
    val today = LocalDate.of(2026, 8, 2)
    val policy = WfaScheduleDatePolicy()
    val useCase = ValidateWfaRequestDraftUseCase(policy)

    listOf(today.minusDays(1), today).forEach { invalidDate ->
        val result = useCase(validDraft.copy(scheduleDate = invalidDate), config, today)
        assertEquals(
            WfaRequestFieldError.FUTURE_DATE_REQUIRED,
            (result as WfaRequestValidationResult.Invalid).errors.scheduleDate
        )
    }
}
```

- [ ] **Step 3: Run the focused tests and confirm failure**

Run:

```bash
./gradlew :app:testDebugUnitTest \
  --tests "*WfaScheduleDatePolicyTest" \
  --tests "*ValidateWfaRequestDraftUseCaseTest"
```

Expected: compilation/test failure because the policy and field error do not exist and validation does not accept `today`.

- [ ] **Step 4: Implement the date policy**

```kotlin
package com.example.infinite_track.domain.validation

import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class WfaScheduleDatePolicy @Inject constructor() {
    fun today(clock: Clock = Clock.system(JAKARTA_ZONE)): LocalDate =
        LocalDate.now(clock.withZone(JAKARTA_ZONE))

    fun minimumDate(today: LocalDate = today()): LocalDate = today.plusDays(1)

    fun isSelectable(
        date: LocalDate,
        today: LocalDate = today()
    ): Boolean = date.isAfter(today)

    companion object {
        val JAKARTA_ZONE: ZoneId = ZoneId.of("Asia/Jakarta")
    }
}
```

- [ ] **Step 5: Extend the field-error enum and validator**

Add:

```kotlin
FUTURE_DATE_REQUIRED
```

Change the validator signature and schedule branch:

```kotlin
class ValidateWfaRequestDraftUseCase @Inject constructor(
    private val datePolicy: WfaScheduleDatePolicy = WfaScheduleDatePolicy()
) {
    operator fun invoke(
        draft: WfaRequestDraft,
        config: WfaRequestConfig,
        today: LocalDate = datePolicy.today()
    ): WfaRequestValidationResult {
        val scheduleError = when {
            draft.scheduleDate == null -> WfaRequestFieldError.REQUIRED
            !datePolicy.isSelectable(draft.scheduleDate, today) ->
                WfaRequestFieldError.FUTURE_DATE_REQUIRED
            else -> null
        }
        // Preserve existing reason, other-reason, notes, and location validation.
    }
}
```

- [ ] **Step 6: Run focused tests and confirm pass**

```bash
./gradlew :app:testDebugUnitTest \
  --tests "*WfaScheduleDatePolicyTest" \
  --tests "*ValidateWfaRequestDraftUseCaseTest"
```

Expected: PASS.

- [ ] **Step 7: Commit the date policy**

```bash
git add app/src/main/java/com/example/infinite_track/domain/validation/WfaScheduleDatePolicy.kt \
  app/src/main/java/com/example/infinite_track/domain/model/booking/WfaRequestDraft.kt \
  app/src/main/java/com/example/infinite_track/domain/use_case/booking/ValidateWfaRequestDraftUseCase.kt \
  app/src/test/java/com/example/infinite_track/domain/validation/WfaScheduleDatePolicyTest.kt \
  app/src/test/java/com/example/infinite_track/domain/use_case/booking/ValidateWfaRequestDraftUseCaseTest.kt
git commit -m "feat(INF-273): enforce future WFA schedule dates"
```

---

### Task 2: Replace the legacy recommendation model and DTO with the INF-272 contract

**Files:**
- Create: `app/src/main/java/com/example/infinite_track/domain/model/wfa/WfaRecommendationContract.kt`
- Modify: `app/src/main/java/com/example/infinite_track/domain/model/wfa/WfaModels.kt`
- Modify: `app/src/main/java/com/example/infinite_track/data/soucre/network/response/WfaRecommendationResponse.kt`
- Modify: `app/src/main/java/com/example/infinite_track/data/mapper/wfa/WfaMapper.kt`
- Modify: `app/src/test/java/com/example/infinite_track/data/mapper/wfa/WfaMapperTest.kt`

**Interfaces:**
- Produces: `WfaRecommendationQuery`, `WfaRecommendationResult`, `WfaRecommendationFailure`, `WfaRecommendationStatus`, `WfaFacilityAvailability`, `WfaFacilityEvidence`, and `WfaRecommendationMeta`.
- Produces: truthful `WfaRecommendation` with nullable final scoring.
- Consumed later by: repository, ViewModel, map mapper, and recommendation cards.

- [ ] **Step 1: Replace mapper fixtures with truthful contract fixtures**

Write tests for all three candidate states:

```kotlin
@Test
fun `ranked candidate maps final score and facility evidence`() {
    val result = rankedRecommendationDto().toDomainOrNull()

    requireNotNull(result)
    assertEquals(WfaRecommendationStatus.Ranked, result.status)
    assertEquals(82.4, result.finalScore!!, 0.0)
    assertEquals("Sangat Tinggi", result.finalLabel)
    assertEquals(WfaFacilityAvailability.AVAILABLE, result.facilities.internetAccess)
    assertEquals(WfaFacilityAvailability.UNKNOWN, result.facilities.wheelchairAccessibility)
}

@Test
fun `insufficient data remains selectable with null final score`() {
    val result = rankedRecommendationDto().copy(
        status = "insufficient_facility_data",
        finalRank = null,
        finalScore = null,
        finalLabel = null
    ).toDomainOrNull()

    requireNotNull(result)
    assertEquals(WfaRecommendationStatus.InsufficientFacilityData, result.status)
    assertNull(result.finalScore)
    assertNull(result.finalLabel)
}

@Test
fun `stable key prefers place id`() {
    val result = rankedRecommendationDto(placeId = "place-123").toDomainOrNull()
    assertEquals("place:place-123", requireNotNull(result).stableKey)
}
```

Also test:

```text
facility_enrichment_failed
true/false/null facility mapping
unknown status
invalid facility confidence outside 0..100
ranked with missing score becomes Unsupported and does not invent a score
```

- [ ] **Step 2: Run mapper tests and confirm failure**

```bash
./gradlew :app:testDebugUnitTest --tests "*WfaMapperTest"
```

Expected: compilation failure because the new DTO and Domain types do not exist.

- [ ] **Step 3: Add the typed Domain contract**

```kotlin
data class WfaRecommendationQuery(
    val origin: GeoCoordinate,
    val scheduleDate: LocalDate
)

sealed interface WfaRecommendationStatus {
    data object Ranked : WfaRecommendationStatus
    data object InsufficientFacilityData : WfaRecommendationStatus
    data object FacilityEnrichmentFailed : WfaRecommendationStatus
    data class Unsupported(val raw: String) : WfaRecommendationStatus
}

enum class WfaFacilityAvailability { AVAILABLE, UNAVAILABLE, UNKNOWN }

data class WfaFacilityEvidence(
    val internetAccess: WfaFacilityAvailability,
    val openingHours: WfaFacilityAvailability,
    val toilets: WfaFacilityAvailability,
    val airConditioning: WfaFacilityAvailability,
    val wheelchairAccessibility: WfaFacilityAvailability
)

sealed interface WfaRecommendationFailure {
    data object CurrentLocationUnavailable : WfaRecommendationFailure
    data object InvalidScheduleDate : WfaRecommendationFailure
    data object DuplicateBooking : WfaRecommendationFailure
    data object NetworkUnavailable : WfaRecommendationFailure
    data object ProviderUnavailable : WfaRecommendationFailure
    data object ServerUnavailable : WfaRecommendationFailure
    data object Unknown : WfaRecommendationFailure
}
```

Define `WfaRecommendationResult.Success` with `scheduleDate`, `timezone`, `recommendations`, and optional `meta`.

- [ ] **Step 4: Replace the active DTO**

Use DTO fields that match INF-272 exactly:

```kotlin
data class RecommendationItem(
    @SerializedName("place_id") val placeId: String?,
    @SerializedName("name") val name: String,
    @SerializedName("address") val address: String,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("distance_meters") val distanceMeters: Double,
    @SerializedName("place_type") val placeType: String,
    @SerializedName("status") val status: String,
    @SerializedName("final_rank") val finalRank: Int?,
    @SerializedName("final_score") val finalScore: Double?,
    @SerializedName("final_label") val finalLabel: String?,
    @SerializedName("facility_score") val facilityScore: Double?,
    @SerializedName("facility_confidence") val facilityConfidence: Int,
    @SerializedName("facilities") val facilities: FacilityEvidenceDto
)

data class FacilityEvidenceDto(
    @SerializedName("internet_access") val internetAccess: Boolean?,
    @SerializedName("opening_hours") val openingHours: Boolean?,
    @SerializedName("toilets") val toilets: Boolean?,
    @SerializedName("air_conditioning") val airConditioning: Boolean?,
    @SerializedName("wheelchair_accessibility") val wheelchairAccessibility: Boolean?
)
```

Add response-level `schedule_date`, `timezone`, `work_window`, and optional `meta` DTOs. Remove the active `ScoreDetails`, `WifiQuality`, `NoiseLevel`, `CrowdDensity`, `OperationalHours`, and `Amenities` DTOs.

- [ ] **Step 5: Implement strict truthful mapping**

```kotlin
private fun Boolean?.toAvailability(): WfaFacilityAvailability = when (this) {
    true -> WfaFacilityAvailability.AVAILABLE
    false -> WfaFacilityAvailability.UNAVAILABLE
    null -> WfaFacilityAvailability.UNKNOWN
}

fun RecommendationItem.toDomainOrNull(): WfaRecommendation? {
    if (facilityConfidence !in 0..100) return null

    val mappedStatus = when (status.trim().lowercase()) {
        "ranked" -> if (finalScore != null && !finalLabel.isNullOrBlank()) {
            WfaRecommendationStatus.Ranked
        } else {
            WfaRecommendationStatus.Unsupported("ranked_missing_final_score")
        }
        "insufficient_facility_data" -> WfaRecommendationStatus.InsufficientFacilityData
        "facility_enrichment_failed" -> WfaRecommendationStatus.FacilityEnrichmentFailed
        else -> WfaRecommendationStatus.Unsupported(status)
    }
    val isRanked = mappedStatus is WfaRecommendationStatus.Ranked

    return WfaRecommendation(
        stableKey = placeId?.trim()?.takeIf(String::isNotEmpty)?.let { "place:$it" }
            ?: WfaRecommendation.stableKeyFor(name, latitude, longitude),
        placeId = placeId,
        name = name,
        address = address,
        coordinate = GeoCoordinate(latitude, longitude),
        placeType = placeType,
        distanceMeters = DistanceMeters(distanceMeters),
        status = mappedStatus,
        finalRank = finalRank.takeIf { isRanked },
        finalScore = finalScore.takeIf { isRanked },
        finalLabel = finalLabel.takeIf { isRanked },
        facilityScore = facilityScore,
        facilityConfidence = facilityConfidence,
        facilities = facilities.toDomain()
    )
}
```

- [ ] **Step 6: Remove unused legacy detail models after a usage check**

Run:

```bash
rg "WfaRecommendationDetail|ScoreItem|AmenityItem" app/src
```

Expected before deletion: definitions only. Delete them from `WfaModels.kt`; do not delete if an active consumer is found without first migrating that consumer in this task.

- [ ] **Step 7: Run mapper tests and confirm pass**

```bash
./gradlew :app:testDebugUnitTest --tests "*WfaMapperTest"
```

Expected: PASS.

- [ ] **Step 8: Commit the truthful contract**

```bash
git add app/src/main/java/com/example/infinite_track/domain/model/wfa \
  app/src/main/java/com/example/infinite_track/data/soucre/network/response/WfaRecommendationResponse.kt \
  app/src/main/java/com/example/infinite_track/data/mapper/wfa/WfaMapper.kt \
  app/src/test/java/com/example/infinite_track/data/mapper/wfa/WfaMapperTest.kt
git commit -m "refactor(INF-273): adopt truthful WFA recommendation contract"
```

---

### Task 3: Add `schedule_date` to Retrofit and typed repository failure mapping

**Files:**
- Modify: `app/src/main/java/com/example/infinite_track/data/soucre/network/retrofit/ApiService.kt`
- Modify: `app/src/main/java/com/example/infinite_track/domain/repository/WfaRepository.kt`
- Modify: `app/src/main/java/com/example/infinite_track/domain/use_case/wfa/GetWfaRecommendationsUseCase.kt`
- Modify: `app/src/main/java/com/example/infinite_track/data/repository/wfa/WfaRepositoryImpl.kt`
- Create: `app/src/main/java/com/example/infinite_track/data/mapper/wfa/WfaRecommendationFailureMapper.kt`
- Create: `app/src/test/java/com/example/infinite_track/data/mapper/wfa/WfaRecommendationFailureMapperTest.kt`
- Create: `app/src/test/java/com/example/infinite_track/data/repository/wfa/WfaRepositoryImplTest.kt`
- Create: `app/src/test/java/com/example/infinite_track/data/soucre/network/retrofit/WfaApiContractTest.kt`

**Interfaces:**
- Consumes: `WfaRecommendationQuery` and response mapper from Task 2.
- Produces: `WfaRecommendationResult` without leaking transport exceptions.
- Consumed later by: `WfaRequestViewModel`.

- [ ] **Step 1: Write the Retrofit annotation contract test**

Use reflection to assert the third query is named `schedule_date`:

```kotlin
@Test
fun `recommendation endpoint requires lat lng and schedule date`() {
    val method = ApiService::class.java.declaredMethods.single {
        it.name == "getWfaRecommendations"
    }
    val queryNames = method.parameterAnnotations
        .flatMap { annotations -> annotations.filterIsInstance<Query>() }
        .map(Query::value)

    assertEquals(listOf("lat", "lng", "schedule_date"), queryNames)
}
```

- [ ] **Step 2: Write failure-mapper tests**

```kotlin
@Test
fun `stable date and duplicate codes map to typed failures`() {
    assertEquals(
        WfaRecommendationFailure.InvalidScheduleDate,
        WfaRecommendationFailureMapper.mapHttp(400, "SAME_DAY_NOT_ALLOWED")
    )
    assertEquals(
        WfaRecommendationFailure.DuplicateBooking,
        WfaRecommendationFailureMapper.mapHttp(409, "DUPLICATE_BOOKING")
    )
}

@Test
fun `io failure maps to network unavailable`() {
    assertEquals(
        WfaRecommendationFailure.NetworkUnavailable,
        WfaRecommendationFailureMapper.mapThrowable(IOException("offline"))
    )
}
```

Cover provider/config codes, HTTP 5xx, and unknown failures.

- [ ] **Step 3: Write repository tests with a dynamic `ApiService` proxy**

Create a focused helper that only handles `getWfaRecommendations` and fails any other method:

```kotlin
private fun apiServiceReturning(response: WfaRecommendationResponse): ApiService =
    Proxy.newProxyInstance(
        ApiService::class.java.classLoader,
        arrayOf(ApiService::class.java)
    ) { _, method, _ ->
        when (method.name) {
            "getWfaRecommendations" -> response
            else -> error("Unexpected API call: ${method.name}")
        }
    } as ApiService
```

Assert:

```text
query date is ISO YYYY-MM-DD
success maps response schedule/timezone/recommendations
HTTP date rejection maps InvalidScheduleDate
network throwable maps NetworkUnavailable
```

- [ ] **Step 4: Run focused tests and confirm failure**

```bash
./gradlew :app:testDebugUnitTest \
  --tests "*WfaApiContractTest" \
  --tests "*WfaRecommendationFailureMapperTest" \
  --tests "*WfaRepositoryImplTest"
```

Expected: FAIL because the API signature and typed repository contract are not implemented.

- [ ] **Step 5: Change the Retrofit and Domain signatures**

```kotlin
@GET("api/wfa/recommendations")
suspend fun getWfaRecommendations(
    @Query("lat") latitude: Double,
    @Query("lng") longitude: Double,
    @Query("schedule_date") scheduleDate: String
): WfaRecommendationResponse
```

```kotlin
interface WfaRepository {
    suspend fun getRecommendations(
        query: WfaRecommendationQuery
    ): WfaRecommendationResult
}
```

```kotlin
class GetWfaRecommendationsUseCase @Inject constructor(
    private val repository: WfaRepository
) {
    suspend operator fun invoke(query: WfaRecommendationQuery): WfaRecommendationResult =
        repository.getRecommendations(query)
}
```

- [ ] **Step 6: Implement failure mapping and repository IO boundary**

Follow the existing `BookingRepositoryImpl` pattern: inject `Gson`, parse the shared WFA error envelope, execute on `Dispatchers.IO`, and map safe codes.

```kotlin
override suspend fun getRecommendations(
    query: WfaRecommendationQuery
): WfaRecommendationResult = withContext(Dispatchers.IO) {
    try {
        val response = apiService.getWfaRecommendations(
            latitude = query.origin.latitude,
            longitude = query.origin.longitude,
            scheduleDate = query.scheduleDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
        )
        response.toDomainResult()
    } catch (exception: HttpException) {
        WfaRecommendationResult.Failure(parseHttpFailure(exception))
    } catch (throwable: Throwable) {
        WfaRecommendationResult.Failure(
            WfaRecommendationFailureMapper.mapThrowable(throwable)
        )
    }
}
```

Do not map a provider failure to an empty list.

- [ ] **Step 7: Run focused tests and confirm pass**

```bash
./gradlew :app:testDebugUnitTest \
  --tests "*WfaApiContractTest" \
  --tests "*WfaRecommendationFailureMapperTest" \
  --tests "*WfaRepositoryImplTest"
```

Expected: PASS.

- [ ] **Step 8: Commit the data boundary**

```bash
git add app/src/main/java/com/example/infinite_track/data/soucre/network/retrofit/ApiService.kt \
  app/src/main/java/com/example/infinite_track/domain/repository/WfaRepository.kt \
  app/src/main/java/com/example/infinite_track/domain/use_case/wfa/GetWfaRecommendationsUseCase.kt \
  app/src/main/java/com/example/infinite_track/data/repository/wfa/WfaRepositoryImpl.kt \
  app/src/main/java/com/example/infinite_track/data/mapper/wfa/WfaRecommendationFailureMapper.kt \
  app/src/test/java/com/example/infinite_track/data
git commit -m "feat(INF-273): request date-aware WFA recommendations"
```

---

### Task 4: Remove coordinate route arguments and centralize the search-result contract

**Files:**
- Modify: `app/src/main/java/com/example/infinite_track/presentation/navigation/Screen.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/navigation/WfaRequestNavGraph.kt`
- Create: `app/src/main/java/com/example/infinite_track/presentation/navigation/LocationSearchResultContract.kt`
- Modify: `app/src/androidTest/java/com/example/infinite_track/presentation/navigation/WfaRequestNavigationTest.kt`
- Modify: `app/src/test/java/com/example/infinite_track/presentation/navigation/WfaShellNavigationContractTest.kt`

**Interfaces:**
- Produces: parent route `wfa_request` with no arguments.
- Produces: one typed saved-state key for `LocationResult`.
- Consumed later by: Attendance entry and WFA Request form search fallback.

- [ ] **Step 1: Update navigation tests first**

Replace all:

```kotlin
Screen.WfaRequestFlow.createRoute(-0.9, 119.8)
```

with:

```kotlin
Screen.WfaRequestFlow.route
```

Add assertions:

```kotlin
assertEquals("wfa_request", Screen.WfaRequestFlow.route)
assertFalse(Screen.WfaRequestFlow.route.contains("{"))
```

Add a result-consumption test that writes a `LocationResult` using `LocationSearchResultContract.RESULT_KEY`, returns to the form, and verifies the fake controller receives one `ManualLocationSelected` event.

- [ ] **Step 2: Run navigation tests and confirm failure**

```bash
./gradlew :app:testDebugUnitTest --tests "*WfaShellNavigationContractTest"
```

Expected: FAIL because the route still requires coordinates.

- [ ] **Step 3: Simplify the route and graph**

```kotlin
data object WfaRequestFlow : Screen("wfa_request")
```

Remove parent `navArgument("latitude")` and `navArgument("longitude")` declarations from `WfaRequestNavGraph`.

- [ ] **Step 4: Add the explicit location-search result contract**

```kotlin
object LocationSearchResultContract {
    const val RESULT_KEY = "selected_location"
}
```

In the form destination, observe the current entry's `SavedStateHandle`, convert the `LocationResult` to `WfaCandidateLocation`, dispatch `ManualLocationSelected`, and remove the value immediately after consumption.

```kotlin
LaunchedEffect(selectedLocation) {
    selectedLocation ?: return@LaunchedEffect
    viewModel.onEvent(
        WfaRequestEvent.ManualLocationSelected(selectedLocation.toWfaCandidateLocation())
    )
    entry.savedStateHandle.remove<LocationResult>(LocationSearchResultContract.RESULT_KEY)
}
```

- [ ] **Step 5: Run navigation tests and confirm pass**

```bash
./gradlew :app:testDebugUnitTest --tests "*WfaShellNavigationContractTest"
```

Run connected navigation tests when an emulator is available:

```bash
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.example.infinite_track.presentation.navigation.WfaRequestNavigationTest
```

- [ ] **Step 6: Commit the route migration**

```bash
git add app/src/main/java/com/example/infinite_track/presentation/navigation \
  app/src/androidTest/java/com/example/infinite_track/presentation/navigation/WfaRequestNavigationTest.kt \
  app/src/test/java/com/example/infinite_track/presentation/navigation/WfaShellNavigationContractTest.kt
git commit -m "refactor(INF-273): remove WFA request coordinate route args"
```

---

### Task 5: Move automatic recommendation orchestration into `WfaRequestViewModel`

**Files:**
- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/wfa_request/WfaRequestRecommendationState.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/wfa_request/WfaRequestUiState.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/wfa_request/WfaRequestEvent.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/wfa_request/WfaRequestViewModel.kt`
- Modify: `app/src/test/java/com/example/infinite_track/presentation/screen/attendance/wfa_request/WfaRequestViewModelTest.kt`

**Interfaces:**
- Consumes: date policy, current-location use case, and typed recommendation use case.
- Produces: request-owned automatic loading, selection, retry, cancellation, and stale-result protection.
- Keeps: `draft.location` as the reviewed/submitted location source of truth.

- [ ] **Step 1: Replace route-bootstrap tests with automatic-load tests**

Add tests with fixed `today = 2026-08-02`:

```kotlin
@Test
fun `bootstrap defaults to tomorrow and loads recommendations automatically`() = runTest {
    val recommendationRepository = FakeWfaRepository(successFor(LocalDate.of(2026, 8, 3)))
    val viewModel = createViewModel(
        recommendationRepository = recommendationRepository,
        datePolicy = FixedWfaDatePolicy(LocalDate.of(2026, 8, 2))
    )

    advanceUntilIdle()

    assertEquals(LocalDate.of(2026, 8, 3), viewModel.uiState.value.draft.scheduleDate)
    assertEquals(1, recommendationRepository.queries.size)
    assertEquals(
        LocalDate.of(2026, 8, 3),
        recommendationRepository.queries.single().scheduleDate
    )
    assertTrue(viewModel.uiState.value.recommendationState is WfaRequestRecommendationState.Content)
}
```

Add tests for:

```text
no SavedStateHandle coordinates required
date change clears draft.location and selectedKey
date change starts exactly one new query
old deferred response cannot overwrite new date
retry preserves reason/notes and reloads current date
current-location failure becomes CurrentLocationUnavailable recommendation failure
non-ranked candidate selection writes draft.location
invalid date does not call repository
repeated retry while same query is loading does not duplicate calls
```

Use two `CompletableDeferred<WfaRecommendationResult>` values to prove stale-response protection.

- [ ] **Step 2: Run ViewModel tests and confirm failure**

```bash
./gradlew :app:testDebugUnitTest --tests "*WfaRequestViewModelTest"
```

Expected: FAIL because the ViewModel still requires route coordinates and has no recommendation state.

- [ ] **Step 3: Add request-owned state and events**

```kotlin
sealed interface WfaRequestRecommendationState {
    data object Initializing : WfaRequestRecommendationState
    data object Loading : WfaRequestRecommendationState
    data object Empty : WfaRequestRecommendationState
    data class Content(
        val recommendations: List<WfaRecommendation>,
        val selectedKey: String? = null
    ) : WfaRequestRecommendationState
    data class Failure(
        val failure: WfaRecommendationFailure,
        val retryable: Boolean
    ) : WfaRequestRecommendationState
}
```

Extend state:

```kotlin
val minimumScheduleDate: LocalDate? = null,
val currentCoordinate: GeoCoordinate? = null,
val recommendationState: WfaRequestRecommendationState =
    WfaRequestRecommendationState.Initializing
```

Add events:

```kotlin
data class RecommendationSelected(val stableKey: String) : WfaRequestEvent
data class ManualLocationSelected(val location: WfaCandidateLocation) : WfaRequestEvent
data object RetryRecommendationsClicked : WfaRequestEvent
```

- [ ] **Step 4: Replace coordinate bootstrap with date/config/user bootstrap**

Inject:

```kotlin
private val getCurrentLocation: GetCurrentLocationUseCase,
private val getRecommendations: GetWfaRecommendationsUseCase,
private val datePolicy: WfaScheduleDatePolicy
```

Remove route latitude/longitude fields and initial reverse-geocode dependency.

Initialize:

```kotlin
private fun loadInitialData() {
    val minimumDate = datePolicy.minimumDate()
    _uiState.update {
        it.copy(
            minimumScheduleDate = minimumDate,
            draft = it.draft.copy(scheduleDate = minimumDate)
        )
    }
    viewModelScope.launch {
        loadEmployeeAndConfig()
        if (_uiState.value.phase == WfaRequestPhase.Editing) {
            loadRecommendations(force = true)
        }
    }
}
```

- [ ] **Step 5: Implement cancellation and stale guards**

Add:

```kotlin
private var recommendationJob: Job? = null
private var nextRecommendationRequestId = 0L
private var activeQuery: WfaRecommendationQuery? = null
```

Core flow:

```kotlin
private fun loadRecommendations(force: Boolean) {
    val date = _uiState.value.draft.scheduleDate ?: return
    if (!datePolicy.isSelectable(date)) return

    recommendationJob?.cancel()
    val requestId = ++nextRecommendationRequestId
    _uiState.update {
        it.copy(recommendationState = WfaRequestRecommendationState.Loading)
    }

    recommendationJob = viewModelScope.launch {
        val coordinate = when (val current = getCurrentLocation()) {
            is CurrentLocationResult.Success -> current.location.coordinate
            is CurrentLocationResult.Failure -> null
        }
        if (coordinate == null) {
            applyRecommendationFailure(
                requestId,
                date,
                WfaRecommendationFailure.CurrentLocationUnavailable
            )
            return@launch
        }

        val query = WfaRecommendationQuery(coordinate, date)
        if (!force && activeQuery == query) return@launch
        activeQuery = query
        when (val result = getRecommendations(query)) {
            is WfaRecommendationResult.Success -> applySuccess(requestId, date, coordinate, result)
            is WfaRecommendationResult.Failure -> applyFailure(requestId, date, coordinate, result.failure)
        }
    }
}

private fun isCurrentRecommendationRequest(requestId: Long, date: LocalDate): Boolean =
    requestId == nextRecommendationRequestId &&
        _uiState.value.draft.scheduleDate == date
```

Every result application must call `isCurrentRecommendationRequest` first.

- [ ] **Step 6: Implement event semantics**

For date change:

```kotlin
private fun onScheduleDateChanged(date: LocalDate?) {
    if (date == null || !datePolicy.isSelectable(date)) {
        _uiState.update {
            it.copy(
                fieldErrors = it.fieldErrors.copy(
                    scheduleDate = WfaRequestFieldError.FUTURE_DATE_REQUIRED
                )
            )
        }
        return
    }

    lastValidatedCommand = null
    activeQuery = null
    _uiState.update {
        it.copy(
            draft = it.draft.copy(scheduleDate = date, location = null),
            recommendationState = WfaRequestRecommendationState.Initializing,
            fieldErrors = it.fieldErrors.copy(scheduleDate = null, location = null)
        )
    }
    loadRecommendations(force = true)
}
```

For recommendation selection, copy only factual location fields into the draft. Do not copy score into booking commands.

- [ ] **Step 7: Run ViewModel tests and confirm pass**

```bash
./gradlew :app:testDebugUnitTest --tests "*WfaRequestViewModelTest"
```

Expected: PASS.

- [ ] **Step 8: Commit graph-owned recommendation state**

```bash
git add app/src/main/java/com/example/infinite_track/presentation/screen/attendance/wfa_request \
  app/src/test/java/com/example/infinite_track/presentation/screen/attendance/wfa_request/WfaRequestViewModelTest.kt
git commit -m "feat(INF-273): auto-load recommendations in WFA request"
```

---

### Task 6: Render truthful recommendation picker and provider-neutral map in the form

**Files:**
- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/wfa_request/WfaRequestMapUiMapper.kt`
- Create: `app/src/test/java/com/example/infinite_track/presentation/screen/attendance/wfa_request/WfaRequestMapUiMapperTest.kt`
- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/wfa_request/components/WfaRecommendationPicker.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/wfa_request/WfaRequestFormScreen.kt`
- Modify: `app/src/androidTest/java/com/example/infinite_track/presentation/screen/attendance/wfa_request/WfaRequestScreensTest.kt`
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: `WfaRequestRecommendationState`, `currentCoordinate`, and `draft.location`.
- Produces: interactive recommendation map/cards and honest nullable-score presentation.
- Emits only typed `WfaRequestEvent` values and semantic `onSearchLocation` callback.

- [ ] **Step 1: Write pure map-mapper tests**

```kotlin
@Test
fun `mapper creates current and recommendation markers without geofence circles`() {
    val state = WfaRequestMapUiMapper.map(
        currentCoordinate = GeoCoordinate(-0.9, 119.87),
        recommendationState = contentState,
        hasPreciseLocationPermission = true
    )

    assertEquals(1, state.markers.count { it.role == MapMarkerRole.CURRENT_LOCATION })
    assertEquals(2, state.markers.count { it.role == MapMarkerRole.WFA_RECOMMENDATION })
    assertTrue(state.circles.isEmpty())
}

@Test
fun `selected recommendation marker is highlighted`() {
    val marker = WfaRequestMapUiMapper.map(...)
        .markers.single { it.role == MapMarkerRole.WFA_RECOMMENDATION && it.isSelected }
    assertEquals("wfa:place:one", marker.id)
}
```

- [ ] **Step 2: Add failing Compose tests for state rendering**

Cover:

```text
loading state
empty state
retryable failure
ranked card displays final score
insufficient-data card displays explicit unavailable copy and no 0
facility-enrichment-failed card displays provider failure copy and no 0
selecting a card dispatches RecommendationSelected
minimum date is passed to DatePickerButton
selected location card is absent before selection and appears after selection
search fallback invokes semantic callback
```

Use stable test tags:

```text
wfaRecommendationLoading
wfaRecommendationEmpty
wfaRecommendationFailure
wfaRecommendationMap
wfaRecommendationCard-<stableKey>
wfaRecommendationScore-<stableKey>
wfaSelectedLocationCard
wfaSearchFallback
```

- [ ] **Step 3: Run focused tests and confirm failure**

```bash
./gradlew :app:testDebugUnitTest --tests "*WfaRequestMapUiMapperTest"
```

Expected: FAIL because mapper/component do not exist.

- [ ] **Step 4: Implement the focused map mapper**

Map current location and recommendation markers only. Marker IDs must be deterministic:

```kotlin
fun recommendationMarkerId(recommendation: WfaRecommendation): String =
    "wfa:${recommendation.stableKey}"
```

Do not set an approved-target role or geofence radius.

- [ ] **Step 5: Implement `WfaRecommendationPicker`**

Required component signature:

```kotlin
@Composable
fun WfaRecommendationPicker(
    state: WfaRequestRecommendationState,
    currentCoordinate: GeoCoordinate?,
    hasPreciseLocationPermission: Boolean,
    onRecommendationSelected: (String) -> Unit,
    onRetry: () -> Unit,
    onSearchLocation: () -> Unit,
    modifier: Modifier = Modifier
)
```

For score display:

```kotlin
when (recommendation.status) {
    WfaRecommendationStatus.Ranked -> FinalScoreContent(
        score = requireNotNull(recommendation.finalScore),
        label = requireNotNull(recommendation.finalLabel)
    )
    WfaRecommendationStatus.InsufficientFacilityData ->
        StatusText(stringResource(R.string.wfa_recommendation_insufficient_data))
    WfaRecommendationStatus.FacilityEnrichmentFailed ->
        StatusText(stringResource(R.string.wfa_recommendation_enrichment_failed))
    is WfaRecommendationStatus.Unsupported ->
        StatusText(stringResource(R.string.wfa_recommendation_score_unavailable))
}
```

Never call `(finalScore ?: 0.0)` for display.

- [ ] **Step 6: Refactor the form without changing submission semantics**

Change `FormContent` so it no longer returns when location is null.

Order items:

```text
schedule date
recommendation picker
selected location card when non-null
employee card
reason/notes
checklist
review button
```

Pass:

```kotlin
DatePickerButton(
    selectedDate = uiState.draft.scheduleDate,
    minimumDate = uiState.minimumScheduleDate,
    onDateSelected = { onEvent(WfaRequestEvent.ScheduleDateChanged(it)) },
    ...
)
```

Add callbacks to `WfaRequestFormScreen`:

```kotlin
onSearchLocation: () -> Unit
hasPreciseLocationPermission: Boolean
```

The navigation host, not the screen, performs navigation.

- [ ] **Step 7: Run focused unit and connected tests**

```bash
./gradlew :app:testDebugUnitTest --tests "*WfaRequestMapUiMapperTest"
```

When an emulator is available:

```bash
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.example.infinite_track.presentation.screen.attendance.wfa_request.WfaRequestScreensTest
```

- [ ] **Step 8: Commit the recommendation UI**

```bash
git add app/src/main/java/com/example/infinite_track/presentation/screen/attendance/wfa_request \
  app/src/test/java/com/example/infinite_track/presentation/screen/attendance/wfa_request/WfaRequestMapUiMapperTest.kt \
  app/src/androidTest/java/com/example/infinite_track/presentation/screen/attendance/wfa_request/WfaRequestScreensTest.kt \
  app/src/main/res/values/strings.xml
git commit -m "feat(INF-273): render truthful WFA recommendations"
```

---

### Task 7: Remove recommendation ownership from Attendance and auto-open new requests

**Files:**
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceViewModel.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceScreen.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/preparation/AttendancePreparationState.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/preparation/AttendancePreparationUiMapper.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/map/mapper/AttendanceMapUiMapper.kt`
- Modify/delete after usage check: `AttendancePreparationReducer.kt`, `WfaMapSelectionEffect.kt`, and request-only map-pick helpers.
- Modify focused tests under:
  - `app/src/test/java/com/example/infinite_track/presentation/screen/attendance/`
  - `app/src/test/java/com/example/infinite_track/presentation/screen/attendance/preparation/`
  - `app/src/test/java/com/example/infinite_track/presentation/map/mapper/`

**Interfaces:**
- Consumes: simplified `Screen.WfaRequestFlow.route`.
- Produces: Attendance that resolves backend truth and navigates only for `WFA_NOT_REQUESTED`.
- Removes: request recommendation/search/map-pick state from Attendance.

- [ ] **Step 1: Add failing Attendance behavior tests**

Test the state matrix:

```text
approved today → no WFA request navigation, authoritative target remains
pending → OPEN_WFA_REQUESTS recovery
rejected → OPEN_WFA_REQUESTS recovery
approval missing for today → OPEN_WFA_REQUESTS recovery
not requested → one NavigationTarget.WfaRequest
repeated WFA selection while current resolution is active → no duplicate navigation
```

Add a constructor/behavior test proving Attendance no longer calls or requires `GetWfaRecommendationsUseCase`.

- [ ] **Step 2: Run focused tests and confirm failure**

Run the exact current test classes found by:

```bash
rg -l "AttendanceViewModel|WFA_NOT_REQUESTED|WfaDiscoveryState" app/src/test
```

Then execute them with `:app:testDebugUnitTest --tests` filters. Expected: FAIL until Attendance ownership is removed.

- [ ] **Step 3: Remove recommendation jobs and use-case injection**

Delete from `AttendanceViewModel`:

```text
GetWfaRecommendationsUseCase constructor dependency
recommendationJob
fetchWfaRecommendations
auto-fit recommendation camera logic
recommendation retry
recommendation marker selection
selected recommendation lookup in onBookingClicked
```

Rename the navigation target to reflect the transaction:

```kotlin
sealed class NavigationTarget {
    data class WfaRequest(val route: String) : NavigationTarget()
    // existing FaceScanner and LocationSearch only if still used elsewhere
}
```

- [ ] **Step 4: Emit automatic navigation only for not-requested resolution**

After applying the latest WFA resolution, add:

```kotlin
private fun maybeOpenWfaRequest(
    request: SelectionRequestToken,
    resolution: TargetLocationResolution
) {
    val unavailable = resolution as? TargetLocationResolution.Unavailable ?: return
    if (unavailable.reason != TargetUnavailableReason.WFA_NOT_REQUESTED) return
    if (!latestSelectionGuard.isCurrent(request, WorkMode.WFA)) return
    if (_uiState.value.navigationTarget != null) return

    _uiState.value = _uiState.value.copy(
        navigationTarget = NavigationTarget.WfaRequest(Screen.WfaRequestFlow.route)
    )
}
```

Call it only after the backend-authoritative resolution is applied.

- [ ] **Step 5: Remove request-discovery state from Attendance presentation**

Remove from `AttendancePreparationState`:

```text
wfaDiscovery
mapPickInteraction
```

Remove from `AttendancePreparationUiMapper`:

```text
retry discovery primary action
recommendation rows
selected recommendation requirement for OPEN_WFA_BOOKING
secondary search action for new request
```

Remove from `AttendanceMapUiMapper`:

```text
WFA_RECOMMENDATION markers
SEARCH_PREVIEW markers for request creation
```

Keep authoritative target and current-location markers unchanged.

- [ ] **Step 6: Remove request-only UI handlers from `AttendanceScreen`**

Delete:

```text
selected_location consumption for WFA request creation
SearchWfaLocation action
PickWfaLocationOnMap action
recommendation marker click branch
recommendation loading overlay
center crosshair for request map pick
```

Update navigation handling:

```kotlin
is NavigationTarget.WfaRequest -> navController.navigate(target.route)
```

- [ ] **Step 7: Delete dead helpers only after proving no usage**

Run:

```bash
rg "WfaDiscoveryState|WfaMapPickInteractionState|WfaMapSelectionEffect|selectRecommendation" app/src
```

Delete a helper only when every active consumer has been migrated. Do not remove shared provider-neutral map models or approved-target behavior.

- [ ] **Step 8: Run focused Attendance tests**

```bash
./gradlew :app:testDebugUnitTest \
  --tests "*EvaluateAttendancePreparationUseCaseTest" \
  --tests "*AttendancePreparationUiMapperTest" \
  --tests "*AttendanceMapUiMapperTest" \
  --tests "*AttendanceViewModel*Test"
```

Expected: PASS.

- [ ] **Step 9: Commit Attendance cleanup**

```bash
git add app/src/main/java/com/example/infinite_track/presentation/screen/attendance \
  app/src/main/java/com/example/infinite_track/presentation/map/mapper/AttendanceMapUiMapper.kt \
  app/src/test/java/com/example/infinite_track/presentation/screen/attendance \
  app/src/test/java/com/example/infinite_track/presentation/map/mapper
git commit -m "refactor(INF-273): move WFA discovery out of attendance"
```

---

### Task 8: Complete graph integration, error copy, and regression coverage

**Files:**
- Modify: `app/src/main/java/com/example/infinite_track/presentation/navigation/WfaRequestNavGraph.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/wfa_request/WfaRequestUiMapper.kt`
- Modify: `app/src/androidTest/java/com/example/infinite_track/presentation/navigation/WfaRequestNavigationTest.kt`
- Modify: `app/src/androidTest/java/com/example/infinite_track/presentation/screen/attendance/wfa_request/WfaRequestScreensTest.kt`
- Modify: relevant unit tests and `app/src/main/res/values/strings.xml`

**Interfaces:**
- Wires recommendation marker/card/search events to the graph-scoped controller.
- Preserves review/result navigation and typed failure copy.

- [ ] **Step 1: Add end-to-end graph tests before wiring**

Cover:

```text
navigate to wfa_request without args
form starts with tomorrow
automatic loading/content is rendered
select recommendation → selected location card
change date → selected location disappears and loading returns
search fallback result is consumed once
review → back → form preserves date/reason/notes/location
submit → result remains unchanged
```

- [ ] **Step 2: Add typed error-copy tests**

Map:

```text
CurrentLocationUnavailable
InvalidScheduleDate
DuplicateBooking
NetworkUnavailable
ProviderUnavailable
ServerUnavailable
Unknown
```

Assert recommendation failures do not use the config-failure title/action.

- [ ] **Step 3: Run tests and confirm failure**

```bash
./gradlew :app:testDebugUnitTest --tests "*WfaRequest*Test"
```

Connected tests when available:

```bash
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.package=com.example.infinite_track.presentation
```

- [ ] **Step 4: Wire graph callbacks and permission state**

The graph host supplies:

```kotlin
onSearchLocation = { navController.navigate(Screen.LocationSearch.route) }
hasPreciseLocationPermission = context.hasPreciseLocationPermission()
```

Do not put navigation in `WfaRequestViewModel` or screen composables.

- [ ] **Step 5: Add safe localized copy and accessibility descriptions**

Add strings for:

```text
recommendation loading/empty/retry
current location unavailable
duplicate selected date
provider unavailable
insufficient facility data
enrichment failed
facility confidence
search another location
future date required
recommendation map content description
```

Use string resources in production Compose code; no hardcoded user-facing copy.

- [ ] **Step 6: Run focused tests and confirm pass**

```bash
./gradlew :app:testDebugUnitTest --tests "*WfaRequest*Test"
```

Run connected tests when available as above.

- [ ] **Step 7: Commit graph integration**

```bash
git add app/src/main/java/com/example/infinite_track/presentation/navigation/WfaRequestNavGraph.kt \
  app/src/main/java/com/example/infinite_track/presentation/screen/attendance/wfa_request \
  app/src/androidTest/java/com/example/infinite_track/presentation \
  app/src/test/java/com/example/infinite_track/presentation \
  app/src/main/res/values/strings.xml
git commit -m "test(INF-273): verify date-aware WFA request flow"
```

---

### Task 9: Run full verification and record evidence

**Files:**
- Modify only if verification reveals an issue in files already owned by INF-273.
- Do not add generated reports, APKs, local properties, or secrets to git.

- [ ] **Step 1: Verify no old recommendation contract remains**

```bash
rg "suitability_score|suitabilityLabel|score_details|wifi_quality|noise_level|crowd_density|amenities" \
  app/src/main/java/com/example/infinite_track/{data,domain,presentation}
```

Expected: no active WFA recommendation consumer uses the legacy fields. Booking-history `suitability_score` may remain because it is a separate persisted booking contract.

- [ ] **Step 2: Verify Attendance no longer owns request discovery**

```bash
rg "GetWfaRecommendationsUseCase|fetchWfaRecommendations|WfaDiscoveryState" \
  app/src/main/java/com/example/infinite_track/presentation/screen/attendance
```

Expected: recommendation ownership exists only under `wfa_request`; approved-target code remains.

- [ ] **Step 3: Run all JVM unit tests**

```bash
./gradlew :app:testDebugUnitTest
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Build the debug APK**

```bash
./gradlew :app:assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Run lint**

```bash
./gradlew :app:lintDebug
```

Expected: BUILD SUCCESSFUL, or every pre-existing non-blocking issue is documented separately with evidence that INF-273 introduced no new blocker.

- [ ] **Step 6: Run connected tests when an emulator/device is available**

```bash
./gradlew :app:connectedDebugAndroidTest
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Perform the runtime matrix**

Verify manually:

```text
approved WFA today stays in Attendance
pending/rejected routes to request history recovery
not requested opens WFA Request automatically
default date is tomorrow
same-day/past cannot be selected
later future date reloads recommendations
old selection clears on date change
stale result cannot replace current date
ranked candidate shows score
insufficient/enrichment-failed candidate never shows 0
search fallback remains secondary
review and submit preserve the selected date/location
backend duplicate-date rejection is typed
```

- [ ] **Step 8: Review the final diff against the spec**

```bash
git diff --stat develop...HEAD
git diff --check develop...HEAD
git log --oneline develop..HEAD
```

Confirm:

```text
no backend files
no generated build artifacts
no sensitive logs
no duplicate route registration
no DTO/entity leakage into Domain
no recommendation ownership left in Attendance
```

- [ ] **Step 9: Commit any final bounded verification fix**

Only when a real issue was found:

```bash
git add <owned INF-273 files>
git commit -m "fix(INF-273): address final verification findings"
```

Do not create an empty verification commit.
