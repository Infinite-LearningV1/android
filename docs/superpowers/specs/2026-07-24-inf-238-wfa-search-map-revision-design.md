# INF-238 WFA Search and Map Revision

**Status:** Design approved

**Linear:** INF-238

**Parent delivery:** PR #99

**Branch:** `codex/inf-238-wfa-search-revision`

**Delivery:** One isolated worktree, one branch, one pull request

## Goal

Refine the merged INF-238 Attendance preparation experience so WFA discovery is intentional, searchable, compact, and visually consistent with the existing Infinite Track design system.

The revision must:

- remove the WFA recommendation list from the primary Attendance bottom sheet;
- show place lists only inside explicit WFA search;
- migrate location search from Places SDK for Android Legacy 2.6.0 to Places SDK for Android (New);
- make a selected WFA place immediately focus on the map and display a compact callout;
- use existing category colors and design tokens only;
- align the bottom-sheet surface with `InfiniteTopBar`;
- remove the redundant debug `isShrinkResources = false` declaration.

WFA search and marker selection remain discovery or booking-draft state. They do not become an authoritative Attendance target without an approved WFA booking.

## Confirmed Problems

### Build configuration

The debug build type explicitly sets:

```kotlin
isMinifyEnabled = false
isShrinkResources = false
```

Resource shrinking is already disabled by default when minification is disabled. The explicit `isShrinkResources = false` assignment produces an avoid-setting warning and has no required behavior.

### Attendance bottom sheet

The merged bottom sheet uses a transparent sheet container plus custom blue gradients, blur, and raw `Color(...)` values. This diverges from the established glass surface used by `InfiniteTopBar`, adds unnecessary visual weight, and introduces colors outside the requested Work Mode palette.

### WFA recommendation density

`AttendanceBottomSheetContent` renders `WfaRecommendationSection` inside the main preparation flow. This makes discovery compete with Work Mode selection, authoritative target evidence, recovery state, and the primary Attendance action.

### Search reliability

The project currently uses Places SDK for Android 2.6.0 and legacy initialization. The shared `InfiniteTrackSearchBar` also stores a second remembered copy of its externally supplied value:

```kotlin
var searchValue by remember { mutableStateOf(value) }
```

This can drift from ViewModel-owned state after clear, navigation restoration, or external updates. Search UI must use a single controlled query state.

### Marker presentation

WFA recommendation markers currently display a large information surface containing score, category, address, and distance. The presentation obscures the map and does not match the approved compact callout reference.

Marker models also do not carry a typed category visual role, so category color is not expressed through a single stable mapping.

## Locked Scope

This pull request includes:

1. the debug shrink-resource warning cleanup;
2. the Attendance bottom-sheet surface and density revision;
3. removal of inline WFA recommendation lists from the main preparation sheet;
4. a redesigned explicit WFA place-search screen;
5. migration to Places SDK for Android (New) using a fixed SDK version;
6. controlled search state, cancellation, debounce, session, and error handling;
7. category-aware map markers;
8. selected-place camera focus and compact marker callout;
9. unit, mapper, ViewModel, Compose, build, lint, and runtime verification for the affected flow.

## Non-Goals

- changing the authoritative target contract delivered by PR #99;
- making a search result an approved WFA Attendance target;
- changing backend Attendance or WFA booking endpoints;
- redesigning WFO/WFH target resolution;
- adding venue photos, reviews, ratings, opening hours, or other higher-cost Places fields;
- creating new color constants or a new design system;
- changing the Gradle wrapper;
- hardcoding a Google Maps Platform API key;
- changing unrelated network, auth, permission, face-verification, or geofence behavior;
- resolving the separately observed Android 16 KB native-library compatibility warning.

## UX Architecture

### Primary Attendance flow

The primary bottom sheet remains the preparation surface:

```text
Work Mode heading
WFO / WFH / WFA option cards
Authoritative Target Location summary
Preparation or recovery state
WFA search action, only when relevant
One primary Attendance action
```

It must not render a recommendation list.

Selecting WFA exposes a compact secondary action such as `Cari lokasi WFA`. That action opens the explicit search flow. Empty, loading, or failed discovery state must not make the main sheet taller.

### Bottom-sheet visual contract

The sheet reuses the same established language as `InfiniteTopBar`:

- `InfiniteColors.AttendanceReportGlassSurface`;
- `InfiniteColors.AttendanceReportGlassBorder`;
- existing Infinite Track shape and elevation tokens;
- existing typography styles;
- no gradient fill;
- no raw color values;
- no new color tokens.

The system sheet container owns the surface and shape. Decorative nested full-sheet backgrounds are removed so the sheet has one visual surface.

### Explicit WFA search flow

Place results appear only after the user intentionally opens search and enters a valid query.

The flow states are:

```text
Idle
-> focused search field and concise guidance

Typing fewer than 2 characters
-> no network request

Searching
-> compact loading state

Results
-> keyed LazyColumn of compact place cards

Empty
-> concise empty state with query context

Failure
-> inline error and retry

Selected
-> return selection to Attendance, focus map, show callout
```

The search field is stateless and controlled by ViewModel state. It does not keep a second remembered query.

### Search-result card

Each result card contains only:

- existing category icon or generic location icon;
- place name, maximum two lines;
- address, maximum one line;
- formatted distance when Google returns distance from origin;
- selected semantics and a compact selection indicator.

The card uses existing typography, spacing, surface, border, and shape tokens. Selection is communicated by border, category tint, indicator, and accessibility state rather than color alone.

No photo, score, rating, review, or oversized action row is added.

## Places SDK (New) Contract

### Dependency and initialization

- Replace Places SDK for Android 2.6.0 with the fixed version `3.5.0`.
- `3.5.0` is the approved compatibility baseline because it provides Autocomplete (New) and session pricing while remaining compatible with the project's Kotlin `1.9.0` toolchain.
- Do not use a dynamic `+` dependency.
- Initialize with `Places.initializeWithNewPlacesApiEnabled(...)`.
- Preserve the existing API-key injection route from `local.properties` through the manifest placeholder.
- Never print or copy the key into logs, source, tests, documentation, or chat.

The Cloud project and Android-restricted key must authorize:

- Maps SDK for Android;
- Places API (New).

The Android application restriction must match the application ID and signing certificate used by the tested build.

### Autocomplete session

One search session owns one fresh session token:

```text
open/focus search
-> create token

type one or more autocomplete queries
-> reuse token

select prediction and fetch Place Details
-> terminate session
-> create a new token for a future session

abandon search
-> discard token
```

Tokens from the legacy and new APIs must never be mixed.

### Query policy

- minimum query length: 2 characters;
- debounce: approximately 400 milliseconds;
- `distinctUntilChanged`;
- latest query wins through `flatMapLatest`, cancellation, or equivalent owned-job identity;
- country filter: Indonesia;
- location bias: current user area when a usable current location exists;
- no hard location restriction that would hide valid Indonesian destinations outside the current viewport;
- at most the provider-supported compact prediction result set;
- stable IDs for lazy-list items.

### Place Details fields

Request only the fields needed by the product:

- place/resource ID;
- display name;
- formatted address;
- coordinate/location;
- primary type or minimal category field only if required for marker/category presentation.

Do not request ratings, reviews, photos, opening hours, phone numbers, website, accessibility details, or atmosphere fields.

### Cost controls

The implementation is designed for low-volume/free-usage operation, but it must not claim Google Places is universally free.

Required controls:

- Autocomplete session tokens;
- debounce and minimum query length;
- latest-query cancellation;
- minimum Place Details field mask;
- Cloud billing budget alert;
- per-method quota alert or cap appropriate for test and production traffic;
- separate monitoring of Places API (New) usage.

Runtime configuration or billing changes in Google Cloud are an operator prerequisite and cannot be inferred as complete from a successful Android build.

## Map Interaction

### Marker visual roles

Extend the provider-neutral marker UI model with a project-owned visual category. Do not expose Google Maps SDK types outside the renderer.

The locked Work Mode mapping is:

```text
WFO -> existing Blue_500 / primary token
WFH -> existing Blue_Accent_500 / accent token
WFA -> existing Orange_500 / secondary token
Current location -> existing neutral or current-location token
```

No new color constant is introduced. WFH must not resolve through the current `Info` semantic if that produces a different accent from `Blue_Accent_500`.

Color mapping lives in one presentation mapper or token helper and is reused by option cards, marker dots, and selected result accents where applicable.

### Selection and camera

Selecting either a WFA search result or an eligible WFA marker must update one shared preview selection key.

The camera behavior is:

```text
selection changes
-> cancel superseded camera focus
-> animate camera to selected coordinate
-> use a detail-level zoom that keeps the callout visible
-> reveal the compact callout
```

The selected marker must not trigger a full recommendation-bounds fit. A later selection wins over an earlier pending camera animation.

Re-selecting the same marker may refocus it without duplicating selection state.

### Compact callout

The approved callout is a small map molecule:

```text
rounded glass/surface card
category icon
place name, maximum two lines
small triangular pointer
category-colored marker dot
```

Optional supporting text is limited to one short line only when it materially distinguishes the place. Score, full address, distance, booking lifecycle, and action buttons are excluded from the callout.

The card uses the existing topbar/surface family, typography, shape, border, and elevation. It must remain readable against light map tiles and avoid covering a large portion of the map.

## Error and Recovery

Typed search failures map to presentation-owned messages:

```text
invalid/missing key configuration
API not authorized or Places API (New) disabled
quota or billing restriction
network unavailable or timeout
no predictions
Place Details unavailable
unknown provider failure
```

User-recoverable failures show an inline state with retry. Configuration, authorization, or quota failures show a concise non-secret message and do not retry in a tight loop.

Provider exceptions and API keys must not be rendered verbatim.

## State Ownership

The search ViewModel owns:

- query;
- current session identity;
- current location bias;
- loading/result/empty/error state;
- selected prediction resolution effect.

The Attendance preparation state owns the returned preview selection. Map UI remains a projection through `AttendanceMapUiMapper`.

The search composable owns only ephemeral focus and keyboard behavior. It does not own business query state.

## Build Configuration

Remove only the explicit debug assignment:

```kotlin
isShrinkResources = false
```

Keep:

```kotlin
isMinifyEnabled = false
isDebuggable = true
```

Release shrinking remains unchanged. The Gradle wrapper remains unchanged.

## Testing Strategy

### Search tests

- fewer than two characters makes no provider request;
- valid input is debounced;
- repeated identical input is ignored;
- a newer query cancels or supersedes an older query;
- location bias is included only when valid;
- country filter remains Indonesia;
- one token is reused during a session;
- selection terminates the session and renews the next token;
- abandoned search discards the token;
- minimal Place Details fields are requested;
- success, empty, network, authorization, quota, and details failures map correctly;
- controlled query state clears and restores correctly.

### Map and mapper tests

- WFO, WFH, and WFA map to their locked existing colors;
- WFH uses `Blue_Accent_500`;
- no Google SDK type appears in domain or ViewModel contracts;
- a selected WFA result creates one selected preview marker;
- selecting a marker updates the same preview key;
- newer selection supersedes an older camera request;
- authoritative target and discovery preview remain distinct.

### Compose tests

- main Attendance bottom sheet does not render recommendation rows;
- WFA exposes the explicit search action;
- result list renders only in search;
- cards use two-line title and one-line address constraints;
- selected card exposes selected semantics;
- loading, empty, failure, retry, and content states render;
- compact callout contains no score or full metadata block;
- bottom sheet uses the established topbar surface family;
- narrow and large-font layouts retain usable controls.

### Build and runtime gates

Run:

```text
app:testDebugUnitTest
app:compileDebugAndroidTestKotlin
app:lintDebug
app:assembleDebug
```

Runtime verification on emulator or physical device must cover:

- opening WFA search from Attendance;
- typing, clearing, abandoning, and reopening search;
- Indonesian predictions near the current location;
- selecting a result and returning to Attendance;
- camera zoom to the selected place;
- compact callout rendering and category marker color;
- selecting a second result while a previous camera animation is active;
- offline/network failure and retry;
- invalid API authorization or disabled Places API (New), when safely reproducible;
- absence of the recommendation list from the main bottom sheet.

If Google Cloud configuration prevents runtime search validation, the PR must be marked `Needs Verification` with the required Cloud-side action rather than claiming completion.

## Acceptance Criteria

### UX

- [ ] Bottom-sheet surface follows the existing `InfiniteTopBar` surface family.
- [ ] No new colors or raw screen-specific colors are introduced.
- [ ] The main Attendance sheet no longer displays a WFA recommendation list.
- [ ] Place lists appear only in the explicit WFA search flow.
- [ ] Search cards are compact, themed, and accessible.
- [ ] Selecting a result or marker zooms directly to it.
- [ ] The selected marker displays the approved compact callout.
- [ ] Marker colors follow WFO, WFH, and WFA category tokens.
- [ ] WFH uses `Blue_Accent_500`.

### Search and cost

- [ ] Places SDK for Android uses fixed version `3.5.0`.
- [ ] Places API (New) initialization is used.
- [ ] Search input is fully controlled.
- [ ] Debounce, minimum length, cancellation, country filter, and location bias work.
- [ ] Session tokens follow one search-to-selection lifecycle.
- [ ] Only minimum required Place Details fields are requested.
- [ ] API-key injection remains in `local.properties` and the manifest placeholder.
- [ ] No key or secret is logged or committed.
- [ ] Quota and billing prerequisites are documented without claiming an unlimited free tier.

### Architecture and build

- [ ] Discovery selection remains separate from the authoritative WFA target.
- [ ] Provider-neutral contracts remain free of Google SDK types.
- [ ] Marker visual categories have one central mapping.
- [ ] `isShrinkResources = false` is removed only from debug.
- [ ] Debug minification, release shrinking, and Gradle wrapper remain unchanged.
- [ ] Required tests, lint, build, and runtime evidence are completed or explicitly marked `Needs Verification`.
