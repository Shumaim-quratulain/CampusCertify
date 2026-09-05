# 4. AI-Influenced Decision Making

## Trade-offs discussed and resolved

### Storage: in-memory vs. database
AI's first answer was a balanced comparison table (in-memory / H2 / MySQL). Pushed twice more ("what about mysql", "so what you prefer") before committing. Final decision: plain in-memory, because the spec names it directly and a DB buys nothing the rubric rewards while adding real demo-day risk (a DB connection failure live, in front of an interviewer).

### Delivery mechanism: Spring Boot server vs. pure browser / CLI
AI recommended keeping Spring Boot even after the user pointed out the spec might mean *no server at all*. The trade-off was made explicit rather than assumed:

| | Spring Boot + static page | Pure browser, no server |
|---|---|---|
| Reuses existing backend skill | ✅ | ❌ — logic moves to JS |
| JUnit test evidence | ✅ | ❌ or needs a JS test runner (itself over-engineering) |
| Simplicity | Slightly more moving parts | Simplest possible |

Decision: keep Spring Boot — the test-evidence argument outweighed the "even simpler" option, and the trade-off was documented, not hidden.

### Security: add it, or not?
The AI's first pass **overstated** the case — claiming "the spec forbids security." Directly challenged: *"did in the problem statement it is mention to not add security?"* Correction issued: the spec forbids **accounts**, not security broadly. Reworked into a precise threat-model answer (no accounts → nothing to authenticate; input validation is the real security surface; DOM XSS mitigated via `textContent` instead of `innerHTML`). This is a case where AI output was corrected rather than accepted at face value.

## How AI recommendations shaped concrete implementation choices

- **`List<String>` instead of `Set<String>`** for completed activity IDs — AI's reasoning: a `Set` would silently deduplicate a repeated activity ID, making the required `DUPLICATE_PARTICIPATION` error physically undetectable. The data structure has to be able to *hold* an invalid state to *report* it.
- **`Category.values()` iteration** instead of three hardcoded `if` checks for failure reasons — ties the contracted ordering (LEARN, BUILD, SHARE) to the enum's declaration order structurally, so it can never drift from a separately-maintained list.
- **`REQUIRED_POINTS` as a named constant** feeding both the eligibility check and the message string (`"POINTS_BELOW_" + REQUIRED_POINTS`) — proposed specifically to make the interview's live-modification exercise a genuine one-line change.

## Bugs AI-assisted design caught before they shipped

1. **The comparator trap.** `Comparator.comparing(eligible).reversed().thenComparing(id)` was flagged in planning as a likely bug — `.reversed()` would silently reverse the *entire* chain, breaking ID ordering. Written correctly the first time as `Comparator.comparing(eligible, Comparator.reverseOrder()).thenComparing(id)`, with a code comment explaining why, so it can't be "simplified" back into the bug later.
2. **The `PUT` reordering bug.** First implementation of participant edit did `delete()` then `addOrUpdate()`. Caught while writing the frontend integration: deleting and re-inserting into a `LinkedHashMap` moves the row to the end, silently breaking the "keep participant inputs synchronized" requirement. Fixed to a plain upsert; a regression test (`updateKeepsDisplayOrder`) now guards it.

## An assumption tested and found impossible

Planned a test: *"all categories covered but still below the point threshold."* While writing it, discovered this cannot happen with the fixed activity table — the cheapest combination that covers all three categories (`A01`+`A04`+`A03` = 2+2+2) sums to exactly 6, the threshold itself. The test was rewritten to assert this insight (the boundary is the *only* place these two rules can conflict) rather than an unreachable scenario.
