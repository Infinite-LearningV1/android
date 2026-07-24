# Task 3 Report: Controlled latest-query-wins search

## Outcome

- `SearchViewModel` is now the single owner of the WFA search query and visible search state.
- Valid queries wait 400 ms before searching.
- A new query immediately cancels the superseded debounce or in-flight search through `flatMapLatest`.
- Queries shorter than two characters return to `Idle` and do not call the repository.
- Retry work is independently owned and is cancelled when the query changes, search is cleared, or the ViewModel is cleared.
- Selection resolution remains cancellable and passes the complete provider-neutral `PlaceSuggestion`.
- `clearSearch()` and `onCleared()` retain Places session abandonment.
- `InfiniteTrackSearchBar` no longer keeps a remembered duplicate of external `value`.
- The controlled search bar renders a trailing clear action only when the supplied value is non-empty.

## TDD evidence

### RED

Command:

```powershell
$env:JAVA_HOME='D:\Java_Home\java 1.8.2'
$env:ANDROID_HOME='C:\Users\Febriyadi\AppData\Local\Android\Sdk'
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
.\gradlew.bat --no-daemon app:testDebugUnitTest `
  --tests '*SearchViewModelTest' --console=plain
```

Pre-production result:

- `BUILD FAILED in 22s`
- 5 tests executed, 4 failed.
- Failures captured the current 500 ms debounce and missing latest-query/retry behavior:
  - `valid query waits four hundred milliseconds`
  - `newer query supersedes older unfinished result`
  - `query change cancels an unfinished retry`
  - `selection resolves complete suggestion and emits selected location`

The short-query test already passed and served as characterization coverage.

### GREEN

The same focused command after the minimal implementation:

- `BUILD SUCCESSFUL in 43s`
- 5 tests passed.

An additional lifecycle regression test was then added for clear/cancel/session-abandonment, bringing `SearchViewModelTest` to 6 tests.

## Final verification

Focused ViewModel and provider-neutral domain command:

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest `
  --tests '*SearchViewModelTest' `
  --tests '*PlaceDiscoveryUseCaseTest' --console=plain
```

Result:

- `BUILD SUCCESSFUL in 26s`
- `SearchViewModelTest`: 6 tests passed.
- `PlaceDiscoveryUseCaseTest`: 4 tests passed.

Compile command:

```powershell
.\gradlew.bat --no-daemon app:compileDebugKotlin --console=plain
```

Result:

- `BUILD SUCCESSFUL in 14s`
- 20 actionable tasks were up-to-date.

Additional checks:

- `git diff --check`: no whitespace errors.
- No `remember` or `mutableStateOf` remains in `InfiniteTrackSearchBar`.
- No provider SDK type was introduced into presentation or domain contracts.

## Files changed

- `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/search/SearchViewModel.kt`
- `app/src/main/java/com/example/infinite_track/presentation/components/search/InfiniteTrackSearchBar.kt`
- `app/src/test/java/com/example/infinite_track/presentation/screen/attendance/search/SearchViewModelTest.kt`
- `.superpowers/sdd/task-3-report.md`

## Commit

- `5356af7 fix: make WFA location search latest query wins`

## Self-review

- Scope is limited to Task 3 query/state ownership and the shared controlled search field.
- Search cancellation happens as soon as a distinct new query is observed; the old request is not allowed to remain active during the new query's debounce window.
- Retry does not mutate the query and cannot overwrite a newer query after cancellation.
- Selection continues to emit the existing provider-neutral `LocationResult`.
- Existing theme colors, shape, and typography behavior are preserved.
- No secret-bearing, build-version, manifest, backend, navigation, or unrelated UI file was changed.

## Fact

- Focused unit tests and Kotlin compile pass on the isolated branch.
- The production/test commit is `5356af7`.

## Assumption

- The screen-level `onClear` wiring belongs to the next explicit-search UI task; Task 3 exposes the controlled callback without changing `LocationSearchScreen`.

## Mismatch

- None against the locked Task 3 contract.

## Risk

- This is an Attendance UI-flow change. JVM tests verify timing and cancellation, but they do not verify IME interaction, navigation restoration, or device rendering.
- Gradle reports existing deprecation warnings. The focused implementation did not add a build failure.

## Needs Verification

- Emulator/device verification of typing, clear, retry, rapid query replacement, and selection navigation remains required by the repo runtime gate.
- Full `app:test` and `app:lint` are deferred to the complete-branch verification task.

## Recommendation

- In Task 4, wire `LocationSearchScreen` to `onClear = viewModel::clearSearch` and exercise the complete explicit WFA search flow on emulator/device.

## Docs / ADR note

- No ADR update is required. This implementation applies the already approved INF-238 spec and does not change backend, authoritative target, navigation shell, or release contracts.
