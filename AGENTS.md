# AGENTS — Android (skrisi/android)

> Draft Fase 1. Tujuan placement: root repo Android sebagai `AGENTS.md`.
> Merujuk ke NORTH_STAR_ROADMAP.md. CLAUDE.md repo tetap berlaku untuk governance + runtime gate.
> Human-curated. Agent mengisi detail, tidak mengubah arah.

## Role repo
Android = trusted data-capture client (Kotlin/Jetpack Compose). Login/session, attendance check-in/out, work mode, geofence, face verification, WFA booking, persistence lokal, Firebase distribution. BUKAN sumber kebenaran akhir; backend yang memutuskan.

## STYLE
- Clean Architecture: presentation -> domain -> data, DI via Hilt.
- Stack: Compose + Material 3, Retrofit/OkHttp, Room/DataStore, WorkManager, Mapbox, CameraX + ML Kit + TFLite, FCM.
- Jangan ekspos/print secret (local.properties, google-services.json, keystore, Mapbox token).

## GOTCHAS
- Base URL backend dipilih hardcoded di NetworkModule.kt: emulator 10.0.2.2:3005, device fisik LAN IP hardcoded (mis. 192.168.1.64:3005). Rawan gagal jika environment network berubah.
- Auth refresh memakai single-flight + interceptor; session expiry global di SessionManager (trigger 401). Ini consumer dari kontrak backend INF-145.
- Runtime-sensitive (auth/attendance/geofence/face/navigation): compile saja TIDAK cukup; perlu emulator/device + Maestro bila flow butuh konfirmasi runtime.
- memory-bank/ lama = stale, bukan status progress otoritatif.

## ARCH_DECISIONS
- Backend final truth; Android capture intent + sinyal device.
- Release/distribution: Firebase App Distribution HANYA dari master. develop = integrasi + verifikasi manusia.
- Promotion: feature/* atau worktree → review/PR → develop → master → Firebase.

## TEST_STRATEGY
- Compile: `./gradlew app:compileDebugKotlin`. Build: `app:assembleDebug` / `app:assembleRelease`.
- Test/lint: `./gradlew app:test`, `app:testDebugUnitTest`, `app:lint`.
- Device: `adb devices -l`, `app:connectedDebugAndroidTest`, Maestro flow saat alur user butuh konfirmasi.
- Jika runtime tak bisa dijalankan → tandai Needs Verification, bukan Done.

## SENSITIVE (gate sebelum edit)
presentation/screen/auth + splash, domain/manager/SessionManager.kt, data/repository/auth + attendance, presentation/geofencing, data/worker/LocationEventWorker.kt, data/face, presentation/navigation + main, di/NetworkModule.kt, app/build.gradle.kts + gradle/libs.versions.toml + local.properties + google-services.json, .github/workflows/.

## DEPENDENCY RULE
Android adalah konsumen, dan untuk auth/session masih Backlog (INF-147). Jangan buka task Android yang bergantung kontrak backend sebelum kontrak itu terkunci. Android default STANDBY kecuali demo path bergeser ke mobile-first.
