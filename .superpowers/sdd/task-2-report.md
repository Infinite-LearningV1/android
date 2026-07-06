# Task 2 Report: Add local runtime cleanup and split remote logout

## Fact
- Menambahkan `ClearAuthenticatedRuntimeUseCase` di `C:/Users/Febriyadi/.claude/worktrees/android-android-inf-207-auth-status-runtime/app/src/main/java/com/example/infinite_track/domain/use_case/auth/ClearAuthenticatedRuntimeUseCase.kt` untuk membersihkan auth token, profile Room, cache today status, runtime attendance state, dan geofence aktif.
- Menambahkan `ForceReauthUseCase` di `C:/Users/Febriyadi/.claude/worktrees/android-android-inf-207-auth-status-runtime/app/src/main/java/com/example/infinite_track/domain/use_case/auth/ForceReauthUseCase.kt` dengan single-flight guard lewat `SessionManager.beginSessionExpiryHandling()` lalu cleanup lokal sebelum `triggerForcedReauth(reason)`.
- Memisahkan boundary repository auth di `C:/Users/Febriyadi/.claude/worktrees/android-android-inf-207-auth-status-runtime/app/src/main/java/com/example/infinite_track/domain/repository/AuthRepository.kt` dengan kontrak baru `logoutRemote(): Result<Unit>` dan mempertahankan `logout()` sebagai jalur deprecated untuk kompatibilitas sementara.
- Mengubah `AuthRepositoryImpl` di `C:/Users/Febriyadi/.claude/worktrees/android-android-inf-207-auth-status-runtime/app/src/main/java/com/example/infinite_track/data/repository/auth/AuthRepositoryImpl.kt` agar logout repository hanya melakukan remote logout best-effort / result-return tanpa cleanup runtime lokal.
- Mengubah `LogoutUseCase` di `C:/Users/Febriyadi/.claude/worktrees/android-android-inf-207-auth-status-runtime/app/src/main/java/com/example/infinite_track/domain/use_case/auth/LogoutUseCase.kt` agar semantik logout user menjadi: panggil `logoutRemote()`, lalu selalu coba cleanup lokal, dan hanya gagal jika cleanup lokal gagal.
- Menambahkan wiring Hilt untuk cleanup use case dan konstruktor `LogoutUseCase` baru di `C:/Users/Febriyadi/.claude/worktrees/android-android-inf-207-auth-status-runtime/app/src/main/java/com/example/infinite_track/di/UseCaseModule.kt`.
- Menambahkan focused unit tests untuk boundary baru:
  - `C:/Users/Febriyadi/.claude/worktrees/android-android-inf-207-auth-status-runtime/app/src/test/java/com/example/infinite_track/domain/use_case/auth/ClearAuthenticatedRuntimeUseCaseTest.kt`
  - `C:/Users/Febriyadi/.claude/worktrees/android-android-inf-207-auth-status-runtime/app/src/test/java/com/example/infinite_track/domain/use_case/auth/ForceReauthUseCaseTest.kt`
  - `C:/Users/Febriyadi/.claude/worktrees/android-android-inf-207-auth-status-runtime/app/src/test/java/com/example/infinite_track/domain/use_case/auth/LogoutUseCaseTest.kt`
- Menyelaraskan repository regression tests yang terdampak oleh split boundary logout:
  - `C:/Users/Febriyadi/.claude/worktrees/android-android-inf-207-auth-status-runtime/app/src/test/java/com/example/infinite_track/data/repository/auth/AuthRepositoryImplLogoutFallbackTest.kt`
  - `C:/Users/Febriyadi/.claude/worktrees/android-android-inf-207-auth-status-runtime/app/src/test/java/com/example/infinite_track/data/repository/auth/AuthRepositoryImplRefreshSessionTest.kt`

## Assumption
- Menjaga `logout()` sebagai deprecated compatibility shim masih dibutuhkan karena caller rewiring penuh memang ditunda ke Task 3.
- Konstruktor test-friendly pada `ClearAuthenticatedRuntimeUseCase` yang menerima lambda `removeAllGeofences` dapat dipertahankan supaya focused JVM tests tetap sederhana tanpa memperluas scope ke perubahan DI lain.
- Focused verification untuk Task 2 cukup di level JVM unit tests; tidak ada kebutuhan emulator/device karena task ini hanya mengubah orchestration auth runtime lokal dan boundary repository.

## Mismatch
- Brief Task 2 hanya mencantumkan tiga focused tests baru, tetapi repository regression tests lama masih mengasumsikan `AuthRepositoryImpl.logout()` ikut membersihkan state lokal. Test tersebut harus diselaraskan agar sesuai boundary baru dan tidak menjadi false regression.
- Default-method dua arah antara `logoutRemote()` dan `logout()` berpotensi rekursif jika suatu implementasi/fake tidak meng-override salah satunya. Kontrak diubah agar hanya `logoutRemote()` yang punya default bridge ke `logout()`, sedangkan `logout()` kembali abstract/deprecated supaya tidak ada recursion trap.

## Risk
- Masih ada beberapa warning test-suite lama terkait override deprecated `logout()` pada fake repository lain, tetapi focused Task 2 tests tetap lulus dan warning tersebut bukan blocker fungsional untuk task ini.
- Karena caller rewiring global memang ditahan untuk Task 3, sebagian area runtime masih bisa memakai `LogoutUseCase(authRepository)` / `logout()` compatibility path dalam test atau wiring lama sampai task berikutnya merapikannya.

## Needs Verification
- Needs Verification untuk runtime emulator/device/backend: perubahan auth/session runtime seperti ini tetap memerlukan verifikasi emulator/device dan, bila relevan, backend/session evidence; belum ada runtime evidence yang dijalankan di task ini.
- Jika ingin confidence lebih luas sebelum merge, jalankan subset auth/repository tests tambahan atau full `app:testDebugUnitTest` setelah Task 3 caller rewiring selesai, karena saat ini verifikasi difokuskan pada boundary Task 2.

## Recommendation
- Lanjutkan Task 3 untuk mengganti caller forced-reauth agar memakai `ForceReauthUseCase` dan bukan lagi cleanup/logout lama.
- Setelah Task 3, pertimbangkan merapikan fake repos yang masih override deprecated `logout()` agar warning suite berkurang dan boundary baru lebih eksplisit.
- Pertahankan `logoutRemote()` sebagai satu-satunya boundary repository untuk server-side session invalidation; semua cleanup lokal sebaiknya tetap berada di use case layer.

## Affected files/areas
- `C:/Users/Febriyadi/.claude/worktrees/android-android-inf-207-auth-status-runtime/app/src/main/java/com/example/infinite_track/domain/use_case/auth/ClearAuthenticatedRuntimeUseCase.kt`
- `C:/Users/Febriyadi/.claude/worktrees/android-android-inf-207-auth-status-runtime/app/src/main/java/com/example/infinite_track/domain/use_case/auth/ForceReauthUseCase.kt`
- `C:/Users/Febriyadi/.claude/worktrees/android-android-inf-207-auth-status-runtime/app/src/main/java/com/example/infinite_track/domain/repository/AuthRepository.kt`
- `C:/Users/Febriyadi/.claude/worktrees/android-android-inf-207-auth-status-runtime/app/src/main/java/com/example/infinite_track/data/repository/auth/AuthRepositoryImpl.kt`
- `C:/Users/Febriyadi/.claude/worktrees/android-android-inf-207-auth-status-runtime/app/src/main/java/com/example/infinite_track/domain/use_case/auth/LogoutUseCase.kt`
- `C:/Users/Febriyadi/.claude/worktrees/android-android-inf-207-auth-status-runtime/app/src/main/java/com/example/infinite_track/di/UseCaseModule.kt`
- `C:/Users/Febriyadi/.claude/worktrees/android-android-inf-207-auth-status-runtime/app/src/test/java/com/example/infinite_track/domain/use_case/auth/ClearAuthenticatedRuntimeUseCaseTest.kt`
- `C:/Users/Febriyadi/.claude/worktrees/android-android-inf-207-auth-status-runtime/app/src/test/java/com/example/infinite_track/domain/use_case/auth/ForceReauthUseCaseTest.kt`
- `C:/Users/Febriyadi/.claude/worktrees/android-android-inf-207-auth-status-runtime/app/src/test/java/com/example/infinite_track/domain/use_case/auth/LogoutUseCaseTest.kt`
- `C:/Users/Febriyadi/.claude/worktrees/android-android-inf-207-auth-status-runtime/app/src/test/java/com/example/infinite_track/data/repository/auth/AuthRepositoryImplLogoutFallbackTest.kt`
- `C:/Users/Febriyadi/.claude/worktrees/android-android-inf-207-auth-status-runtime/app/src/test/java/com/example/infinite_track/data/repository/auth/AuthRepositoryImplRefreshSessionTest.kt`

## Verification plan executed
1. Review brief Task 2 dan perubahan in-progress yang sudah ada di working tree.
2. Validasi bentuk boundary baru pada repository, logout use case, cleanup use case, force reauth use case, dan wiring Hilt.
3. Menutup gap kontrak default method pada `AuthRepository` agar tidak ada recursion trap.
4. Menyelaraskan fake repository / repository regression tests yang terdampak split logout.
5. Menjalankan focused Task 2 unit tests dengan workaround JDK/KAPT yang sudah terbukti dari Task 1.
6. Menjalankan `git diff --check` untuk memastikan tidak ada whitespace error.

## Verification evidence
- Passed command:
  - `JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" JAVA_TOOL_OPTIONS="--add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.jvm=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.model=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED" ./gradlew --no-daemon -Pkotlin.compiler.execution.strategy=in-process app:testDebugUnitTest --tests "com.example.infinite_track.domain.use_case.auth.ClearAuthenticatedRuntimeUseCaseTest" --tests "com.example.infinite_track.domain.use_case.auth.ForceReauthUseCaseTest" --tests "com.example.infinite_track.domain.use_case.auth.LogoutUseCaseTest" --tests "com.example.infinite_track.data.repository.auth.AuthRepositoryImplLogoutFallbackTest" --tests "com.example.infinite_track.data.repository.auth.AuthRepositoryImplRefreshSessionTest.logout remote uses auth session api service without clearing local state"`
- Result: `BUILD SUCCESSFUL in 2m 2s`; `34 actionable tasks: 11 executed, 23 up-to-date`.
- `git diff --check` completed with no output.

## Self-review verdict
- Scope tetap terbatas pada Task 2: cleanup runtime lokal, split remote logout, dan regression coverage yang langsung terdampak.
- Tidak melakukan Task 3 caller rewiring.
- Tidak menyentuh secret-bearing config, network environment policy, atau release workflow.
- File untracked `docs/superpowers/plans/2026-07-05-inf-207-auth-status-runtime.md` sudah ada sebelumnya dan tidak disentuh / tidak akan di-commit sebagai bagian Task 2.

## Docs / ADR note
- Tidak ada update docs/ADR pada task ini. Perubahan hanya memindahkan orchestration cleanup/logout di layer Android dan tidak mengubah kontrak backend refresh/logout, attendance source-of-truth, navigation shell, atau release process.

## PR / release notes
- Memisahkan server logout dari cleanup runtime lokal.
- Menambahkan use case khusus untuk cleanup authenticated runtime dan forced reauth.
- Menjadikan logout user-intent selalu membersihkan state lokal meski remote logout gagal.
- Menambahkan focused regression coverage untuk boundary auth runtime baru.

## Commit note
- Created commit `239221d feat: separate forced reauth from logout`.

---

# Task 2 Critical Review Fix: Forced reauth must not call backend logout

## Fact
- Fixed the forced-reauth caller gap flagged in review by replacing `LogoutUseCase` with `ForceReauthUseCase` at:
  - `C:/Users/Febriyadi/.claude/worktrees/android-android-inf-207-auth-status-runtime/app/src/main/java/com/example/infinite_track/di/auth/AuthRefreshInterceptor.kt`
  - `C:/Users/Febriyadi/.claude/worktrees/android-android-inf-207-auth-status-runtime/app/src/main/java/com/example/infinite_track/presentation/main/ForegroundSessionLifecycleObserver.kt`
  - `C:/Users/Febriyadi/.claude/worktrees/android-android-inf-207-auth-status-runtime/app/src/main/java/com/example/infinite_track/presentation/screen/splash/SplashViewModel.kt`
- Updated `C:/Users/Febriyadi/.claude/worktrees/android-android-inf-207-auth-status-runtime/app/src/main/java/com/example/infinite_track/di/NetworkModule.kt` so `AuthRefreshInterceptor` receives `Provider<ForceReauthUseCase>` instead of `Provider<LogoutUseCase>` / `Provider<SessionManager>`.
- Kept `ForceReauthUseCase` as the forced-reauth boundary and added a test-friendly function constructor while preserving the Hilt constructor that uses `ClearAuthenticatedRuntimeUseCase`.
- Updated focused tests so forced reauth asserts local runtime cleanup / forced reauth state and remote logout call count stays zero where applicable.

## Assumption
- Keeping the existing `SplashBootstrapGate.runTerminalLogoutIfOwner` method name is acceptable for this review fix because changing the gate naming would be broader Task 3 cleanup; its body now invokes `ForceReauthUseCase`, not logout.
- The internal function constructor on `ForceReauthUseCase` is acceptable as a deterministic JVM-test seam and does not change production injection or runtime behavior.

## Mismatch
- Previous Task 2 report said Task 3 would handle caller rewiring, but this critical review finding required rewiring the three forced-reauth callers now to satisfy the no-backend-logout constraint.
- `SplashViewModelTest` is still class-level `@Ignore`, so Gradle compiles it and reports it as skipped rather than executed.

## Risk
- Forced reauth now depends on `ForceReauthUseCase` single-flight semantics consistently at these three call sites; this is intended and avoids duplicate local cleanup / duplicate dialog triggering.
- Existing suite warnings about deprecated auth fake overrides and coroutine test opt-ins remain; they are pre-existing/non-blocking for this focused fix.

## Needs Verification
- Runtime emulator/device/backend verification: Needs Verification for this fix because auth/session runtime behavior still requires emulator/device evidence, and no runtime evidence was collected for this task.
- `SplashViewModelTest`: compiled but skipped due existing `@Ignore("Requires Android Main looper in JVM; bootstrap behavior is covered by CheckSessionUseCaseTest and SplashBootstrapGateTest.")`; practical execution remains blocked by that existing test annotation/environment caveat.

## Recommendation
- Continue Task 3 only for broader auth freshness/caller cleanup after this boundary fix; do not reintroduce backend logout into forced-reauth paths.
- Consider renaming splash gate terminology in Task 3 if desired, but keep behavior routed through `ForceReauthUseCase`.

## Affected files/areas
- `C:/Users/Febriyadi/.claude/worktrees/android-android-inf-207-auth-status-runtime/app/src/main/java/com/example/infinite_track/di/NetworkModule.kt`
- `C:/Users/Febriyadi/.claude/worktrees/android-android-inf-207-auth-status-runtime/app/src/main/java/com/example/infinite_track/di/auth/AuthRefreshInterceptor.kt`
- `C:/Users/Febriyadi/.claude/worktrees/android-android-inf-207-auth-status-runtime/app/src/main/java/com/example/infinite_track/domain/use_case/auth/ForceReauthUseCase.kt`
- `C:/Users/Febriyadi/.claude/worktrees/android-android-inf-207-auth-status-runtime/app/src/main/java/com/example/infinite_track/presentation/main/ForegroundSessionLifecycleObserver.kt`
- `C:/Users/Febriyadi/.claude/worktrees/android-android-inf-207-auth-status-runtime/app/src/main/java/com/example/infinite_track/presentation/screen/splash/SplashViewModel.kt`
- `C:/Users/Febriyadi/.claude/worktrees/android-android-inf-207-auth-status-runtime/app/src/test/java/com/example/infinite_track/di/auth/AuthRefreshInterceptorTest.kt`
- `C:/Users/Febriyadi/.claude/worktrees/android-android-inf-207-auth-status-runtime/app/src/test/java/com/example/infinite_track/presentation/main/ForegroundSessionLifecycleObserverTest.kt`
- `C:/Users/Febriyadi/.claude/worktrees/android-android-inf-207-auth-status-runtime/app/src/test/java/com/example/infinite_track/presentation/screen/splash/SplashViewModelTest.kt`

## Verification evidence
- Passed focused command:
  - `JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" JAVA_TOOL_OPTIONS="--add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.jvm=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.model=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED" ./gradlew --no-daemon -Pkotlin.compiler.execution.strategy=in-process app:testDebugUnitTest --tests "com.example.infinite_track.di.auth.AuthRefreshInterceptorTest" --tests "com.example.infinite_track.presentation.main.ForegroundSessionLifecycleObserverTest" --tests "com.example.infinite_track.presentation.screen.splash.SplashViewModelTest" --tests "com.example.infinite_track.domain.use_case.auth.ForceReauthUseCaseTest" --tests "com.example.infinite_track.domain.use_case.auth.LogoutUseCaseTest"`
- Result: `BUILD SUCCESSFUL in 37s`; `34 actionable tasks: 3 executed, 31 up-to-date`.
- Covered results from test XML: `AuthRefreshInterceptorTest` 14 passed, `ForegroundSessionLifecycleObserverTest` 5 passed, `ForceReauthUseCaseTest` 2 passed, `LogoutUseCaseTest` 2 passed, `SplashViewModelTest` 2 skipped by existing `@Ignore`.
- `git diff --check` produced no whitespace errors; Git emitted only the line-ending warning for `ForceReauthUseCase.kt` (`LF will be replaced by CRLF the next time Git touches it`).

## Self-review verdict
- Review finding is fixed: forced reauth at the three named caller sites now clears local runtime through `ForceReauthUseCase` and no longer calls backend `/api/auth/logout` via `LogoutUseCase`.
- Scope stayed limited to the critical boundary fix and focused tests; no `/me` freshness or broader Task 3 refactor was implemented.

## Docs / ADR note
- No separate docs/ADR update beyond this task report append; this is a bug fix aligning existing Task 2 auth boundary semantics rather than changing backend contract or release policy.

## PR / release notes
- Forced reauth no longer invokes user-initiated logout or backend `/api/auth/logout`.
- Interceptor, foreground session validation, and splash bootstrap reauth now share `ForceReauthUseCase` cleanup semantics.

## Fix notes
- Corrected the Task 2 report so auth/session runtime changes remain `Needs Verification` for emulator/device/backend validation instead of being marked `Not Applicable`.
- Replaced token/email/full-name/identifier-like fixture literals in the amended tests with explicit sentinel values that do not resemble real credentials or personal data.
- Focused verification rerun with the JDK workaround passed for:
  - `com.example.infinite_track.domain.use_case.auth.ClearAuthenticatedRuntimeUseCaseTest`
  - `com.example.infinite_track.presentation.screen.splash.SplashViewModelTest`
- Verification result: `BUILD SUCCESSFUL in 44s`.

## Task 2 follow-up fix
- Hardened `ForceReauthUseCase` so `triggerForcedReauth(reason)` still runs in a `finally` block after cleanup, preventing the single-flight guard from being left in a half-completed state when cleanup throws before publishing reauth state.
- Updated `LogoutUseCase` to rethrow `CancellationException` instead of swallowing it inside the generic `Exception` catch.
- Added/kept focused regression coverage in:
  - `C:/Users/Febriyadi/.claude/worktrees/android-android-inf-207-auth-status-runtime/app/src/test/java/com/example/infinite_track/domain/use_case/auth/ForceReauthUseCaseTest.kt`
  - `C:/Users/Febriyadi/.claude/worktrees/android-android-inf-207-auth-status-runtime/app/src/test/java/com/example/infinite_track/domain/use_case/auth/LogoutUseCaseTest.kt`
- Verification rerun with the same JDK workaround:
  - `JAVA_HOME="C:\Program Files\Android\Android Studio\jbr" JAVA_TOOL_OPTIONS="--add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.jvm=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.model=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED" ./gradlew --no-daemon --% -Pkotlin.compiler.execution.strategy=in-process app:testDebugUnitTest --tests com.example.infinite_track.domain.use_case.auth.ForceReauthUseCaseTest --tests com.example.infinite_track.domain.use_case.auth.LogoutUseCaseTest`
- Result: `BUILD SUCCESSFUL in 1m 47s`.

## Task 2 review-followup fix
- `AuthRepositoryImpl.logoutRemote()` now rethrows `CancellationException` and only converts non-cancellation failures into `Result.failure(...)`.
- `ClearAuthenticatedRuntimeUseCase` now does best-effort cleanup across every local runtime step, keeps running after a step fails, and throws a single aggregated failure at the end if any cleanup step failed.
- Added focused regression coverage in:
  - `C:/Users/Febriyadi/.claude/worktrees/android-android-inf-207-auth-status-runtime/app/src/test/java/com/example/infinite_track/domain/use_case/auth/ClearAuthenticatedRuntimeUseCaseTest.kt`
  - `C:/Users/Febriyadi/.claude/worktrees/android-android-inf-207-auth-status-runtime/app/src/test/java/com/example/infinite_track/data/repository/auth/AuthRepositoryImplLogoutFallbackTest.kt`
  - `C:/Users/Febriyadi/.claude/worktrees/android-android-inf-207-auth-status-runtime/app/src/test/java/com/example/infinite_track/domain/use_case/auth/LogoutUseCaseTest.kt`
- Verification rerun with the same JDK workaround:
  - `JAVA_HOME="C:\Program Files\Android\Android Studio\jbr" JAVA_TOOL_OPTIONS="--add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.jvm=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.model=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED" ./gradlew --no-daemon --% -Pkotlin.compiler.execution.strategy=in-process app:testDebugUnitTest --tests com.example.infinite_track.domain.use_case.auth.ClearAuthenticatedRuntimeUseCaseTest --tests com.example.infinite_track.domain.use_case.auth.LogoutUseCaseTest --tests com.example.infinite_track.data.repository.auth.AuthRepositoryImplLogoutFallbackTest`
- Result: `BUILD SUCCESSFUL in 1m 48s`.
- Covered focused tests: `ClearAuthenticatedRuntimeUseCaseTest`, `LogoutUseCaseTest`, and `AuthRepositoryImplLogoutFallbackTest`.
