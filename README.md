# TeachFlow AI

TeachFlow AI is a demo of teachable Android UI workflows. It combines a rule-based FastAPI backend with an Android app that captures accessibility semantics, stores parameterized workflows, and replays actions using semantic UI matching instead of screen coordinates.

## Architecture

- `android/`: Kotlin/Jetpack Compose app, AccessibilityService, action executor, and OkHttp API client.
- `backend/`: FastAPI routes for intent/parameter extraction, workflow storage/matching, execution plans, safety checks, and UI match/recovery.
- `shared/`: representative JSON contracts for workflows, UI trees, and execution results.
- `docs/api_contract.md`: the implemented HTTP API and payloads.

The intent and parameter components are deterministic rule-based MVP logic, not an ML model. Workflow data is stored locally in `backend/data/workflows.json`.

## Backend Flow

Live Android Learn/Replay calls classify text, extract parameters, match stored `order_food`, build a bound plan, submit the current UI tree for semantic match/recovery, check sensitive actions, execute locally through Android accessibility nodes, then submit an execution-result report. The result endpoint validates that report; it does not execute Android actions or persist the report.

The actual routes are `POST /intent/classify`, `POST /intent/parameters`, `POST /flows/synthesize`, `POST /flows/learn`, `POST /flows/match`, `GET /flows` and `GET /flows/{flow_id}`, `POST /execution/plan`, `POST /ui/match`, `POST /ui/recover`, `POST /safety/check`, and `POST /execution/result`. There is no standalone `/ui-tree` upload route; Android includes `ui_tree` in UI match/recovery requests. `/execution/result` only validates a result payload. See [docs/api_contract.md](docs/api_contract.md) for request/response details.

## Demo Flow

First, enter **“Order 2 pizzas”** in Learn and demonstrate **Search → select pizza → set quantity → add to cart**. Android captures simulator callbacks or accessibility events; the live client asks the backend to synthesize and store a parameterized `ORDER_FOOD(item, quantity)` workflow.

Later, enter or speak **“Get me 3 burgers”** in Replay. The intended integrated sequence classifies `ORDER_FOOD`, extracts `item=burger` and `quantity=3`, retrieves the stored workflow, binds parameters, generates an execution plan, inspects the current UI tree, semantically matches or attempts recovery, checks safety, requests human approval when required, executes through Android `AccessibilityService`, and reports the result.

The backend API sequence is covered by deterministic tests/evaluation. Android build and real-device execution have not been verified in this environment; the intended flow is not a claim of device-tested behavior.

## Modes and Limitations

Live mode is the default for Learn/Replay and uses the FastAPI endpoints. Mock mode routes those client calls through `MockBackendEngine`. Independently of mode, the dashboard workflow count and Workflows screen still read from `MockBackendEngine`; they are not authoritative for backend-stored workflows. Workflow browsing needs a later backend-list integration.

The backend currently supports a narrow food-order demo, rule-based extraction, and local JSON persistence. The small deterministic evaluation is not a statistical benchmark. Sensitive-action detection is text-based. Android needs an enabled accessibility service and a reachable backend; no real-device validation has been performed.

## Run and Validate

From the repository root:

```powershell
python -m uvicorn backend.app.main:app --reload
python -m pytest -q
python -m backend.evaluation.evaluate
```

Evaluation cases and measured counts are described in [backend/evaluation/README.md](backend/evaluation/README.md). The Android project targets Java 17/SDK 34 and uses Gradle 8.4. In this checkout the wrapper JAR and system Gradle are absent, no Android SDK path or `local.properties` is configured, no `adb` is available, and the installed Java runtime is 25. A wrapper-JAR-only repair would not provide the missing SDK/device prerequisites, so Android build and real-device results remain unverified. Android setup and demo instructions are in [android/README.md](android/README.md); backend setup is in [backend/README.md](backend/README.md).
