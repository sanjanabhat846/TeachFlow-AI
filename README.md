# TeachFlow AI — Teachable Voice Automation

> **Teach once. Reuse anywhere.**

TeachFlow AI allows users to demonstrate a complex task once inside any third-party Android app (e.g., ordering food, booking rides, adjusting settings). TeachFlow captures the Accessibility/UI hierarchy and semantic user actions, converts them into parameterized execution workflows via AI, and re-executes them on demand with new voice inputs without hardcoding screen `(x,y)` coordinates.

---

## Repository Overview

```text
TeachFlow-AI/
├── android/                 # Android App (Kotlin, AccessibilityService, Compose UI, Semantic Matching, Action Execution)
├── backend/                 # Python AI Flow Engine (Intent extraction, workflow synthesis, parameter matching)
├── shared/                  # Shared JSON API contracts between Android & Backend
├── docs/                    # Architecture diagrams & hackathon documentation
├── README.md                # Root project documentation
└── .gitignore
```

---

## Backend API Contract

The backend routes, JSON request and response examples, required fields, errors, and
safety behavior are documented in [docs/api_contract.md](docs/api_contract.md).
Backend setup and run instructions are in [backend/README.md](backend/README.md).

## Branches

- `feat/android-frontend` (Android Application & Frontend Integration)
- `feat/ai-flow-engine` (Python AI / Flow Engine Backend)

---

## Core Features (Android Frontend)

1. **Accessibility Service (`TeachFlowAccessibilityService`)**:
   - Zero hard-coded `(x,y)` coordinates.
   - Robust node traversal & UI hierarchy extraction.
   - Resilient against node updates, screen changes, and null root states.

2. **Semantic UI Node Finder (`SemanticNodeFinder`)**:
   - Matches targets by resource ID, role/class, text, content description, normalized text, and semantic fuzzy fallback.

3. **Demonstration Capture & Action Executor**:
   - Captures tap, type, scroll actions.
   - Executes interactive actions via standard Accessibility API actions.

4. **Human-in-the-Loop Approval Checkpoint**:
   - Pauses execution on sensitive operations (payment, authentication, checkout).
   - Requires explicit user approval before proceeding.

5. **Isolated Mock Engine**:
   - Enables offline testing and development independent of backend state.

6. **Jetpack Compose UI**:
   - Complete UI for Learning, Replaying, Workflow Management, and Accessibility status monitoring.

---

## Getting Started (Android)

See [android/README.md](file:///c:/Users/sanja/OneDrive/Desktop/TeachFlow-AI/android/README.md) for build, execution, and setup instructions.
