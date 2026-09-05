# CampusCertify — Execution & Deployment Log (execution.md)

Continuation of `Implementation.md` (build phase). This file records **execution and deployment readiness**.

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

---

## Plain-language execution steps

### Step 1 — Open a terminal in the project folder
```bash
cd /Users/shumaimquratualain/Documents/CampusCertify
```

### Step 2 — Start the application
```bash
./mvnw spring-boot:run
```
Wait for: `Started CampuscertifyApplication in 0.7... seconds`. Leave this terminal running — it's the live server; don't close it or press Ctrl+C while the app should stay up.

### Step 3 — Open the website
```
http://localhost:8080
```
Shows the Certificate Eligibility Board — fixed activity table, editable participant table, Evaluate/Reset buttons.

### Step 4 — Use the app

| Action | What to do |
|---|---|
| Score everyone | Click **Evaluate** |
| Edit a participant | Click into the Name or Activity IDs box for that row, change it, press Tab or click elsewhere (triggers save) |
| Add a participant | Fill the three boxes at the bottom (ID, name, activity IDs like `A01, A02`) → click **Add participant** |
| Remove a participant | Click **Remove** on that row |
| Restore sample data | Click **Reset sample** |

### Step 5 — Stop the application
In the terminal running the server, press `Control + C`.

### Quick reference card
```bash
cd /Users/shumaimquratualain/Documents/CampusCertify   # go to project folder
./mvnw spring-boot:run                                    # start server
```
→ open **http://localhost:8080** → click **Evaluate** → press **Ctrl+C** to stop.

### Alternative — run the packaged JAR (no Maven wrapper needed)
```bash
java -jar target/campuscertify-0.0.1-SNAPSHOT.jar
```
Same result, same URL — the "hand it to someone with just a JDK" version. Requires `./mvnw clean package` to have been run first.

### Run the tests instead (no browser needed)
```bash
./mvnw test
```
Expected: `Tests run: 41, Failures: 0, Errors: 0, Skipped: 0` and `BUILD SUCCESS`.

---

## Commit verification and file-integrity check

**Q: "Is implementation, execution, and deployment all committed?"**

Checked `git status`, local log, remote log, and diffed local vs `origin/main` — all identical at that point, working tree clean.

**Caught along the way:** `Chats/execution.md` had been silently emptied on disk (113 lines → blank), unrelated to any tool action in this session — likely an editor autosave glitch. Confirmed the last committed version on GitHub still had the full content, then restored it:
```bash
git restore --staged Chats/execution.md
git checkout -- Chats/execution.md
```
Result: 114 lines back, `nothing to commit, working tree clean`. Nothing was lost because the file had already been pushed before the local copy was emptied.

**Lesson:** before committing any file, check whether a local file was unexpectedly emptied/shrunk versus the remote — `git diff origin/main --stat` surfaces this immediately as a large deletion with no corresponding edit.

---

## Committing via the VS Code UI (no terminal)

For manual commits without typing git commands:

1. Click the **Source Control** icon in the left sidebar (or `Ctrl+Shift+G` / click the icon directly)
2. Review the **Changes** list — click any file to see a diff (red = removed, green = added)
3. Stage changes: hover a file → click **`+`**, or stage everything via the **Changes** header **`+`**
   (skippable — committing with nothing staged auto-stages everything, same as `git add -A && git commit`)
4. Type a commit message in the box at the top
5. Click **Commit**
6. Click **Sync Changes** (cloud icon, sometimes shows "1↑") to push to GitHub

```
Source Control icon → Changes list → type message → Commit → Sync Changes
```

Git identity (`Shumaim Quratulain` / `shumaimquaratulain@gmail.com`) is already configured globally, so no login prompt is expected.

---

## Postman collection for manual API testing

Added [postman/CampusCertify.postman_collection.json](../postman/CampusCertify.postman_collection.json) — 9 pre-built requests covering every REST endpoint, all verified live against the running server before handing off:

| # | Request | Method & path | Verifies |
|---|---|---|---|
| 1 | GET Activities | `GET /api/activities` | The 4 fixed rows |
| 2 | GET Participants | `GET /api/participants` | Current rows |
| 3 | POST Reset | `POST /api/reset` | Restores the 5 built-in rows |
| 4 | POST Evaluate | `POST /api/evaluate` | Oracle: totals 7,6,7,7,4, counts 2/3 |
| 5 | POST Add participant | `POST /api/participants` | Adds C06/Farah |
| 6 | PUT Update participant | `PUT /api/participants/C05` | Adds A04 to C05 |
| 7 | DELETE participant | `DELETE /api/participants/C06` | Removes C06 |
| — | Scenario: Clear C01 activities | `PUT /api/participants/C01` | 0 points, 4 reasons, counts 1/4 |
| — | Scenario: Duplicate A01 on C01 | `PUT /api/participants/C01` | `DUPLICATE_PARTICIPATION`, no results |

**Verification method:** each request body was replayed via `curl` against the live `./mvnw spring-boot:run` instance before committing — confirmed 200 responses and correct payloads (activity list, oracle summary `{eligibleCount:2, ineligibleCount:3}`, 6-row participant list after add, 200 on delete).

**Import steps:** Postman → File → Import → select the JSON file → 9 requests appear under a "CampusCertify" collection, using a `{{baseUrl}}` variable (`http://localhost:8080`) so the port is changeable in one place.

**Suggested demo flow in Postman:** Reset → Evaluate (see oracle) → Update C05 (add A04) → Evaluate again (counts 3/2) → Reset → run the duplicate-participation scenario → Evaluate (see `errors` populated, `results` empty, `summary` absent).

