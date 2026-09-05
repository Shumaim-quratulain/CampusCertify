# 6. Live Modification Capability

## Why the architecture is designed for this specifically

Three deliberate design decisions exist **specifically** to make live modification safe and fast, not as an afterthought:

1. **`REQUIRED_POINTS` is a named constant, and the failure message is derived from it** (`"POINTS_BELOW_" + REQUIRED_POINTS`) — changing the threshold can never leave the message saying the wrong number, because there's only one number to change.
2. **`Category.values()` drives both category-coverage counting and failure-reason ordering** — adding, removing, or reordering categories touches exactly one enum declaration, never the logic that consumes it.
3. **The fixed activity table is built in exactly one method** (`BoardState.buildActivities()`), called once from the constructor — adding a new activity is a single new line in a single method, not a change scattered across the codebase.
4. **41 automated tests act as an immediate safety net** — any live change that has a wider effect than intended shows up as test failures within seconds, not as a silent bug discovered later.

## Rehearsed modification #1 — raise the eligibility threshold

**Change:** one line in `EligibilityEvaluator`:
```java
public static final int REQUIRED_POINTS = 6;   →   = 7;
```

**What happened when tested:**
- The failure-reason message **auto-updated to `POINTS_BELOW_7`** with no second edit — it's derived, not a separate literal
- `./mvnw test` immediately showed **15 of 41 tests failing** — every oracle test that hardcodes the spec's 6-point examples (e.g., C02 at exactly 6 points flips from eligible to ineligible)
- Reverted the constant, re-ran: **41/41 green again**

**Why this is good evidence, not a bad result:** it proves the test suite verifies actual behavior rather than just existing. If asked "how do you know this one-line change didn't silently break something," the answer is concrete: the suite caught every ripple immediately, by name, within seconds.

Full transcript of this rehearsal, including the exact failing test list, is in `Chats/execution.md`.

## Rehearsed modification #2 — add a 4th activity category

**Change:** one line in `Category`:
```java
public enum Category {
    LEARN,
    BUILD,
    SHARE
    // →
    LEARN,
    BUILD,
    SHARE,
    PRESENT
}
```

**Nothing else was touched** — not `EligibilityEvaluator`, not `BoardService`, not the frontend. This is the claim the design decisions make (`Category.values()` iteration drives both coverage-counting and reason-ordering), tested for real rather than just asserted.

**What happened when tested:**
- `./mvnw test` immediately showed **19 of 41 tests failing** — every oracle test that assumes 3-category coverage is achievable, because no built-in activity has category `PRESENT`, so nobody can cover all 4 anymore
- Live via the API (`POST /api/evaluate`, after restarting the dev server to pick up the recompiled class), **`MISSING_CATEGORY: PRESENT` appeared automatically**, correctly positioned after existing missing categories and before `POINTS_BELOW_6` where applicable:
  ```
  C05 → ["MISSING_CATEGORY: BUILD", "MISSING_CATEGORY: PRESENT", "POINTS_BELOW_6"]
  ```
- Even **C01 and C02 — previously the only two eligible participants — correctly flipped to ineligible**, counts going from 2/3 to 0/5
- Reverted the enum, re-ran: **41/41 green again**, confirmed via `git diff --stat` showing zero net changes to `Category.java`

**Why this is strong evidence:** it directly demonstrates the specific design claim under test — that category handling is centralized in one enum — rather than describing the claim without proof. Both the automated tests and a live API call agreed on the exact same ripple effect.

## Third candidate — ready but not yet rehearsed

| Candidate | Touches | Claim being tested | Why it's next in line |
|---|---|---|---|
| **Add a new activity** (e.g., A05, some category, some points) | `BoardState.buildActivities()` only | The fixed table is defined in exactly one place | Would prove claim #3 above the same way #1 and #2 already proved claims #1 and #2 |
| **Change sort order** (e.g., ID descending within each group) | `BoardService.RESULT_ORDER` comparator | Whether the `.reversed()` trap (documented in `4-AI-Influenced-Decision-Making.md`) can be explained and correctly avoided live, under time pressure | Highest-difficulty option — good if asked for something harder than a config value |

Either is ready to rehearse the same way (make the change, run tests, observe the exact failure list, verify live via API or browser, revert, confirm green) if there's time before the interview.

## How to perform this live, step by step, if asked

1. Open `EligibilityEvaluator.java` (or `Category.java`, or `BoardState.java`, depending on which change is requested)
2. Make the one-line change
3. Run `./mvnw test` — narrate what fails and why, out loud, before looking at the output in detail (this demonstrates understanding, not just execution)
4. If a browser demo is wanted: restart the app (`Cmd+Shift+B`, or kill the running process on port 8080 first if one is already up) and re-run the Evaluate action to show the change live
5. Revert the change, re-run `./mvnw test` to confirm 41/41 green again, so the codebase is left in its verified state

## Environment readiness

- App starts via `./mvnw spring-boot:run` (dev) or `Cmd+Shift+B` (VS Code task, zero typing) in under 1 second
- Full test suite (`./mvnw test`) runs in under 2 seconds — fast enough to run after every single live-modification step without breaking the flow of a demo
- Two unrelated environment issues surfaced and were fixed during development:
  1. A **port-8080 conflict** from a stale process left running by an earlier task — resolved by identifying and killing the specific PID (`lsof -ti:8080`) rather than guessing
  2. A **misconfigured Code Runner VS Code extension** intercepting the ▷ button and throwing a config-driven placeholder error — root-caused by reading the extension's own `package.json` configuration defaults, then fixed with a two-line workspace setting (`code-runner.shellScriptText`) rather than uninstalling anything
- Both are documented with full root-cause analysis in `Chats/execution.md`. A final clean run (stop any running instance, `Cmd+Shift+B`, confirm the browser loads, confirm `./mvnw test` is green) should be done immediately before the interview to confirm neither issue has recurred.
