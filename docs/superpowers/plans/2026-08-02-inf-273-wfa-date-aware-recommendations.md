# INF-273 Date-Aware WFA Recommendations Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move date-aware WFA recommendation discovery into the existing graph-scoped WFA Request transaction, default the request date to tomorrow in Jakarta, and automatically reload truthful INF-272 recommendations whenever the date changes.

**Architecture:** Attendance keeps ownership of today's backend-authoritative WFA state and approved attendance target. `WfaRequestViewModel` becomes the single owner of future date, current-coordinate snapshot, recommendation lifecycle, selected candidate, request draft, review, and submission. The implementation preserves `Screen → ViewModel → UseCase → Repository → RepositoryImpl → ApiService`.

**Tech Stack:** Kotlin, Android SDK 34 / minSdk 26, Jetpack Compose, Material 3, Navigation Compose, Kotlin Coroutines/StateFlow, Hilt, Retrofit/Gson, JUnit4, Compose UI Test.

## Global Constraints

- Use branch `feature/inf-273-wfa-date-aware-recommendations` from an isolated worktree.
- Integration target is `develop`.
- Backend dependency is INF-272; this plan changes Android only.
- Calendar policy is `Asia/Jakarta`: today and past are invalid, tomorrow is the default minimum.
- Same-day booking remains unsupported.
- Approved WFA booking remains the only authoritative WFA attendance target.
- `AttendanceViewModel` must not own new-request recommendations after migration.
- One graph-scoped `WfaRequestViewModel` owns form, review, result, and recommendation state.
- Domain must not depend on DTO, Retrofit, Compose, `Context`, or `NavController`.
- Missing final score is `null`, never `0`.
- Missing facility evidence is `UNKNOWN`, never inferred false.
- Do not restore wifi/noise/crowd/generic-amenity score details.
- Reuse the provider-neutral map adapter; do not rewrite the map stack.
- Preserve INF-265 form → review → submit → result semantics.
- Each task starts with a failing focused test and ends with a bounded commit.

## File Map

### New production files

- `app/src/main/java/com/example/infinite_track/domain/validation/WfaScheduleDatePolicy.kt`
- `app/src/main/java/com/example/infinite_track/domain/model/wfa/WfaRecommendationContract.kt`
- `app/src/main/java/com/example/infinite_track/data/mapper/wfa/WfaRecommendationFailureMapper.kt`
- `app/src/main/java/com/example/infinite_track/presentation/navigation/LocationSearchResultContract.kt`
- `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/wfa_request/WfaRequestRecommendationState.kt`
- `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/wfa_request/WfaRequestMapUiMapper.kt`
- `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/wfa_request/components/WfaRecommendationPicker.kt`

### Main modified production files

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
- `presentation/screen/attendance/search/LocationSearchScreen.kt`
- `presentation/screen/attendance/wfa_request/WfaRequestEvent.kt`
- `presentation/screen/attendance/wfa_request/WfaRequestUiState.kt`
- `presentation/screen/attendance/wfa_request/WfaRequestViewModel.kt`
- `presentation/screen/attendance/wfa_request/WfaRequestFormScreen.kt`
- `presentation/screen/attendance/AttendanceViewModel.kt`
- `presentation/screen/attendance/AttendanceScreen.kt`
- `presentation/screen/attendance/preparation/AttendancePreparationState.kt`
- `presentation/screen/attendance/preparation/AttendancePreparationUiModel.kt`
- `presentation/screen/attendance/preparation/AttendancePreparationUiMapper.kt`
- `presentation/components/button/attendance/AttendanceBottomSheetContent.kt`
- `presentation/map/mapper/AttendanceMapUiMapper.kt`
- `app/src/main/res/values/strings.xml`

---

### Task 1: Add the Jakarta date policy and Domain validation

**Files:**
- Create: `app/src/main/java/com/example/infinite_track/domain/validation/WfaScheduleDatePolicy.kt`
- Create: `app/src/test/java/com/example/infinite_track/domain/validation/WfaScheduleDatePolicyTest.kt`
- Modify: `app/src/main/java/com/example/infinite_track/domain/model/booking/WfaRequestDraft.kt`
- Modify: `app/src/main/java/com/example/infinite_track/domain/use_case/booking/ValidateWfaRequestDraftUseCase.kt`
- Modify: `app/src/test/java/com/example/infinite_track/domain/use_case/booking/ValidateWfaRequestDraftUseCaseTest.kt`

**Interfaces:**
- Produces `WfaScheduleDatePolicy.today()`, `minimumDate(today)`, and `isSelectable(date, today)`.
- Produces `WfaRequestFieldError.FUTURE_DATE_REQUIRED`.
- Consumed by Task 5 and Task 6.

- [ ] **Step 1: Write failing policy tests**

```kotlin
class WfaScheduleDatePolicyTest {
    private val today = LocalDate.of(2026, 8, 2)

    @Test
    fun `tomorrow is the minimum date`() {
        val policy = WfaScheduleDatePolicy.fixed(
            Clock.fixed(Instant.parse("2026-08-02T02:00:00Z"), ZoneOffset.UTC)
        )

        assertEquals(today, policy.today())
        assertEquals(today.plusDays(1), policy.minimumDate())
    }

    @Test
    fun `today and past are invalid while future is valid`() {
        val policy = WfaScheduleDatePolicy.fixed(
            Clock.fixed(Instant.parse("2026-08-02T02:00:00Z"), ZoneOffset.UTC)
        )

        assertFalse(policy.isSelectable(today.minusDays(1)))
        assertFalse(policy.isSelectable(today))
        assertTrue(policy.isSelectable(today.plusDays(1)))
        assertTrue(policy.isSelectable(today.plusDays(30)))
    }
}
```

- [ ] **Step 2: Add failing validator coverage**

```kotlin
@Test
fun `same day and past dates return FUTURE_DATE_REQUIRED`() {
    val today = LocalDate.of(2026, 8, 2)
    val policy = WfaScheduleDatePolicy.fixed(
        Clock.fixed(Instant.parse("2026-08-02T02:00:00Z"), ZoneOffset.UTC)
    )
    val useCase = ValidateWfaRequestDraftUseCase(policy)

    listOf(today.minusDays(1), today).forEach { date ->
        val result = useCase(validDraft.copy(scheduleDate = date), config)
        val errors = (result as WfaRequestValidationResult.Invalid).errors
        assertEquals(WfaRequestFieldError.FUTURE_DATE_REQUIRED, errors.scheduleDate)
    }
}
```

- [ ] **Step 3: Run the focused tests and verify failure**

```bash
./gradlew :app:testDebugUnitTest \
  --tests "*WfaScheduleDatePolicyTest" \
  --tests "*ValidateWfaRequestDraftUseCaseTest"
```

Expected: compilation/test failure because the policy, factory, and field error do not exist.

- [ ] **Step 4: Implement the testable date policy**

```kotlin
package com.example.infinite_track.domain.validation

import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class WfaScheduleDatePolicy private constructor(
    private val clock: Clock
) {
    @Inject
    constructor() : this(Clock.system(JAKARTA_ZONE))

    fun today(): LocalDate = LocalDate.now(clock.withZone(JAKARTA_ZONE))

    fun minimumDate(today: LocalDate = today()): LocalDate = today.plusDays(1)

    fun isSelectable(
        date: LocalDate,
        today: LocalDate = today()
    ): Boolean = date.isAfter(today)

    companion object {
        val JAKARTA_ZONE: ZoneId = ZoneId.of("Asia/Jakarta")

        fun fixed(clock: Clock): WfaScheduleDatePolicy =
            WfaScheduleDatePolicy(clock)
    }
}
```

- [ ] **Step 5: Extend validation**

Add `FUTURE_DATE_REQUIRED` to `WfaRequestFieldError`, inject the policy, and replace the schedule branch with:

```kotlin
scheduleDate = when {
    draft.scheduleDate == null -> WfaRequestFieldError.REQUIRED
    !datePolicy.isSelectable(draft.scheduleDate) ->
        WfaRequestFieldError.FUTURE_DATE_REQUIRED
    else -> null
}
```

Keep reason, Other, notes, and location validation unchanged.

- [ ] **Step 6: Run tests and verify pass**

```bash
./gradlew :app:testDebugUnitTest \
  --tests "*WfaScheduleDatePolicyTest" \
  --tests "*ValidateWfaRequestDraftUseCaseTest"
```

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/example/infinite_track/domain/validation/WfaScheduleDatePolicy.kt \
  app/src/main/java/com/example/infinite_track/domain/model/booking/WfaRequestDraft.kt \
  app/src/main/java/com/example/infinite_track/domain/use_case/booking/ValidateWfaRequestDraftUseCase.kt \
  app/src/test/java/com/example/infinite_track/domain/validation/WfaScheduleDatePolicyTest.kt \
  app/src/test/java/com/example/infinite_track/domain/use_case/booking/ValidateWfaRequestDraftUseCaseTest.kt
git commit -m "feat(INF-273): enforce future WFA schedule dates"
```

---

### Task 2: Replace the legacy recommendation DTO and Domain model

**Files:**
- Create: `app/src/main/java/com/example/infinite_track/domain/model/wfa/WfaRecommendationContract.kt`
- Modify: `app/src/main/java/com/example/infinite_track/domain/model/wfa/WfaModels.kt`
- Modify: `app/src/main/java/com/example/infinite_track/data/soucre/network/response/WfaRecommendationResponse.kt`
- Modify: `app/src/main/java/com/example/infinite_track/data/mapper/wfa/WfaMapper.kt`
- Modify: `app/src/test/java/com/example/infinite_track/data/mapper/wfa/WfaMapperTest.kt`

**Interfaces:**
- Produces `WfaRecommendationQuery`, `WfaRecommendationResult`, `WfaRecommendationFailure`, `WfaRecommendationStatus`, `WfaFacilityAvailability`, `WfaFacilityEvidence`, and `WfaRecommendationMeta`.
- Produces a truthful `WfaRecommendation` with nullable final scoring.
- Consumed by Task 3, Task 5, and Task 6.

- [ ] **Step 1: Replace mapper tests with INF-272 fixtures**

```kotlin
@Test
fun `ranked candidate maps score and tri-state facilities`() {
    val result = rankedDto().toDomainOrNull()

    requireNotNull(result)
    assertEquals(WfaRecommendationStatus.Ranked, result.status)
    assertEquals(82.4, result.finalScore!!, 0.0)
    assertEquals("Sangat Tinggi", result.finalLabel)
    assertEquals(WfaFacilityAvailability.AVAILABLE, result.facilities.internetAccess)
    assertEquals(WfaFacilityAvailability.UNKNOWN, result.facilities.wheelchairAccessibility)
}

@Test
fun `insufficient candidate preserves null final score`() {
    val result = rankedDto().copy(
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
    val result = rankedDto(placeId = "place-123").toDomainOrNull()
    assertEquals("place:place-123", requireNotNull(result).stableKey)
}
```

Add cases for enrichment failure, true/false/null facility mapping, unknown status, confidence outside `0..100`, and ranked payload missing score/label.

- [ ] **Step 2: Run mapper tests and verify failure**

```bash
./gradlew :app:testDebugUnitTest --tests "*WfaMapperTest"
```

Expected: compilation failure because the new contract does not exist.

- [ ] **Step 3: Add the Domain contract**

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

Define `WfaRecommendationResult.Success` with `scheduleDate`, `timezone`, `recommendations`, and optional meta.

- [ ] **Step 4: Replace the active DTO**

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
```

Add response DTOs for `schedule_date`, `timezone`, `work_window`, and `meta`. Remove active `ScoreDetails`, wifi, noise, crowd, operational-hours-score, and generic amenities DTOs.

- [ ] **Step 5: Implement strict mapping**

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
    val ranked = mappedStatus is WfaRecommendationStatus.Ranked

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
        finalRank = finalRank.takeIf { ranked },
        finalScore = finalScore.takeIf { ranked },
        finalLabel = finalLabel.takeIf { ranked },
        facilityScore = facilityScore,
        facilityConfidence = facilityConfidence,
        facilities = facilities.toDomain()
    )
}
```

- [ ] **Step 6: Remove unused legacy detail models**

```bash
rg "WfaRecommendationDetail|ScoreItem|AmenityItem" app/src
```

Expected before deletion: definitions only. Delete only after the usage search confirms no active consumer.

- [ ] **Step 7: Run mapper tests and verify pass**

```bash
./gradlew :app:testDebugUnitTest --tests "*WfaMapperTest"
```

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/example/infinite_track/domain/model/wfa \
  app/src/main/java/com/example/infinite_track/data/soucre/network/response/WfaRecommendationResponse.kt \
  app/src/main/java/com/example/infinite_track/data/mapper/wfa/WfaMapper.kt \
  app/src/test/java/com/example/infinite_track/data/mapper/wfa/WfaMapperTest.kt
git commit -m "refactor(INF-273): adopt truthful WFA recommendation contract"
```

---

### Task 3: Add `schedule_date` and typed repository failures

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
- Consumes Task 2 query/result/DTO mapping.
- Produces `WfaRecommendationResult` without leaking transport exceptions.
- Consumed by Task 5.

- [ ] **Step 1: Write the Retrofit query contract test**

```kotlin
@Test
fun `recommendation endpoint declares lat lng and schedule date queries`() {
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
fun `date and duplicate codes map to typed failures`() {
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
fun `io exception maps to network unavailable`() {
    assertEquals(
        WfaRecommendationFailure.NetworkUnavailable,
        WfaRecommendationFailureMapper.mapThrowable(IOException("offline"))
    )
}
```

Also cover provider/config codes, HTTP 5xx, and unknown failures.

- [ ] **Step 3: Write repository tests**

Use a dynamic `ApiService` proxy that handles only `getWfaRecommendations` and errors on any unexpected call. Assert ISO date forwarding, response mapping, HTTP rejection mapping, and IO mapping.

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

- [ ] **Step 4: Run focused tests and verify failure**

```bash
./gradlew :app:testDebugUnitTest \
  --tests "*WfaApiContractTest" \
  --tests "*WfaRecommendationFailureMapperTest" \
  --tests "*WfaRepositoryImplTest"
```

- [ ] **Step 5: Change API and Domain signatures**

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

- [ ] **Step 6: Implement the repository boundary**

Follow `BookingRepositoryImpl`: inject `Gson`, use `Dispatchers.IO`, parse the existing WFA error envelope, and map safe codes.

```kotlin
override suspend fun getRecommendations(
    query: WfaRecommendationQuery
): WfaRecommendationResult = withContext(Dispatchers.IO) {
    try {
        apiService.getWfaRecommendations(
            latitude = query.origin.latitude,
            longitude = query.origin.longitude,
            scheduleDate = query.scheduleDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
        ).toDomainResult()
    } catch (exception: HttpException) {
        WfaRecommendationResult.Failure(parseHttpFailure(exception))
    } catch (throwable: Throwable) {
        WfaRecommendationResult.Failure(
            WfaRecommendationFailureMapper.mapThrowable(throwable)
        )
    }
}
```

Provider failure must not become an empty recommendation list.

- [ ] **Step 7: Run focused tests and verify pass**

```bash
./gradlew :app:testDebugUnitTest \
  --tests "*WfaApiContractTest" \
  --tests "*WfaRecommendationFailureMapperTest" \
  --tests "*WfaRepositoryImplTest"
```

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/example/infinite_track/data/soucre/network/retrofit/ApiService.kt \
  app/src/main/java/com/example/infinite_track/domain/repository/WfaRepository.kt \
  app/src/main/java/com/example/infinite_track/domain/use_case/wfa/GetWfaRecommendationsUseCase.kt \
  app/src/main/java/com/example/infinite_track/data/repository/wfa/WfaRepositoryImpl.kt \
  app/src/main/java/com/example/infinite_track/data/mapper/wfa/WfaRecommendationFailureMapper.kt \
  app/src/test/java/com/example/infinite_track/data/mapper/wfa/WfaRecommendationFailureMapperTest.kt \
  app/src/test/java/com/example/infinite_track/data/repository/wfa/WfaRepositoryImplTest.kt \
  app/src/test/java/com/example/infinite_track/data/soucre/network/retrofit/WfaApiContractTest.kt
git commit -m "feat(INF-273): request date-aware WFA recommendations"
```

---

### Task 4: Simplify the parent route and centralize the location-search key

**Files:**
- Modify: `app/src/main/java/com/example/infinite_track/presentation/navigation/Screen.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/navigation/WfaRequestNavGraph.kt`
- Create: `app/src/main/java/com/example/infinite_track/presentation/navigation/LocationSearchResultContract.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/search/LocationSearchScreen.kt`
- Modify: `app/src/androidTest/java/com/example/infinite_track/presentation/navigation/WfaRequestNavigationTest.kt`
- Modify: `app/src/test/java/com/example/infinite_track/presentation/navigation/WfaShellNavigationContractTest.kt`

**Interfaces:**
- Produces parent route `wfa_request` without arguments.
- Produces one result key used by search writer and later graph consumer.
- Does not consume the search result yet; Task 7 wires it after the event exists.

- [ ] **Step 1: Update route tests first**

```kotlin
assertEquals("wfa_request", Screen.WfaRequestFlow.route)
assertFalse(Screen.WfaRequestFlow.route.contains("{"))
```

Replace navigation calls using `createRoute(latitude, longitude)` with `Screen.WfaRequestFlow.route`.

- [ ] **Step 2: Run route tests and verify failure**

```bash
./gradlew :app:testDebugUnitTest --tests "*WfaShellNavigationContractTest"
```

- [ ] **Step 3: Simplify `Screen` and the parent graph**

```kotlin
data object WfaRequestFlow : Screen("wfa_request")
```

Remove parent latitude/longitude `navArgument` declarations. Keep child routes unchanged.

- [ ] **Step 4: Centralize the search result key**

```kotlin
object LocationSearchResultContract {
    const val RESULT_KEY = "selected_location"
}
```

Replace the hardcoded writer key in `LocationSearchScreen` with this constant. Do not add graph consumption in this task.

- [ ] **Step 5: Run route tests and verify pass**

```bash
./gradlew :app:testDebugUnitTest --tests "*WfaShellNavigationContractTest"
```

Run `WfaRequestNavigationTest` on an emulator when available.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/example/infinite_track/presentation/navigation/Screen.kt \
  app/src/main/java/com/example/infinite_track/presentation/navigation/WfaRequestNavGraph.kt \
  app/src/main/java/com/example/infinite_track/presentation/navigation/LocationSearchResultContract.kt \
  app/src/main/java/com/example/infinite_track/presentation/screen/attendance/search/LocationSearchScreen.kt \
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
- Consumes Task 1 date policy, `GetCurrentLocationUseCase`, and Task 3 recommendation use case.
- Produces request-owned automatic loading, selection, retry, cancellation, and stale-result protection.
- Keeps `draft.location` as the reviewed/submitted source of truth.

- [ ] **Step 1: Replace coordinate-bootstrap tests**

Create the ViewModel with an empty `SavedStateHandle`. Use a fixed policy:

```kotlin
val datePolicy = WfaScheduleDatePolicy.fixed(
    Clock.fixed(Instant.parse("2026-08-02T02:00:00Z"), ZoneOffset.UTC)
)
```

Add tests:

```kotlin
@Test
fun `bootstrap defaults to tomorrow and loads recommendations automatically`() = runTest {
    val repository = FakeWfaRepository(successFor(LocalDate.of(2026, 8, 3)))
    val viewModel = createViewModel(repository, datePolicy)

    advanceUntilIdle()

    assertEquals(LocalDate.of(2026, 8, 3), viewModel.uiState.value.draft.scheduleDate)
    assertEquals(1, repository.queries.size)
    assertEquals(LocalDate.of(2026, 8, 3), repository.queries.single().scheduleDate)
    assertTrue(viewModel.uiState.value.recommendationState is WfaRequestRecommendationState.Content)
}
```

Also test:

```text
no route coordinates required
date change clears location and selected key
date change starts one new query
old deferred response cannot overwrite a newer date
retry preserves reason and notes
current-location failure is distinct
invalid date performs no repository call
non-ranked candidate remains selectable
repeated identical load while in flight is guarded
```

- [ ] **Step 2: Run ViewModel tests and verify failure**

```bash
./gradlew :app:testDebugUnitTest --tests "*WfaRequestViewModelTest"
```

- [ ] **Step 3: Add state and events**

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

Add to `WfaRequestUiState`:

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

- [ ] **Step 4: Replace route-coordinate bootstrap**

Inject:

```kotlin
private val getCurrentLocation: GetCurrentLocationUseCase,
private val getRecommendations: GetWfaRecommendationsUseCase,
private val datePolicy: WfaScheduleDatePolicy
```

Remove latitude/longitude and initial reverse-geocode dependencies.

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

```kotlin
private var recommendationJob: Job? = null
private var nextRecommendationRequestId = 0L
private var activeQuery: WfaRecommendationQuery? = null
```

```kotlin
private fun loadRecommendations(force: Boolean) {
    val date = _uiState.value.draft.scheduleDate ?: return
    if (!datePolicy.isSelectable(date)) return
    if (!force && recommendationJob?.isActive == true && activeQuery?.scheduleDate == date) return

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
            applyFailure(requestId, date, WfaRecommendationFailure.CurrentLocationUnavailable)
            return@launch
        }

        val query = WfaRecommendationQuery(coordinate, date)
        activeQuery = query
        when (val result = getRecommendations(query)) {
            is WfaRecommendationResult.Success -> applySuccess(requestId, date, coordinate, result)
            is WfaRecommendationResult.Failure -> applyFailure(requestId, date, result.failure)
        }
    }
}

private fun isCurrentRequest(requestId: Long, date: LocalDate): Boolean =
    requestId == nextRecommendationRequestId &&
        _uiState.value.draft.scheduleDate == date
```

Every apply function checks `isCurrentRequest` before state mutation.

- [ ] **Step 6: Implement event semantics**

Valid date change:

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

Recommendation selection copies only name, address, and coordinate into `draft.location`. Scores never enter `SubmitWfaRequestCommand`.

- [ ] **Step 7: Run ViewModel tests and verify pass**

```bash
./gradlew :app:testDebugUnitTest --tests "*WfaRequestViewModelTest"
```

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/example/infinite_track/presentation/screen/attendance/wfa_request/WfaRequestRecommendationState.kt \
  app/src/main/java/com/example/infinite_track/presentation/screen/attendance/wfa_request/WfaRequestUiState.kt \
  app/src/main/java/com/example/infinite_track/presentation/screen/attendance/wfa_request/WfaRequestEvent.kt \
  app/src/main/java/com/example/infinite_track/presentation/screen/attendance/wfa_request/WfaRequestViewModel.kt \
  app/src/test/java/com/example/infinite_track/presentation/screen/attendance/wfa_request/WfaRequestViewModelTest.kt
git commit -m "feat(INF-273): auto-load recommendations in WFA request"
```

---

### Task 6: Build the truthful recommendation picker and map

**Files:**
- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/wfa_request/WfaRequestMapUiMapper.kt`
- Create: `app/src/test/java/com/example/infinite_track/presentation/screen/attendance/wfa_request/WfaRequestMapUiMapperTest.kt`
- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/wfa_request/components/WfaRecommendationPicker.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/wfa_request/WfaRequestFormScreen.kt`
- Modify: `app/src/androidTest/java/com/example/infinite_track/presentation/screen/attendance/wfa_request/WfaRequestScreensTest.kt`
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes Task 5 recommendation state/current coordinate/draft location.
- Emits typed recommendation events and a semantic search callback.
- Uses the existing provider-neutral `AttendanceMap` adapter without approved-target circles.

- [ ] **Step 1: Write pure map-mapper tests**

```kotlin
@Test
fun `map contains current and recommendation markers without circles`() {
    val result = WfaRequestMapUiMapper.map(
        currentCoordinate = GeoCoordinate(-0.9, 119.87),
        recommendationState = contentState,
        hasPreciseLocationPermission = true
    )

    assertEquals(1, result.markers.count { it.role == MapMarkerRole.CURRENT_LOCATION })
    assertEquals(2, result.markers.count { it.role == MapMarkerRole.WFA_RECOMMENDATION })
    assertTrue(result.circles.isEmpty())
}
```

Add selected-marker and stable-ID tests.

- [ ] **Step 2: Add failing Compose tests**

Cover loading, empty, retryable failure, ranked score, insufficient-data copy without `0`, enrichment-failed copy without `0`, card selection, selected-location card, minimum date, and search callback.

Use test tags:

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

- [ ] **Step 3: Run focused tests and verify failure**

```bash
./gradlew :app:testDebugUnitTest --tests "*WfaRequestMapUiMapperTest"
```

- [ ] **Step 4: Implement `WfaRequestMapUiMapper`**

Marker identity:

```kotlin
fun recommendationMarkerId(recommendation: WfaRecommendation): String =
    "wfa:${recommendation.stableKey}"
```

Create current-location and WFA recommendation markers only. Do not create geofence circles or authoritative-target markers.

- [ ] **Step 5: Implement `WfaRecommendationPicker`**

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

Score rendering must branch by status:

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

Never use `finalScore ?: 0.0` in presentation.

- [ ] **Step 6: Refactor the form**

Remove the early return when location is null. Render in this order:

```text
schedule date
recommendation picker
selected location when present
employee
reason/notes
checklist
review action
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

Add `onSearchLocation` and `hasPreciseLocationPermission` parameters. The screen does not navigate directly.

- [ ] **Step 7: Run focused tests**

```bash
./gradlew :app:testDebugUnitTest --tests "*WfaRequestMapUiMapperTest"
```

When an emulator is available:

```bash
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.example.infinite_track.presentation.screen.attendance.wfa_request.WfaRequestScreensTest
```

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/example/infinite_track/presentation/screen/attendance/wfa_request/WfaRequestMapUiMapper.kt \
  app/src/main/java/com/example/infinite_track/presentation/screen/attendance/wfa_request/components/WfaRecommendationPicker.kt \
  app/src/main/java/com/example/infinite_track/presentation/screen/attendance/wfa_request/WfaRequestFormScreen.kt \
  app/src/test/java/com/example/infinite_track/presentation/screen/attendance/wfa_request/WfaRequestMapUiMapperTest.kt \
  app/src/androidTest/java/com/example/infinite_track/presentation/screen/attendance/wfa_request/WfaRequestScreensTest.kt \
  app/src/main/res/values/strings.xml
git commit -m "feat(INF-273): render truthful WFA recommendations"
```

---

### Task 7: Wire the graph and consume manual search exactly once

**Files:**
- Modify: `app/src/main/java/com/example/infinite_track/presentation/navigation/WfaRequestNavGraph.kt`
- Modify: `app/src/androidTest/java/com/example/infinite_track/presentation/navigation/WfaRequestNavigationTest.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/wfa_request/WfaRequestUiMapper.kt`
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes Task 4 result key and Task 5 event.
- Supplies semantic search callback and permission state to Task 6 UI.
- Preserves one graph-scoped controller for form/review/result.

- [ ] **Step 1: Add failing graph tests**

Test:

```text
parent route opens without args
form/review/result share one controller
LocationResult is consumed once
ManualLocationSelected is dispatched once
saved-state result is removed after consumption
review/back/result behavior remains unchanged
```

- [ ] **Step 2: Run navigation tests and verify failure**

Run on an emulator:

```bash
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.example.infinite_track.presentation.navigation.WfaRequestNavigationTest
```

- [ ] **Step 3: Wire the search callback and result collector**

```kotlin
onSearchLocation = { navController.navigate(Screen.LocationSearch.route) }
```

Observe the form back-stack entry:

```kotlin
val selectedLocation by entry.savedStateHandle
    .getStateFlow<LocationResult?>(LocationSearchResultContract.RESULT_KEY, null)
    .collectAsStateWithLifecycle()

LaunchedEffect(selectedLocation) {
    selectedLocation ?: return@LaunchedEffect
    viewModel.onEvent(
        WfaRequestEvent.ManualLocationSelected(
            WfaCandidateLocation(
                latitude = selectedLocation.latitude,
                longitude = selectedLocation.longitude,
                displayName = selectedLocation.placeName,
                formattedAddress = selectedLocation.address
            )
        )
    )
    entry.savedStateHandle.remove<LocationResult>(LocationSearchResultContract.RESULT_KEY)
}
```

Compute precise-location permission in the graph host with `ContextCompat.checkSelfPermission`; do not move `Context` into the ViewModel.

- [ ] **Step 4: Add typed recommendation failure copy**

Map current location, invalid date, duplicate booking, network, provider, server, and unknown failures to safe string resources. Keep config and submit failures separate.

- [ ] **Step 5: Run graph tests and verify pass**

Run the same connected test command. Also run:

```bash
./gradlew :app:testDebugUnitTest --tests "*WfaRequest*Test"
```

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/example/infinite_track/presentation/navigation/WfaRequestNavGraph.kt \
  app/src/main/java/com/example/infinite_track/presentation/screen/attendance/wfa_request/WfaRequestUiMapper.kt \
  app/src/androidTest/java/com/example/infinite_track/presentation/navigation/WfaRequestNavigationTest.kt \
  app/src/main/res/values/strings.xml
git commit -m "feat(INF-273): wire WFA recommendation transaction graph"
```

---

### Task 8: Remove request discovery from Attendance and auto-open not-requested flow

**Files:**
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceViewModel.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceScreen.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/preparation/AttendancePreparationState.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/preparation/AttendancePreparationUiModel.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/preparation/AttendancePreparationUiMapper.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/components/button/attendance/AttendanceBottomSheetContent.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/map/mapper/AttendanceMapUiMapper.kt`
- Modify or delete after usage check: request-only reducer/map-pick helpers.
- Modify focused tests under `app/src/test/java/com/example/infinite_track/presentation/screen/attendance/` and `presentation/map/mapper/`.

**Interfaces:**
- Produces Attendance that resolves backend truth and navigates only for `WFA_NOT_REQUESTED`.
- Removes recommendation/search/map-pick ownership from Attendance.
- Keeps approved target, range, permission, face, and submission behavior.

- [ ] **Step 1: Add failing Attendance state-matrix tests**

Test:

```text
approved today → no request navigation and approved target remains
pending → request-history recovery
rejected → request-history recovery
approval missing for date → request-history recovery
not requested → one NavigationTarget.WfaRequest
rapid repeated WFA selection → no duplicate in-flight navigation
```

Add a compile/constructor assertion that Attendance no longer needs `GetWfaRecommendationsUseCase`.

- [ ] **Step 2: Run focused tests and verify failure**

Discover exact current test classes:

```bash
rg -l "AttendanceViewModel|WFA_NOT_REQUESTED|WfaDiscoveryState" app/src/test
```

Run those classes through `:app:testDebugUnitTest --tests`.

- [ ] **Step 3: Remove recommendation orchestration from `AttendanceViewModel`**

Delete:

```text
GetWfaRecommendationsUseCase dependency
recommendationJob
fetchWfaRecommendations
recommendation retry
recommendation auto-fit
recommendation selection
coordinate-based onBookingClicked logic
```

Use:

```kotlin
sealed class NavigationTarget {
    data class WfaRequest(val route: String) : NavigationTarget()
    // preserve existing unrelated targets
}
```

- [ ] **Step 4: Auto-open only after authoritative not-requested resolution**

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

Call it after the latest resolution is applied. Do not call it for unresolved/failure/pending/rejected/approved states.

- [ ] **Step 5: Remove request-only state and UI**

Remove from Attendance:

```text
wfaDiscovery
mapPickInteraction
recommendation rows
search/pick secondary actions
recommendation marker handling
search-result consumption
loading overlay
map-pick crosshair
```

Keep current location and authoritative target markers.

- [ ] **Step 6: Delete dead helpers only after usage search**

```bash
rg "WfaDiscoveryState|WfaMapPickInteractionState|WfaMapSelectionEffect|selectRecommendation" app/src
```

Delete only helpers with no active consumers. Do not delete shared map infrastructure.

- [ ] **Step 7: Run focused Attendance tests**

```bash
./gradlew :app:testDebugUnitTest \
  --tests "*EvaluateAttendancePreparationUseCaseTest" \
  --tests "*AttendancePreparationUiMapperTest" \
  --tests "*AttendanceMapUiMapperTest" \
  --tests "*AttendanceViewModel*Test"
```

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/example/infinite_track/presentation/screen/attendance \
  app/src/main/java/com/example/infinite_track/presentation/components/button/attendance/AttendanceBottomSheetContent.kt \
  app/src/main/java/com/example/infinite_track/presentation/map/mapper/AttendanceMapUiMapper.kt \
  app/src/test/java/com/example/infinite_track/presentation/screen/attendance \
  app/src/test/java/com/example/infinite_track/presentation/map/mapper
git commit -m "refactor(INF-273): move WFA discovery out of attendance"
```

---

### Task 9: Complete regression coverage and verification

**Files:**
- Modify only files already owned by Tasks 1–8 when a failing test identifies a real defect.
- Do not commit APKs, generated reports, local properties, secrets, or temporary logs.

- [ ] **Step 1: Run the entire WFA-focused JVM suite**

```bash
./gradlew :app:testDebugUnitTest --tests "*Wfa*Test"
```

Expected: PASS.

- [ ] **Step 2: Verify the old recommendation contract is gone**

```bash
rg "score_details|wifi_quality|noise_level|crowd_density" \
  app/src/main/java/com/example/infinite_track/{data,domain,presentation}
```

Expected: no active WFA recommendation consumer. Booking-history `suitability_score` may remain because it is a separate persisted booking contract.

- [ ] **Step 3: Verify Attendance no longer owns discovery**

```bash
rg "GetWfaRecommendationsUseCase|fetchWfaRecommendations|WfaDiscoveryState" \
  app/src/main/java/com/example/infinite_track/presentation/screen/attendance
```

Expected: no request recommendation ownership under Attendance; request ownership exists under `wfa_request`.

- [ ] **Step 4: Run all JVM unit tests**

```bash
./gradlew :app:testDebugUnitTest
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Build debug**

```bash
./gradlew :app:assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Run lint**

```bash
./gradlew :app:lintDebug
```

Expected: BUILD SUCCESSFUL, or pre-existing non-blocking findings are documented with evidence that INF-273 introduced no new blocker.

- [ ] **Step 7: Run connected tests when an emulator/device is available**

```bash
./gradlew :app:connectedDebugAndroidTest
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 8: Execute the runtime matrix**

```text
approved WFA today stays in Attendance
pending/rejected/missing approval keeps history recovery
not requested opens WFA Request automatically
default date is tomorrow
today/past cannot be selected
later future date reloads recommendations
old selection clears on date change
stale result cannot replace current date
ranked candidate shows score
insufficient/enrichment-failed candidate never shows 0
manual search is secondary and consumed once
review and submit preserve selected date/location
duplicate-date rejection is typed
```

- [ ] **Step 9: Review the final diff**

```bash
git diff --stat develop...HEAD
git diff --check develop...HEAD
git log --oneline develop..HEAD
```

Confirm no Backend files, generated artifacts, sensitive logs, duplicate routes, DTO leakage, or recommendation ownership left in Attendance. Fix any failure in the task that owns the affected file, rerun that task's tests, and commit with a bounded `fix(INF-273): ...` message.
