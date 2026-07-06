# Task 3 Report: Rewire terminal auth callers and apply `/me` freshness

## Fact
- Implemented shared `/me` profile freshness in `C:/Users/Febriyadi/.claude/worktrees/android-android-inf-207-auth-status-runtime/app/src/main/java/com/example/infinite_track/domain/use_case/auth/CheckSessionUseCase.kt`.
- `CheckSessionUseCase` now validates refresh first, then returns the fresh local user without calling bootstrap `/me` when `UserPreference.getLastProfileSyncAt()` is within `AuthRuntimePolicy.SHARED_TTL_MILLIS` and a local user exists.
- `CheckSessionUseCase` now saves `lastProfileSyncAt` after successful bootstrap profile sync, including success returned through the existing unauthorized/refresh retry path.
- Implemented foreground freshness in `C:/Users/Febriyadi/.claude/worktrees/android-android-inf-207-auth-status-runtime/app/src/main/java/com/example/infinite_track/domain/use_case/auth/ValidateForegroundSessionUseCase.kt`.
- `ValidateForegroundSessionUseCase` now skips foreground `/me` sync and returns `ForegroundSessionValidationResult.Valid` while `lastProfileSyncAt` is fresh, after checking existing forced reauth state, tokens, and bootstrap-in-progress state.
- `ValidateForegroundSessionUseCase` now saves `lastProfileSyncAt` after successful foreground profile sync.
- Confirmed the terminal auth caller rewiring already present from the reviewed Task 2 follow-up remains in place:
  - `C:/Users/Febriyadi/.claude/worktrees/android-android-inf-207-auth-status-runtime/app/src/main/java/com/example/infinite_track/di/NetworkModule.kt`
  - `C:/Users/Febriyadi/.claude/worktrees/android-android-inf-207-auth-status-runtime/app/src/main/java/com/example/infinite_track/di/auth/AuthRefreshInterceptor.kt`
  - `C:/Users/Febriyadi/.claude/worktrees/android-android-inf-207-auth-status-runtime/app/src/main/java/com/example/infinite_track/presentation/main/ForegroundSessionLifecycleObserver.kt`
  - `C:/Users/Febriyadi/.claude/worktrees/android-android-inf-207-auth-status-runtime/app/src/main/java/com/example/infinite_track/presentation/screen/splash/SplashViewModel.kt`
- Updated focused tests:
  - `C:/Users/Febriyadi/.claude/worktrees/android-android-inf-207-auth-status-runtime/app/src/test/java/com/example/infinite_track/di/auth/AuthRefreshInterceptorTest.kt`
  - `C:/Users/Febriyadi/.claude/worktrees/android-android-inf-207-auth-status-runtime/app/src/test/java/com/example/infinite_track/domain/use_case/auth/CheckSessionUseCaseTest.kt`
  - `C:/Users/Febriyadi/.claude/worktrees/android-android-inf-207-auth-status-runtime/app/src/test/java/com/example/infinite_track/domain/use_case/auth/ValidateForegroundSessionUseCaseTest.kt`
- Created commit `d89a16b feat: apply auth runtime freshness and reauth wiring`.

## Assumption
- The brief's caller-rewiring examples are satisfied by the existing branch state from the reviewed Task 2 follow-up, so Task 3 implementation only needed to preserve that wiring and add the missing `/me` freshness behavior/tests.
- Refresh validation before freshness is intentional because the brief explicitly orders `validateRefreshSessionIfAvailable()?.let { return it }` before `freshLocalUserOrNull()`.
- Returning a fresh local profile skips face-embedding generation during bootstrap; this matches the brief's requirement that `CheckSessionUseCase` can return fresh local user without calling `/me`.

## Mismatch
- The Task 3 brief listed `NetworkModule.kt`, `AuthRefreshInterceptor.kt`, `ForegroundSessionLifecycleObserver.kt`, and `SplashViewModel.kt` as modify targets, but those rewires were already present before this Task 3 diff. I left them unchanged after confirming they already use `ForceReauthUseCase`.
- The brief's focused test list did not include `CheckSessionUseCaseTest`, but the requirements explicitly say `CheckSessionUseCase` must be able to return a fresh local user without calling `/me`; I added a focused test there because it is the direct owner of that behavior.
- `SplashViewModelTest` remains class-level skipped via existing `@Ignore`; the focused Gradle command compiles it but reports it skipped.

## Risk
- Auth/session runtime is high-risk. JVM unit tests verify the orchestration logic, but emulator/device/backend runtime validation is still required before claiming full runtime Done.
- Freshness uses `System.currentTimeMillis()` directly, matching the task brief, but this remains wall-clock based and can be affected by device time changes.
- The current worktree still has unrelated/uncommitted changes outside this Task 3 commit after commit creation. I did not include or revert them because they were outside the Task 3 files I changed.

## Needs Verification
- Needs Verification: emulator/device runtime auth/session flow. No emulator/device or Maestro runtime evidence was collected for Task 3.
- Needs Verification: full quality gate (`./gradlew app:lint` and `./gradlew app:test`) was not run; only focused Task 3 unit coverage was run.
- Needs Verification: `SplashViewModelTest` remains skipped by existing `@Ignore` and therefore did not execute despite being included in the focused command.

## Recommendation
- Run emulator/device login/session bootstrap and foreground-resume validation before closing INF-207 runtime behavior.
- Run full `./gradlew app:test` and `./gradlew app:lint` once the worktree is clean or after coordinating the unrelated pending changes.
- Consider replacing direct wall-clock calls with an injected clock in a later cleanup if deterministic runtime testing becomes important.

## Affected files/areas
- `C:/Users/Febriyadi/.claude/worktrees/android-android-inf-207-auth-status-runtime/app/src/main/java/com/example/infinite_track/domain/use_case/auth/CheckSessionUseCase.kt`
- `C:/Users/Febriyadi/.claude/worktrees/android-android-inf-207-auth-status-runtime/app/src/main/java/com/example/infinite_track/domain/use_case/auth/ValidateForegroundSessionUseCase.kt`
- `C:/Users/Febriyadi/.claude/worktrees/android-android-inf-207-auth-status-runtime/app/src/test/java/com/example/infinite_track/di/auth/AuthRefreshInterceptorTest.kt`
- `C:/Users/Febriyadi/.claude/worktrees/android-android-inf-207-auth-status-runtime/app/src/test/java/com/example/infinite_track/domain/use_case/auth/CheckSessionUseCaseTest.kt`
- `C:/Users/Febriyadi/.claude/worktrees/android-android-inf-207-auth-status-runtime/app/src/test/java/com/example/infinite_track/domain/use_case/auth/ValidateForegroundSessionUseCaseTest.kt`

## Verification plan executed
1. Read Task 3 brief and current implementation/tests.
2. Added focused failing tests for interceptor forced reauth assertion and foreground/bootstrap profile freshness.
3. Ran focused Task 3 test set before implementation.
4. Implemented shared freshness in bootstrap and foreground validation.
5. Ran focused Task 3 test set after implementation.
6. Ran `git diff --check` before commit.
7. Self-reviewed the diff manually and committed only the Task 3 scoped files.

## Verification evidence
- Expected failing pre-implementation command:
  - `JAVA_HOME="C:\Program Files\Android\Android Studio\jbr" JAVA_TOOL_OPTIONS="--add-exports=..." ./gradlew --no-daemon -Pkotlin.compiler.execution.strategy=in-process app:testDebugUnitTest --tests com.example.infinite_track.di.auth.AuthRefreshInterceptorTest --tests com.example.infinite_track.presentation.main.ForegroundSessionLifecycleObserverTest --tests com.example.infinite_track.presentation.screen.splash.SplashViewModelTest --tests com.example.infinite_track.domain.use_case.auth.ValidateForegroundSessionUseCaseTest --tests com.example.infinite_track.data.repository.auth.AuthRepositoryImplRefreshSessionTest --tests com.example.infinite_track.domain.use_case.auth.CheckSessionUseCaseTest`
- Expected failing result:
  - `BUILD FAILED in 41s`
  - `82 tests completed, 2 failed, 2 skipped`
  - Failed tests were the newly added freshness tests in `CheckSessionUseCaseTest` and `ValidateForegroundSessionUseCaseTest`.
- Passing post-implementation command:
  - `JAVA_HOME="C:\Program Files\Android\Android Studio\jbr" JAVA_TOOL_OPTIONS="--add-exports=..." ./gradlew --no-daemon -Pkotlin.compiler.execution.strategy=in-process app:testDebugUnitTest --tests com.example.infinite_track.di.auth.AuthRefreshInterceptorTest --tests com.example.infinite_track.presentation.main.ForegroundSessionLifecycleObserverTest --tests com.example.infinite_track.presentation.screen.splash.SplashViewModelTest --tests com.example.infinite_track.domain.use_case.auth.ValidateForegroundSessionUseCaseTest --tests com.example.infinite_track.data.repository.auth.AuthRepositoryImplRefreshSessionTest --tests com.example.infinite_track.domain.use_case.auth.CheckSessionUseCaseTest`
- Passing result:
  - `BUILD SUCCESSFUL in 1m 49s`
  - `34 actionable tasks: 11 executed, 23 up-to-date`
- `git diff --check` produced no whitespace errors.

## Self-review verdict
- Scope stayed limited to Task 3 caller/freshness requirements.
- Task 4 status cache behavior was not implemented.
- No auth-bearing secrets or runtime tokens were copied into this report.
- Automated code-review subagent launch was interrupted/not available, so self-review was performed manually against the full Task 3 diff.

## Docs / ADR note
- No ADR update was made. The change uses existing `AuthRuntimePolicy.SHARED_TTL_MILLIS` and Task 1/2 interfaces, and does not change backend contract or release policy.

## PR / release notes
- Bootstrap and foreground session validation now reuse fresh local profile state for the shared auth runtime TTL instead of always calling `/me`.
- Successful bootstrap/foreground profile sync updates `lastProfileSyncAt`.
- Terminal auth caller tests now assert forced reauth path does not call remote logout.

## Task 3 fix addendum
- Moved `CheckSessionUseCase.saveLastProfileSyncAt(...)` to the end of the successful bootstrap path, after any required face-embedding recovery finishes.
- Added a focused regression test in `CheckSessionUseCaseTest` to assert freshness is not persisted when embedding recovery cannot complete.
- Verification rerun with the JDK workaround was blocked by an unrelated existing compile error in `SplashViewModelBootstrapReauthTest` (`MainDispatcherRule` unresolved), so the changed test group could not be fully re-executed in this worktree.
