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

## How to Run Mock Backend Mode (Independent Testing)

Mock mode is enabled by default to allow complete Android development & testing independent of the Python backend.

- Toggle **Mock Engine** ON/OFF via the switch on the TeachFlow Dashboard.
- When Mock mode is ON:
  - Demonstrating "Order 2 pizzas" automatically synthesizes `ORDER_FOOD(item, quantity)`.
  - Replaying "Get me 3 burgers" automatically extracts `item = burger` and `quantity = 3` and executes the steps using `SemanticNodeFinder`.

---

## How to Connect to Python Backend (`feat/ai-flow-engine`)

1. Start the Python backend server (e.g. `http://localhost:8000`).
2. If running on Android Emulator, `10.0.2.2` maps directly to your host machine's `localhost`.
3. In TeachFlow AI, go to **Accessibility Status** screen -> **BACKEND_BASE_URL**.
4. Set URL to `http://10.0.2.2:8000` (or host IP if using a physical device over WiFi).
5. Toggle **Mock Engine** OFF on the dashboard.

---

## Demo Procedure

### 1. Learn Phase ("Order 2 pizzas")
- Go to **Learn a Task**.
- Prompt: `"Order 2 pizzas"`.
- Tap **Start Demonstration**.
- Perform actions in the embedded simulator or 3rd-party food app:
  - Search -> Select Pizza -> Set Quantity to 2 -> Tap "Add to Cart".
- Tap **Stop Demonstration**.
- TeachFlow sends captured actions to backend/mock engine and saves `ORDER_FOOD(item, quantity)`.

### 2. Replay Phase ("Get me 3 burgers")
- Go to **Run a Learned Task**.
- Speak or type: `"Get me 3 burgers"`.
- TeachFlow matches workflow `ORDER_FOOD` and extracts parameters: `item: burger`, `quantity: 3`.
- Tap **Run**.
- TeachFlow inspects active UI tree, semantically resolves target nodes, and executes search, selection, quantity set, and add to cart.

### 3. Human-in-the-Loop Sensitive Action Checkpoint
- When the execution reaches a sensitive step (e.g. "Checkout & Pay"), execution pauses.
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
