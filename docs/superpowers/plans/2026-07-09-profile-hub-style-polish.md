# Profile Hub Style Polish Plan

Date: 2026-07-09
Branch: `fix/android-profile-card-style-refresh`
Spec: `docs/superpowers/specs/2026-07-09-profile-hub-style-polish.md`

## Plan

1. Reuse existing `ProfileScreen.kt` structure and callbacks exactly as-is.
2. Add local styling helpers in `ProfileScreen.kt` for synchronized card surface/shadow treatment.
3. Restyle:
   - identity hero card
   - summary cards
   - section cards
   - menu row icon containers
4. Soften typography hierarchy:
   - reduce over-bold titles
   - align labels/descriptions/value weights
5. Preserve current transparent global background behavior.
6. Run static checks and build attempt.
7. Mark runtime/build as `Needs Verification` if Gradle loopback blocks local verification.
