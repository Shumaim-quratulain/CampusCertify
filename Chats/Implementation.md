# CampusCertify — Implementation Log (chat2.md)

Continuation of `chat1.md` (exploration + planning). This file records the **implementation** phase.
Blueprint being executed: `plan.md`.

---

## Environment (verified before starting)

| Item | Value |
|---|---|
| OS | macOS |
| JDK | Java 24.0.1 (HotSpot 64-bit) |
| Maven | **Not installed** — resolved by using the Maven Wrapper (`./mvnw`) from Spring Initializr |
| IDE | VS Code |
| Workspace | `/Users/shumaimquratualain/Documents/CampusCertify` |

---

## Phase 0 — Scaffold  ✅

**Goal:** generate the Spring Boot project skeleton and prove it builds.

### What was done
1. Checked toolchain — found Java 24 present but **no `mvn` on PATH**.
   - *Decision:* rather than installing Maven via Homebrew, generate the project from Spring Initializr, which ships `mvnw` (Maven Wrapper). The wrapper downloads its own Maven, so there is **zero global install** and the project is reproducible on any machine.
2. Queried Initializr metadata for valid versions.
3. Downloaded the starter zip via `curl` and unzipped into the workspace root.
4. Ran `./mvnw -B test` to verify.

### Problems hit and how they were solved

**Problem 1 — `HTTP 400: Invalid Spring Boot version '3.5.5'`**
Initializr reported its compatibility range as `>= 4.0.0`, so the plan's assumed Boot 3.x was rejected outright.
*Fix:* queried `https://start.spring.io/metadata/client` for the real list of accepted IDs.

**Problem 2 — `Non-resolvable parent POM ... 4.1.1.RELEASE (absent)`**
Initializr accepted `4.1.1.RELEASE` and generated a POM with it, but Maven Central could not resolve that artifact.
*Fix:* queried Maven Central directly:
`https://search.maven.org/solrsearch/select?q=g:org.springframework.boot+AND+a:spring-boot-starter-parent`
The newest **actually published** version is **3.5.3**. The Initializr metadata ID did not correspond to a real artifact.

**Problem 3 — Boot 4 starter names in the generated POM**
The generated POM used `spring-boot-starter-webmvc` / `spring-boot-starter-webmvc-test` (Boot 4 naming).
*Fix:* reverted to the Boot 3.x names `spring-boot-starter-web` / `spring-boot-starter-test` when pinning to 3.5.3.

### Final configuration
| Setting | Value | Why |
|---|---|---|
| Spring Boot | `3.5.3` | Newest version actually resolvable from Maven Central |
| Java release target | `21` | LTS; compiles cleanly on the installed JDK 24. Targeting 24 or 25 would break portability for no gain |
| Build tool | Maven via `./mvnw` | No global Maven install required |
| Dependencies | `spring-boot-starter-web`, `spring-boot-starter-test` | Exactly the two from the plan — no JPA, no DB driver, no Lombok |
| groupId / package | `com.campus` / `com.campus.campuscertify` | — |

### Checkpoint result
```
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### Plan deviation recorded
> The plan said "Spring Boot 3.x". That still holds, but the *specific* version had to be discovered
> empirically rather than assumed, because Initializr's advertised range and Maven Central's actual
> contents disagreed. Lesson: verify the artifact resolves before writing any code on top of it.

---

## Phase 1 — Domain model  ✅

**Goal:** create the 9 domain types, with the contracted ordering enforced structurally.

### Files created — `src/main/java/com/campus/campuscertify/domain/`

| File | Type | Key decision |
|---|---|---|
| `Category.java` | enum | Values declared `LEARN, BUILD, SHARE` — this order **is** the contracted reason order, so iterating `Category.values()` produces it for free |
| `Activity.java` | record | Immutable — the activity table is fixed by the spec. `int points` avoids float comparison risk at the `>= 6` boundary |
| `Participant.java` | mutable class | The only mutable domain type; the UI edits it live |
| `ErrorCode.java` | enum | Exactly the 4 codes the spec names — a closed set, so tests assert on the enum not on strings |
| `ValidationError.java` | record | `code` + `participantId` + `offendingValue`, matching "report ... with the participant and offending value" |
| `ParticipantResult.java` | record | Carries `coveredCategories` so the progress strip uses the *same derived set*, not a second derivation in JS |
| `EvaluationSummary.java` | record | `eligibleCount` / `ineligibleCount` |
| `EvaluationResponse.java` | record | The envelope, with two static factories |

### Design decisions made while writing the code

**1. `Participant.completedActivityIds` is `List<String>`, never `Set<String>`.**
A `Set` would silently deduplicate a second `A01`, making `DUPLICATE_PARTICIPATION` *physically undetectable* — and that is a required acceptance scenario. The data structure must be able to hold the invalid state in order to report it. There is a dedicated test locking this in.

**2. Trimming moved from the controller into `Participant` itself.**
The plan said "trim in the DTO→domain mapping". While writing it, a stronger option appeared: normalize inside the constructor and setters. That makes normalization **impossible to bypass** — any code path that creates a `Participant` gets canonical values. One place instead of one *convention*.
This matters because if `"C01"` and `"C01 "` were both stored, they would be treated as two different participants and `DUPLICATE_PARTICIPANT_ID` would never fire.

**3. Blank activity tokens are dropped during normalization.**
A trailing comma in the UI input (`"A01, A02,"`) produces an empty token. That is a formatting artifact, not a data error, so it is stripped rather than reported as `UNKNOWN_ACTIVITY`.

**4. `EvaluationResponse.ofErrors(...)` / `ofResults(...)` static factories.**
Rather than trusting callers to remember "if errors, results must be empty and summary null", the factories make the invariant unbreakable at the construction site.

**5. `completedActivityIds()` returns an unmodifiable view.**
Prevents a caller from mutating participant state through the getter and bypassing normalization.

### Checkpoint result
`DomainModelTest` — 4 tests:
- Category order is LEARN, BUILD, SHARE
- Participant trims id / name / activity ids
- Participant **preserves** repeated ids (guards decision 1)
- Error envelope carries no results and no summary

```
Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

---

## Phase 2 — BoardState  ✅

**Goal:** one class holding the fixed activity table and the mutable participant rows, with a reset that is provably identical to startup.

### File created — `src/main/java/com/campus/campuscertify/state/BoardState.java`

`@Component`, two fields:

| Field | Type | Why |
|---|---|---|
| `activities` | `Map<String, Activity>` (unmodifiable `LinkedHashMap`) | One structure does two jobs — O(1) lookup for point summing **and** stable display order for the fixed table |
| `participants` | `LinkedHashMap<String, Participant>` | O(1) lookup for edit/delete by id, plus deterministic pre-sort order so screenshots are reproducible |

Methods: `activities()`, `activityIndex()`, `findActivity(id)`, `participants()`, `findParticipant(id)`, `upsert(p)`, `remove(id)`, `reset()`.

### Design decisions made while writing the code

**1. `seedParticipants()` is the single definition of the built-in rows, called by both the constructor and `reset()`.**
The constructor literally just calls `reset()`. Two separate copies of the seed data is the number-one way "Reset restores the five built-in rows" silently breaks — this makes drift impossible.

**2. `seedParticipants()` returns *fresh* `Participant` objects on every call.**
`Participant` is mutable. If the seed were a shared static list, editing C05 in the UI would corrupt the seed itself and `reset()` would restore the *corrupted* rows. The test proves this: it deletes C01, adds an intruder, clears C05's activities, then resets and asserts an exact match.

**3. `activities` is wrapped in `Collections.unmodifiableMap` and there is no setter.**
The spec says the activity table is fixed. Enforcing that in the type is stronger than enforcing it by convention — the API surface simply has no way to write to it.

**4. `activityIndex()` exposes the raw map so the validator and evaluator can stay pure functions.**
They receive the index as a parameter instead of depending on `BoardState`, which keeps them unit-testable with no Spring context and no fixture setup.

**5. `findActivity` / `findParticipant` return `Optional` and strip the incoming id.**
`Optional` makes "unknown activity" an explicit branch rather than a null check, and the strip guards against a caller that skipped normalization.

**6. No synchronization.**
Single-user local tool. Documented as an assumption in a one-line class comment rather than defended with locks, which would be over-engineering for the scope.

### Checkpoint result
`BoardStateTest` — 5 tests:
- Fixed activity table matches the spec exactly (all 4 rows)
- Built-in participants match the spec on startup (all 5 rows)
- **Reset restores startup state after arbitrary mutation** (delete + insert + clear)
- Activity lookup trims input and returns empty for `A99`
- Upsert replaces rather than duplicates

```
Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

---

## Phase 3 — ParticipantValidator  ✅

**Goal:** produce all four contracted error codes, collecting every problem rather than failing fast.

### File created — `src/main/java/com/campus/campuscertify/service/ParticipantValidator.java`

`@Component`, stateless. Public entry:
`List<ValidationError> validate(List<Participant> participants, Map<String, Activity> activityIndex)`

Three private checks:

| Check | Produces |
|---|---|
| `checkIdentity` | `INVALID_PARTICIPANT` — blank id and/or blank name |
| `checkUniqueId` | `DUPLICATE_PARTICIPANT_ID` — id already seen |
| `checkActivities` | `UNKNOWN_ACTIVITY` and `DUPLICATE_PARTICIPATION` |

### Design decisions made while writing the code

**1. Returns a list, never throws.**
Invalid user input is an *expected* outcome, not an exceptional condition. Throwing would also force a `@ControllerAdvice` that the plan deliberately cut, and would make "report the offending value" awkward.

**2. No fail-fast anywhere.**
Every participant is checked, and within a participant every activity id is checked. A grader who types two bad rows sees both. A dedicated test (`collectsAllErrors`) asserts five distinct errors come back from two bad participants.

**3. `UNKNOWN_ACTIVITY` and `DUPLICATE_PARTICIPATION` are mutually exclusive per id (`else if`).**
An id that isn't in the fixed table can't meaningfully be "a duplicate participation" — it isn't a participation at all. Reporting both for the same token would be noise.

**4. Duplicate detection uses a `Set` seeded *inside* the loop, per participant.**
Duplicates are scoped to one participant — C01 and C02 may both legitimately have `A01`. The `seenActivities` set is created fresh for each participant; the `seenIds` set for participant ids is shared across all of them.

**5. `DUPLICATE_PARTICIPANT_ID` fires once, on the second occurrence.**
`seenIds.add()` returns false only on the repeat, so three copies of `C01` produce two errors, not three or one. Blank ids are skipped here so they report `INVALID_PARTICIPANT` only, not both.

**6. `offendingValue` for `INVALID_PARTICIPANT` names the *field*, not the value.**
The offending value is blank by definition, so echoing `""` would tell the user nothing. Reporting `"participantId"` or `"participantName"` identifies which field is wrong. This is a judgement call on an ambiguous line in the spec, worth flagging in the interview.

### Checkpoint result
`ParticipantValidatorTest` — 8 tests:
- Built-in records produce **no** errors
- `DUPLICATE_PARTICIPATION` names C01 and A01 (the required acceptance scenario)
- `UNKNOWN_ACTIVITY` for `A99`
- `INVALID_PARTICIPANT` for blank id and blank name
- `DUPLICATE_PARTICIPANT_ID` once, on the second occurrence
- Collects **all** errors, no fail-fast
- Trimming prevents `"C01"` vs `"  C01  "` bypassing uniqueness
- Empty completed-activity list is valid input

```
Tests run: 18, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

---

## Phase 4 — EligibilityEvaluator  ✅

**Goal:** the calculator — covered categories, total points, eligibility, and the ordered failure reasons.

### File created — `src/main/java/com/campus/campuscertify/service/EligibilityEvaluator.java`

`@Component`, stateless. Public entry:
`ParticipantResult evaluate(Participant participant, Map<String, Activity> activityIndex)`

Private helpers: `coveredCategories(...)`, `totalPoints(...)`, `failureReasons(...)`.

### Design decisions made while writing the code

**1. `REQUIRED_POINTS = 6` is a public constant, and the reason string is derived from it.**
`POINTS_BELOW_THRESHOLD = "POINTS_BELOW_" + REQUIRED_POINTS`. Changing the threshold to 8 updates both the rule *and* the message in one edit — no chance of the code saying 8 while the UI still says `POINTS_BELOW_6`. This is deliberate preparation for the interview's live-modification exercise.

**2. `failureReasons` iterates `Category.values()` instead of three hardcoded `if` blocks.**
The enum's declaration order already *is* LEARN → BUILD → SHARE, so the contracted ordering cannot be typed wrong. Adding a fourth category later requires **zero** changes to this method.

**3. Eligibility uses `covered.size() == Category.values().length`, not a hardcoded `3`.**
Same reason — one less place to update when the category set changes.

**4. No early return. Both requirements always evaluated.**
The spec explicitly says *"Evaluate both requirements completely rather than stopping at the first failure."* C05 must show `MISSING_CATEGORY: BUILD` **and** `POINTS_BELOW_6`. An early `return` after the category loop is the single most likely way to fail that acceptance criterion.

**5. `EnumSet.noneOf(Category.class)` rather than `HashSet`.**
`EnumSet` is bitset-backed and iterates in declaration order, so the covered-categories set that feeds the UI progress strip is already in the right order for free.

**6. Eligible participants get `List.of()` for reasons, computed at the call site.**
The spec says an eligible participant shows *no* failure reasons. Making that a ternary at the single construction point is clearer than having `failureReasons` know about eligibility.

**7. No defensive null checks on `activityIndex.get(...)`.**
Documented as a precondition in a one-line class comment: `evaluate` is only called after validation passed, so every id resolves. Adding "what if it's missing?" branches would be unreachable error handling.

### Discovery made while writing the tests
A test originally named *"five points with all categories still fails"* turned out to be **impossible** to construct. Working through the fixed table: the cheapest way to cover all three categories is `A01`(LEARN,2) + `A04`(BUILD,2) + `A03`(SHARE,2) = **exactly 6**. Any other full-coverage combination uses `A02`(BUILD,3) and scores 7+.

So full category coverage *implies* at least 6 points with this activity table — the two rules can never disagree in the "categories pass, points fail" direction. That makes the `>=` vs `>` boundary the only thing separating eligible from ineligible at the minimum, which is exactly why the spec calls out the exact-point-boundary check. The test was rewritten to assert this insight rather than an impossible scenario.

### Checkpoint result
`EligibilityEvaluatorTest` — 9 tests, covering the entire spec oracle:

| Test | Asserts |
|---|---|
| C01 Asha | 7 points, all categories, eligible, no reasons |
| C02 Bilal | **exactly 6 → eligible** (proves `>=` not `>`) |
| C03 Chen | 7 points but **only** `MISSING_CATEGORY: SHARE` |
| C04 Divya | 7 points but **only** `MISSING_CATEGORY: LEARN` |
| C05 Eshan | 4 points, `MISSING_CATEGORY: BUILD` **then** `POINTS_BELOW_6` |
| Empty list | 0 points, 0 categories, **all four reasons in contracted order** |
| C05 + A04 | 6 points, all three categories, eligible |
| Cheapest full coverage | Sits exactly on the boundary |
| Both requirements fail | Category and point shortfall reported together |

```
Tests run: 27, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

---

## Phase 5 — BoardService  ✅

**Goal:** orchestrate validate → evaluate → sort → summarize, and enforce the "errors clear results" rule in one place.

### File created — `src/main/java/com/campus/campuscertify/service/BoardService.java`

`@Service`, constructor-injected with `BoardState`, `ParticipantValidator`, `EligibilityEvaluator`.
Pass-through methods for reads/writes plus the core `evaluate()`.

### The `evaluate()` pipeline — six steps, exactly as planned
1. Read participants + activity index from `BoardState`
2. `validator.validate(...)`
3. **If errors non-empty → `EvaluationResponse.ofErrors(errors)` and stop**
4. Map each participant through `evaluator.evaluate(...)`
5. Sort with `RESULT_ORDER`
6. Count and return the full envelope

### Design decisions made while writing the code

**1. The comparator trap the plan flagged — avoided, and documented in the code.**
```
Comparator.comparing(ParticipantResult::eligible, Comparator.reverseOrder())
          .thenComparing(ParticipantResult::participantId)
```
Writing `.comparing(...).reversed().thenComparing(...)` instead would reverse the **entire chain built so far**, silently flipping the id ordering to descending. Boolean natural order is `false < true`, so `reverseOrder()` on the eligible key alone puts eligible first while `thenComparing` keeps ids ascending within each group. A one-line comment in the source explains this so a future reader does not "simplify" it back into a bug.

**2. The comparator is a `private static final` field, not built inline.**
It is a fixed rule, so building it per call is wasteful, and naming it `RESULT_ORDER` makes the sort line read as intent.

**3. Step 3 is the *only* place that implements "any input error clears result rows and counts".**
Combined with the `EvaluationResponse.ofErrors` factory from Phase 1, the invariant is enforced twice — once by policy, once by construction.

**4. Summary counted from the results list, with `ineligible = size - eligible`.**
Counting both independently would allow them to disagree; deriving the second guarantees they always sum to the row count.

**5. `deleteParticipant` returns `boolean` so the controller can answer 404 vs 200 later.**

### Checkpoint result
`BoardServiceTest` — 7 tests. **All five required acceptance criteria are now verified end-to-end at the service layer:**

| Test | Acceptance criterion |
|---|---|
| `builtInOracle` | **#1** — totals 7,6,7,7,4; C01+C02 eligible; counts 2/3 |
| `reasonsPerParticipant` | **#2** — C03 only SHARE, C04 only LEARN, C05 BUILD then POINTS_BELOW_6 |
| `addA04ToC05` | **#3** — total 6, all 3 categories, counts 3/2 |
| `clearC01Activities` | **#4** — 0 points, four reasons in order, counts 1/4 |
| `duplicateParticipationClearsResults` | **#5** — DUPLICATE_PARTICIPATION naming C01 and A01, **results empty, summary null** |
| `orderingIsEligibleFirstThenIdAscending` | Ordering — asserts `C01, C05, C02, C03, C04` |
| `resetRestoresOracle` | Reset restores the oracle after edits |

The ordering test is deliberately constructed so a `.reversed()` bug would produce `C05, C01, C04, C03, C02` and fail loudly.

```
Tests run: 34, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

---

## Phase 6 — REST API  ✅

**Goal:** expose the board over HTTP with the envelope contract intact.

### Files created — `src/main/java/com/campus/campuscertify/web/`

**`ParticipantDto.java`** — the only DTO in the project. Carries raw, untrimmed strings from the browser.
Helpers: `from(Participant)`, `toDomain()`, `toDomain(idOverride)`.

**`BoardController.java`** — `@RestController` on `/api`:

| Verb & path | Returns |
|---|---|
| `GET /api/activities` | `List<Activity>` (records serialize directly, no DTO) |
| `GET /api/participants` | `List<ParticipantDto>` |
| `POST /api/participants` | fresh participant list |
| `PUT /api/participants/{id}` | fresh participant list |
| `DELETE /api/participants/{id}` | fresh list, or 404 if the id was absent |
| `POST /api/reset` | fresh participant list |
| `POST /api/evaluate` | `EvaluationResponse` — **always 200** |

### Design decisions made while writing the code

**1. Mutating endpoints return the *fresh full list*.**
One round trip instead of POST-then-GET, and the UI table can never drift from server state — which is exactly the "keep participant inputs synchronized" acceptance criterion.

**2. `PUT` uses the **path** id, not the body id (`toDomain(idOverride)`).**
The id is the resource key; letting the body silently rename it would create a second row instead of editing one.

**3. Bug caught and fixed during writing: `PUT` originally did `delete` then `addOrUpdate`.**
That removes the key and re-inserts it, which moves the row to the **end** of the `LinkedHashMap` and scrambles the display order. `LinkedHashMap.put` on an *existing* key preserves the original insertion position, so the delete was removed. There is now a test (`updateKeepsDisplayOrder`) that edits C01 and asserts it is still row 0 and C05 is still row 4.

**4. `POST /api/evaluate` takes no body and always returns 200.**
It evaluates the server's canonical state. Validation errors are domain output, not HTTP faults, so the frontend gets one `.then()` path with no `catch` branch that has to remember to clear stale results.

**5. Activities are read-only — no POST/PUT exposed.**
The spec says the activity table is fixed; not offering a write endpoint enforces that at the API surface rather than by convention.

**6. Only `Participant` got a DTO.**
`Activity`, `ParticipantResult`, `ValidationError`, `EvaluationSummary` and `EvaluationResponse` are already immutable records shaped exactly like their JSON, so they serialize directly. Creating parallel DTO twins would add files whose only possible contribution is drift.

### Checkpoint result
`BoardControllerTest` — 7 MockMvc tests asserting the real JSON shape:
- `/api/activities` returns the 4 fixed rows in order with correct categories and points
- `/api/participants` returns the 5 built-in rows
- `/api/evaluate` returns the oracle: C01=7 eligible … C05=4 with `MISSING_CATEGORY: BUILD` then `POINTS_BELOW_6`, counts 2/3
- Duplicate participation → **200** with `errors[0].code = DUPLICATE_PARTICIPATION`, `participantId = C01`, `offendingValue = A01`, `results` empty, **`summary` absent from the JSON**
- `PUT` keeps the edited row in its original display position
- Untrimmed input (`"  Eshan  "`, `"  A01"`) is normalized, and the resulting counts are 3/2
- `POST /api/reset` restores the rows and the 2/3 oracle counts

```
Tests run: 41, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### IDE note (not a code problem)
VS Code's Java language server reported ~600 phantom errors like
*"declared package ... does not match expected package src.main.java..."*.
This happens because the workspace was opened **before** `pom.xml` existed, so the extension never imported the Maven project and treated the folder as loose files. The real compiler (`./mvnw clean test`) reports zero errors.
Fix: `Cmd+Shift+P → "Java: Clean Java Language Server Workspace"`, or reload the window.

---

## Phase 7 — Frontend  ✅

**Goal:** one attractive screen with everything the spec asks for, kept synchronized.

### Files created — `src/main/resources/static/`

**`index.html`** — fixed activity table, editable participant table, Evaluate / Reset buttons, add-participant row, validation panel, summary counts, results table, empty state.

**`app.js`** — `api()` fetch helper plus render functions:
`renderActivities`, `renderParticipants`, `categoryStrip`, `renderResults`, `renderValidationErrors`, `renderEvaluation`, `clearEvaluation`, `parseActivityTokens`, `saveParticipant`, `deleteParticipant`, `addParticipant`, `evaluate`, `reset`, `init`.

**`style.css`** — dark theme, per-category colours, three-segment progress strip.

### Design decisions made while writing the code

**1. Every DOM node is built with `createElement` + `textContent`. No `innerHTML` anywhere.**
Participant names and ids are free-text user input rendered back into the results table. String-concatenated `innerHTML` would let a name like `<img src=x onerror=...>` execute. This is the DOM-XSS mitigation identified in the security discussion in `chat1.md` — one line of discipline, not a layer.

**2. `renderEvaluation(response)` is the single place that decides what is visible.**
It flips all four panels (validation / results / summary / empty) from one branch on `response.errors.length`. There is no second code path that could forget to clear something.

**3. `clearEvaluation()` is called after *every* mutation** — edit, add, delete and reset.
Without it, stale result rows survive an edit and the "no stale result rows or counts" criterion fails. Verified interactively: editing C05 made the results and summary panels disappear immediately.

**4. Completed activities are edited as comma-separated text, not checkboxes.**
A checkbox list makes a duplicate `A01` **impossible to enter**, which would make the required `DUPLICATE_PARTICIPATION` scenario undemonstrable. The input control must be able to express invalid states or the validation logic can never be shown working. A hint line under the table states this rationale in the UI itself.

**5. Both row inputs share one `save` closure, declared after both inputs exist.**
The first draft attached the name listener referencing `activityInput` before its `const` declaration. It happened to work (the closure runs after the block completes) but relied on temporal-dead-zone subtleties. Restructured so both inputs are created first, then one shared `save` handler is bound to both.

**6. `categoryStrip` is driven by `result.coveredCategories` from the server.**
The optional progress strip uses the *same derived set* the spec requires — no second derivation in JavaScript that could disagree with the backend.

**7. Points render as `4 / 6` rather than bare `4`**, coloured green/red against the threshold, so the point requirement is visible without reading the reasons column.

### Bugs caught during this phase
- **`PUT` reordering** (fixed in Phase 6, found while wiring the UI edit path).
- **Invisible progress segments** — `.segment.covered` originally set `background: currentColor` *and* `color: currentColor`, making the letter the same colour as its background. Replaced with explicit per-category rules and dark text.
- **Wrong column header** — the participant table's last column read "Row" but contains the Remove button; renamed to "Actions".

### Checkpoint result — all five acceptance scenarios reproduced in the browser

| Scenario | Observed in UI |
|---|---|
| **#1** oracle | C01=7, C02=6, C03=7, C04=7, C05=4; counts **2 / 3** |
| **#2** reasons | C03 → only `MISSING_CATEGORY: SHARE`; C04 → only `MISSING_CATEGORY: LEARN`; C05 → `MISSING_CATEGORY: BUILD` then `POINTS_BELOW_6` |
| **#3** C05 + A04 | 6 / 6, all three strip segments solid, counts **3 / 2**, order `C01, C02, C05, C03, C04` |
| **#4** clear C01 | 0 points, four reasons in order, counts **1 / 4** |
| **#5** duplicate A01 | Validation panel shows `DUPLICATE_PARTICIPATION / C01 / A01`; results and summary panels hidden entirely |
| Reset | Restores the five built-in rows and clears validation, results and counts |
| Sync | Editing any row wipes results and counts immediately |

---

## Phase 8 — Final verification  ✅

### Full clean build
```
Tests run: 1  CampuscertifyApplicationTests
Tests run: 4  DomainModelTest
Tests run: 5  BoardStateTest
Tests run: 8  ParticipantValidatorTest
Tests run: 9  EligibilityEvaluatorTest
Tests run: 7  BoardServiceTest
Tests run: 7  BoardControllerTest
-----------------------------------------
Tests run: 41, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### Final project structure
```
src/main/java/com/campus/campuscertify/
├── CampuscertifyApplication.java
├── domain/     Activity, Category, ErrorCode, EvaluationResponse,
│               EvaluationSummary, Participant, ParticipantResult, ValidationError
├── service/    BoardService, EligibilityEvaluator, ParticipantValidator
├── state/      BoardState
└── web/        BoardController, ParticipantDto

src/main/resources/static/     index.html, app.js, style.css

src/test/java/com/campus/campuscertify/
├── domain/     DomainModelTest
├── service/    BoardServiceTest, EligibilityEvaluatorTest, ParticipantValidatorTest
├── state/      BoardStateTest
└── web/        BoardControllerTest
```

**16 main classes, 3 static files, 6 test classes, 41 tests.**
No database, no JPA, no Lombok, no Node, no build step, no security framework.

### Spec contract → implementation trace

| Contract line | Where it lives | Test |
|---|---|---|
| Trim participant and activity ids | `Participant` constructor / setters | `DomainModelTest`, `ParticipantValidatorTest.trimmingPreventsPhantomIds` |
| Ids and names non-empty, ids unique | `ParticipantValidator.checkIdentity` / `checkUniqueId` | `invalidParticipant`, `duplicateParticipantId` |
| Activity ids must exist in the fixed table | `ParticipantValidator.checkActivities` | `unknownActivity` |
| Repeated activity id is invalid, not double points | `List<String>` in `Participant` + `checkActivities` | `duplicateParticipation`, `participantPreservesDuplicates` |
| Four error codes with participant + offending value | `ErrorCode`, `ValidationError` | `ParticipantValidatorTest` (all) |
| Any input error clears results and counts | `BoardService.evaluate` step 3 + `EvaluationResponse.ofErrors` | `duplicateParticipationClearsResults`, `evaluateWithDuplicateParticipation` |
| Derive category set, sum points once | `EligibilityEvaluator.coveredCategories` / `totalPoints` | `EligibilityEvaluatorTest` (all) |
| All three categories AND points >= 6 | `EligibilityEvaluator.evaluate` | `exactPointBoundaryIsEligible` |
| Evaluate both requirements completely | `failureReasons`, no early return | `c05EvaluatesBothRequirements` |
| Reason order LEARN, BUILD, SHARE, POINTS_BELOW_6 | `Category.values()` iteration | `emptyCompletionList` |
| Eligible first, then id ascending | `BoardService.RESULT_ORDER` | `orderingIsEligibleFirstThenIdAscending` |
| Empty list valid → 0 points, all four reasons | `EligibilityEvaluator` | `emptyCompletionList`, `clearC01Activities` |
| Reset restores fixed activities + five rows, clears results | `BoardState.reset` + `clearEvaluation()` | `resetRestoresStartupState`, `resetEndpoint` |

### How to run
```
./mvnw spring-boot:run      # then open http://localhost:8080
./mvnw test                 # 41 tests
```

### Summary of plan deviations
1. **Spring Boot version discovered, not assumed** — Initializr advertised 4.x, Maven Central only had 3.5.3.
2. **Trimming moved into `Participant`** rather than the controller mapping — makes normalization impossible to bypass.
3. **`PUT` delete-then-insert removed** — it scrambled display order; plain upsert preserves it.
4. **An impossible test was rewritten** — full category coverage implies >= 6 points with this activity table, so "all categories but below threshold" cannot occur.

Everything else followed `plan.md` as written, including both flagged traps
(`Comparator.reversed()` and `Set` vs `List`), which were avoided by design rather than debugged after the fact.

---

# Post-implementation Q&A

Questions raised after the build was complete. Useful interview preparation material.

---

## Q1 — "The declared package does not match the expected package. Remove all these red errors."

**Symptom:** VS Code showed ~600 errors like
`The declared package "com.campus.campuscertify.service" does not match the expected package "src.main.java.com.campus.campuscertify.service" Java(536871240)`

**Diagnosis:** Not a code problem. `./mvnw clean test` passed with 41/41 green the whole time, so the real compiler saw nothing wrong.

The cause: the JDT (Java language server) project was named `CampusCertify_91bba2f8`. That suffixed naming is what VS Code uses for an **"invisible project"** — a plain folder with no recognised build file. A genuine Maven import would have been named `campuscertify`, taken from the `<artifactId>` in `pom.xml`.

The workspace was opened **before** `pom.xml` existed (Phase 0 created it), so the extension classified the folder as loose Java files and never re-scanned. Treating the folder as the source root makes it expect the package to be `src.main.java.com.campus...`, which is exactly the error text.

**Fix:** delete the language server's cached workspace metadata and reload, so the extension re-imports the project as Maven and picks `src/main/java` as the source root.
`Cmd+Shift+P → "Java: Clean Java Language Server Workspace"` does the same thing through the UI.

**Lesson:** when the IDE and the build tool disagree, the build tool is the source of truth. Check `./mvnw clean test` before believing red squiggles.

---

## Q2 — "Why two folders, `java` and `test`? Why not all in one? Why is test not inside main?"

Because they compile to **two different places** and only one of them ships. This is Maven's Standard Directory Layout — a hard convention, not a style choice.

```
src/main/java   ──compiles to──▶  target/classes        ──▶ goes INTO the JAR
src/test/java   ──compiles to──▶  target/test-classes   ──▶ discarded after tests run
```

**Demonstrated on this project.** Running `./mvnw package -DskipTests` and listing the JAR contents produced 15 main classes plus the three static files, and **zero** `Test` classes:

```
BOOT-INF/classes/static/index.html
BOOT-INF/classes/static/style.css
BOOT-INF/classes/static/app.js
BOOT-INF/classes/com/campus/campuscertify/web/BoardController.class
BOOT-INF/classes/com/campus/campuscertify/service/BoardService.class
...   (no DomainModelTest, no BoardServiceTest, no BoardControllerTest)
```

### The four reasons

**1. Test code must never ship.**
`BoardServiceTest` contains fake participants and an "Intruder" row. None of that belongs in a deployable artifact. The separation makes shipping a test structurally impossible.

**2. The classpaths genuinely differ.**
In `pom.xml`:
```xml
<artifactId>spring-boot-starter-test</artifactId>
<scope>test</scope>
```
`scope=test` means JUnit, AssertJ, Mockito and MockMvc exist **only** for `src/test/java`. Moving a test into `src/main/java` would fail to compile — `import org.junit.jupiter.api.Test` would not resolve. The compiler enforces the boundary. It also keeps those libraries out of the shipped JAR.

**3. Maven's lifecycle depends on it.**

| Phase | Acts on |
|---|---|
| `compile` | `src/main/java` only |
| `test-compile` | `src/test/java` only |
| `test` | runs `target/test-classes` |
| `package` | bundles `target/classes` only |

`./mvnw package -DskipTests` works precisely because Maven knows which folder is which.

**4. Same package, different folder — deliberately.**
```
src/main/java/com/campus/campuscertify/service/BoardService.java
src/test/java/com/campus/campuscertify/service/BoardServiceTest.java
```
Both declare `package com.campus.campuscertify.service`. At **test runtime** Maven puts both output folders on one classpath, so a test can reach package-private members of the class it tests. At **package time** only `target/classes` is used. Same package for access, different folder for packaging.

### What breaks if merged into one folder

| Problem | Consequence |
|---|---|
| Tests ship in the JAR | Bigger artifact, test code in production |
| JUnit/AssertJ become `compile` scope | Test libraries bundled into the deployable |
| `-DskipTests` can't work | No way to distinguish what to skip |
| Tooling breaks | IDEs, CI, coverage, Sonar all assume this layout |
| Interview optics | Non-standard layout reads as unfamiliarity with the ecosystem |

### Mapping to this project

| Folder | Contents | Ships? |
|---|---|---|
| `src/main/java` | 15 classes — the app | ✅ |
| `src/main/resources/static` | index.html, app.js, style.css | ✅ (as `BOOT-INF/classes/static/`) |
| `src/test/java` | 6 test classes, 41 tests | ❌ |

Both `main` folders ship — `java` and `resources` are just different *kinds* of content (compiled code vs. files served as-is). That is why the frontend lives under `src/main/resources/static/` and Spring Boot serves it automatically at `/`.

**One-sentence version:** *"`src/main` is what runs in production, `src/test` is what proves it works — Maven compiles them separately so test code and test dependencies never reach the deployable artifact."*

---

## Q3 — "Why `com.campus.campuscertify`? What is `campus`? I don't have a folder called campus."

It does exist — three nested folders:

```
src/main/java/com/campus/campuscertify/
├── domain/
├── service/
├── state/
└── web/
```

**Why it looks like one folder:** VS Code's **compact folders** feature collapses a chain of single-child folders onto one row, so `com/` → `campus/` → `campuscertify/` renders as `com.campus.campuscertify`. Disable via Settings → `explorer.compactFolders`.

**Where the name came from:** the Spring Initializr URL used in Phase 0:
```
groupId=com.campus&artifactId=campuscertify&packageName=com.campus.campuscertify
```

**What the coordinates mean:**

| Maven term | Value | Purpose |
|---|---|---|
| `groupId` | `com.campus` | Who owns it — the organization namespace |
| `artifactId` | `campuscertify` | What it is — the project name |
| `version` | `0.0.1-SNAPSHOT` | Which build |

Together: `com.campus:campuscertify:0.0.1-SNAPSHOT`, a globally unique identifier.

**Why the dotted format:** Java packages use **reverse domain name** convention. Owning `campus.com` gives `com.campus`. It prevents collisions — two organisations can each define an `Activity` class because one is `com.campus.campuscertify.domain.Activity` and the other is `org.example.foo.Activity`.

The Java compiler **enforces** the mapping: a file declaring `package com.campus.campuscertify.domain;` must live at `com/campus/campuscertify/domain/`. That is a language rule, not a convention — and it is exactly why the language server in Q1 complained when it failed to recognise `src/main/java` as the source root.

`com.campus` is idiomatic and was kept. Alternatives considered: `com.yourname`, `io.github.username`, `com.college`.

---

## Q4 — "So implementation part is done? Code is done?"

**Yes — confirmed with a fresh clean build:** 41/41 tests passing, zero IDE errors.

| Layer | Status |
|---|---|
| Domain model (8 types) | ✅ |
| `BoardState` (fixed activities + mutable participants) | ✅ |
| `ParticipantValidator` (4 error codes) | ✅ |
| `EligibilityEvaluator` (categories, points, reasons) | ✅ |
| `BoardService` (orchestration, sort, summary) | ✅ |
| REST API (7 endpoints) | ✅ |
| Frontend (HTML/CSS/JS, dark theme, progress strip) | ✅ |
| All 5 required acceptance scenarios | ✅ verified via tests + live HTTP + browser clicks |
| Test suite | ✅ 41 tests, 6 test classes |

**What remains is not code, but interview packaging:**
1. Live demo readiness — already true (`./mvnw spring-boot:run`)
2. Prompt history / iteration narrative — already captured in `Chats/Idea.md` and `Chats/Implementation.md`
3. A full screenshot set covering all 5 acceptance scenarios as test evidence
4. Rehearsing the live-modification exercise (change `REQUIRED_POINTS`, add a category, add an activity)
5. Git history — not yet committed at the time this question was asked

---

## Q5 — "Login git and commit all these"

**No login was actually needed.** Git identity was already configured globally:
```
user.name  = Shumaim Quratulain
user.email = shumaimquaratulain@gmail.com
```
This was a purely local repository at this point (no remote), so no authentication was required — "login" only applies once pushing to a remote host.

### What was done
```
git init -b main
git add -A
git commit -m "CampusCertify: Certificate Eligibility Board implementation ..."
```

**Result:** 37 files, 4078 insertions, single commit `902a772` on branch `main`.

### What's under version control
- Full Spring Boot app (`src/main`, `src/test`) — 16 main classes, 6 test classes, 3 static frontend files
- `pom.xml`, `mvnw` / `mvnw.cmd` (Maven Wrapper)
- Planning docs: `Chats/Idea.md`, `Chats/Implementation.md`, `Chats/plan.md`
- Both problem-statement PDFs

### Correctly excluded via `.gitignore`
`target/` (build output), `.vscode/` (local editor settings), `HELP.md` (Spring Initializr boilerplate).

---

## Q6 — "I have created a repository in GitHub named campusCertify. How to upload this project?"

### Checked first
- GitHub CLI (`gh`) — not installed, so used plain `git` + HTTPS remote instead
- No remote configured yet on the local repo

### Steps taken
1. Asked for the repo URL and connection method (HTTPS vs SSH) — user provided:
   `https://github.com/Shumaim-quratulain/CampusCertify.git`, HTTPS.
2. `git remote add origin https://github.com/Shumaim-quratulain/CampusCertify.git`
3. `git push -u origin main` → **rejected**:
   ```
   ! [rejected]  main -> main (fetch first)
   ```

### Diagnosis
GitHub auto-creates a `README.md` commit when a repository is created through the web UI (if that option is left checked). The remote's `main` therefore already had one commit (`Initial commit`, containing only `README.md`) that the local repo didn't know about — a classic diverged-history rejection, not an authentication problem. Confirmed via:
```
git fetch origin
git log origin/main --oneline        → 3defc98 Initial commit
git ls-tree -r --name-only origin/main → README.md
```

### Fix
```
git rebase origin/main
```
Replayed the local commit on top of the remote's README commit, producing clean linear history:
```
* c809680 CampusCertify: Certificate Eligibility Board implementation
* 3defc98 Initial commit
```
Then:
```
git push -u origin main
```
**Result:** pushed successfully — 63 objects, ~198 KB. Branch `main` now tracks `origin/main`. Project live at:
**https://github.com/Shumaim-quratulain/CampusCertify**

### Lesson
"Rejected — fetch first" almost always means diverged history, not a permissions/auth problem — check `git log origin/<branch> --oneline` before assuming credentials are the issue. A `rebase` (not `merge`) was the right call here because the remote had exactly one small, unrelated commit and a linear history reads better for a project being reviewed.

---

## Phase 9 — Execution & deployment readiness  ✅

**Decision:** local execution readiness + a standalone runnable JAR only. **No cloud deployment.**

**Why:** the problem statement says *"Evaluate local records only"* and names a browser/local tool as the expected form — it never asks for hosting. The STUDENT_GUIDE explicitly warns against *"complex frameworks or unnecessary layers for the given scope,"* and a cloud deploy (Azure/Render/Railway) is exactly that — extra infrastructure that doesn't move any grading criterion. What the guide *does* grade is *"Development Environment Ready"* and *"Quick startup"* — a local concern, not a hosting concern.

### 1. Standalone executable JAR
```
./mvnw clean package
```
Produced `target/campuscertify-0.0.1-SNAPSHOT.jar` (~21 MB, embedded Tomcat included).

**Verified it runs with no Maven, no wrapper, no IDE:**
```
java -jar target/campuscertify-0.0.1-SNAPSHOT.jar
```
Started in **1.083 seconds**. Confirmed live via `/api/evaluate` — same oracle (7,6,7,7,4 / counts 2,3) as every other run. This is the artifact you'd hand to someone with only a JDK installed.

### 2. Dev-mode startup timing (the actual interview command)
```
./mvnw spring-boot:run
```
Started in **0.757 seconds** — well within "quick startup."

### 3. Live-modification rehearsal — raise the eligibility threshold
Simulated the most likely live-modification ask: *"raise the point requirement to 7."*

**Change:** one line in `EligibilityEvaluator`:
```java
public static final int REQUIRED_POINTS = 6;   →   = 7;
```

**Result of `./mvnw test` immediately after:**
- The failure-reason string **automatically became `POINTS_BELOW_7`** — no second edit needed, because it's derived (`"POINTS_BELOW_" + REQUIRED_POINTS`) rather than a separate literal.
- **15 of 41 tests failed** — all of them oracle tests that hardcode the spec's example data, which assumes a 6-point threshold (e.g. C02 at exactly 6 points, previously eligible, is now correctly ineligible).

**Why this is good, not bad:** it proves the test suite verifies real behavior instead of just existing. A grader asking "how do you know this one-line change didn't silently break something?" gets an immediate, concrete answer — the suite caught every ripple.

**Reverted** `REQUIRED_POINTS` back to `6`, re-ran tests: **41/41 green again.**

### Demo script for the interview (rehearsed)
1. `./mvnw spring-boot:run` → open `http://localhost:8080`
2. Click **Evaluate** → built-in oracle: totals 7,6,7,7,4; counts 2 eligible / 3 ineligible
3. Edit C05's activities to add `A04` → **Evaluate** → 6/6, counts 3/2
4. Click **Reset sample** → clear C01's activities → **Evaluate** → 0 points, 4 reasons, counts 1/4
5. **Reset sample** → add a second `A01` to C01 → **Evaluate** → `DUPLICATE_PARTICIPATION` banner, no stale rows/counts
6. **Live modification:** change `REQUIRED_POINTS` in `EligibilityEvaluator`, re-run `./mvnw test`, show the test suite catching the ripple, then revert

### Checkpoint
- [x] `java -jar` runs standalone (1.083s startup)
- [x] `./mvnw spring-boot:run` runs for live dev (0.757s startup)
- [x] Full acceptance-scenario oracle re-verified against the standalone JAR
- [x] Live-modification exercise rehearsed end-to-end, including the test-suite safety net
- [x] Suite returned to 41/41 green after revert


