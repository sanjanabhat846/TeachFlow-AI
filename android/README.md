# TeachFlow AI Android App

The Kotlin/Jetpack Compose client captures Android accessibility events and UI semantics, calls the FastAPI backend in live mode, and performs actions locally through `AccessibilityService`. It uses semantic labels and resource metadata rather than coordinates as primary selectors.

## Architecture

- `accessibility/TeachFlowAccessibilityService.kt` captures action events and maintains the latest UI tree.
- `accessibility/UIHierarchyReader.kt` extracts role, text, content description, resource ID, class, enabled/clickable state, bounds, and labeled ancestor context.
- `network/TeachFlowApiClient.kt` calls the current unversioned backend routes; `NetworkConfig.kt` selects live/mock mode.
- `executor/SemanticNodeFinder.kt` resolves targets locally; `ActionExecutor.kt` performs Android accessibility actions and reports their result to the caller.
- `ui/screens/LearnScreen.kt` captures the built-in simulator or service events; `ReplayScreen.kt` coordinates plan, safety, UI match/recovery, approval, execution, and result reporting.
- `MockBackendEngine.kt` supplies offline intent/flow behavior. The embedded `DemoAppSimulator` is a local Compose demo target, not a third-party app.

## Live Backend Routes

With **Mock Engine** off, Learn/Replay use `POST /intent/classify`, `POST /intent/parameters`, `POST /flows/synthesize`, `POST /flows/learn`, `POST /flows/match`, `GET /flows/{flow_id}`, `POST /execution/plan`, `POST /safety/check`, `POST /ui/match`, `POST /ui/recover`, and `POST /execution/result` as applicable. There is no standalone UI-tree upload endpoint; the current tree is nested in UI match/recovery requests. Exact JSON contracts are in [../docs/api_contract.md](../docs/api_contract.md).

For the Android Emulator, the default URL `http://10.0.2.2:8000` reaches the host machine. For a physical device, configure a reachable host address under **Accessibility Status**. Run the backend from the repository root with `python -m uvicorn backend.app.main:app --reload`.

## Learning and Replay Demo

1. Turn **Mock Engine** off and start the backend.
2. Enable the TeachFlow accessibility service in Android Settings. Use the built-in simulator for the documented demo.
3. In Learn, enter **“Order 2 pizzas”**. Demonstrate **Search → select pizza → set quantity to 2 → add to cart** and stop. Android sends prompt-derived intent/parameters, synthesizes a workflow, refines it with captured semantic targets, and stores it with `/flows/learn`.
4. In Replay, enter or speak **“Get me 3 burgers”**. The client classifies/extracts, matches the saved flow, retrieves it, and requests a parameter-bound plan. Before each targeted action it checks safety and submits the current UI tree for semantic matching. If the first match fails, it refreshes the tree and calls `/ui/recover`; low-confidence or ambiguous recovery stops for clarification. Android then resolves and executes the action through accessibility nodes and reports an execution result.
5. For checkout, payment, password, OTP, and supported authentication labels, the client pauses for **Approve** or **Cancel** before executing the sensitive step.

This is the intended integrated flow. Backend routes and logic have deterministic tests/evaluation; Android compilation and this sequence on an emulator or physical device have not been verified in this environment.

## Mock and Live Scope

Live Learn/Replay uses the real FastAPI routes. Mock mode bypasses those network calls through `MockBackendEngine`, but Replay still relies on the Android accessibility service and local executor; mock mode is not a real app-action simulator.

The dashboard workflow count and **Workflows** browser read `MockBackendEngine` even when live mode is enabled. They do not show authoritative backend storage. Backend-backed listing and browser actions remain incomplete.

## Build and Tests

The Android project targets Java 17, SDK 34, and Gradle 8.4. In a complete Android build environment, run from `android/TeachFlow`:

```powershell
.\gradlew.bat testDebugUnitTest
```

In this checkout, `gradlew.bat` and `gradle-wrapper.properties` (Gradle 8.4) exist, but `gradle-wrapper.jar` is missing; no system Gradle is installed. The environment has no `ANDROID_HOME`/`ANDROID_SDK_ROOT`, no `android/TeachFlow/local.properties`, no Android SDK in the checked common locations, and no `adb`. Java 25 is on PATH, while this project targets Java 17. Restoring only the wrapper JAR would therefore not resolve all build prerequisites. Android build/test status is unverified, and no emulator or device test was performed. Backend tests and deterministic evaluation commands are documented in [../backend/README.md](../backend/README.md) and [../backend/evaluation/README.md](../backend/evaluation/README.md).

## Known Limitations

- Workflow synthesis is deterministic and focused on `ORDER_FOOD`; it is not general-purpose AI planning.
- Backend workflow storage is local JSON; backend `/execution/result` validates a payload but does not persist it or execute Android actions.
- Dashboard count/workflow browser are mock-backed in live mode.
- Safety classification is text-based and requires client approval handling.
- Semantic matching depends on accessibility metadata and labeled context; uncertainty should stop for clarification.
- Android build and real-device behavior remain unverified because the wrapper JAR/system Gradle are unavailable here.
