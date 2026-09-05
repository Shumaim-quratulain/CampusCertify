# 6. Live Modification Capability

## Rehearsed modification #1 — raise the eligibility threshold

**Change:** one line in `EligibilityEvaluator`:
```java
public static final int REQUIRED_POINTS = 6;   →   = 7;
```

**What happened when tested:**
- The failure-reason message **auto-updated to `POINTS_BELOW_7`** with no second edit — it's derived (`"POINTS_BELOW_" + REQUIRED_POINTS`), not a separate literal
- `./mvnw test` immediately showed **15 of 41 tests failing** — every oracle test that hardcodes the spec's 6-point examples (e.g., C02 at exactly 6 points flips from eligible to ineligible)
- Reverted the constant, re-ran: **41/41 green again**

**Why this is good evidence, not a bad result:** it proves the test suite verifies actual behavior rather than just existing. If asked "how do you know this one-line change didn't silently break something," the answer is concrete: the suite caught every ripple immediately.

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
- Live via the API (`POST /api/evaluate`), **`MISSING_CATEGORY: PRESENT` appeared automatically**, correctly positioned after existing missing categories and before `POINTS_BELOW_6` where applicable:
  ```
  C05 → ["MISSING_CATEGORY: BUILD", "MISSING_CATEGORY: PRESENT", "POINTS_BELOW_6"]
  ```
- Even **C01 and C02 — previously the only two eligible participants — correctly flipped to ineligible**, counts going from 2/3 to 0/5
- Reverted the enum, re-ran: **41/41 green again**

**Why this is strong evidence:** it directly demonstrates the specific design claim under test — that category handling is centralized in one enum — rather than describing the claim without proof.

## Third candidate — not yet rehearsed

| Candidate | Touches | Claim being tested |
|---|---|---|
| **Add a new activity** (e.g., A05) | `BoardState.buildActivities()` only | The fixed table is defined in exactly one place |
| **Change sort order** (e.g., ID descending within each group) | `BoardService.RESULT_ORDER` comparator | Tests whether the `.reversed()` trap can be explained and avoided live, under time pressure |

Either is ready to rehearse the same way if there's time before the interview.

## Environment readiness

- App starts via `./mvnw spring-boot:run` (dev) or `Cmd+Shift+B` (VS Code task, zero typing) in under 1 second
- Full test suite (`./mvnw test`) runs in under 2 seconds
- Two unrelated environment issues surfaced and were fixed during development — a port-8080 conflict from a stale process, and a misconfigured Code Runner extension intercepting the ▷ button. Both are documented with root cause in `Chats/execution.md`, and a final clean run should be done immediately before the interview to confirm neither has recurred.
