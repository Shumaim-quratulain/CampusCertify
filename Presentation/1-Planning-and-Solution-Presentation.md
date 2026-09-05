# 1. Planning and Solution Presentation

## The 3–5 Step Implementation Plan

1. **Domain & State** — define the fixed data (`Activity`, `Category`) and the mutable data (`Participant`), plus the in-memory `BoardState` holding both with a `reset()` that restores the 5 built-in rows.
2. **Business Logic** — `ParticipantValidator` (4 error codes, collect-all) and `EligibilityEvaluator` (categories, points, ordered failure reasons), kept as two separate stateless components with no Spring dependency, so they're instantly unit-testable.
3. **Orchestration & REST API** — `BoardService` ties validation → evaluation → sorting → summary into one pipeline; `BoardController` exposes it over 7 REST endpoints.
4. **Frontend** — a single static HTML/CSS/JS page (no build step) that renders the fixed table, an editable participant table, and results — synced so any edit clears stale results.
5. **Verification** — 41 automated tests plus a full manual walkthrough of all 5 required acceptance scenarios, in both the API and the browser.

## How the work followed — and changed — the plan

The plan started as a 9-phase engineering blueprint (see `Chats/plan.md`) because that's genuinely how the code was built, phase by phase with a checkpoint after each. The 5 steps above are that same work condensed to presentation scale.

**What changed along the way, and why:**

| Planned | What actually happened | Why |
|---|---|---|
| Spring Boot 3.x (assumed) | Had to discover the exact resolvable version empirically | Spring Initializr advertised Boot 4.x; Maven Central only had 3.5.3 published |
| Trim input in the controller/DTO layer | Moved trimming into the `Participant` constructor itself | Makes normalization impossible to bypass — any code path that builds a `Participant` gets canonical values |
| `PUT` implemented as delete-then-insert | Changed to a plain upsert | Delete-then-insert moved the edited row to the end of the display list, breaking "keep participant inputs synchronized" |
| A test asserting "all categories but below threshold" | Rewritten | Discovered this scenario is mathematically impossible with the given activity table — the cheapest full-category-coverage combination is *exactly* 6 points |

## Demonstrating the working solution

- **Run it:** `./mvnw spring-boot:run` → `http://localhost:8080`
- **Prove it:** `./mvnw test` → 41/41 passing
- **Show it live:** walk through the 5 required acceptance scenarios in the browser (see `5-Testing-and-Validation.md` for the exact sequence and expected numbers)
