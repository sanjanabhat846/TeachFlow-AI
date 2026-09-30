# TeachFlow AI — Android Application

This directory contains the Android application for **TeachFlow AI — Teachable Voice Automation**.

TeachFlow AI allows users to demonstrate a UI task once in any third-party app (or the built-in target app simulator) and re-execute it on demand with new voice inputs, without relying on hardcoded `(x,y)` coordinates.

---

## Architecture Overview

```text
android/TeachFlow/app/src/main/java/com/teachflow/ai/
├── accessibility/
│   ├── TeachFlowAccessibilityService.kt  # Android AccessibilityService & event capture
│   └── UIHierarchyReader.kt              # Recursively extracts UI tree & roles into JSON
├── executor/
│   ├── SemanticNodeFinder.kt             # Matches execution targets semantically (resourceId, role, text, normalized text)
│   └── ActionExecutor.kt                 # Executes tap, type, scroll, and set_quantity with approval safeguards
├── network/
│   ├── NetworkConfig.kt                  # Base URL & Mock mode toggle
│   ├── MockBackendEngine.kt              # Offline mock flow engine for zero-dependency testing
│   └── TeachFlowApiClient.kt             # REST client communicating with Python backend
├── model/                                # Canonical JSON models matching shared contracts
└── ui/
    ├── MainActivity.kt                   # Jetpack Compose navigation & state host
    ├── components/
    │   ├── ApprovalDialog.kt             # Human-in-the-loop sensitive action dialog
    │   └── DemoAppSimulator.kt           # Embedded food ordering app simulator for live demos
    └── screens/
        ├── HomeScreen.kt                 # TeachFlow dashboard & feature cards
        ├── LearnScreen.kt                # Demonstration capture interface
        ├── ReplayScreen.kt               # Voice replay runner with speech-to-text
        ├── WorkflowsScreen.kt            # Saved workflow contract browser
        └── AccessibilityStatusScreen.kt  # Service status inspector & backend config
```

---

## How to Enable Accessibility Service

1. Build and install the APK on an Android Emulator or physical device (Android 8.0+ / API 26+).
2. Open Android **Settings** -> **Accessibility**.
3. Under **Downloaded Apps** / **Services**, select **TeachFlow Automation Service**.
4. Turn the toggle **ON** and accept accessibility permissions.
5. Return to TeachFlow AI. The status badge on the dashboard will turn green: **"Accessibility Service Connected"**.

---

## Backend Modes

Live backend mode is enabled by default. The emulator connects to the host backend at `http://10.0.2.2:8000`; a physical device should use the host's reachable LAN address. Mock mode remains available for offline testing.

- Toggle **Mock Engine** ON/OFF via the switch on the TeachFlow Dashboard.
- When Mock mode is ON:
  - Demonstrating "Order 2 pizzas" automatically synthesizes `ORDER_FOOD(item, quantity)`.
  - Replaying "Get me 3 burgers" automatically extracts `item = burger` and `quantity = 3` and executes the steps using `SemanticNodeFinder`.

### Phase 16 Workflow Listing Limitation

With Mock mode OFF, Learn and Replay communicate with the real FastAPI backend for workflow learning, matching, planning, safety checks, and execution reporting. The dashboard workflow count and the workflow browser still read from `MockBackendEngine`, even when live mode is enabled. They are not authoritative views of workflows stored by the backend. Backend-backed workflow listing is a known Phase 16 limitation and is deferred to a later integration step.

---

## How to Connect to Python Backend (`feat/ai-flow-engine`)

1. Start the Python backend server (e.g. `http://localhost:8000`).
2. If running on Android Emulator, `10.0.2.2` maps directly to your host machine's `localhost`.
3. In TeachFlow AI, go to **Accessibility Status** screen -> **BACKEND_BASE_URL**.
4. Set URL to `http://10.0.2.2:8000` (or host IP if using a physical device over WiFi).
5. Keep **Mock Engine** OFF on the dashboard for backend integration.

---

## Demo Procedure

### 1. Learn Phase ("Order 2 pizzas")
- Go to **Learn a Task**.
- Prompt: `"Order 2 pizzas"`.
- Tap **Start Demonstration**.
- Perform actions in the embedded simulator or 3rd-party food app:
  - Search -> Select Pizza -> Set Quantity to 2 -> Tap "Add to Cart".
- Tap **Stop Demonstration**.
- TeachFlow sends intent and parameters to the backend, synthesizes the flow from the demonstration, refines it with captured semantic targets, and stores it through `/flows/learn`.

### 2. Replay Phase ("Get me 3 burgers")
- Go to **Run a Learned Task**.
- Speak or type: `"Get me 3 burgers"`.
- TeachFlow classifies the command, extracts `item: burger` and `quantity: 3`, matches the stored workflow, and requests a parameter-bound execution plan.
- Tap **Run**.
- Before each action, TeachFlow asks the backend to check safety and semantically match the target against the current accessibility tree. Android resolves and executes the action through accessibility nodes, then reports the result.

### 3. Human-in-the-Loop Sensitive Action Checkpoint
- When the backend identifies a sensitive step (e.g. "Checkout & Pay"), execution pauses before the action.
- A popup appears: `⚠️ User Approval Required`.
- Tap **Approve** to proceed or **Cancel** to abort.

---

## Unit Testing

Run unit tests via command line or Android Studio:
```bash
./gradlew test
```
Tests cover:
- UI hierarchy extraction & JSON serialization
- Semantic target matching & normalized text recovery ("Add to Cart" vs "Add")
- Parameter substitution (`{{item}}` -> "burger")
- Sensitive action approval triggers
- Mock backend engine synthesis
