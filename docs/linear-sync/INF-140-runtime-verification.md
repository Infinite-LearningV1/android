# INF-140 Runtime Verification

Date: 2026-07-23

Branch: `codex/inf-140-provider-neutral-location`

Source state: uncommitted worktree based on `4ef3747`

## Automated evidence

| Gate | Result | Evidence |
|---|---|---|
| Provider-neutral/current-location/place/address/map tests | Pass | `app:testDebugUnitTest` |
| Full debug unit suite | Pass | `app:testDebugUnitTest` |
| Main Kotlin/Hilt wiring | Pass | `app:compileDebugKotlin` |
| Android test source wiring | Pass | `app:compileDebugAndroidTestKotlin` |
| Debug APK | Pass | `app:assembleDebug` |
| Patch whitespace | Pass | `git diff --check` |
| Gradle wrapper unchanged | Pass | Gradle 8.7; no wrapper diff |
| Coordinate-pair compatibility contracts | Pass | no `Pair<Double, Double>` or `GetCurrentCoordinatesUseCase` references |
| Provider imports | Pass | Google map SDK types are confined to data provider and map adapter packages |

The debug APK was produced at `app/build/outputs/apk/debug/app-debug.apk`.

## Runtime matrix

Runtime execution is intentionally deferred until this work reaches `develop`.
An emulator was connected during the automated gate, but the feature-branch APK
was not installed or launched. Every row below remains blocking evidence.

| Runtime row | Status |
|---|---|
| Maps SDK initializes from injected configuration | Needs Verification |
| Missing/default and invalid/restricted key failure is safe | Needs Verification |
| Google base map renders on Play-enabled emulator | Needs Verification |
| Google base map renders on a physical device | Needs Verification |
| Precise current location and freshness/accuracy are correct | Needs Verification |
| Current-location failure recovery works | Needs Verification |
| Project current-location marker works with Google My Location disabled | Needs Verification |
| Authoritative marker and radius are correct | Needs Verification |
| WFA recommendation remains preview-only | Needs Verification |
| Current-location focus works | Needs Verification |
| Target focus works | Needs Verification |
| Recommendation bounds fit works | Needs Verification |
| Pick-on-map camera idle returns the correct coordinate | Needs Verification |
| Insets preserve controls and Google attribution | Needs Verification |
| TalkBack and non-gesture actions work | Needs Verification |
| Places search works with current-location proximity | Needs Verification |
| Places search works without proximity | Needs Verification |
| Suggestion/detail place identity is preserved | Needs Verification |
| Search completion/cancellation renews the session token | Needs Verification |
| Reverse geocode resolved and coordinate-only behavior is correct | Needs Verification |
| Geofence registration and reboot restoration remain compatible | Needs Verification |
| Attendance submission uses only the authoritative target | Needs Verification |
| No legacy provider runtime code executes | Static Verified; Runtime Needs Verification |

The INF-140 cleanup branch removes legacy provider code, dependencies, token
wiring, and the rollback renderer now that the Google adapter is the only
production map path. The remaining runtime rows still require emulator/device
verification after merge to `develop`.
