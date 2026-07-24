# INF-238 WFA Search and Map Revision Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver a compact, themed WFA discovery flow with reliable Places SDK (New) search, category-aware markers, selected-marker zoom, and no recommendation list in the primary Attendance bottom sheet.

**Architecture:** Keep the provider-neutral domain contracts and the cohesive Attendance preparation state delivered by PR #99. Upgrade only the Google Places adapter, make SearchViewModel the single owner of query/session-visible UI state, project typed marker categories through `AttendanceMapUiMapper`, and render Google-specific marker/camera behavior only in the map adapter.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Hilt, Kotlin Coroutines/Flow, Google Maps Compose 2.11.0, Places SDK for Android (New) 3.5.0, JUnit4, kotlinx-coroutines-test, Compose UI tests.

## Global Constraints

- Work only in `E:\skrisi\android\.worktrees\inf-238-wfa-search-revision` on `codex/inf-238-wfa-search-revision`.
- Preserve the authoritative target contract: search results and WFA recommendations are preview/booking-draft state, never an Attendance target without an approved booking.
- Use Places SDK for Android fixed version `3.5.0`; never use `+` or `latest`.
- `3.5.0` is the approved compatibility baseline for the existing Kotlin `1.9.0` toolchain; do not expand this task into a Kotlin, Compose, AGP, or Gradle-wrapper upgrade.
- Initialize with `Places.initializeWithNewPlacesApiEnabled(...)`.
- API-key injection remains `local.properties -> MAPS_API_KEY manifest placeholder`; never print, log, document, or commit a key.
- Keep the Gradle wrapper unchanged.
- Remove only debug `isShrinkResources = false`; retain debug `isMinifyEnabled = false`, `isDebuggable = true`, and release shrinking.
- Do not add a color constant or raw screen-specific color.
- Marker mapping is WFO `Blue_500`, WFH `Blue_Accent_500`, WFA `Orange_500`.
- Bottom-sheet and compact-callout surfaces reuse `InfiniteColors.AttendanceReportGlassSurface` and `InfiniteColors.AttendanceReportGlassBorder`.
- Main Attendance bottom sheet contains no WFA recommendation list.
- Place lists appear only inside explicit WFA search.
- Autocomplete uses minimum query length 2, 400 ms debounce, `distinctUntilChanged`, latest-query-wins, Indonesia filter, optional location bias/origin, and one session token through selected Place Details.
- Place Details requests only ID/resource identity, formatted address, and location; the selected autocomplete prediction supplies display name and category evidence.
- Do not request photos, ratings, reviews, opening hours, phone, website, or atmosphere fields.
- Runtime-sensitive Attendance/map/search changes require emulator/device verification; otherwise mark them `Needs Verification`.

---

### Task 1: Upgrade Places SDK (New) and clean the debug build warning

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/java/com/example/infinite_track/data/location/discovery/GooglePlacesClientProvider.kt`
- Test: `app/src/test/java/com/example/infinite_track/data/location/discovery/GooglePlacesInitializationContractTest.kt`

**Interfaces:**
- Consumes: manifest metadata name `com.google.android.geo.API_KEY`.
- Produces: `GooglePlacesClientProvider.get(): PlacesClient` initialized through Places API (New), with no key exposed.

- [ ] **Step 1: Write the failing source-contract test**

Create `GooglePlacesInitializationContractTest.kt`:

```kotlin
package com.example.infinite_track.data.location.discovery

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GooglePlacesInitializationContractTest {
    private val repoRoot = File(System.getProperty("user.dir"))

    @Test
    fun `places uses fixed new sdk and new initialization`() {
        val versions = repoRoot.resolve("gradle/libs.versions.toml").readText()
        val provider = repoRoot.resolve(
            "app/src/main/java/com/example/infinite_track/data/location/discovery/" +
                "GooglePlacesClientProvider.kt"
        ).readText()

        assertTrue(versions.contains("places = \"3.5.0\""))
        assertTrue(provider.contains("Places.initializeWithNewPlacesApiEnabled("))
        assertFalse(provider.contains("Places.initialize(context"))
    }

    @Test
    fun `debug does not redundantly disable resource shrinking`() {
        val buildFile = repoRoot.resolve("app/build.gradle.kts").readText()
        val debugBlock = buildFile.substringAfter("getByName(\"debug\")")
            .substringBefore("}")

        assertFalse(debugBlock.contains("isShrinkResources = false"))
        assertTrue(debugBlock.contains("isMinifyEnabled = false"))
        assertTrue(debugBlock.contains("isDebuggable = true"))
    }
}
```

- [ ] **Step 2: Run the test and verify it fails**

Run:

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest `
  --tests '*GooglePlacesInitializationContractTest' --console=plain
```

Expected: FAIL because the catalog still contains `places = "2.6.0"`, the provider still calls legacy `Places.initialize`, and debug still declares `isShrinkResources = false`.

- [ ] **Step 3: Apply the minimal SDK and build configuration changes**

In `gradle/libs.versions.toml`:

```toml
places = "3.5.0"
```

In the debug block of `app/build.gradle.kts`, keep:

```kotlin
isMinifyEnabled = false
isDebuggable = true
```

and delete only:

```kotlin
isShrinkResources = false
```

In `GooglePlacesClientProvider.get()` replace legacy initialization with:

```kotlin
if (!Places.isInitialized()) {
    Places.initializeWithNewPlacesApiEnabled(
        context,
        apiKey,
        Locale("id", "ID")
    )
}
return Places.createClient(context)
```

Do not change `mapsApiKeyFromManifest()` or add a fallback key.

- [ ] **Step 4: Run the focused test and compile the Google adapter**

Run:

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest `
  --tests '*GooglePlacesInitializationContractTest' --console=plain
.\gradlew.bat --no-daemon app:compileDebugKotlin --console=plain
```

Expected: both commands PASS. If the new SDK changed a Places API symbol, correct only the touched adapter to the 3.5.0 API before proceeding.

- [ ] **Step 5: Commit Task 1**

```powershell
git add gradle/libs.versions.toml app/build.gradle.kts `
  app/src/main/java/com/example/infinite_track/data/location/discovery/GooglePlacesClientProvider.kt `
  app/src/test/java/com/example/infinite_track/data/location/discovery/GooglePlacesInitializationContractTest.kt
git commit -m "build: migrate location search to Places SDK New"
```

---

### Task 2: Make autocomplete requests, fields, and sessions explicit and testable

**Files:**
- Modify: `app/src/main/java/com/example/infinite_track/domain/repository/location/PlaceDiscoveryRepository.kt`
- Modify: `app/src/main/java/com/example/infinite_track/domain/use_case/location/ResolvePlaceDetailsUseCase.kt`
- Create: `app/src/main/java/com/example/infinite_track/data/location/discovery/GooglePlacesRequestFactory.kt`
- Modify: `app/src/main/java/com/example/infinite_track/data/location/discovery/GooglePlacesDataSource.kt`
- Modify: `app/src/main/java/com/example/infinite_track/data/location/discovery/GooglePlacesMapper.kt`
- Modify: `app/src/main/java/com/example/infinite_track/data/location/discovery/GooglePlacesSessionManager.kt`
- Modify: `app/src/main/java/com/example/infinite_track/data/location/discovery/GooglePlacesDiscoveryRepository.kt`
- Test: `app/src/test/java/com/example/infinite_track/data/location/discovery/GooglePlacesRequestFactoryTest.kt`
- Test: `app/src/test/java/com/example/infinite_track/data/location/discovery/GooglePlacesSessionManagerTest.kt`
- Modify test: `app/src/test/java/com/example/infinite_track/domain/use_case/location/PlaceDiscoveryUseCaseTest.kt`

**Interfaces:**
- Consumes: `GeoCoordinate?`, query, place ID, and `AutocompleteSessionToken`.
- Produces:
  - `GooglePlacesRequestFactory.autocomplete(query, proximity, token): FindAutocompletePredictionsRequest`
  - `GooglePlacesRequestFactory.details(placeId, token, cancellationToken): FetchPlaceRequest`
  - `ResolvePlaceDetailsUseCase(suggestion: PlaceSuggestion): PlaceDetailsResult`
  - one token reused by search and selected details, renewed after resolution or abandonment.

- [ ] **Step 1: Write failing request-factory tests**

Create `GooglePlacesRequestFactoryTest.kt`:

```kotlin
package com.example.infinite_track.data.location.discovery

import com.example.infinite_track.domain.model.location.GeoCoordinate
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GooglePlacesRequestFactoryTest {
    private val factory = GooglePlacesRequestFactory()
    private val token = AutocompleteSessionToken.newInstance()

    @Test
    fun `autocomplete restricts Indonesia and adds optional proximity evidence`() {
        val coordinate = GeoCoordinate(-0.899, 119.877)
        val request = factory.autocomplete("kopi", coordinate, token, cancellationToken = null)

        assertEquals("kopi", request.query)
        assertEquals(listOf("ID"), request.countries)
        assertNotNull(request.locationBias)
        assertEquals(-0.899, request.origin?.latitude ?: 0.0, 0.000001)
        assertEquals(119.877, request.origin?.longitude ?: 0.0, 0.000001)
        assertEquals(token, request.sessionToken)
    }

    @Test
    fun `autocomplete works without location bias`() {
        val request = factory.autocomplete("palu", null, token, cancellationToken = null)

        assertNull(request.locationBias)
        assertNull(request.origin)
    }

    @Test
    fun `details request contains only approved essentials fields`() {
        val request = factory.details("opaque-id", token, cancellationToken = null)

        assertEquals("opaque-id", request.placeId)
        assertEquals(token, request.sessionToken)
        assertEquals(
            setOf(
                Place.Field.ID,
                Place.Field.FORMATTED_ADDRESS,
                Place.Field.LOCATION
            ),
            request.placeFields.toSet()
        )
        assertTrue(Place.Field.RATING !in request.placeFields)
        assertTrue(Place.Field.PHOTO_METADATAS !in request.placeFields)
        assertTrue(Place.Field.REVIEWS !in request.placeFields)
    }
}
```

- [ ] **Step 2: Run the request tests and verify the missing factory failure**

Run:

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest `
  --tests '*GooglePlacesRequestFactoryTest' --console=plain
```

Expected: compilation FAIL because `GooglePlacesRequestFactory` does not exist.

- [ ] **Step 3: Implement the request factory**

Create `GooglePlacesRequestFactory.kt`:

```kotlin
package com.example.infinite_track.data.location.discovery

import com.example.infinite_track.domain.model.location.GeoCoordinate
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationToken
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.CircularBounds
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import javax.inject.Inject

class GooglePlacesRequestFactory @Inject constructor() {
    fun autocomplete(
        query: String,
        proximity: GeoCoordinate?,
        token: AutocompleteSessionToken,
        cancellationToken: CancellationToken?
    ): FindAutocompletePredictionsRequest {
        val builder = FindAutocompletePredictionsRequest.builder()
            .setQuery(query)
            .setCountries("ID")
            .setSessionToken(token)

        proximity?.let {
            val origin = LatLng(it.latitude, it.longitude)
            builder
                .setOrigin(origin)
                .setLocationBias(CircularBounds.newInstance(origin, LOCATION_BIAS_METERS))
        }
        cancellationToken?.let(builder::setCancellationToken)
        return builder.build()
    }

    fun details(
        placeId: String,
        token: AutocompleteSessionToken,
        cancellationToken: CancellationToken?
    ): FetchPlaceRequest {
        val builder = FetchPlaceRequest.builder(
            placeId,
            listOf(
                Place.Field.ID,
                Place.Field.FORMATTED_ADDRESS,
                Place.Field.LOCATION
            )
        )
            .setSessionToken(token)
        cancellationToken?.let(builder::setCancellationToken)
        return builder.build()
    }

    private companion object {
        const val LOCATION_BIAS_METERS = 50_000.0
    }
}
```

Do not use a location restriction; the circle is a bias so valid Indonesian results are not hidden.

- [ ] **Step 4: Refactor the data source and mapper to the new request/response fields**

Inject the factory:

```kotlin
class GooglePlacesDataSource @Inject constructor(
    private val clientProvider: GooglePlacesClientProvider,
    private val requestFactory: GooglePlacesRequestFactory
)
```

Build autocomplete with:

```kotlin
val request = requestFactory.autocomplete(
    query = query,
    proximity = proximity,
    token = sessionToken,
    cancellationToken = cancellation.token
)

return clientProvider.get()
    .findAutocompletePredictions(request)
    .await(cancellation)
    .result
    .autocompletePredictions
```

Build details with:

```kotlin
val request = requestFactory.details(
    placeId = placeId,
    token = sessionToken,
    cancellationToken = cancellation.token
)

return clientProvider.get().fetchPlace(request).await(cancellation).place
```

Change the provider-neutral resolve contract so the already-returned autocomplete name is reused instead of requesting the higher-tier Place Details display-name field:

```kotlin
interface PlaceDiscoveryRepository {
    suspend fun search(
        query: String,
        proximity: GeoCoordinate? = null
    ): PlaceSearchResult

    suspend fun resolve(suggestion: PlaceSuggestion): PlaceDetailsResult

    fun abandonSession()
}
```

Update `ResolvePlaceDetailsUseCase`:

```kotlin
suspend operator fun invoke(suggestion: PlaceSuggestion): PlaceDetailsResult {
    return if (suggestion.placeId.isBlank()) {
        PlaceDetailsResult.Failure(PlaceDiscoveryFailure.INVALID_REQUEST)
    } else {
        repository.resolve(suggestion)
    }
}
```

Update `GooglePlacesDiscoveryRepository.resolve()` to fetch with `suggestion.placeId`, pass the suggestion into the mapper, and retain `sessions.renew()` in `finally`.

Update `GooglePlacesMapper.toDetails()`:

```kotlin
fun toDetails(place: Place, suggestion: PlaceSuggestion): PlaceDetails? {
    val id = place.id?.takeIf(String::isNotBlank) ?: return null
    val coordinate = place.location ?: return null
    return PlaceDetails(
        placeId = id,
        displayName = suggestion.primaryText,
        formattedAddress = place.formattedAddress ?: suggestion.secondaryText,
        coordinate = GeoCoordinate(coordinate.latitude, coordinate.longitude)
    )
}
```

Update `PlaceDiscoveryUseCaseTest`:

```kotlin
@Test
fun `resolve rejects blank place id`() = runBlocking {
    val repository = FakePlaceDiscoveryRepository()
    val invalid = suggestion("", "Invalid")

    val result = ResolvePlaceDetailsUseCase(repository)(invalid)
        as PlaceDetailsResult.Failure

    assertEquals(PlaceDiscoveryFailure.INVALID_REQUEST, result.reason)
    assertEquals(null, repository.lastResolvedSuggestion)
}

@Test
fun `resolve forwards autocomplete evidence and coordinate`() = runBlocking {
    val selected = suggestion("opaque-id", "Kopi Kita")
    val details = PlaceDetails(
        placeId = "opaque-id",
        displayName = selected.primaryText,
        formattedAddress = selected.secondaryText,
        coordinate = GeoCoordinate(-0.90, 119.88)
    )
    val repository = FakePlaceDiscoveryRepository(
        detailsResult = PlaceDetailsResult.Success(details)
    )

    val result = ResolvePlaceDetailsUseCase(repository)(selected)
        as PlaceDetailsResult.Success

    assertEquals(details, result.details)
    assertEquals(selected, repository.lastResolvedSuggestion)
}
```

Change the fake member and override:

```kotlin
var lastResolvedSuggestion: PlaceSuggestion? = null

override suspend fun resolve(suggestion: PlaceSuggestion): PlaceDetailsResult {
    lastResolvedSuggestion = suggestion
    return detailsResult
}
```

- [ ] **Step 5: Write and run the session lifecycle tests**

Create `GooglePlacesSessionManagerTest.kt`:

```kotlin
package com.example.infinite_track.data.location.discovery

import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test

class GooglePlacesSessionManagerTest {
    @Test
    fun `current token is stable throughout one search session`() {
        val manager = GooglePlacesSessionManager()

        assertSame(manager.current(), manager.current())
    }

    @Test
    fun `renew starts a different session`() {
        val manager = GooglePlacesSessionManager()
        val first = manager.current()

        manager.renew()

        assertNotSame(first, manager.current())
    }
}
```

Run:

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest `
  --tests '*GooglePlacesRequestFactoryTest' `
  --tests '*GooglePlacesSessionManagerTest' `
  --tests '*PlaceDiscoveryUseCaseTest' --console=plain
```

Expected: PASS. Verify `GooglePlacesDiscoveryRepository.resolve()` still renews in `finally` and `abandonSession()` still renews.

- [ ] **Step 6: Commit Task 2**

```powershell
git add app/src/main/java/com/example/infinite_track/data/location/discovery `
  app/src/main/java/com/example/infinite_track/domain/repository/location/PlaceDiscoveryRepository.kt `
  app/src/main/java/com/example/infinite_track/domain/use_case/location/ResolvePlaceDetailsUseCase.kt `
  app/src/test/java/com/example/infinite_track/data/location/discovery `
  app/src/test/java/com/example/infinite_track/domain/use_case/location/PlaceDiscoveryUseCaseTest.kt
git commit -m "refactor: make Places search sessions cost aware"
```

---

### Task 3: Enforce controlled, latest-query-wins search state

**Files:**
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/search/SearchViewModel.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/components/search/InfiniteTrackSearchBar.kt`
- Test: `app/src/test/java/com/example/infinite_track/presentation/screen/attendance/search/SearchViewModelTest.kt`

**Interfaces:**
- Consumes: `SearchPlacesUseCase`, `ResolvePlaceDetailsUseCase`, `GetCurrentLocationUseCase`.
- Produces:
  - `searchQuery: StateFlow<String>`
  - `searchState: StateFlow<SearchUiState>`
  - `selectionEvents: SharedFlow<LocationResult>`
  - controlled `InfiniteTrackSearchBar(value, onChange, onClear)`.

- [ ] **Step 1: Write failing ViewModel timing and cancellation tests**

Create `SearchViewModelTest.kt` with repository-backed real use cases:

```kotlin
package com.example.infinite_track.presentation.screen.attendance.search

import com.example.infinite_track.domain.model.location.CurrentLocationResult
import com.example.infinite_track.domain.model.location.GeoCoordinate
import com.example.infinite_track.domain.model.location.PlaceDetailsResult
import com.example.infinite_track.domain.model.location.PlaceSearchResult
import com.example.infinite_track.domain.model.location.PlaceSuggestion
import com.example.infinite_track.domain.repository.location.CurrentLocationRepository
import com.example.infinite_track.domain.repository.location.PlaceDiscoveryRepository
import com.example.infinite_track.domain.use_case.location.GetCurrentLocationUseCase
import com.example.infinite_track.domain.use_case.location.ResolvePlaceDetailsUseCase
import com.example.infinite_track.domain.use_case.location.SearchPlacesUseCase
import com.example.infinite_track.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    @get:Rule val dispatcherRule = MainDispatcherRule()

    @Test
    fun `query shorter than two characters never searches`() =
        runTest(dispatcherRule.dispatcher) {
            val repository = RecordingPlaceRepository()
            val viewModel = viewModel(repository)

            viewModel.updateSearchQuery("k")
            advanceTimeBy(400)
            runCurrent()

            assertEquals(emptyList<String>(), repository.queries)
            assertEquals(SearchUiState.Idle, viewModel.searchState.value)
        }

    @Test
    fun `valid query waits four hundred milliseconds`() =
        runTest(dispatcherRule.dispatcher) {
            val repository = RecordingPlaceRepository()
            val viewModel = viewModel(repository)

            viewModel.updateSearchQuery("kopi")
            advanceTimeBy(399)
            runCurrent()
            assertEquals(emptyList<String>(), repository.queries)

            advanceTimeBy(1)
            runCurrent()
            assertEquals(listOf("kopi"), repository.queries)
        }

    @Test
    fun `newer query supersedes older unfinished result`() =
        runTest(dispatcherRule.dispatcher) {
            val first = CompletableDeferred<PlaceSearchResult>()
            val repository = RecordingPlaceRepository(firstResult = first)
            val viewModel = viewModel(repository)

            viewModel.updateSearchQuery("kopi")
            advanceTimeBy(400)
            runCurrent()
            viewModel.updateSearchQuery("kopi palu")
            advanceTimeBy(400)
            runCurrent()

            assertEquals(listOf("kopi", "kopi palu"), repository.queries)
            val success = viewModel.searchState.value as SearchUiState.Success
            assertEquals("kopi palu", success.suggestions.single().primaryText)
        }

    private fun viewModel(repository: RecordingPlaceRepository) = SearchViewModel(
        searchPlaces = SearchPlacesUseCase(repository),
        resolvePlaceDetails = ResolvePlaceDetailsUseCase(repository),
        getCurrentLocation = GetCurrentLocationUseCase(
            object : CurrentLocationRepository {
                override suspend fun getCurrentLocation(): CurrentLocationResult =
                    CurrentLocationResult.Failure.Unavailable
            }
        )
    )

    private class RecordingPlaceRepository(
        private val firstResult: CompletableDeferred<PlaceSearchResult>? = null
    ) : PlaceDiscoveryRepository {
        val queries = mutableListOf<String>()

        override suspend fun search(
            query: String,
            proximity: GeoCoordinate?
        ): PlaceSearchResult {
            queries += query
            if (queries.size == 1 && firstResult != null) return firstResult.await()
            return PlaceSearchResult.Success(
                listOf(PlaceSuggestion(query, query, "Palu", null))
            )
        }

        override suspend fun resolve(suggestion: PlaceSuggestion): PlaceDetailsResult =
            PlaceDetailsResult.Failure(
                com.example.infinite_track.domain.model.location
                    .PlaceDiscoveryFailure.UNAVAILABLE
            )

        override fun abandonSession() = Unit
    }
}
```

- [ ] **Step 2: Run the tests and verify debounce mismatch or stale-result failure**

Run:

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest `
  --tests '*SearchViewModelTest' --console=plain
```

Expected: FAIL because the current debounce is 500 ms and query processing is split across manually managed jobs rather than one latest-query flow.

- [ ] **Step 3: Replace manual search jobs with a latest-query flow**

In `SearchViewModel`, remove `searchJob` and make `observeQuery()`:

```kotlin
private var retryJob: Job? = null

private fun observeQuery() {
    _searchQuery
        .map(String::trim)
        .debounce(DEBOUNCE_DELAY)
        .distinctUntilChanged()
        .flatMapLatest { query ->
            flow {
                if (query.length < MIN_QUERY_LENGTH) {
                    emit(SearchUiState.Idle)
                } else {
                    emit(SearchUiState.Loading)
                    emit(searchStateFor(query))
                }
            }
        }
        .onEach { _searchState.value = it }
        .launchIn(viewModelScope)
}

private suspend fun searchStateFor(query: String): SearchUiState =
    when (val result = searchPlaces(query, proximity)) {
        is PlaceSearchResult.Success ->
            if (result.suggestions.isEmpty()) SearchUiState.Empty
            else SearchUiState.Success(result.suggestions)
        is PlaceSearchResult.Failure ->
            SearchUiState.Error(result.reason.toUserMessage())
    }
```

Set:

```kotlin
private const val DEBOUNCE_DELAY = 400L
```

Cancel an in-flight retry whenever the user changes the query:

```kotlin
fun updateSearchQuery(query: String) {
    retryJob?.cancel()
    _searchQuery.value = query
}
```

Implement retry without mutating the query:

```kotlin
fun retrySearch() {
    val query = _searchQuery.value.trim()
    if (query.length < MIN_QUERY_LENGTH) return
    retryJob?.cancel()
    retryJob = viewModelScope.launch {
        _searchState.value = SearchUiState.Loading
        _searchState.value = searchStateFor(query)
    }
}
```

Cancel `retryJob` in `clearSearch()` and `onCleared()`. Keep selection resolution cancellable and keep `abandonSession()` in both lifecycle paths.

In `onSuggestionSelected`, resolve the complete suggestion so its autocomplete name can be reused without a higher-tier Place Details field:

```kotlin
when (val result = resolvePlaceDetails(suggestion)) {
    is PlaceDetailsResult.Success -> {
        val details = result.details
        _selectionEvents.emit(
            LocationResult(
                placeName = details.displayName,
                address = details.formattedAddress.orEmpty(),
                latitude = details.coordinate.latitude,
                longitude = details.coordinate.longitude
            )
        )
    }
    is PlaceDetailsResult.Failure -> {
        _searchState.value = SearchUiState.Error(result.reason.toUserMessage())
    }
}
```

- [ ] **Step 4: Make `InfiniteTrackSearchBar` fully controlled**

Delete:

```kotlin
var searchValue by remember { mutableStateOf(value) }
```

Use:

```kotlin
OutlinedTextField(
    value = value,
    onValueChange = onChange,
    // existing themed shape and colors
)
```

Add:

```kotlin
onClear: () -> Unit = {}
```

and a trailing clear icon only when `value.isNotEmpty()`. The clear icon calls `onClear`; it does not mutate local state.

- [ ] **Step 5: Run focused and existing search-domain tests**

Run:

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest `
  --tests '*SearchViewModelTest' `
  --tests '*PlaceDiscoveryUseCaseTest' --console=plain
```

Expected: PASS with no leaked coroutine and no stale first-query result.

- [ ] **Step 6: Commit Task 3**

```powershell
git add app/src/main/java/com/example/infinite_track/presentation/screen/attendance/search/SearchViewModel.kt `
  app/src/main/java/com/example/infinite_track/presentation/components/search/InfiniteTrackSearchBar.kt `
  app/src/test/java/com/example/infinite_track/presentation/screen/attendance/search/SearchViewModelTest.kt
git commit -m "fix: make WFA location search latest query wins"
```

---

### Task 4: Redesign the explicit WFA search screen and result cards

**Files:**
- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/search/WfaPlaceResultCard.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/search/LocationSearchScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Test: `app/src/androidTest/java/com/example/infinite_track/presentation/screen/attendance/search/LocationSearchContentTest.kt`

**Interfaces:**
- Consumes: controlled query, `SearchUiState`, retry/clear/select callbacks.
- Produces: `LocationSearchContent(...)` as a stateless Compose content function and compact keyed search-result cards.

- [ ] **Step 1: Write failing Compose tests for list ownership and compact content**

Create `LocationSearchContentTest.kt`:

```kotlin
package com.example.infinite_track.presentation.screen.attendance.search

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.example.infinite_track.domain.model.location.PlaceSuggestion
import com.example.infinite_track.presentation.theme.Infinite_TrackTheme
import org.junit.Rule
import org.junit.Test

class LocationSearchContentTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun results_render_only_inside_explicit_search_content() {
        compose.setContent {
            Infinite_TrackTheme {
                LocationSearchContent(
                    query = "kopi",
                    state = SearchUiState.Success(
                        listOf(PlaceSuggestion("id-1", "Kopi Palu", "Tondo", null))
                    ),
                    resolvingPlaceId = null,
                    onQueryChange = {},
                    onClear = {},
                    onRetry = {},
                    onSuggestionSelected = {}
                )
            }
        }

        compose.onNodeWithTag("wfaSearchResults").assertIsDisplayed()
        compose.onNodeWithText("Kopi Palu").assertIsDisplayed()
        compose.onNodeWithText("Tondo").assertIsDisplayed()
    }

    @Test
    fun resolving_card_exposes_selected_semantics() {
        compose.setContent {
            Infinite_TrackTheme {
                WfaPlaceResultCard(
                    suggestion = PlaceSuggestion("id-1", "Kopi Palu", "Tondo", null),
                    selected = true,
                    onClick = {}
                )
            }
        }

        compose.onNodeWithTag("wfaPlace:id-1").assertIsSelected()
    }
}
```

- [ ] **Step 2: Run Android test compilation and verify missing composables**

Run:

```powershell
.\gradlew.bat --no-daemon app:compileDebugAndroidTestKotlin --console=plain
```

Expected: FAIL because `LocationSearchContent` and `WfaPlaceResultCard` do not exist.

- [ ] **Step 3: Implement the compact result card**

Create `WfaPlaceResultCard.kt` around the existing atoms:

```kotlin
@Composable
internal fun WfaPlaceResultCard(
    suggestion: PlaceSuggestion,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    InfiniteCard(
        modifier = modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick)
            .semantics { this.selected = selected }
            .testTag("wfaPlace:${suggestion.placeId}"),
        variant = InfiniteSurfaceVariant.Outlined,
        semantic = if (selected) InfiniteSemantic.Secondary else InfiniteSemantic.Neutral,
        showShadow = false
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.md)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(InfiniteColors.Secondary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = InfiniteIcons.Location,
                    contentDescription = null,
                    tint = InfiniteColors.Secondary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.xs)
            ) {
                Text(
                    text = suggestion.primaryText,
                    style = MaterialTheme.typography.titleSmall,
                    color = InfiniteColors.Text,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                suggestion.secondaryText?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = InfiniteColors.AttendanceReportBodyText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                suggestion.distance?.let {
                    Text(
                        text = formatDistanceMeters(it.value),
                        style = MaterialTheme.typography.labelSmall,
                        color = InfiniteColors.Secondary
                    )
                }
            }
            if (selected) {
                Icon(
                    imageVector = InfiniteIcons.Success,
                    contentDescription = null,
                    tint = InfiniteColors.Secondary
                )
            }
        }
    }
}
```

Add this private pure formatter in the same file:

```kotlin
internal fun formatDistanceMeters(value: Double): String =
    if (value < 1_000.0) "${value.toInt()} m"
    else String.format(Locale("id", "ID"), "%.1f km", value / 1_000.0)
```

- [ ] **Step 4: Extract stateless search content and replace hardcoded legacy UI**

Keep `LocationSearchScreen` responsible only for collecting ViewModel state and navigation. Add:

```kotlin
@Composable
internal fun LocationSearchContent(
    query: String,
    state: SearchUiState,
    resolvingPlaceId: String?,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onRetry: () -> Unit,
    onSuggestionSelected: (PlaceSuggestion) -> Unit,
    modifier: Modifier = Modifier
)
```

Use `InfiniteTopBar(title = stringResource(R.string.wfa_search_title), ...)`, the controlled `InfiniteTrackSearchBar`, and:

```kotlin
val suggestions = when (state) {
    is SearchUiState.Success -> state.suggestions
    is SearchUiState.Resolving -> state.suggestions
    else -> emptyList()
}

LazyColumn(
    modifier = Modifier
        .fillMaxSize()
        .testTag("wfaSearchResults"),
    contentPadding = PaddingValues(bottom = InfiniteSpacing.Default.xl),
    verticalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.sm)
) {
    items(suggestions, key = PlaceSuggestion::placeId) { suggestion ->
        WfaPlaceResultCard(
            suggestion = suggestion,
            selected = suggestion.placeId == resolvingPlaceId,
            onClick = { onSuggestionSelected(suggestion) }
        )
    }
}
```

At the stateful wrapper call site, derive selection without duplicating state:

```kotlin
LocationSearchContent(
    query = searchQuery,
    state = searchState,
    resolvingPlaceId = (searchState as? SearchUiState.Resolving)?.selectedPlaceId,
    onQueryChange = viewModel::updateSearchQuery,
    onClear = viewModel::clearSearch,
    onRetry = viewModel::retrySearch,
    onSuggestionSelected = viewModel::onSuggestionSelected
)
```

Replace the corrupted emoji placeholder with `InfiniteIcons.Search`. Use existing `MaterialTheme.typography` and Infinite design tokens only. Error state must render an actual retry button wired to `onRetry`.

Add Indonesian strings for title, placeholder, guidance, empty title/body, generic error title, retry, and clear-search content description. Do not leave new user-facing copy hardcoded in Kotlin.

- [ ] **Step 5: Compile and run the search Compose tests**

Run:

```powershell
.\gradlew.bat --no-daemon app:compileDebugAndroidTestKotlin --console=plain
.\gradlew.bat --no-daemon app:connectedDebugAndroidTest `
  -Pandroid.testInstrumentationRunnerArguments.class=com.example.infinite_track.presentation.screen.attendance.search.LocationSearchContentTest `
  --console=plain
```

Expected: compile PASS; connected tests PASS when an emulator/device is available. If none is available, record connected tests as `Needs Verification` and do not claim runtime completion.

- [ ] **Step 6: Commit Task 4**

```powershell
git add app/src/main/java/com/example/infinite_track/presentation/screen/attendance/search `
  app/src/main/res/values/strings.xml `
  app/src/androidTest/java/com/example/infinite_track/presentation/screen/attendance/search
git commit -m "feat: redesign explicit WFA place search"
```

---

### Task 5: Simplify the Attendance bottom sheet and remove its recommendation list

**Files:**
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceScreen.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/components/button/attendance/AttendanceBottomSheetContent.kt`
- Delete: `app/src/main/java/com/example/infinite_track/presentation/components/button/attendance/WfaRecommendationSection.kt`
- Delete: `app/src/main/java/com/example/infinite_track/presentation/components/button/attendance/WfaRecommendationOption.kt`
- Modify: `app/src/androidTest/java/com/example/infinite_track/presentation/screen/attendance/WorkModeTargetLocationScreenTest.kt`

**Interfaces:**
- Consumes: `AttendancePreparationUiModel` and the existing search/map secondary actions.
- Produces: a single topbar-family sheet surface with no `WfaRecommendationSection` call site.

- [ ] **Step 1: Add the failing absence assertion**

Extend `WorkModeTargetLocationScreenTest` with a WFA model that contains a recommendation and assert:

```kotlin
composeRule.setContent {
    Infinite_TrackTheme {
        AttendanceBottomSheetContent(
            model = wfaModelWithRecommendation(name = "Kopi Rekomendasi"),
            onEvent = {}
        )
    }
}

composeRule.onNodeWithText("Cari lokasi WFA").assertIsDisplayed()
composeRule.onNodeWithText("Kopi Rekomendasi").assertDoesNotExist()
```

Keep or add a separate assertion that WFO and WFH do not show the search action.

- [ ] **Step 2: Compile Android tests and verify recommendation is still rendered**

Run:

```powershell
.\gradlew.bat --no-daemon app:compileDebugAndroidTestKotlin --console=plain
```

Expected: test compiles; running it would fail because `WfaRecommendationSection` is still called.

- [ ] **Step 3: Remove recommendation-list ownership from the primary sheet**

Delete `RecommendationSelected` from `AttendancePreparationEvent` and delete:

```kotlin
WfaRecommendationSection(
    model = model.wfaDiscovery,
    onRecommendationSelected = { stableKey ->
        onEvent(AttendancePreparationEvent.RecommendationSelected(stableKey))
    }
)
```

Remove the matching event branch from `AttendanceScreen`. Keep recommendation state in the ViewModel/map; only the bottom-sheet list disappears.

Delete `WfaRecommendationSection.kt` and `WfaRecommendationOption.kt` after confirming with:

```powershell
rg -n 'WfaRecommendationSection|WfaRecommendationOption' app/src
```

that their only production call chain was the removed bottom-sheet list.

- [ ] **Step 4: Replace the custom gradient wrapper with the topbar surface family**

Configure `BottomSheetScaffold`:

```kotlin
sheetContainerColor = InfiniteColors.AttendanceReportGlassSurface,
sheetContentColor = InfiniteColors.Text,
sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
sheetTonalElevation = 8.dp,
```

Remove the nested `Brush.linearGradient`, `Brush.radialGradient`, `.blur(2.dp)`, raw blue `Color(...)`, and redundant full-sheet overlay boxes.

Keep one drag handle:

```kotlin
BottomSheetDefaults.DragHandle(
    color = InfiniteColors.AttendanceReportBodyText.copy(alpha = 0.42f)
)
```

Wrap the sheet content with this one existing-token border:

```kotlin
Modifier.border(
    width = 1.dp,
    color = InfiniteColors.AttendanceReportGlassBorder,
    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
)
```

Do not add any color constant or gradient.

- [ ] **Step 5: Run the Attendance Compose test**

Run:

```powershell
.\gradlew.bat --no-daemon app:connectedDebugAndroidTest `
  -Pandroid.testInstrumentationRunnerArguments.class=com.example.infinite_track.presentation.screen.attendance.WorkModeTargetLocationScreenTest `
  --console=plain
```

Expected: PASS with the search action present only for WFA and the recommendation row absent.

- [ ] **Step 6: Commit Task 5**

```powershell
git add app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceScreen.kt `
  app/src/main/java/com/example/infinite_track/presentation/components/button/attendance/AttendanceBottomSheetContent.kt `
  app/src/androidTest/java/com/example/infinite_track/presentation/screen/attendance/WorkModeTargetLocationScreenTest.kt
git add -u `
  app/src/main/java/com/example/infinite_track/presentation/components/button/attendance/WfaRecommendationSection.kt `
  app/src/main/java/com/example/infinite_track/presentation/components/button/attendance/WfaRecommendationOption.kt
git commit -m "refactor: keep WFA recommendations out of preparation sheet"
```

---

### Task 6: Add category markers, compact callout, and selected-place camera focus

**Files:**
- Create: `app/src/main/java/com/example/infinite_track/presentation/design/tokens/WorkModeVisualTokens.kt`
- Create: `app/src/main/java/com/example/infinite_track/presentation/map/components/CompactMapCallout.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/map/model/MapMarkerUiModel.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/map/mapper/AttendanceMapUiMapper.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/map/adapter/GoogleAttendanceMap.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceViewModel.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceScreen.kt`
- Delete: `app/src/main/java/com/example/infinite_track/presentation/components/maps/MarkerViewWfa.kt`
- Test: `app/src/test/java/com/example/infinite_track/presentation/map/mapper/AttendanceMapUiMapperTest.kt`
- Test: `app/src/test/java/com/example/infinite_track/presentation/design/tokens/WorkModeVisualTokensTest.kt`
- Test: `app/src/test/java/com/example/infinite_track/presentation/screen/attendance/preparation/WfaMapSelectionEffectTest.kt`
- Test: `app/src/androidTest/java/com/example/infinite_track/presentation/map/components/CompactMapCalloutTest.kt`

**Interfaces:**
- Consumes: `WorkMode`, marker role, selected WFA recommendation/search preview.
- Produces:
  - `MapMarkerCategory { CURRENT_LOCATION, WFO, WFH, WFA }`
  - category field on `MapMarkerUiModel`
  - `MapCameraEffect.Focus(zoom = 17f)` for explicit WFA selection
  - anchored compact info-window content.

- [ ] **Step 1: Write failing marker-category and token tests**

Add to `AttendanceMapUiMapperTest`:

```kotlin
@Test
fun `marker categories follow work mode and discovery roles`() {
    val mapped = AttendanceMapUiMapper.map(
        preparation = AttendancePreparationState(
            selectedMode = WorkMode.WFA,
            targetResolution = TargetLocationResolution.Resolved(approvedTarget),
            wfaDiscovery = WfaDiscoveryState.Content(
                recommendations = listOf(
                    recommendation("cafe", -0.90, 119.88)
                ),
                selectedKey = "cafe"
            )
        ),
        hasPreciseLocationPermission = true
    )

    assertEquals(
        MapMarkerCategory.WFA,
        mapped.markers.single { it.role == MapMarkerRole.AUTHORITATIVE_TARGET }.category
    )
    assertEquals(
        MapMarkerCategory.WFA,
        mapped.markers.single { it.role == MapMarkerRole.WFA_RECOMMENDATION }.category
    )
}
```

Create `WorkModeVisualTokensTest.kt`:

```kotlin
package com.example.infinite_track.presentation.design.tokens

import com.example.infinite_track.domain.model.attendance.WorkMode
import com.example.infinite_track.presentation.theme.Blue_500
import com.example.infinite_track.presentation.theme.Blue_Accent_500
import com.example.infinite_track.presentation.theme.Orange_500
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkModeVisualTokensTest {
    @Test fun `WFO uses existing blue`() =
        assertEquals(Blue_500, WorkModeVisualTokens.color(WorkMode.WFO))

    @Test fun `WFH uses existing blue accent`() =
        assertEquals(Blue_Accent_500, WorkModeVisualTokens.color(WorkMode.WFH))

    @Test fun `WFA uses existing orange`() =
        assertEquals(Orange_500, WorkModeVisualTokens.color(WorkMode.WFA))
}
```

- [ ] **Step 2: Run unit tests and verify missing types**

Run:

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest `
  --tests '*AttendanceMapUiMapperTest' `
  --tests '*WorkModeVisualTokensTest' --console=plain
```

Expected: compilation FAIL because `MapMarkerCategory` and `WorkModeVisualTokens` do not exist.

- [ ] **Step 3: Add central existing-color mapping and typed marker category**

Create `WorkModeVisualTokens.kt`:

```kotlin
object WorkModeVisualTokens {
    fun color(mode: WorkMode): Color = when (mode) {
        WorkMode.WFO -> Blue_500
        WorkMode.WFH -> Blue_Accent_500
        WorkMode.WFA -> Orange_500
    }
}
```

In `MapMarkerUiModel.kt` add:

```kotlin
val category: MapMarkerCategory
```

and:

```kotlin
enum class MapMarkerCategory {
    CURRENT_LOCATION,
    WFO,
    WFH,
    WFA
}
```

In `AttendanceMapUiMapper` map:

```kotlin
CURRENT_LOCATION -> MapMarkerCategory.CURRENT_LOCATION
AUTHORITATIVE_TARGET -> target.mode.toMarkerCategory()
WFA_RECOMMENDATION -> MapMarkerCategory.WFA
SEARCH_PREVIEW -> MapMarkerCategory.WFA
```

Use one private exhaustive `WorkMode.toMarkerCategory()` function.

- [ ] **Step 4: Write the failing selection-focus test**

Create a pure helper in the test first:

```kotlin
@Test
fun `selected WFA coordinate produces detail zoom focus`() {
    val effect = WfaMapSelectionEffect.focus(
        id = 7L,
        coordinate = GeoCoordinate(-0.90, 119.88)
    )

    assertEquals(7L, effect.id)
    assertEquals(17f, effect.zoom)
    assertEquals(GeoCoordinate(-0.90, 119.88), effect.coordinate)
}
```

Run:

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest `
  --tests '*WfaMapSelectionEffectTest' --console=plain
```

Expected: compilation FAIL because `WfaMapSelectionEffect` does not exist.

- [ ] **Step 5: Implement and wire selected-place focus**

Add next to the preparation selection helpers:

```kotlin
internal object WfaMapSelectionEffect {
    fun focus(id: Long, coordinate: GeoCoordinate) = MapCameraEffect.Focus(
        id = id,
        coordinate = coordinate,
        zoom = 17f
    )
}
```

After `onWfaMarkerClicked()` successfully updates selection:

```kotlin
_mapCameraEffects.tryEmit(
    WfaMapSelectionEffect.focus(
        id = nextMapCameraEffectId++,
        coordinate = recommendation.coordinate
    )
)
```

After `onLocationSelected()` successfully updates search preview:

```kotlin
val coordinate = GeoCoordinate(location.latitude, location.longitude)
_mapCameraEffects.tryEmit(
    WfaMapSelectionEffect.focus(
        id = nextMapCameraEffectId++,
        coordinate = coordinate
    )
)
```

Keep the latest WFA selection guard before both state mutation and effect emission. Existing `LaunchedEffect(cameraEffect?.id, mapLoaded)` already cancels the superseded animation when a newer effect ID arrives.

- [ ] **Step 6: Build the compact callout and render it as marker info-window content**

Create `CompactMapCallout.kt`:

```kotlin
@Composable
internal fun CompactMapCallout(
    title: String,
    category: MapMarkerCategory,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.testTag("compactMapCallout"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = 220.dp)
                .clip(MaterialTheme.shapes.large)
                .background(InfiniteColors.AttendanceReportGlassSurface)
                .border(
                    1.dp,
                    InfiniteColors.AttendanceReportGlassBorder,
                    MaterialTheme.shapes.large
                )
                .padding(
                    horizontal = InfiniteSpacing.Default.md,
                    vertical = InfiniteSpacing.Default.sm
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.sm)
        ) {
            Icon(
                imageVector = category.icon(),
                contentDescription = null,
                tint = category.color(),
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = InfiniteColors.Text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(
            modifier = Modifier
                .offset(y = (-5).dp)
                .size(12.dp)
                .rotate(45f)
                .background(InfiniteColors.AttendanceReportGlassSurface)
                .border(1.dp, InfiniteColors.AttendanceReportGlassBorder)
        )
    }
}
```

Use category extension functions that reuse `WorkModeVisualTokens`; current location uses an existing neutral/primary token. Do not add a color constant.

In `GoogleAttendanceMap`, replace `Marker` with `MarkerInfoWindow` so the approved pointer and glass container own the complete callout:

```kotlin
LaunchedEffect(marker.isSelected, markerState) {
    if (marker.isSelected) markerState.showInfoWindow()
    else markerState.hideInfoWindow()
}

MarkerInfoWindow(
    state = markerState,
    title = marker.title,
    snippet = marker.snippet,
    icon = rememberMarkerDescriptor(marker.category),
    zIndex = if (marker.isSelected) 2f else 1f,
    onClick = {
        currentOnEvent(AttendanceMapEvent.MarkerClicked(marker))
        true
    }
) {
    CompactMapCallout(
        title = marker.title,
        category = marker.category
    )
}
```

Derive the default marker hue from existing token colors:

```kotlin
@Composable
private fun rememberMarkerDescriptor(category: MapMarkerCategory): BitmapDescriptor {
    val color = category.color()
    return remember(category, color) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(color.toArgb(), hsv)
        BitmapDescriptorFactory.defaultMarker(hsv[0])
    }
}
```

Delete the full-screen `MarkerViewWfa` overlay from `AttendanceScreen` and delete `MarkerViewWfa.kt`. The anchored map info window is now the only WFA marker detail surface.

- [ ] **Step 7: Add and run compact-callout Compose assertions**

Create `CompactMapCalloutTest.kt`:

```kotlin
@Test
fun compact_callout_shows_only_name_and_no_large_metadata() {
    composeRule.setContent {
        Infinite_TrackTheme {
            CompactMapCallout(
                title = "Infinite Track Office Palu",
                category = MapMarkerCategory.WFO
            )
        }
    }

    composeRule.onNodeWithTag("compactMapCallout").assertIsDisplayed()
    composeRule.onNodeWithText("Infinite Track Office Palu").assertIsDisplayed()
    composeRule.onNodeWithText("WFA score", substring = true).assertDoesNotExist()
    composeRule.onNodeWithText("Radius", substring = true).assertDoesNotExist()
}
```

Run:

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest `
  --tests '*AttendanceMapUiMapperTest' `
  --tests '*WorkModeVisualTokensTest' `
  --tests '*WfaMapSelectionEffectTest' --console=plain
.\gradlew.bat --no-daemon app:compileDebugAndroidTestKotlin --console=plain
```

Expected: all unit tests PASS and Android test sources compile.

- [ ] **Step 8: Commit Task 6**

```powershell
git add app/src/main/java/com/example/infinite_track/presentation/design/tokens `
  app/src/main/java/com/example/infinite_track/presentation/map `
  app/src/main/java/com/example/infinite_track/presentation/screen/attendance `
  app/src/test/java/com/example/infinite_track/presentation `
  app/src/androidTest/java/com/example/infinite_track/presentation/map
git add -u app/src/main/java/com/example/infinite_track/presentation/components/maps/MarkerViewWfa.kt
git commit -m "feat: focus compact category markers on WFA selection"
```

---

### Task 7: Complete verification and Cloud/operator handoff

**Files:**
- Create: `docs/verification/inf-238-wfa-search-map-revision.md`
- Modify only if verification finds an in-scope defect: files already listed in Tasks 1-6.

**Interfaces:**
- Consumes: completed Tasks 1-6 and an emulator/device with an Android-restricted key authorized for Maps SDK for Android and Places API (New).
- Produces: fresh automated and runtime evidence, or an explicit `Needs Verification` entry for any unavailable Cloud/device gate.

- [ ] **Step 1: Run source hygiene checks**

Run:

```powershell
git diff develop...HEAD --check
rg -n 'Color\\(0x|Brush\\.(linear|radial)Gradient|isShrinkResources = false' `
  app/src/main/java/com/example/infinite_track/presentation/screen/attendance `
  app/src/main/java/com/example/infinite_track/presentation/map `
  app/build.gradle.kts
rg -n 'RATING|REVIEWS|PHOTO_METADATAS|OPENING_HOURS|WEBSITE_URI' `
  app/src/main/java/com/example/infinite_track/data/location/discovery
```

Expected:

- `git diff --check` has no output.
- no new raw colors/gradients in the touched Attendance/map flow;
- no debug `isShrinkResources = false`;
- no forbidden Places fields.

- [ ] **Step 2: Run the complete automated gate**

Use the configured Java and Android SDK environment:

```powershell
$env:JAVA_HOME = 'D:\Java_Home\java 1.8.2'
$env:ANDROID_HOME = 'C:\Users\Febriyadi\AppData\Local\Android\Sdk'
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
$env:Path = "$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:Path"

.\gradlew.bat --no-daemon app:testDebugUnitTest --console=plain
.\gradlew.bat --no-daemon app:compileDebugAndroidTestKotlin --console=plain
.\gradlew.bat --no-daemon app:lintDebug --console=plain
.\gradlew.bat --no-daemon app:assembleDebug --console=plain
```

Expected: all four commands report `BUILD SUCCESSFUL`. Record unrelated pre-existing warnings separately; do not silently broaden scope.

- [ ] **Step 3: Install and verify runtime behavior**

Run:

```powershell
adb devices -l
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Verify manually:

1. Open Attendance and choose WFA.
2. Confirm the primary bottom sheet contains no recommendation rows.
3. Confirm the sheet surface matches the topbar family and has no blue gradient overlay.
4. Open `Cari lokasi WFA`.
5. Enter one character and confirm no request/loading starts.
6. Enter at least two characters and confirm Indonesian predictions appear.
7. Clear, type rapidly, and confirm only the newest query is rendered.
8. Select a result and confirm Attendance zooms directly to it at detail level.
9. Confirm the compact anchored callout shows only icon and name.
10. Select another recommendation marker and confirm the second selection supersedes the first camera animation.
11. Confirm WFO, WFH, and WFA markers use blue, blue-accent, and orange respectively.
12. Disable network, retry search, and confirm the inline recovery state works.

Do not capture or paste API keys, auth headers, user identifiers, or raw secret-bearing logs.

- [ ] **Step 4: Record verification without secrets**

Create `docs/verification/inf-238-wfa-search-map-revision.md`:

```markdown
# INF-238 WFA Search and Map Revision Verification

## Source

- Branch: `codex/inf-238-wfa-search-revision`
- Base: `develop`

## Automated gates

- `app:testDebugUnitTest`: PASS / FAIL
- `app:compileDebugAndroidTestKotlin`: PASS / FAIL
- `app:lintDebug`: PASS / FAIL
- `app:assembleDebug`: PASS / FAIL

## Runtime

- Device/emulator: [non-sensitive model/API level]
- Main sheet recommendation list removed: PASS / NEEDS VERIFICATION
- WFA search predictions: PASS / NEEDS VERIFICATION
- Latest query wins: PASS / NEEDS VERIFICATION
- Selected result zoom and compact callout: PASS / NEEDS VERIFICATION
- Work Mode marker colors: PASS / NEEDS VERIFICATION
- Offline recovery: PASS / NEEDS VERIFICATION

## Google Cloud prerequisite

- Places API (New) enabled: operator-confirmed / NEEDS VERIFICATION
- Android application restriction matches tested signing certificate: operator-confirmed / NEEDS VERIFICATION
- Maps SDK for Android and Places API (New) authorized on the key: operator-confirmed / NEEDS VERIFICATION
- Budget and per-method quota alerts configured: operator-confirmed / NEEDS VERIFICATION

No API key or secret value is recorded in this document.
```

- [ ] **Step 5: Commit verification evidence**

```powershell
git add docs/verification/inf-238-wfa-search-map-revision.md
git commit -m "test: record INF-238 WFA search verification"
```

- [ ] **Step 6: Review the complete branch**

Run:

```powershell
git status --short
git log --oneline develop..HEAD
git diff --stat develop...HEAD
git diff --check develop...HEAD
```

Expected: clean worktree, focused commits for Tasks 1-7, and no whitespace errors.

Do not push or create a PR until the implementation has passed `superpowers:verification-before-completion` and a code review gate.
