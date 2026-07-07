# INF-222 — Infinite Track Material 3 UI Component System Foundation

## Scope

INF-222 adds an additive Material 3 friendly reusable UI foundation under `presentation/design/`.

The issue intentionally does not redesign Home Dashboard, Profile, My Attendance Report, WFA Requests, Attendance, Permission, WFA Recommendation, PDF preview, or other full screens.

## Worktree / branch

- Worktree: `C:\Users\Febriyadi\.claude\worktrees\android-infinite-material3-ui-system`
- Branch: `fix/android-infinite-material3-ui-system`
- Base: `develop` at `079c6a3`

## Mapping answers

1. The isolated worktree is correct and is on `fix/android-infinite-material3-ui-system`.
2. Existing reusable attempts include button, status, calendar, textfield, navigation, empty, loading, and card components.
3. Existing screen-specific components, especially cards and attendance/navigation helpers, are preserved and not expanded blindly.
4. Existing repo tokens are mostly colors in `presentation/theme/Color.kt`; spacing, radius, elevation, border, motion, and icon mapping tokens were absent.
5. Material 3 is present but not consistently tokenized; several legacy components hardcode color/radius/elevation and some use heavy blur.
6. `StatefulButton` is limited; INF-222 adds a new `InfiniteButton` wrapper instead of changing legacy behavior in place.
7. INF-219 status components exist and are wrapped by new `InfiniteInlineAlert`, `InfiniteStatusDialog`, and `InfiniteConfirmDialog` foundation APIs.
8. Adding `presentation/design` is safe because it is additive and does not delete legacy components.
9. No additional dependency is required; Material icons extended already exists.
10. Build risk is mainly local Gradle loopback; CI/local Android Studio should provide compile evidence.

## Architecture added

```text
presentation/design/
├── tokens/
├── components/
│   ├── surface/
│   ├── button/
│   ├── status/
│   ├── input/
│   ├── navigation/
│   ├── data/
│   └── state/
└── preview/
```

## Design decisions

- Light theme only foundation.
- Material 3 primitives are used internally.
- Rounded Material icons are centralized in `InfiniteIcons`.
- Legacy `presentation/components/*` remains intact for gradual adoption.
- New foundation components use controlled variants such as size, semantic, density, state, selected, enabled, and loading.
- No backend, navigation graph, auth/session, or attendance business logic changes.

## Verification note

Local Gradle build was attempted but blocked before Kotlin compile by:

```text
java.io.IOException: Unable to establish loopback connection
```

This blocker is environment-level and has appeared on prior Android work. CI or a local Android Studio environment should be used for final compile/build and preview screenshot evidence.
