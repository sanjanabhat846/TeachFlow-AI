# TeachFlow AI Shared Contracts

This directory contains the canonical JSON contracts shared between the Android frontend (`feat/android-frontend`) and the Python backend (`feat/ai-flow-engine`).

## Contracts

- `schemas/ui_tree.json`: Structure of Accessibility/UI tree extracted by Android.
- `schemas/workflow.json`: Structure of synthesized parameterized workflows returned by the flow engine.
- `schemas/execution_result.json`: Execution status reported by Android back to backend/flow-engine.

## Data Flow

1. **Demonstration Phase**: Android captures `UIElement` nodes & user actions -> sends to backend (`POST /api/v1/workflows/learn`).
2. **Replay Phase**: User speaks command -> Android calls backend (`POST /api/v1/intent`) -> Backend extracts intent & parameters -> returns `workflow.json` execution plan.
3. **Execution Phase**: Android `SemanticNodeFinder` resolves targets against Accessibility UI tree -> `ActionExecutor` executes steps -> reports `execution_result.json`.
