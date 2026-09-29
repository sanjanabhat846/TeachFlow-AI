# TeachFlow AI Backend

This is the Python backend for the TeachFlow AI hackathon project.

## API

Run the API from the repository root with:

```powershell
python -m uvicorn backend.app.main:app --reload
```

The service exposes health, intent and parameter extraction, workflow synthesis and
storage, flow matching, execution planning, approval checks, and semantic UI matching
and recovery. Interactive API documentation is available at `/docs` while the server
is running.

For Android-facing request and response examples, required fields, errors, and safety
behavior, see [../docs/api_contract.md](../docs/api_contract.md).

## Modules

- `app/intent`: rule-based intent and parameter extraction
- `app/flow`: workflow synthesis, matching, and JSON-backed storage
- `app/semantic`: semantic UI matching and recovery
- `app/executor`: execution planning and approval checks
- `app/main.py`: FastAPI application and HTTP routes
