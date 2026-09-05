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

## Second modification — ready candidates, not yet rehearsed

The rubric allows for "possibly a second if time permits." Three candidates are ready, ranked by how directly they test the architecture's claimed flexibility:

| Candidate | Touches | Claim being tested |
|---|---|---|
| **Add a 4th category** (e.g., `PRESENT`) | `Category` enum only | `failureReasons` and eligibility both iterate `Category.values()` — should need zero other code changes |
| **Add a new activity** (e.g., A05) | `BoardState.buildActivities()` only | The fixed table is defined in exactly one place |
| **Change sort order** (e.g., ID descending within each group) | `BoardService.RESULT_ORDER` comparator | Tests whether the `.reversed()` trap can be explained and avoided live, under time pressure |

**Recommended pick if asked live:** adding a 4th category — it's the most visually obvious change (a new column appears in the UI progress strip) and directly demonstrates the enum-driven-ordering design decision.

## Environment readiness

- App starts via `./mvnw spring-boot:run` (dev) or `Cmd+Shift+B` (VS Code task, zero typing) in under 1 second
- Full test suite (`./mvnw test`) runs in under 2 seconds
- Two unrelated environment issues surfaced and were fixed during development — a port-8080 conflict from a stale process, and a misconfigured Code Runner extension intercepting the ▷ button. Both are documented with root cause in `Chats/execution.md`, and a final clean run should be done immediately before the interview to confirm neither has recurred.
