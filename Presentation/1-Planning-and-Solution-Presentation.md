# 1. Planning and Solution Presentation

## Problem summary (in one paragraph)

CampusCertify is a College Event Certificate Eligibility Board. It loads a **fixed activity table** (4 activities, each with a category and a point value) and an **editable participant table** (5 built-in participants, each with a list of completed activity IDs). On demand, it evaluates every participant: they're **ELIGIBLE** only if their completed activities cover all three categories — LEARN, BUILD, SHARE — **and** their total points are **≥ 6**. Invalid input (blank fields, duplicate IDs, unknown activities, duplicate participation) is reported with structured error codes instead of being silently ignored, and any input error clears prior results so nothing stale is ever shown.

## The 3–5 Step Implementation Plan (as presented)

### Step 1 — Domain & State
**What:** Define the fixed data (`Activity` record, `Category` enum) and the mutable data (`Participant` class), plus an in-memory `BoardState` holding both, with a `reset()` that restores the 5 built-in rows.
**Why first:** Nothing else can be built or tested without a data model to build against.
**Checkpoint:** `BoardStateTest` — the fixed activity table matches the spec exactly, the 5 built-in participants match on startup, and reset restores them byte-for-byte after arbitrary mutation (delete a row, add an intruder row, clear another row's activities — reset undoes all of it).

### Step 2 — Business Logic
**What:** `ParticipantValidator` (4 error codes: `INVALID_PARTICIPANT`, `DUPLICATE_PARTICIPANT_ID`, `UNKNOWN_ACTIVITY`, `DUPLICATE_PARTICIPATION`, collecting every error rather than stopping at the first) and `EligibilityEvaluator` (covered categories, total points, ordered failure reasons).
**Why separated:** They answer different questions — "is the input well-formed?" vs. "does this person qualify?" — so eligibility tests never need malformed fixtures and validation tests never need point arithmetic. Both are plain Spring `@Component` classes with zero HTTP dependency, so they're unit-testable with no Spring context at all.
**Checkpoint:** `ParticipantValidatorTest` (8 tests) and `EligibilityEvaluatorTest` (9 tests) — every contracted error code and every entry from the spec's built-in oracle verified individually.

### Step 3 — Orchestration & REST API
**What:** `BoardService` ties validation → evaluation → sorting → summary into one six-step pipeline; `BoardController` exposes it over 7 REST endpoints (`GET/POST/PUT/DELETE /api/participants`, `GET /api/activities`, `POST /api/reset`, `POST /api/evaluate`).
**Why this shape:** `POST /api/evaluate` always returns HTTP 200 with an envelope `{errors, results, summary}` — when `errors` is non-empty, `results` is `[]` and `summary` is `null`, by construction (`EvaluationResponse.ofErrors(...)` / `.ofResults(...)`). This turns "any input error clears result rows and counts" from a rule someone has to remember into something that's structurally impossible to violate.
**Checkpoint:** `BoardServiceTest` (7 tests, one per required acceptance scenario) and `BoardControllerTest` (7 MockMvc tests) verifying the real JSON shape.

### Step 4 — Frontend
**What:** A single static HTML/CSS/JS page (`index.html`, `app.js`, `style.css`) — no Node, no build step, no CORS — served straight from Spring Boot's `static/` resources. Renders the fixed table, an editable participant table (activities edited as comma-separated tokens), Evaluate/Reset buttons, a validation panel, a summary panel, a results table, and an optional 3-segment LEARN/BUILD/SHARE progress strip per participant.
**Why plain JS:** Matches the STUDENT_GUIDE's own advice to "use technologies you can explain and debug confidently," and avoids the "complex frameworks... for the given scope" penalty explicitly named as something to avoid.
**Key synchronization rule:** every write action (add/edit/delete/reset) calls a single `clearEvaluation()` function, so stale results can never survive an edit — verified manually by editing a row and watching the results panel disappear immediately.

### Step 5 — Verification
**What:** 41 automated tests (JUnit 5 + AssertJ + MockMvc) plus a full manual walkthrough of all 5 required acceptance scenarios, exercised three separate ways: unit tests, raw `curl`/Postman calls against the running server, and clicking through the actual browser UI.
**Why three ways:** Each layer can hide a bug the others catch — a passing unit test doesn't prove the JSON serializes correctly, and a working API doesn't prove the frontend renders it correctly. Requiring all three to agree is what makes the acceptance-criteria numbers trustworthy.

## Guide-compliance mapping (why this plan satisfies the rubric, line by line)

| Guide requirement | How this plan satisfies it |
|---|---|
| Prompt history | `Chats/Idea.md` — the real, unedited conversation |
| Iteration examples (broad → specific) | See `2-AI-Prompting-Strategy.md` |
| Architecture decisions (prioritize simplicity) | See `3-Design-Constraints-and-Technology-Choices.md` |
| AI influence on design | See `4-AI-Influenced-Decision-Making.md` |
| Trade-offs | Same |
| Test plan + edge cases | See `5-Testing-and-Validation.md` |
| Live modification ready | See `6-Live-Modification-Capability.md` |
| ⚠ Avoid over-engineering | Drove real cuts: 4 planned store-interface types collapsed to 1 `BoardState` class; a DTO-per-domain-type plan cut to just 1 DTO; a planned `@ControllerAdvice` removed entirely |

## How the work actually followed — and changed — this plan

The plan was originally built out as a **9-phase engineering blueprint** (`Chats/plan.md`), because that's genuinely how the code was constructed — phase by phase, with a green-test checkpoint after each one before moving on. The 5 steps above are that same work condensed to presentation scale; nothing was skipped, only re-grouped.

**Concrete deviations from the original plan, and why each happened:**

| Planned | What actually happened | Why |
|---|---|---|
| Spring Boot 3.x (assumed available) | Had to discover the exact resolvable version empirically | Spring Initializr's own metadata advertised a `>= 4.0.0` compatibility range and generated a POM pinned to `4.1.1.RELEASE` — but that artifact doesn't exist on Maven Central. Queried Central directly (`search.maven.org`) and found `3.5.3` is the newest version actually published, then reverted the Boot-4-style starter names (`spring-boot-starter-webmvc`) back to the Boot-3 names (`spring-boot-starter-web`) |
| Trim input in the controller/DTO mapping layer | Moved trimming into the `Participant` constructor and setters directly | Makes normalization **impossible to bypass** — any code path that constructs a `Participant` gets canonical values automatically. If `"C01"` and `"C01 "` could both reach the store untrimmed, `DUPLICATE_PARTICIPANT_ID` would never fire for a genuinely duplicated but differently-spaced ID |
| `PUT /api/participants/{id}` implemented as `delete()` then `addOrUpdate()` | Changed to a plain upsert (`Map.put` on the existing key) | Delete-then-insert on a `LinkedHashMap` moves the row to the **end** of iteration order, silently breaking "keep participant inputs synchronized" — caught while wiring the frontend edit path, before it ever shipped |
| A planned test: "all categories covered but still below the point threshold" | Rewritten entirely | Discovered while writing it that this scenario is **mathematically impossible** with the given activity table — the cheapest combination that covers all three categories (`A01`+`A04`+`A03` = 2+2+2) sums to *exactly* 6, the threshold itself. Rewrote the test to assert this insight instead of an unreachable case |
| 4 separate store-interface types (`ActivityStore`/`InMemoryActivityStore`/`ParticipantStore`/`InMemoryParticipantStore`) | Collapsed into 1 `BoardState` class | The STUDENT_GUIDE explicitly penalizes "unnecessary layers for the given scope"; interfaces exist to enable substitution, and there is no second implementation and never will be, since the spec forbids persistence-dependent features |
| A DTO per domain type | Cut to just 1 (`ParticipantDto`) | Every other domain type (`Activity`, `ParticipantResult`, `ValidationError`, `EvaluationSummary`, `EvaluationResponse`) is already an immutable record shaped exactly like its JSON — extra DTO twins would only add files that could drift from the real model |
| A `@ControllerAdvice` global exception handler | Removed entirely | There are no expected exceptions — validation errors are structured **data**, not faults, so an unused exception handler would be dead code |

## Demonstrating the working solution

- **Run it:** `./mvnw spring-boot:run` (or `Cmd+Shift+B` in VS Code, zero typing) → open `http://localhost:8080`
- **Prove it automatically:** `./mvnw test` → `Tests run: 41, Failures: 0, Errors: 0, Skipped: 0` — `BUILD SUCCESS`
- **Prove it as a standalone artifact:** `./mvnw clean package && java -jar target/campuscertify-0.0.1-SNAPSHOT.jar` — starts in ~1 second with no Maven, no IDE, just a JDK
- **Show it live:** walk through all 5 required acceptance scenarios in the browser, in the exact order and with the exact expected numbers documented in `5-Testing-and-Validation.md`
- **Show it's genuinely modifiable:** perform the rehearsed live modification from `6-Live-Modification-Capability.md` on request
