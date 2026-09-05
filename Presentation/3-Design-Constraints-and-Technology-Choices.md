# 3. Design Constraints and Technology Choices

## Constraints given to the AI up front

- Backend must be **Java + Spring Boot** — stated as existing background and a hard requirement, not something to evaluate against alternatives
- **No code until the architecture was fully explored and approved** — *"don't write code yet, i just need to explore things now"*
- The problem statement's own restrictions were treated as hard constraints, not guidelines: *"Evaluate local records only; do not add certificate document generation, event registration, payments, accounts, or scheduling"*
- The STUDENT_GUIDE's explicit warning against *"complex frameworks or unnecessary layers for the given scope"* was fed back into every subsequent design decision, and directly drove multiple cuts (see below)
- Design patterns were not prescribed up front — the AI was asked to propose an architecture, then that proposal was challenged and trimmed rather than accepted wholesale

## Full storage decision — three options compared and defended

| | Plain in-memory (Java collections) | Spring Data JPA + H2 | Real DB (MySQL) |
|---|---|---|---|
| Survives app restart | No | No | Yes |
| Setup complexity | None | Entities, repositories, seeding | All of the above + connection config |
| Unit test speed | Instant, no Spring context | Needs `@DataJpaTest` | Same, slower |
| Matches spec's own wording ("in-memory data structures") | ✅ Direct match | Gray area | ❌ Not asked for |
| Extra service to run during a live demo | No | No | **Yes — real operational risk** |
| Depth of Spring skill shown | Lower | Higher | Highest |

**Decision: plain in-memory.** The spec names it directly, every acceptance scenario (reset, edit, duplicate check) becomes a one-line state mutation, and a database adds real demo-day risk (a connection failure live, in front of an interviewer) for zero rubric benefit.

## Full delivery-mechanism decision — Spring Boot server vs. pure browser

| | Spring Boot + static page | Pure browser, no server at all |
|---|---|---|
| Matches "in-memory" | Yes | Yes |
| Simplest possible | No | Yes |
| Uses existing strongest skill (Java/Spring) | **Yes** | No — logic moves to JavaScript |
| Reuses prior backend work | **Yes** | No — thrown away |
| Real automated test evidence (JUnit) | **Yes** | No, or needs a JS test runner (itself over-engineering) |

**Decision: keep Spring Boot.** The STUDENT_GUIDE's *Testing and Validation* criterion specifically rewards demonstrated tests — a pure-browser rewrite would have sacrificed that for marginal simplicity gains, and the spec never actually says "no server," only "no accounts/payments/scheduling/etc."

## Frontend technology comparison

| Option | Verdict | Reason |
|---|---|---|
| **Static HTML/CSS/JS (chosen)** | ✅ Best fit | No Node build step, no CORS, one JAR — instant startup, debuggable with tools already known |
| Thymeleaf (server-rendered) | Rejected | Every button click needs a full page POST+redirect, harder to keep the progress strip/results "live" |
| React/Vue SPA | Rejected | Adds a Node build pipeline and CORS config for a "compact board" scored on logic correctness, not frontend sophistication |

## Complete technology choice table

| Choice | Alternative considered | Why this one won |
|---|---|---|
| **Spring Boot 3.5.3** | Boot 4.x (initially assumed) | 4.x isn't actually published to Maven Central yet — discovered empirically, not assumed |
| **Java 21 compile target** | Java 24/25 (the installed JDK) | LTS; compiles cleanly on JDK 24 without tying the build to a non-LTS version for no benefit |
| **Plain in-memory `LinkedHashMap`** | H2 in-memory DB, MySQL | See storage comparison above |
| **Static HTML/CSS/JS frontend** | Thymeleaf, React/Vue SPA | See frontend comparison above |
| **Only 1 DTO (`ParticipantDto`)** | A DTO per domain type | Every other domain type is already an immutable record shaped exactly like its JSON — extra DTOs would only add drift risk |
| **No `@ControllerAdvice`** | Global exception handler | Validation errors are data, not faults — an unused handler is dead code |
| **No Spring Security** | Auth middleware on all endpoints | Spec explicitly forbids "accounts" — there's no identity to protect, so auth would guard nothing; see the full security reasoning in `4-AI-Influenced-Decision-Making.md` |
| **Maven Wrapper (`./mvnw`)** | Global Maven install | Maven wasn't installed on the dev machine; the wrapper downloads its own copy, so the project is reproducible on any machine with just a JDK |

## Architectural pattern

**Three layers, not four:** Web → Service → State.

```
BoardController → BoardService → { ParticipantValidator, EligibilityEvaluator, BoardState }
```

This is a deliberate *reduction* from an earlier 4-layer proposal that included separate store interfaces per data type (`ActivityStore`/`InMemoryActivityStore`/`ParticipantStore`/`InMemoryParticipantStore` — 4 types for 2 collections) and a DTO per domain type. Both were cut specifically because the STUDENT_GUIDE penalizes "unnecessary layers for the given scope": interfaces exist to enable substitution, and there both is and will be no second implementation, since the spec forbids persistence-dependent features. See `Chats/plan.md` Part 2 for the complete list of what was proposed, then cut, and why.

## What was deliberately kept, despite the simplification pass

The validator/evaluator split (`ParticipantValidator` vs. `EligibilityEvaluator`) was the **one** structural separation kept even after every other cut, because it earns its keep: the two components answer genuinely different questions ("is the input well-formed?" vs. "does this person qualify?"), so their tests never need each other's fixtures. Not every layer is over-engineering — only the ones that don't pay for themselves.
