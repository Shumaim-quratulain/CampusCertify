# 3. Design Constraints and Technology Choices

## Constraints given to the AI up front

- Backend must be **Java + Spring Boot** — this was stated as existing background, not a suggestion to evaluate
- **No code until the architecture was fully explored and approved** — "don't write code yet, i just need to explore things now"
- The problem statement's own restrictions were treated as hard constraints, not guidelines: *"do not add certificate document generation, event registration, payments, accounts, or scheduling"*
- The STUDENT_GUIDE's explicit warning against *"complex frameworks or unnecessary layers for the given scope"* was fed back into every subsequent design decision

## Technology choices and the reasoning behind each

| Choice | Alternative considered | Why this one won |
|---|---|---|
| **Spring Boot 3.5.3** | Plain CLI (no server) | Spec allows a browser interface; keeping Java meant business logic stayed in the strongest language, with real JUnit test evidence |
| **Plain in-memory `LinkedHashMap`** | H2 in-memory DB, MySQL | Spec names "in-memory data structures" as an accepted approach directly; a DB adds setup/demo risk for zero rubric benefit |
| **Static HTML/CSS/JS frontend** | Thymeleaf, React/Vue SPA | No Node build step, no CORS config, one JAR — matches "quick startup" and "use technologies you can debug confidently" |
| **Only 1 DTO (`ParticipantDto`)** | A DTO per domain type | Every other domain type is already an immutable record shaped exactly like its JSON — extra DTOs would only add drift risk |
| **No `@ControllerAdvice`** | Global exception handler | Validation errors are data, not faults — an unused handler is dead code |
| **No Spring Security** | Auth on all endpoints | Spec explicitly forbids "accounts" — there's no identity to protect, so auth would guard nothing |

## Architectural pattern

**Three layers, not four:** Web → Service → State.

```
BoardController → BoardService → { ParticipantValidator, EligibilityEvaluator, BoardState }
```

This was a deliberate *reduction* from an earlier 4-layer proposal (separate store interfaces per data type, a DTO per domain type) — cut specifically because the STUDENT_GUIDE penalizes "unnecessary layers for the given scope." See `Chats/plan.md` Part 2 for the full list of what was cut and why.
