# 4. AI-Influenced Decision Making

## Trade-off 1 — Storage: in-memory vs. database

AI's first answer was a balanced comparison table (in-memory / H2 / MySQL) rather than a single recommendation. This was pushed on twice more (*"what about mysql"*, *"so what you prefer"*) before a final, committed answer was accepted.

**Assumption tested along the way:** that a database would demonstrate "more Spring skill." The AI's counter-argument — that a DB adds real operational risk (a connection failure live, in front of an interviewer) for zero rubric benefit, since the spec directly names in-memory as acceptable — won out. **Final decision: plain in-memory Java collections, no DB at all, not even H2.**

## Trade-off 2 — Delivery mechanism: Spring Boot server vs. pure browser / CLI

After the STUDENT_GUIDE's "avoid unnecessary layers" wording was re-read carefully, the user directly questioned whether running *any* server contradicted "evaluate local records only." This produced a genuine trade-off discussion rather than a quick reassurance:

| | Spring Boot + static page | Pure browser, no server at all |
|---|---|---|
| Reuses existing backend skill | ✅ | ❌ — logic moves to JavaScript |
| Real JUnit test evidence | ✅ | ❌, or needs a JS test runner (itself over-engineering) |
| Simplicity | Slightly more moving parts | Simplest possible |
| Matches literal spec wording | Debatable — spec names "a browser" as accepted | Unambiguous |

**Decision: keep Spring Boot.** The test-evidence argument was judged to outweigh the marginal simplicity gain, and — critically — the trade-off was written down and defended rather than silently assumed. This is exactly the kind of reasoning the rubric's "discuss trade-offs and assumptions" line is asking for.

## Trade-off 3 — Security: add it, or not?

The AI's **first pass overstated its own case** — claiming outright that "the spec forbids security." This was directly challenged: *"did in the problem statement it is mention to not add security?"* The correction that followed was more precise: the spec forbids **accounts**, which removes the entire *basis* for authentication (no identity to protect, no roles to authorize) — but it says nothing about security as a broader engineering concern.

**What survived the corrected analysis:**
- Input validation (already the core feature — 4 error codes)
- DOM-XSS mitigation — all frontend rendering uses `createElement`/`textContent`, never `innerHTML`, so a participant name like `<img src=x onerror=...>` can't execute
- Explicit reasoning for why SQL injection, path traversal, and deserialization attacks are structurally impossible here (no database, no file I/O, no polymorphic JSON binding)

This is the clearest example in the whole project of **AI output being corrected rather than accepted at face value** — the first answer was wrong in a way that would have looked bad if repeated verbatim to an evaluator.

## How AI recommendations shaped concrete data-structure and component choices

- **`List<String>` instead of `Set<String>`** for completed activity IDs. AI's reasoning, adopted directly: a `Set` would silently deduplicate a repeated activity ID, making the required `DUPLICATE_PARTICIPATION` error **physically undetectable**. A data structure has to be able to *hold* an invalid state in order to *report* it — this single insight shaped the entire `Participant` class design.
- **`Category.values()` iteration** instead of three hardcoded `if` checks for failure reasons. Ties the contracted ordering (LEARN, BUILD, SHARE) to the enum's declaration order structurally, so it can never drift from a separately-maintained ordering list — and was later **proven correct under test** when a 4th category was added live and the ordering held with zero code changes (see `6-Live-Modification-Capability.md`).
- **`REQUIRED_POINTS` as a named constant**, with the failure message derived from it (`"POINTS_BELOW_" + REQUIRED_POINTS`) rather than a separate string literal. Proposed specifically to make the interview's live-modification exercise a genuine one-line change instead of a two-place edit that could drift out of sync.
- **`EvaluationResponse.ofErrors(...)` / `.ofResults(...)` static factories** instead of trusting every caller to remember "if errors, results must be empty and summary null." Turns a rule that could be violated by a careless future edit into something enforced at the construction site.

## Bugs AI-assisted design caught before they ever shipped

### Bug 1 — the comparator trap
During planning, `Comparator.comparing(eligible).reversed().thenComparing(id)` was flagged as a likely bug before it was ever written: `.reversed()` applies to the *entire chain built so far*, so it would silently reverse the ID ordering too, not just the eligible/ineligible grouping. Written correctly the first time as:
```java
Comparator.comparing(ParticipantResult::eligible, Comparator.reverseOrder())
          .thenComparing(ParticipantResult::participantId);
```
with a code comment explaining *why*, specifically so a future reader wouldn't "simplify" it back into the bug. A dedicated test (`orderingIsEligibleFirstThenIdAscending`) is deliberately constructed so a `.reversed()` regression would produce a visibly wrong order and fail loudly.

### Bug 2 — the PUT reordering bug
The first implementation of participant editing did `service.deleteParticipant(id)` then `service.addOrUpdate(...)`. Caught while wiring the frontend integration, before any commit: deleting and re-inserting into a `LinkedHashMap` moves the row to the **end** of iteration order, silently breaking "keep participant inputs synchronized." Fixed to a plain upsert (`Map.put` on the existing key, which preserves position); a regression test (`updateKeepsDisplayOrder`) now guards it permanently.

## An assumption tested, and found to be impossible

A test was planned: *"all categories covered but still below the point threshold."* While writing it, this turned out to be **mathematically impossible** with the given activity table — the cheapest combination that covers all three categories (`A01` LEARN 2pts + `A04` BUILD 2pts + `A03` SHARE 2pts = 6) sums to *exactly* the eligibility threshold. Any other full-coverage combination uses `A02` (BUILD, 3 points) and scores 7+.

**The insight this produced:** full category coverage *implies* at least 6 points with this specific activity table — the two eligibility rules can never disagree in the "categories pass, points fail" direction. That's precisely why the spec calls out the exact-point-boundary case (C02 at exactly 6) as a required test — it's the *only* place these two rules can conflict at the minimum. The test was rewritten to assert this discovered structural fact rather than an unreachable scenario, and is now named `cheapestFullCoverageSitsOnTheBoundary`.
