# TeachFlow AI Backend

FastAPI backend for the deterministic TeachFlow AI food-order workflow demo.

## Run

From the repository root:

```powershell
python -m uvicorn backend.app.main:app --reload
```

The API docs are available at `/docs`. The service uses rule-based intent and parameter extraction and stores workflows in local JSON at `backend/data/workflows.json`.

## Integration Flow

`POST /intent/classify` and `/intent/parameters` process voice-recognized or typed text. Learn calls `/flows/synthesize` with demonstration descriptions and `/flows/learn` with captured semantic steps. Replay calls `/flows/match`, loads `/flows/{flow_id}`, and requests a bound action list from `/execution/plan`. Android supplies its current tree inside `/ui/match` or `/ui/recover`, checks each step with `/safety/check`, performs accessibility actions locally, and posts a report to `/execution/result`.

There is no `/intent`, `/learn`, `/match`, `/execute-plan`, or standalone `/ui-tree` route. Use the concrete routes documented in [../docs/api_contract.md](../docs/api_contract.md). `/execution/result` validates a report only; safety checks report whether approval is needed but do not themselves pause a client.

## Tests and Evaluation

From the repository root:

```powershell
python -m pytest -q
python -m backend.evaluation.evaluate
```

The evaluation is a small deterministic contract suite, not a statistical benchmark. See [evaluation/README.md](evaluation/README.md) for actual counts and boundaries.

## Modules and Limits

- `app/intent`: rule-based intent/parameter extraction; missing quantity defaults to one; invalid explicit quantity yields empty parameters.
- `app/flow`: deterministic food-flow synthesis/matching and JSON-backed storage.
- `app/semantic`: semantic matching with a confidence cutoff and clarification on ambiguous/weak recovery.
- `app/executor`: parameter binding and safety detection.
- `app/main.py`: actual HTTP routes.

The backend does not operate Android UI, authenticate callers, store execution results, or provide production-grade persistence.
