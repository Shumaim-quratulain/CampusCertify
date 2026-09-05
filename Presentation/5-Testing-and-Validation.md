# 5. Testing and Validation

## Automated test suite — 41 tests, all passing

```
./mvnw test
Tests run: 41, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

| Test class | Count | Covers |
|---|---|---|
| `DomainModelTest` | 4 | Category order, Participant normalization, error-envelope invariant |
| `BoardStateTest` | 5 | Fixed activity table, built-in seed, reset fidelity after mutation |
| `ParticipantValidatorTest` | 8 | All 4 error codes, collect-all (no fail-fast), trimming |
| `EligibilityEvaluatorTest` | 9 | Built-in oracle, exact point boundary, empty list, reason ordering |
| `BoardServiceTest` | 7 | All 5 required acceptance scenarios, end-to-end |
| `BoardControllerTest` | 7 | JSON shape of every REST endpoint via MockMvc |
| `CampuscertifyApplicationTests` | 1 | Spring context loads |

## Typical usage scenarios — verified live (browser + API)

| # | Scenario | Verified result |
|---|---|---|
| 1 | Built-in oracle | Totals 7, 6, 7, 7, 4 — 2 eligible / 3 ineligible |
| 2 | Reasons per participant | C03 → only `MISSING_CATEGORY: SHARE`; C04 → only `MISSING_CATEGORY: LEARN`; C05 → `MISSING_CATEGORY: BUILD` then `POINTS_BELOW_6` |
| 3 | Add A04 to C05 | 6/6 points, all 3 categories, counts become 3 eligible / 2 ineligible |
| 4 | Reset, clear C01's activities | 0 points, all 4 reasons in order, counts 1 eligible / 4 ineligible |
| 5 | Reset, add a second A01 to C01 | `DUPLICATE_PARTICIPATION` naming C01 and A01, **no stale result rows or counts** |

## Edge cases specifically targeted

- **Exact point boundary** — C02 sits at exactly 6 points and is eligible, proving the check is `>=` not `>`
- **Empty completed-activity list** — valid input, gives 0 points, 0 covered categories, and all 4 failure reasons in the contracted order
- **Duplicate participant ID** — fires once, on the second occurrence, not on every repeat
- **Untrimmed input** (`"  Eshan  "`, `"  A01"`) — normalized before comparison so it doesn't create phantom duplicate/unique-ID bugs
- **Blank participant fields** — caught live during manual testing (see `Chats/execution.md`): an accidentally-added blank row correctly triggered two `INVALID_PARTICIPANT` errors and cleared the results panel, confirming the fail-safe path works outside of unit tests too

## API-level validation (Postman)

`Presentation/../postman/CampusCertify.postman_collection.json` — 9 requests covering every endpoint, each verified against the live server via `curl` before being committed. Screenshots of each request/response are in `postman/*.png`.

## Test-first discovery worth mentioning live

A planned test — *"all categories covered but still below the point threshold"* — turned out to be mathematically impossible with the given activity table (see `4-AI-Influenced-Decision-Making.md`). Finding this out **while writing the test**, rather than assuming it was reachable, is itself evidence of a validation mindset, not just a validation suite.
