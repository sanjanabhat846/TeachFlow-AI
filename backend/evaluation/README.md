# TeachFlow AI Evaluation

Run the deterministic evaluation from the repository root:

```powershell
python -m backend.evaluation.evaluate
```

The runner reads [`dataset.json`](dataset.json), calls the current FastAPI app and backend functions, seeds `order_food` in a temporary JSON workflow store, and restores the configured workflow store path afterward. It makes no network requests, uses no random sampling, and does not alter `backend/data/workflows.json`. Exit code `1` means at least one expected outcome failed; known gaps are printed rather than hidden.

## Measured Results

These counts are from the Phase 18 evaluation run in this worktree. A case passes only when the observed outcome equals the dataset's expected outcome. The counts are case counts, not population estimates or statistical accuracy measurements.

| Area | Cases | Passed | What was tested / calculation |
|---|---:|---:|---|
| Intent extraction | 4 | 4 | `/intent/classify`; count whose returned intent equals the expected label. Includes food commands and an unsupported request. |
| Parameter extraction | 10 | 10 | `/intent/parameters`; exact intent/parameter comparison, plus rejection of zero, negative (digit and spoken), fractional, and nonnumeric counts. Includes absent item and the missing-quantity default. |
| Workflow matching and binding | 3 | 3 | `/flows/match`, `/execution/plan`; expected flow ID, fully bound action list, and `404` for a nonexistent workflow. |
| Semantic UI matching | 8 | 8 | `match_ui_element`; expected match decision and node ID for exact, text, resource-ID and context variation, disabled/missing targets, ambiguity, and low confidence. |
| Recovery | 3 | 3 | `recover_target`; expected recovery or clarification decision, including no node for low-confidence results. |
| Safety detection | 6 | 6 | `/safety/check`; approval boolean for ordinary action, checkout, payment, password, OTP, and sign-in/authentication. |
| End-to-end logical backend sequence | 1 | 1 | API calls for classification, extraction, match, binding/plan, UI match, safety, then a valid execution-result report (`204`). Each expected value/status in the sequence must match. |
| Android approval-gate source check | 1 | 1 | Static ordering check in `ReplayScreen.kt`: backend safety check, approval/wait/cancel branch, then accessibility executor call. This is not an Android runtime test. |

### Quantity Validation Result

The extractor now rejects explicit non-positive, fractional, ambiguous, and unrecognized quantity expressions by returning the existing `ORDER_FOOD` response with an empty `parameters` object. Positive digit counts and supported spoken counts (`one` through `ten`) produce integer quantities. When no quantity expression is present, the existing default of one is retained. The evaluator accepts HTTP `422` or empty parameters as a safe rejection; the current implementation uses the latter response shape.

## Validation Boundaries

**Backend evaluation:** These are deterministic calls against backend API routes/functions using local test inputs. The end-to-end case checks the backend request/response sequence only.

**Android/static integration validation:** The one passing source-order check establishes that the current replay source places a safety/approval gate before the action-executor call. It does not prove Kotlin compilation, callback behavior on a running app, accessibility service behavior, or successful network communication on Android. Android Gradle execution was unavailable because `gradle-wrapper.jar` and system Gradle are absent.

**Real-device validation:** Not performed. No emulator or physical-device actions are represented in these measurements.

The `/execution/result` endpoint validates a report and returns `204`; it does not execute or persist Android actions. UI examples are small, hand-authored cases for this demo and do not establish general matching accuracy across apps or languages. The deterministic rule-based intent and parameter examples likewise do not represent a statistical benchmark.
