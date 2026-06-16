# INF-147 Closure Audit Design

## Context

Linear `INF-147` asks Android to consume the backend refresh-token contract defined by `INF-145` so mobile session continuity stays truthful to backend session state. The Android repository has already absorbed two major waves of work:

- PR #21 (`fix/inf-147-login-refresh-token-compat`) aligned Android with the backend refresh-session contract.
- PR #22 (`fix/auth-bootstrap-session-contract`) tightened startup/bootstrap behavior, logout side effects, and notification-permission timing during invalid-session flows.

The remaining problem is no longer “implement silent refresh from scratch.” The remaining problem is deciding whether `INF-147` is actually ready to close. The current Linear issue still sits in `Backlog`, while the repo and runtime evidence show substantial implementation progress. That creates a governance gap:

- code reality may already satisfy most of the issue,
- but the issue cannot be closed honestly unless each verification claim is mapped to current evidence.

This design therefore treats the next work as a focused closure audit, not as a feature implementation task.

## Goals

- Build an explicit acceptance matrix for `INF-147` using the issue’s own verification bullets.
- Define which evidence sources are strong enough to mark a criterion as proven.
- Reconcile repo state, merged PRs, docs, and runtime observations into one current closure view.
- Run only the minimum additional verification needed to decide whether `INF-147` is close-ready.
- Produce a closure outcome that is honest, auditable, and easy to paste back into Linear.

## Non-Goals

- Re-implement Android auth continuity from scratch.
- Audit unrelated auth-adjacent issues outside the scope of `INF-147`.
- Re-run the entire Android test suite as a substitute for runtime evidence.
- Close backend issue `INF-145` again or redefine its contract.
- Expand into unrelated Android concerns such as face bootstrap semantics, maps, or attendance flow unless they block interpreting a specific `INF-147` criterion.

## Recommended Approach

Treat `INF-147` closure as a four-step audit:

1. Build an acceptance matrix from the issue’s verification section.
2. Reconcile all existing evidence against the current `develop` branch.
3. Verify only the remaining gaps that affect the close/no-close decision.
4. Produce a closure draft for Linear with one of three outcomes:
   - `close-ready`
   - `not-close-ready`
   - `needs-split-follow-up`

This keeps the work focused on the real question: whether the issue can be closed truthfully today.

## Work Structure

### Step 1 — Build the Acceptance Matrix

Use the `INF-147` verification bullets as the source of truth. Each criterion should be represented in a matrix with these fields:

- `Acceptance criterion`
- `Expected behavior`
- `Current evidence`
- `Evidence source`
- `Status`
- `Next action`

The matrix is the central artifact. All later decisions must point back to it rather than to conversational memory.

### Step 2 — Reconcile Existing Evidence

Evidence should be collected from these sources:

1. Linear issue `INF-147`
2. Backend contract issue `INF-145`
3. Merged PR/commit history (`PR #21`, `PR #22`)
4. Repo docs and ADR notes
5. Current runtime observations captured from the app

This step exists to answer two questions:

- What was claimed historically?
- What is still true on current `develop`?

Evidence that was valid on an old branch but not yet checked against current `develop` should not be treated as automatically current.

### Step 3 — Verify the Remaining Gaps

Do not re-test everything. Only verify criteria that remain `Partially Proven` or `Not Proven` after reconciliation.

Gap verification should be constrained to the closure decision. If a scenario is outside the issue’s core contract, note it as context but do not let it expand the audit into a new engineering project.

### Step 4 — Produce the Closure Outcome

The audit should end with a Linear-ready summary that says one of:

- `close-ready` — all issue-critical criteria are proven.
- `not-close-ready` — one or more issue-critical criteria remain unproven.
- `needs-split-follow-up` — the issue’s core goal is proven, but the remaining concern belongs in a separate follow-up issue.

## Acceptance Matrix Model

Each verification point from `INF-147` should be evaluated with one of three statuses.

### Proven

Use `Proven` when:

- the expected behavior has been observed directly in runtime, or
- an existing evidence artifact is specific, current, and still aligned with the current `develop` implementation.

Examples of candidate `Proven` items in the current state may include:

- startup invalid-session flow landing on login,
- single terminal logout side effect during startup failure,
- notification prompt not overlapping the invalid-session bootstrap path.

### Partially Proven

Use `Partially Proven` when:

- code, tests, and prior docs strongly support the expected behavior,
- but the issue’s stated verification expects a more direct or current runtime proof than is presently available.

This status is important because it prevents over-claiming while still recognizing strong implementation evidence.

### Not Proven

Use `Not Proven` when:

- no sufficiently specific evidence exists,
- the scenario has not been reproduced directly enough,
- or the current environment cannot truthfully demonstrate the behavior.

This is likely to apply to backend-stateful scenarios such as a true `48-hour inactivity` outcome unless a controlled environment can reproduce it.

## Evidence Rules

Not all evidence sources have equal weight.

### Strongest evidence

1. Current runtime observation on the app
2. Current repo artifacts that explicitly capture runtime behavior

These are the preferred basis for marking a criterion `Proven`.

### Supporting but insufficient alone

1. Linear comments
2. Code structure
3. Merged commit history
4. Unit/integration tests described in docs

These can justify `Partially Proven`, but they should not by themselves close a runtime-focused verification bullet.

### Evidence principles

- A historical comment is not enough if it has not been reconciled with current `develop`.
- Code presence is not proof of runtime behavior.
- Runtime observations should be preferred over inferred correctness.
- If a criterion cannot be demonstrated truthfully in the current environment, keep it unproven and explain why.

## Target Verification Scope

The audit should focus on the exact verification bullets in `INF-147`:

1. `login -> access token expired -> refresh succeeds`
2. `app resume / foreground transition with expired access token`
3. `refresh fails because token is invalid or revoked`
4. `refresh fails because inactivity > 48 hours -> full login required`
5. `offline/server-down during refresh does not trigger misleading auth cleanup`

The expected initial classification before fresh auditing is:

### Likely Proven or near-Proven

- terminal invalid-session startup behavior leading back to login
- startup logout side-effect control
- notification-permission timing during invalid-session startup

These do not map one-to-one to every original `INF-147` bullet, but they matter because PR #22 tightened Android startup auth continuity in ways directly related to truthful session handling.

### Likely Partially Proven

- app resume / foreground continuity with expired access token
- offline/server-down semantics during refresh

These appear strongly supported by the code and docs, but may still need current runtime-specific evidence for closure confidence.

### Likely Not Proven

- refresh success after an actually expired access token in a controlled runtime scenario
- true `48-hour inactivity` denial path
- explicit revoked-refresh-token path under the current backend environment

These should not be assumed complete without more direct proof.

## Gap Verification Rules

Gap verification should stay tightly bounded.

Allowed work:

- reconcile stale evidence against current `develop`
- run targeted runtime checks for specific unproven criteria
- inspect current docs and merged commits to support status decisions

Disallowed work unless a criterion clearly requires it:

- broad new refactors
- unrelated UX polish
- unrelated auth cleanup not needed for the close/no-close decision
- turning the closure audit into a general Android auth review

If a new implementation defect is discovered that directly invalidates a criterion, record it explicitly and treat the issue as `not-close-ready`.

## Final Output Design

The closure audit should always produce three deliverables.

### 1. Repo-side working note

A written spec/note in the repository that records:

- the acceptance matrix,
- evidence mapping,
- remaining gaps,
- and the final closure recommendation.

This ensures the decision is durable and reviewable outside chat history.

### 2. Linear-ready draft comment

Prepare one of two comment shapes:

#### If close-ready

- summarize what Android now implements,
- cite the evidence for each criterion,
- state that `INF-147` acceptance criteria are satisfied,
- recommend moving the issue to done.

#### If not close-ready

- summarize what is already complete,
- list what is proven,
- list what remains unproven,
- explain exactly why the issue should remain open,
- suggest the next verification step or follow-up split.

### 3. Explicit closure recommendation

The audit must end with one of:

- `Close INF-147`
- `Do not close INF-147 yet`
- `Close core scope but split follow-up issue`

## Decision Rules

### Close-ready

Only if every issue-critical verification criterion is `Proven`, or if any remaining concern is clearly outside the intended scope of `INF-147` and does not weaken the issue’s core claim.

### Not close-ready

If any issue-critical verification bullet remains `Not Proven`, especially where the issue explicitly calls for runtime-semantic confidence rather than code-only confidence.

### Needs split follow-up

If the issue’s core silent-refresh contract is proven, but a remaining concern is better represented as a separate follow-up issue instead of holding the main issue open indefinitely.

## Expected Likely Outcome

Based on current evidence, the most likely final outcomes are:

- `close-ready with minor caveats`, or
- `not-close-ready because one or more backend-stateful verification scenarios remain unproven`, most likely the `48-hour inactivity` path.

The audit should not prejudge this result. The acceptance matrix determines the answer.

## Acceptance Criteria for This Closure Audit Design

- The next work starts from a written acceptance matrix, not an informal checklist.
- Evidence strength is defined explicitly before new verification starts.
- Gap verification is limited to issue-critical closure questions.
- The audit can end truthfully with either close or no-close.
- The final output can be pasted into Linear with minimal rewriting.
