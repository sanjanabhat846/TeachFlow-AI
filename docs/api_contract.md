# TeachFlow AI Backend API Contract

This document describes the JSON interface currently implemented by the Python backend. It is intended for the Android client. The service does not accept audio bytes: Android must provide recognized voice text as JSON text.

## Connection

Run from the repository root:

```powershell
python -m uvicorn backend.app.main:app --reload
```

The default local base URL is `http://127.0.0.1:8000`. Interactive OpenAPI documentation is served at `/docs`; the generated schema is at `/openapi.json`. Routes use JSON request bodies where shown. Typed Pydantic request objects reject unknown fields (`422 Unprocessable Entity`); semantic `target` and `actions` dictionaries are intentionally open objects.

The API currently has no authentication and no versioned URL prefix. Keep it on a trusted development network; do not expose it publicly as-is.

## Endpoint Summary

| Method | Endpoint | Purpose |
|---|---|---|
| `GET` | `/health` | Service health |
| `POST` | `/intent/classify` | Classify voice-to-text input |
| `POST` | `/intent/parameters` | Extract supported parameters |
| `POST` | `/flows/synthesize` | Synthesize and persist a workflow from demonstration text steps |
| `POST` | `/flows/learn` | Persist a supplied semantic workflow |
| `GET` | `/flows` | List workflows |
| `GET` | `/flows/{flow_id}` | Get a workflow |
| `DELETE` | `/flows/{flow_id}` | Delete a workflow |
| `POST` | `/flows/match` | Match a command to a flow ID |
| `POST` | `/execution/plan` | Build a semantic action plan |
| `POST` | `/execution/result` | Validate an Android execution result; returns no body |
| `POST` | `/safety/check` | Report whether a plan requires approval |
| `POST` | `/ui/match` | Match a semantic target against a current UI tree |
| `POST` | `/ui/recover` | Re-match after a UI change or request clarification |

## Common Error Shape

Request validation failures return `422` with FastAPI's `detail` array. The exact `loc`, `type`, and `input` values depend on the invalid field.

```json
{
  "detail": [
    {
      "type": "missing",
      "loc": ["body", "text"],
      "msg": "Field required",
      "input": {}
    }
  ]
}
```

Custom route errors use `{"detail":"..."}`. Relevant statuses are listed with each route below.

## 1. Health

### `GET /health`

No request body.

Response `200`:

```json
{"status": "ok"}
```

## 2. Voice Text and Intent

Voice audio is not part of the backend contract. Send the speech-recognizer's text result.

### `POST /intent/classify`

Request body (`text` required, `context` optional and currently ignored):

```json
{"text": "Order 2 pizzas"}
```

Response `200` (`IntentResponse`):

```json
{"intent": "ORDER_FOOD", "confidence": 1.0, "source": "rule_based"}
```

Known intents are `ORDER_FOOD`, `SHOP_PRODUCT`, and `TRAVEL_DESTINATION`. Unrecognized text returns `{"intent":"UNKNOWN","confidence":0.0,"source":"rule_based"}`. Confidence is the current rule-based result (`1.0` or `0.0`), not a calibrated model probability.

### `POST /intent/parameters`

Request body (`text` required, optional `intent` accepted but currently ignored):

```json
{"text": "Get me 3 burgers"}
```

Response `200` (`ParameterExtractionResponse`):

```json
{"intent": "ORDER_FOOD", "parameters": {"item": "burger", "quantity": 3}}
```

For unsupported or unrecognized parameter combinations, `parameters` may be `{}`. The current food extractor returns canonical item text and integer quantity.

## 3. Demonstrations and Workflows

### Demonstration format

`POST /flows/synthesize` accepts **a list of strings**, not Android touch events or a UI-tree recording. The current synthesizer produces the deterministic `order_food` workflow.

Request body (`demonstration` required):

```json
{
  "demonstration": ["Search", "Pizza", "Quantity 2", "Add to Cart"]
}
```

Response `200` (`WorkflowDefinition`):

```json
{
  "flow_id": "order_food",
  "intent": "ORDER_FOOD",
  "parameters": ["item", "quantity"],
  "steps": [
    {"action": "search", "target": {"role": "edit_text"}, "value": "{{item}}"},
    {"action": "select", "target": {"role": "product"}, "value": null},
    {"action": "set_quantity", "target": null, "value": 2},
    {"action": "tap", "target": {"text": "Add to Cart"}, "value": null}
  ]
}
```

The endpoint saves the synthesized workflow and replaces any stored workflow with the same `flow_id`.

### `POST /flows/learn`

Use this route when Android or another caller already has semantic workflow steps. `flow_id` and `intent` are required; `parameters` and `steps` are optional and default to empty arrays. Each step requires `action`; `target` (object) and `value` (string or integer) are optional.

Request body:

```json
{
  "flow_id": "order_food",
  "intent": "ORDER_FOOD",
  "parameters": ["item", "quantity"],
  "steps": [
    {"action": "search", "target": {"role": "edit_text"}, "value": "{{item}}"},
    {"action": "tap", "target": {"text": "Add to Cart"}}
  ]
}
```

Response `200` (`LearnWorkflowResponse`):

```json
{"success": true, "flow_id": "order_food", "message": "Workflow saved successfully"}
```

### `GET /flows`

No request body. Response `200` is a JSON array of `WorkflowDefinition` objects. An empty store returns `[]`; a populated response uses the `WorkflowDefinition` shape shown below.

```json
[]
```

### `GET /flows/{flow_id}`

No request body. Response `200` is one `WorkflowDefinition`:

```json
{
  "flow_id": "order_food",
  "intent": "ORDER_FOOD",
  "parameters": ["item", "quantity"],
  "steps": [
    {"action": "search", "target": {"role": "edit_text"}, "value": "{{item}}"},
    {"action": "set_quantity", "target": null, "value": 2}
  ]
}
```

Missing workflow returns `404`:

```json
{"detail": "Workflow not found"}
```

### `DELETE /flows/{flow_id}`

No request body. Response `200`:

```json
{"deleted": true, "flow_id": "order_food"}
```

Missing workflow returns `404` with `{"detail":"Workflow not found"}`.

### `POST /flows/match`

Request body (`text` required):

```json
{"text": "Get me 3 burgers"}
```

Response `200` (`FlowMatchResponse`):

```json
{"flow_id": "order_food", "confidence": 0.94}
```

No match returns `{"flow_id":"unknown","confidence":0.0}`. This route selects a flow ID; it does not return the stored workflow definition.

## 4. Execution

### `POST /execution/plan`

Request body (`intent` required; `parameters` defaults to `{}`; `flow_id` is optional):

```json
{
  "intent": "ORDER_FOOD",
  "parameters": {"item": "burger", "quantity": 3},
  "flow_id": "order_food"
}
```

If `flow_id` is omitted, the backend selects a stored workflow with the requested intent only when exactly one candidate exists. Supplying `flow_id` is recommended when the client already received a flow match.

Response `200` (`ExecutionPlanResponse`):

```json
{
  "actions": [
    {"action": "search", "target": {"role": "edit_text"}, "value": "burger"},
    {"action": "select", "target": {"role": "product"}},
    {"action": "set_quantity", "value": 3},
    {"action": "tap", "target": {"text": "Add to Cart"}}
  ]
}
```

Each action is a semantic object; the backend does not send screen coordinates or execute Android actions. String placeholders such as `{{item}}` are replaced from `parameters`; a `set_quantity` action uses the supplied `quantity` when present. Android resolves each `target` against its current accessibility/UI tree.

Errors:

- `404` for an unknown explicit flow ID: `{"detail":"Workflow not found"}`.
- `404` when no stored flow has the requested intent: `{"detail":"No workflow found for intent"}`.
- `409` when more than one stored flow has the intent and `flow_id` was omitted: `{"detail":"flow_id is required when multiple workflows match the intent"}`.
- `422` for an intent mismatch: `{"detail":"Workflow intent does not match request"}`.
- `422` when a referenced template parameter is absent, for example: `{"detail":"Missing execution parameter: item"}`.

### `POST /execution/result`

Report one Android execution result. `success` and `step` are required; `message` and `error` are optional strings and may be `null`. No step-index base is enforced by the backend.

Success request:

```json
{"success": true, "step": 0, "message": "Action completed"}
```

Failure request:

```json
{"success": false, "step": 1, "error": "Target not found"}
```

A valid report returns `204 No Content` with an empty response body. This route validates the existing `ExecutionResult` schema only; it does not persist reports or change execution state. Invalid bodies return `422`.

## 5. Current Android UI Tree and Semantic Targets

The UI tree is nested in the request body as `ui_tree`. `screen` is optional and defaults to `""`; `elements` is optional and defaults to `[]`. Each element requires `id`; `role`, `text`, `content_description`, and `resource_id` default to `""`; `clickable` defaults to `false`; `enabled` defaults to `true`. Unknown fields are rejected.

### `POST /ui/match`

Request body (`target` and `ui_tree` required):

```json
{
  "target": {"role": "button", "text": "Add to Cart"},
  "ui_tree": {
    "screen": "food_app",
    "elements": [
      {
        "id": "add_button",
        "role": "button",
        "text": "Add",
        "content_description": "Add item to cart",
        "resource_id": "add_btn",
        "clickable": true,
        "enabled": true
      }
    ]
  }
}
```

Response `200` (`UiMatchResponse`):

```json
{"matched": true, "node_id": "add_button", "confidence": 0.65}
```

A non-confident match still returns `200`, with `matched:false`, `node_id:null`, and a confidence score. The acceptance threshold is `0.6`.

### `POST /ui/recover`

Uses the same request body as `/ui/match`. Response `200` (`UiRecoveryResponse`) on a confident recovery:

```json
{
  "matched": true,
  "node_id": "add_button",
  "confidence": 0.65,
  "requires_clarification": false,
  "reason": null
}
```

If confidence is insufficient, the response is `200` with `matched:false`, `node_id:null`, `requires_clarification:true`, and a `reason`; Android must ask the user rather than selecting a guessed target.

There is no standalone UI-tree upload endpoint. The tree is consumed by the match and recovery requests.

## 6. Safety Approval

### `POST /safety/check`

Request body (`actions` defaults to `[]`):

```json
{
  "actions": [
    {"action": "tap", "target": {"text": "Checkout"}}
  ]
}
```

Response `200` (`SafetyCheckResult`):

```json
{
  "requires_approval": true,
  "reason": "Sensitive action requires approval: checkout"
}
```

A routine plan returns `{"requires_approval":false,"reason":null}`. Current text-based detection covers checkout, payment/pay, authentication/login/sign-in, password/passcode/PIN, and OTP or verification-code wording. The route reports the requirement; it does not pause execution or collect approval. Android must check the plan and obtain explicit user approval before executing a sensitive action.

## Required and Optional Fields

Pydantic request models reject unknown fields. In brief:

- Required text inputs: `text` for classify, parameter extraction, and flow matching.
- Required workflow identity: `flow_id` and `intent` for `/flows/learn`; workflow steps require `action`.
- Required plan field: `intent`; `parameters` defaults to `{}` and `flow_id` is optional.
- Required UI match fields: `target`, `ui_tree`; every tree element requires `id`.
- Required execution report fields: `success`, `step`.
- Optional safety data: `actions` defaults to an empty array.

The shared `UiTreeRequest` Pydantic model is not currently mounted as a standalone route. Use the nested `ui_tree` shape shown above.
