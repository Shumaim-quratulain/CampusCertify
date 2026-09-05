# CampusCertify — College Event Certificate Eligibility Board

A compact tool for an organizing team to review activity participation, look up points/categories per completed activity, and determine which participants have earned enough points across all required categories.

**Eligibility rule:** a participant is ELIGIBLE only if their completed activities cover **LEARN**, **BUILD**, and **SHARE**, and their total points are **≥ 6**.

## Tech stack

- **Backend:** Java 21, Spring Boot 3.5.3 (Maven, via the included wrapper — no global Maven install required)
- **Frontend:** plain HTML/CSS/JavaScript, served as static resources from the same JAR (no Node, no build step)
- **Storage:** in-memory only, as permitted by the assignment ("evaluate local records only") — no database
- **Tests:** JUnit 5 + AssertJ + Spring's MockMvc, 41 tests across 6 test classes

## Project structure

```
src/main/java/com/campus/campuscertify/
├── domain/     Category, Activity, Participant, ErrorCode, ValidationError,
│               ParticipantResult, EvaluationSummary, EvaluationResponse
├── service/    ParticipantValidator, EligibilityEvaluator, BoardService
├── state/      BoardState  (fixed activities + mutable participants, reset)
└── web/        BoardController (REST API), ParticipantDto

src/main/resources/static/   index.html, app.js, style.css  (the dashboard)
src/test/java/...            41 tests mirroring the package structure above
postman/                     Postman collection covering every REST endpoint
Chats/                       Full design/implementation/execution log (see below)
```

## Running it

**Option A — dev mode (recommended)**
```bash
./mvnw spring-boot:run
```
Then open **http://localhost:8080**.

**Option B — standalone JAR**
```bash
./mvnw clean package
java -jar target/campuscertify-0.0.1-SNAPSHOT.jar
```

**Option C — VS Code**
Open the workspace, then either:
- press `Cmd+Shift+B` (runs the pre-configured task), or
- click the ▷ icon in the editor toolbar (runs the same command via a workspace setting)

## Running the tests

```bash
./mvnw test
```
Expected: `Tests run: 41, Failures: 0, Errors: 0, Skipped: 0` — `BUILD SUCCESS`.

## API

All endpoints are under `/api`. See [postman/CampusCertify.postman_collection.json](postman/CampusCertify.postman_collection.json) for ready-to-import requests.

| Method & path | Purpose |
|---|---|
| `GET /api/activities` | Fixed activity table (4 rows, read-only) |
| `GET /api/participants` | Current participant rows |
| `POST /api/participants` | Add a participant |
| `PUT /api/participants/{id}` | Edit a participant (by path id) |
| `DELETE /api/participants/{id}` | Remove a participant |
| `POST /api/reset` | Restore the 5 built-in participant rows |
| `POST /api/evaluate` | Validate + score all participants; always returns `200` |

`POST /api/evaluate` returns `{ errors, results, summary }`. When `errors` is non-empty, `results` is empty and `summary` is `null` — any input error clears prior results and counts.

## Design notes

- **Validation before evaluation.** Every participant is checked for `INVALID_PARTICIPANT`, `DUPLICATE_PARTICIPANT_ID`, `UNKNOWN_ACTIVITY`, and `DUPLICATE_PARTICIPATION` before any scoring happens. If any error exists, evaluation stops and nothing scores.
- **`REQUIRED_POINTS` is a single named constant** (`EligibilityEvaluator`) — the point threshold and its failure message are derived from one place, making it a genuine one-line change.
- **`Category.values()` declaration order** (`LEARN, BUILD, SHARE`) is the contracted order for `MISSING_CATEGORY` reasons — no separate list to keep in sync.
- **Completed activity IDs are a `List`, not a `Set`**, so a duplicate entry is representable and can be reported as `DUPLICATE_PARTICIPATION` instead of being silently deduplicated.

## Documentation

The full design-to-deployment narrative — architecture decisions, prompts used, bugs found and fixed, and rehearsed demo scripts — is in [Chats/](Chats/):

| File | Contents |
|---|---|
| [Idea.md](Chats/Idea.md) | Requirements exploration and architecture discussion |
| [plan.md](Chats/plan.md) | The locked-in implementation plan |
| [Implementation.md](Chats/Implementation.md) | Phase-by-phase build log with design rationale |
| [execution.md](Chats/execution.md) | Packaging, startup verification, live-modification rehearsal, and tooling fixes |