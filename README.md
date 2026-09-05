# 🎓 CampusCertify — College Event Certificate Eligibility Board

<p align="center">
  <strong>Spring Boot REST API + Static Frontend — Certificate Eligibility Evaluation System</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=java" />
  <img src="https://img.shields.io/badge/Spring_Boot-3.5.3-brightgreen?style=flat-square&logo=spring" />
  <img src="https://img.shields.io/badge/Maven-Wrapper-blue?style=flat-square&logo=apachemaven" />
  <img src="https://img.shields.io/badge/Storage-In--Memory-lightgrey?style=flat-square" />
  <img src="https://img.shields.io/badge/Frontend-HTML%2FCSS%2FJS-yellow?style=flat-square&logo=html5" />
  <img src="https://img.shields.io/badge/Tests-41%20Passing-success?style=flat-square&logo=junit5" />
  <img src="https://img.shields.io/badge/Postman-API_Tested-orange?style=flat-square&logo=postman" />
</p>

<p align="center">
  <em>A compact eligibility board for an organizing team to review activity participation, aggregate points per participant, and determine who has earned a certificate by covering every required activity category.</em>
</p>

---

## 📋 Table of Contents

- [Overview](#-overview)
- [Key Features](#-key-features)
- [System Architecture](#-system-architecture)
- [Technology Stack](#-technology-stack)
- [Project Structure](#-project-structure)
- [Fixed Activity Table](#-fixed-activity-table)
- [Eligibility Rules](#-eligibility-rules)
- [API Endpoints](#-api-endpoints)
- [Validation & Error Codes](#-validation--error-codes)
- [Design Decisions](#-design-decisions)
- [Local Setup & Running](#-local-setup--running)
- [Running the Tests](#-running-the-tests)
- [Postman API Testing](#-postman-api-testing)

---

## 🔍 Overview

CampusCertify is an internal tool built for a college event organizing team. It loads a **fixed activity table** with point values and categories, maintains an **editable participant table**, and on demand evaluates every participant's eligibility by checking whether their completed activities cover all three required categories (LEARN, BUILD, SHARE) and reach the minimum point threshold.

The application runs entirely in-memory — no database, no accounts, no external services — exactly matching the scope defined in the problem statement: *"evaluate local records only."*

---

## ✨ Key Features

- ✅ **One-click evaluation** — scores all participants in a single action
- ✅ **Strict validation** — four error codes with the exact participant ID and offending value
- ✅ **Ordered failure reasons** — `MISSING_CATEGORY: LEARN` → `BUILD` → `SHARE` → `POINTS_BELOW_6` (in that exact contract order)
- ✅ **Correct sort order** — eligible participants first, then ineligible, each group sorted by participant ID ascending
- ✅ **Synchronized UI** — any edit wipes stale results immediately, so counts are never stale
- ✅ **Three-segment category progress strip** — visual LEARN / BUILD / SHARE coverage per participant
- ✅ **Reset / Sample** — restores the five built-in participants with one click
- ✅ **41 automated tests** — JUnit 5 + AssertJ + MockMvc, covering every acceptance criterion

---

## 🏗 System Architecture

```
Browser (index.html + app.js + style.css)
        │   fetch JSON (HTTP)
        ▼
┌─────────────────────────────────────────┐
│           Spring Boot (port 8080)       │
│                                         │
│  BoardController  (/api/* — 7 routes)   │
│        │                                │
│        ▼                                │
│  BoardService  (orchestrator)           │
│    ├── ParticipantValidator             │
│    │     └── 4 error codes             │
│    ├── EligibilityEvaluator             │
│    │     └── categories + points       │
│    └── BoardState                       │
│          ├── activities  (fixed)        │
│          └── participants (mutable)     │
└─────────────────────────────────────────┘
```

**Three-layer design:** Web → Service → State.
Validation and eligibility are separated by design — they answer different questions, have different fixtures in tests, and cannot interfere with each other.

---

## 🛠 Technology Stack

| Layer | Technology | Why |
|---|---|---|
| Language | Java 21 | LTS, `record` types reduce boilerplate |
| Framework | Spring Boot 3.5.3 | Embedded Tomcat, zero-deployment-config JAR |
| Build tool | Maven Wrapper (`./mvnw`) | No global Maven install needed |
| Storage | Java `LinkedHashMap` (in-memory) | Spec permits; `O(1)` lookup + stable display order in one structure |
| Frontend | Vanilla HTML / CSS / JS | No Node, no build step, no CORS; one command and it's live |
| Testing | JUnit 5 + AssertJ + MockMvc | Unit tests with no Spring context; controller tests with `@SpringBootTest` |
| API testing | Postman | Pre-built collection with all 9 requests + acceptance scenarios |

---

## 📁 Project Structure

```
CampusCertify/
├── src/
│   ├── main/
│   │   ├── java/com/campus/campuscertify/
│   │   │   ├── domain/
│   │   │   │   ├── Activity.java              ← immutable record (id, name, category, points)
│   │   │   │   ├── Category.java              ← enum: LEARN, BUILD, SHARE
│   │   │   │   ├── ErrorCode.java             ← enum: 4 validation error codes
│   │   │   │   ├── EvaluationResponse.java    ← envelope: errors / results / summary
│   │   │   │   ├── EvaluationSummary.java     ← eligible + ineligible counts
│   │   │   │   ├── Participant.java            ← mutable class (id, name, List<String> activityIds)
│   │   │   │   ├── ParticipantResult.java     ← per-participant score + reasons record
│   │   │   │   └── ValidationError.java       ← code + participantId + offendingValue
│   │   │   ├── service/
│   │   │   │   ├── BoardService.java           ← orchestrator: validate → evaluate → sort → summarize
│   │   │   │   ├── EligibilityEvaluator.java  ← categories, points, ordered failure reasons
│   │   │   │   └── ParticipantValidator.java  ← 4 error codes, collect-all (no fail-fast)
│   │   │   ├── state/
│   │   │   │   └── BoardState.java            ← fixed activities + mutable participants + reset
│   │   │   └── web/
│   │   │       ├── BoardController.java        ← 7 REST endpoints
│   │   │       └── ParticipantDto.java         ← only DTO (untrimmed inbound strings)
│   │   └── resources/
│   │       ├── static/
│   │       │   ├── index.html                  ← single-page dashboard
│   │       │   ├── app.js                      ← fetch + render + clearEvaluation
│   │       │   └── style.css                   ← dark theme, category progress strip
│   │       └── application.properties
│   └── test/
│       └── java/com/campus/campuscertify/
│           ├── domain/DomainModelTest.java
│           ├── service/BoardServiceTest.java
│           ├── service/EligibilityEvaluatorTest.java
│           ├── service/ParticipantValidatorTest.java
│           ├── state/BoardStateTest.java
│           └── web/BoardControllerTest.java
├── postman/
│   └── CampusCertify.postman_collection.json
├── .vscode/
│   ├── tasks.json                              ← Cmd+Shift+B → runs the app
│   └── launch.json                             ← Run & Debug config
├── pom.xml
└── mvnw  /  mvnw.cmd
```

---

## 📊 Fixed Activity Table

| Activity ID | Activity | Category | Points |
|---|---|---|---|
| A01 | Emerging Tech Talk | LEARN | 2 |
| A02 | Soldering Mini Lab | BUILD | 3 |
| A03 | Project Pitch Circle | SHARE | 2 |
| A04 | Open Source Clinic | BUILD | 2 |

This table is **read-only** at runtime — no API endpoint allows modifying it.

---

## ✅ Eligibility Rules

A participant is **ELIGIBLE** when **both** conditions are met:

1. Their completed activities collectively cover **all three categories**: LEARN, BUILD, and SHARE
2. Their total points are **≥ 6**

Both rules are always evaluated completely — the evaluator never short-circuits after the first failure.

**Failure reasons** are reported in this exact order:
```
MISSING_CATEGORY: LEARN
MISSING_CATEGORY: BUILD
MISSING_CATEGORY: SHARE
POINTS_BELOW_6
```

### Built-in participant oracle

| Participant ID | Name | Activities | Points | Eligible | Failure reason(s) |
|---|---|---|---|---|---|
| C01 | Asha | A01, A02, A03 | 7 | ✅ | — |
| C02 | Bilal | A01, A03, A04 | 6 | ✅ | — |
| C03 | Chen | A01, A02, A04 | 7 | ❌ | MISSING_CATEGORY: SHARE |
| C04 | Divya | A02, A03, A04 | 7 | ❌ | MISSING_CATEGORY: LEARN |
| C05 | Eshan | A01, A03 | 4 | ❌ | MISSING_CATEGORY: BUILD, POINTS_BELOW_6 |

---

## 🌐 API Endpoints

Base URL: `http://localhost:8080/api`

| Method | Endpoint | Description | Returns |
|---|---|---|---|
| `GET` | `/activities` | Fixed activity table | `List<Activity>` |
| `GET` | `/participants` | Current participant rows | `List<ParticipantDto>` |
| `POST` | `/participants` | Add a new participant | Fresh `List<ParticipantDto>` |
| `PUT` | `/participants/{id}` | Edit a participant by path ID | Fresh `List<ParticipantDto>` |
| `DELETE` | `/participants/{id}` | Remove a participant | `200` with fresh list, or `404` |
| `POST` | `/reset` | Restore 5 built-in rows | Fresh `List<ParticipantDto>` |
| `POST` | `/evaluate` | Validate + score all participants | `EvaluationResponse` (always `200`) |

### Evaluate response envelope

```json
{
  "errors": [],
  "results": [
    {
      "participantId": "C01",
      "participantName": "Asha",
      "totalPoints": 7,
      "coveredCategories": ["LEARN", "BUILD", "SHARE"],
      "eligible": true,
      "failureReasons": []
    }
  ],
  "summary": {
    "eligibleCount": 2,
    "ineligibleCount": 3
  }
}
```

When `errors` is non-empty, `results` is `[]` and `summary` is `null` — any input error clears all prior results and counts.

---

## 🔒 Validation & Error Codes

All four error codes carry `participantId` and `offendingValue`:

| Code | Triggered when |
|---|---|
| `INVALID_PARTICIPANT` | ID or name is blank after trimming |
| `DUPLICATE_PARTICIPANT_ID` | Same ID appears more than once in the participant list |
| `UNKNOWN_ACTIVITY` | Completed activity ID not found in the fixed table |
| `DUPLICATE_PARTICIPATION` | Same activity ID listed twice for the same participant |

**Validation is collect-all** — every participant is checked, every error is reported. A grader who enters two bad rows sees both errors, not just the first.

---

## 💡 Design Decisions

| Decision | Reasoning |
|---|---|
| **`List<String>` for completed activity IDs, not `Set`** | A `Set` silently deduplicates — making `DUPLICATE_PARTICIPATION` physically undetectable. The data structure must hold invalid states to report them. |
| **`Category.values()` iteration for failure reasons** | The enum's declaration order (`LEARN, BUILD, SHARE`) *is* the contracted order — no separate ordering list that could drift. Adding a 4th category needs zero changes to `failureReasons`. |
| **`REQUIRED_POINTS` is a named constant, not a literal** | `POINTS_BELOW_THRESHOLD = "POINTS_BELOW_" + REQUIRED_POINTS` — the message auto-updates. Changing the threshold to 8 is a genuine one-line change. |
| **`POST /evaluate` always returns `200`** | Validation errors are domain output, not HTTP failures. One `fetch().then()` path in JS — no `catch` branch that could forget to clear stale results. |
| **`Comparator.comparing(eligible, Comparator.reverseOrder()).thenComparing(id)`** | `.reversed()` would reverse the entire chain including the ID ordering. `reverseOrder()` applies only to the eligible key. |
| **In-memory `LinkedHashMap` for the store** | O(1) lookup *and* stable display order in one structure — no database needed, matching the spec's own wording ("in-memory data structures"). |
| **No `@ControllerAdvice`** | Validation errors are data, not exceptions. An unused global exception handler is dead code. |

---

## 🚀 Local Setup & Running

**Prerequisites:** JDK 17+ installed (JDK 24 was used in development). No Maven install needed — the Maven Wrapper handles it.

### Dev mode (recommended)
```bash
./mvnw spring-boot:run
```
Open **http://localhost:8080** in your browser.

### Standalone JAR
```bash
./mvnw clean package
java -jar target/campuscertify-0.0.1-SNAPSHOT.jar
```

### VS Code — zero-typing start
- `Cmd+Shift+B` to run the pre-configured build task, **or**
- Click the ▷ icon in the editor toolbar (configured via workspace settings)

**To stop:** press `Ctrl+C` in the terminal running the server, or click the trash icon on the task terminal panel.

---

## 🧪 Running the Tests

```bash
./mvnw test
```

Expected output:
```
Tests run: 41, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

| Test class | What it covers |
|---|---|
| `DomainModelTest` | Category order, Participant normalization, error envelope invariant |
| `BoardStateTest` | Fixed activity table, built-in seed, reset fidelity |
| `ParticipantValidatorTest` | All 4 error codes, collect-all, trimming |
| `EligibilityEvaluatorTest` | Built-in oracle, point boundary (`= 6` eligible), empty list, reason order |
| `BoardServiceTest` | All 5 required acceptance scenarios end-to-end |
| `BoardControllerTest` | JSON shape of every endpoint via MockMvc |

---

## 📬 Postman API Testing

A ready-to-import Postman collection is included:

```
postman/CampusCertify.postman_collection.json
```

**Import:** Postman → File → Import → select the JSON → 9 requests appear under **CampusCertify** using `{{baseUrl}} = http://localhost:8080`.

**Suggested demo flow:**
1. **POST /reset** → restore built-in rows
2. **POST /evaluate** → see oracle (totals 7, 6, 7, 7, 4 — counts 2 eligible / 3 ineligible)
3. **PUT /participants/C05** → add A04, making C05 eligible (6 points, all 3 categories)
4. **POST /evaluate** → counts now 3 eligible / 2 ineligible
5. **POST /reset** → restore
6. **Scenario: Duplicate A01 on C01** → POST /evaluate → `DUPLICATE_PARTICIPATION` error, no results, no counts