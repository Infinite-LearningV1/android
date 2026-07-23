# AGENTS — Android (skrisi/android)

> Active agent operating guide for the Infinite Track Android repository.
> Official operating model: `Cowork -> Claude Desktop Host -> Claude Code CLI -> GitHub + Linear`.
> `CLAUDE.md` remains the detailed governance and runtime gate; this file is the concise repo-local agent routing guide.

## Role repo
Android = trusted data-capture client (Kotlin/Jetpack Compose). Login/session, attendance check-in/out, work mode, geofence, face verification, WFA booking, persistence lokal, Firebase distribution. BUKAN sumber kebenaran akhir; backend yang memutuskan.

## STYLE
- Clean Architecture: presentation -> domain -> data, DI via Hilt.
- Stack: Compose + Material 3, Retrofit/OkHttp, Room/DataStore, WorkManager, Google Maps Compose + Places, CameraX + ML Kit + TFLite, FCM.
- Jangan ekspos/print secret (local.properties, google-services.json, keystore, Google Maps Platform key).

## GOTCHAS
- Base URL backend dipilih hardcoded di NetworkModule.kt: emulator 10.0.2.2:3005, device fisik LAN IP hardcoded (mis. 192.168.1.64:3005). Rawan gagal jika environment network berubah.
- Auth refresh memakai single-flight + interceptor; session expiry global di SessionManager (trigger 401). Ini consumer dari kontrak backend INF-145.
- Runtime-sensitive (auth/attendance/geofence/face/navigation): compile saja TIDAK cukup; perlu emulator/device + Maestro bila flow butuh konfirmasi runtime.
- memory-bank/ lama = stale, bukan status progress otoritatif.

## ARCH_DECISIONS
- Backend final truth; Android capture intent + sinyal device.
- Release/distribution: Firebase App Distribution HANYA dari master. develop = integrasi + verifikasi manusia.
- Promotion: feature/* atau worktree → review/PR → develop → master → Firebase.

## ACTIVE_CONTEXT_FLOW
- Cowork captures product collaboration and high-level intent.
- Claude Desktop Host holds PM/cockpit context and decides routing.
- Claude Code CLI executes repo work in isolated worktrees.
- GitHub PRs and Linear issues are the active evidence/status systems.
- Source-of-truth order: live repo/runtime > GitHub PR/diff/checks > Linear issue context > active cockpit docs > archived docs.

## TEST_STRATEGY
- Compile: `./gradlew app:compileDebugKotlin`. Build: `app:assembleDebug` / `app:assembleRelease`.
- Test/lint: `./gradlew app:test`, `app:testDebugUnitTest`, `app:lint`.
- Device: `adb devices -l`, `app:connectedDebugAndroidTest`, Maestro flow saat alur user butuh konfirmasi.
- Jika runtime tak bisa dijalankan → tandai Needs Verification, bukan Done.

## SENSITIVE (gate sebelum edit)
presentation/screen/auth + splash, domain/manager/SessionManager.kt, data/repository/auth + attendance, presentation/geofencing, data/worker/LocationEventWorker.kt, data/face, presentation/navigation + main, di/NetworkModule.kt, app/build.gradle.kts + gradle/libs.versions.toml + local.properties + google-services.json, .github/workflows/.

## DEPENDENCY RULE
Android adalah konsumen, dan untuk auth/session masih Backlog (INF-147). Jangan buka task Android yang bergantung kontrak backend sebelum kontrak itu terkunci. Android default STANDBY kecuali demo path bergeser ke mobile-first.
