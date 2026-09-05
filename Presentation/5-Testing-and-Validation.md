# 5. Testing and Validation

## Testing strategy — three independent layers, deliberately

Every required behavior is verified **three separate ways**, because each layer can hide a bug the others catch: a passing unit test doesn't prove the JSON serializes correctly; a working API doesn't prove the browser renders it correctly.

1. **Unit/service tests** (JUnit 5 + AssertJ, no Spring context — instant) — the business rules in isolation
2. **Controller tests** (MockMvc, real Spring context) — the actual JSON shape over HTTP
3. **Manual verification** (raw `curl`, Postman, and clicking through the live browser UI) — the real, end-to-end behavior a user or evaluator would actually see

## Automated test suite — 41 tests, all passing

```
./mvnw test
Tests run: 41, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### `DomainModelTest` (4 tests) — domain type invariants
- Category declaration order is the contracted reason order: LEARN, BUILD, SHARE
- Participant trims id, name and activity ids on construction
- Participant keeps repeated activity ids so DUPLICATE_PARTICIPATION stays detectable
- Evaluation envelope with errors carries no results and no summary

### `BoardStateTest` (5 tests) — fixed table + reset fidelity
- Fixed activity table matches the spec exactly
- Built-in participants match the spec on startup
- **Reset restores the five built-in rows byte-for-byte after arbitrary mutation** (delete a row, add an intruder row, clear another row's activities — reset undoes all three at once)
- Activity lookup trims input and reports unknown ids as empty
- Upsert replaces an existing participant rather than duplicating it

### `ParticipantValidatorTest` (8 tests) — all 4 error codes
- Built-in records produce no validation errors
- Repeated activity id for one participant reports DUPLICATE_PARTICIPATION with C01 and A01
- Activity id outside the fixed table reports UNKNOWN_ACTIVITY
- Blank id or blank name reports INVALID_PARTICIPANT
- Repeated participant id reports DUPLICATE_PARTICIPANT_ID once, on the second occurrence
- Validation collects every error rather than stopping at the first
- Untrimmed ids do not create phantom participants that bypass uniqueness
- Empty completed-activity list is valid input

### `EligibilityEvaluatorTest` (9 tests) — the entire spec oracle
- C01 Asha: A01+A02+A03 = 7 points, all categories, eligible, no reasons
- C02 Bilal: **exactly 6 points is eligible, proving `>=` not `>`**
- C03 Chen: 7 points but shows only `MISSING_CATEGORY: SHARE`
- C04 Divya: 7 points but shows only `MISSING_CATEGORY: LEARN`
- C05 Eshan: 4 points, `MISSING_CATEGORY: BUILD` followed by `POINTS_BELOW_6`
- Empty completion list gives 0 points, no categories, all four reasons in order
- Adding A04 to C05 lifts it to 6 points with all three categories covered
- Cheapest full-coverage combination is exactly 6 points, so `>=` vs `>` decides eligibility
- Dropping any category from the cheapest combination reports that category and the point shortfall together

### `BoardServiceTest` (7 tests) — every required acceptance scenario, end-to-end
- Acceptance 1: built-in records give totals 7,6,7,7,4 with counts 2 eligible / 3 ineligible
- Acceptance 2: C03 shows only SHARE, C04 only LEARN, C05 BUILD then POINTS_BELOW_6
- Acceptance 3: adding A04 to C05 gives total 6 and counts 3 eligible / 2 ineligible
- Acceptance 4: clearing C01 gives 0 points, four reasons, counts 1 eligible / 4 ineligible
- Acceptance 5: a second A01 on C01 reports DUPLICATE_PARTICIPATION with no stale rows or counts
- Eligible participants come first, then ineligible, each group sorted by id ascending
- Reset restores the built-in oracle after edits

### `BoardControllerTest` (7 tests) — real JSON shape via MockMvc
- GET /api/activities returns the four fixed rows in order
- GET /api/participants returns the five built-in rows
- POST /api/evaluate returns the built-in oracle with counts 2 and 3
- Duplicate participation returns 200 with errors and no results or summary
- PUT keeps the edited participant in its original display position
- Untrimmed input is normalized before it reaches the board
- POST /api/reset restores the built-in rows and the oracle counts

### `CampuscertifyApplicationTests` (1 test)
- Spring context loads successfully

## Typical usage scenarios — verified live, three ways (unit test + raw API + browser)

| # | Scenario | Verified result |
|---|---|---|
| 1 | Built-in oracle | Totals 7, 6, 7, 7, 4 — 2 eligible / 3 ineligible |
| 2 | Reasons per participant | C03 → only `MISSING_CATEGORY: SHARE`; C04 → only `MISSING_CATEGORY: LEARN`; C05 → `MISSING_CATEGORY: BUILD` then `POINTS_BELOW_6` |
| 3 | Add A04 to C05 | 6/6 points, all 3 categories, counts become 3 eligible / 2 ineligible, order `C01, C02, C05, C03, C04` |
| 4 | Reset, clear C01's activities | 0 points, all 4 reasons in order, counts 1 eligible / 4 ineligible |
| 5 | Reset, add a second A01 to C01 | `DUPLICATE_PARTICIPATION` naming C01 and A01, **no stale result rows or counts** — verified in the browser: the validation panel appears and the results/summary panels disappear entirely |

## Edge cases specifically targeted

- **Exact point boundary** — C02 sits at exactly 6 points and is eligible, proving the check is `>=` not `>`
- **The cheapest full-coverage combination is exactly on the boundary** — a structural discovery (see `4-AI-Influenced-Decision-Making.md`) turned into its own dedicated test, `cheapestFullCoverageSitsOnTheBoundary`
- **Empty completed-activity list** — valid input, gives 0 points, 0 covered categories, and all 4 failure reasons in the contracted order
- **Duplicate participant ID** — fires once, on the second occurrence, not on every repeat
- **Untrimmed input** (`"  Eshan  "`, `"  A01"`) — normalized before comparison so it doesn't create phantom duplicate/unique-ID bugs
- **Reset fidelity under mutation** — delete a row, insert an unrelated row, clear another row's activities, then reset; the test asserts the result is byte-for-byte identical to startup
- **Blank participant fields, caught live outside of any test** — during manual browser testing, an accidentally-added blank row correctly triggered two `INVALID_PARTICIPANT` errors (one for the blank id, one for the blank name) and the results panel disappeared immediately — confirming the fail-safe path holds even for input scenarios never explicitly unit-tested

## API-level validation (Postman)

`postman/CampusCertify.postman_collection.json` — 9 requests covering every endpoint plus 2 dedicated acceptance-scenario requests ("Clear C01 activities", "Duplicate A01 on C01"). Every request was verified against the live server via `curl` before being committed, so nothing in the collection is speculative. Screenshots of each request/response pair are captured in `postman/*.png`.

## Manual browser verification, captured as evidence

`Output/FirstPage.png` and `Output/AfterEvaluate.png` capture the actual rendered UI — the fixed activity table, the editable participant table, the category progress strip (solid segments for covered categories, dashed for missing), and the results table sorted eligible-first.

## Test-first discovery worth mentioning live

A planned test — *"all categories covered but still below the point threshold"* — turned out to be mathematically impossible with the given activity table (full detail in `4-AI-Influenced-Decision-Making.md`). Finding this out **while writing the test**, rather than assuming it was reachable, is itself evidence of a validation mindset, not just a validation checklist being ticked off.
