"""Tests for the Android-facing Phase 15 API contracts."""

from fastapi.testclient import TestClient

from backend.app.flow import flow_store
from backend.app.main import create_app


def test_execution_result_contract_accepts_success_and_failure_reports() -> None:
    client = TestClient(create_app())

    success = client.post(
        "/execution/result",
        json={"success": True, "step": 0, "message": "Action completed"},
    )
    failure = client.post(
        "/execution/result",
        json={"success": False, "step": 1, "error": "Target not found"},
    )
    invalid = client.post("/execution/result", json={"success": True})

    assert success.status_code == 204
    assert success.content == b""
    assert failure.status_code == 204
    assert invalid.status_code == 422


def test_ui_match_and_recovery_responses_have_stable_fields() -> None:
    client = TestClient(create_app())
    request = {
        "target": {"role": "button", "text": "Add to Cart"},
        "ui_tree": {
            "screen": "food_app",
            "elements": [{"id": "add", "role": "button", "text": "Add"}],
        },
    }

    match = client.post("/ui/match", json=request)
    recovery = client.post("/ui/recover", json=request)

    assert match.status_code == 200
    assert match.json() == {"matched": True, "node_id": "add", "confidence": 0.65}
    assert recovery.status_code == 200
    assert recovery.json() == {
        "matched": True,
        "node_id": "add",
        "confidence": 0.65,
        "requires_clarification": False,
        "reason": None,
    }

    uncertain_request = {
        "target": {"role": "button", "text": "Checkout"},
        "ui_tree": {
            "screen": "food_app",
            "elements": [{"id": "help", "role": "button", "text": "Help"}],
        },
    }
    uncertain_match = client.post("/ui/match", json=uncertain_request)
    uncertain_recovery = client.post("/ui/recover", json=uncertain_request)

    assert uncertain_match.status_code == 200
    assert uncertain_match.json()["matched"] is False
    assert uncertain_match.json()["node_id"] is None
    assert uncertain_recovery.status_code == 200
    assert uncertain_recovery.json()["matched"] is False
    assert uncertain_recovery.json()["requires_clarification"] is True
    assert uncertain_recovery.json()["reason"] == (
        "No sufficiently confident semantic match found"
    )


def test_openapi_describes_result_and_typed_ui_responses() -> None:
    client = TestClient(create_app())
    openapi = client.get("/openapi.json").json()

    result_operation = openapi["paths"]["/execution/result"]["post"]
    assert result_operation["requestBody"]["content"]["application/json"]["schema"]["$ref"] == (
        "#/components/schemas/ExecutionResult"
    )
    assert "204" in result_operation["responses"]

    match_response = openapi["paths"]["/ui/match"]["post"]["responses"]["200"]
    assert match_response["content"]["application/json"]["schema"]["$ref"] == (
        "#/components/schemas/UiMatchResponse"
    )

    recovery_response = openapi["paths"]["/ui/recover"]["post"]["responses"]["200"]
    assert recovery_response["content"]["application/json"]["schema"]["$ref"] == (
        "#/components/schemas/UiRecoveryResponse"
    )


def test_synthesis_response_matches_documented_workflow_json(
    tmp_path, monkeypatch
) -> None:
    monkeypatch.setattr(flow_store, "STORE_PATH", tmp_path / "workflows.json")
    client = TestClient(create_app())

    response = client.post(
        "/flows/synthesize",
        json={"demonstration": ["Search", "Pizza", "Quantity 2", "Add to Cart"]},
    )

    assert response.json() == {
        "flow_id": "order_food",
        "intent": "ORDER_FOOD",
        "parameters": ["item", "quantity"],
        "steps": [
            {"action": "search", "target": {"role": "edit_text"}, "value": "{{item}}"},
            {"action": "select", "target": {"role": "product"}, "value": None},
            {"action": "set_quantity", "target": None, "value": 2},
            {"action": "tap", "target": {"text": "Add to Cart"}, "value": None},
        ],
    }