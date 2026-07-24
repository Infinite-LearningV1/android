# Task 2 Report: Explicit Places requests, fields, and sessions

## Outcome

- Added `GooglePlacesRequestFactory` so autocomplete and details request configuration is explicit and JVM-testable.
- Autocomplete requests use query, Indonesia country filter (`ID`), one stable autocomplete session token, optional origin, and an optional 50 km `CircularBounds` location bias. No location restriction is used.
- Place Details requests use the same session token and request only the approved essentials available in Places SDK 3.5.0: `ID`, `ADDRESS`, and `LAT_LNG`.
- Changed the provider-neutral resolve contract to accept the selected `PlaceSuggestion`, allowing autocomplete `primaryText` to supply `PlaceDetails.displayName` without requesting the higher-tier `NAME` field.
- Place Details formatted address uses the provider address when present and falls back to autocomplete `secondaryText`.
- Resolution still renews the session in `finally`; abandonment still renews the session explicitly.

## API compatibility decision

The approved plan examples use newer enum/property names:

- `Place.Field.FORMATTED_ADDRESS`
- `Place.Field.LOCATION`
- `place.formattedAddress`
- `place.location`

Places SDK for Android 3.5.0 does not expose those names. Inspection of the actual 3.5.0 AAR with `javap` confirmed the equivalent API is:

- `Place.Field.ADDRESS`
- `Place.Field.LAT_LNG`
- `place.address`
- `place.latLng`

The implementation uses those exact 3.5.0 equivalents and does not add `NAME`, rating, photo, review, or any other higher-tier field.

## TDD evidence

### RED 1: request factory missing

Command:

```powershell
$env:JAVA_HOME='D:\Java_Home\java 1.8.2'
$env:ANDROID_HOME='C:\Users\Febriyadi\AppData\Local\Android\Sdk'
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
.\gradlew.bat --no-daemon app:testDebugUnitTest --tests '*GooglePlacesRequestFactoryTest' --console=plain
```

Initial result:

- `BUILD FAILED`
- `Unresolved reference: GooglePlacesRequestFactory`
- The first plan-literal test also reported that `FORMATTED_ADDRESS` and `LOCATION` do not exist in SDK 3.5.0.

After changing only the test field names to the verified 3.5.0 equivalents (`ADDRESS`, `LAT_LNG`), the same command was rerun before production code.

Valid RED result:

- `BUILD FAILED in 19s`
- The only compilation failure was `Unresolved reference: GooglePlacesRequestFactory`.

### GREEN 1: request factory

Same focused command after adding the minimal factory:

- `BUILD SUCCESSFUL in 38s`
- `GooglePlacesRequestFactoryTest`: 3 tests, 0 failures, 0 errors, 0 skipped.

### RED 2: provider-neutral selected-suggestion contract

Command:

```powershell
$env:JAVA_HOME='D:\Java_Home\java 1.8.2'
$env:ANDROID_HOME='C:\Users\Febriyadi\AppData\Local\Android\Sdk'
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
.\gradlew.bat --no-daemon app:testDebugUnitTest --tests '*PlaceDiscoveryUseCaseTest' --console=plain
```

Result after changing only the domain test/fake first:

- `BUILD FAILED in 22s`
- `Type mismatch: inferred type is PlaceSuggestion but String was expected`
- Fake repository did not implement the old `resolve(placeId: String)` contract.
- The new `resolve(suggestion: PlaceSuggestion)` override did not yet exist in production.

### GREEN 2: selected-suggestion contract

Same focused command after the minimal contract/use-case/repository/call-site migration:

- `BUILD SUCCESSFUL in 49s`
- `PlaceDiscoveryUseCaseTest`: 4 tests, 0 failures, 0 errors, 0 skipped.

The two session-manager tests are characterization coverage for the already-correct stable-token and renew behavior required by the brief.

## Final verification

Focused test command:

```powershell
$env:JAVA_HOME='D:\Java_Home\java 1.8.2'
$env:ANDROID_HOME='C:\Users\Febriyadi\AppData\Local\Android\Sdk'
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
.\gradlew.bat --no-daemon app:testDebugUnitTest `
  --tests '*GooglePlacesRequestFactoryTest' `
  --tests '*GooglePlacesSessionManagerTest' `
  --tests '*PlaceDiscoveryUseCaseTest' --console=plain
```

Result:

- `BUILD SUCCESSFUL in 20s`
- 9 focused tests total, 0 failures, 0 errors, 0 skipped:
  - `GooglePlacesRequestFactoryTest`: 3
  - `GooglePlacesSessionManagerTest`: 2
  - `PlaceDiscoveryUseCaseTest`: 4

Compile command:

```powershell
$env:JAVA_HOME='D:\Java_Home\java 1.8.2'
$env:ANDROID_HOME='C:\Users\Febriyadi\AppData\Local\Android\Sdk'
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
.\gradlew.bat --no-daemon app:compileDebugKotlin --console=plain
```

Result:

- `BUILD SUCCESSFUL in 13s`
- `20 actionable tasks: 20 up-to-date`

Additional checks:

- `git diff --check`: no whitespace errors.
- Search for old `resolve(placeId: String)` and `resolvePlaceDetails(suggestion.placeId)` call sites: none remain.
- Search for production requests of `NAME`, `RATING`, `PHOTO_METADATAS`, or `REVIEWS`: none.
- Manual lifecycle inspection confirmed `sessions.current()` is shared by search/details, `sessions.renew()` remains in resolve `finally`, and `abandonSession()` renews.

## Files changed

- `app/src/main/java/com/example/infinite_track/data/location/discovery/GooglePlacesRequestFactory.kt`
- `app/src/main/java/com/example/infinite_track/data/location/discovery/GooglePlacesDataSource.kt`
- `app/src/main/java/com/example/infinite_track/data/location/discovery/GooglePlacesMapper.kt`
- `app/src/main/java/com/example/infinite_track/data/location/discovery/GooglePlacesDiscoveryRepository.kt`
- `app/src/main/java/com/example/infinite_track/domain/repository/location/PlaceDiscoveryRepository.kt`
- `app/src/main/java/com/example/infinite_track/domain/use_case/location/ResolvePlaceDetailsUseCase.kt`
- `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/search/SearchViewModel.kt`
- `app/src/test/java/com/example/infinite_track/data/location/discovery/GooglePlacesRequestFactoryTest.kt`
- `app/src/test/java/com/example/infinite_track/data/location/discovery/GooglePlacesSessionManagerTest.kt`
- `app/src/test/java/com/example/infinite_track/domain/use_case/location/PlaceDiscoveryUseCaseTest.kt`
- `.superpowers/sdd/task-2-report.md`

`SearchViewModel.kt` is an additional directly affected call site: after the provider-neutral contract changed, it had to pass the selected `PlaceSuggestion` instead of only `suggestion.placeId` for the app to compile and for autocomplete display evidence to reach details mapping.

## Commit

- Implementation: `172a7ff refactor: make Places search sessions cost aware`

## Self-review

- Scope remains limited to Task 2 request construction, selected-suggestion evidence flow, and session lifecycle coverage.
- Domain types and repository contract remain provider-neutral; Google SDK types stay in the data layer.
- Details field set is exact and minimal for SDK 3.5.0: `ID`, `ADDRESS`, `LAT_LNG`.
- Autocomplete uses a bias, not a restriction, so valid Indonesian results are not hidden by proximity.
- Cancellation token propagation remains intact for both autocomplete and details.
- Resolution renews the token on success, provider failure, invalid mapped place data, and coroutine cancellation because renewal remains in `finally`.
- No secret-bearing files, build configuration, backend contract, navigation structure, or release workflow were changed.
- No unrelated worktree changes were present or included.

## Concerns / needs verification

- Runtime Places behavior still needs emulator/device verification with a valid restricted Maps Platform key and network access. This task provides JVM request-contract coverage and Kotlin compile evidence only.
- Gradle reports pre-existing deprecation warnings about Gradle 9 compatibility; they did not fail the focused tests or compile.
- The SDK 3.5.0 enum/property names differ from newer documentation examples, but the implementation is pinned to and compiled against the actual 3.5.0 API as required.
