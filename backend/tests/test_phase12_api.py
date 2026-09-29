"""HTTP integration tests for the backend API."""

from fastapi.testclient import TestClient

from backend.app.flow import flow_store
from backend.app.main import create_app


def test_api_exposes_intent_and_parameter_routes() -> None:
    client = TestClient(create_app())

    health_response = client.get("/health")
    intent_response = client.post("/intent/classify", json={"text": "Order 2 pizzas"})
    parameters_response = client.post(
        "/intent/parameters", json={"text": "Order 2 pizzas"}
    )
    match_response = client.post("/flows/match", json={"text": "Order 2 pizzas"})

    assert health_response.json() == {"status": "ok"}
    assert intent_response.status_code == 200
    assert intent_response.json()["intent"] == "ORDER_FOOD"
    assert parameters_response.status_code == 200
    assert parameters_response.json() == {
        "intent": "ORDER_FOOD",
        "parameters": {"item": "pizza", "quantity": 2},
    }
    assert match_response.json()["flow_id"] == "order_food"


def test_api_learns_and_plans_workflow_using_isolated_storage(
    tmp_path, monkeypatch
) -> None:
    monkeypatch.setattr(flow_store, "STORE_PATH", tmp_path / "workflows.json")
    client = TestClient(create_app())
    workflow = {
        "flow_id": "order_food",
        "intent": "ORDER_FOOD",
        "parameters": ["item", "quantity"],
        "steps": [
            {"action": "search", "target": {"role": "edit_text"}, "value": "{{item}}"},
            {"action": "set_quantity", "value": 1},
        ],
    }

    learn_response = client.post("/flows/learn", json=workflow)
    plan_response = client.post(
        "/execution/plan",
        json={"intent": "ORDER_FOOD", "parameters": {"item": "pizza", "quantity": 2}},
    )

    assert learn_response.status_code == 200
    assert learn_response.json()["success"] is True
    assert plan_response.status_code == 200
    assert plan_response.json()["actions"] == [
        {"action": "search", "target": {"role": "edit_text"}, "value": "pizza"},
        {"action": "set_quantity", "value": 2},
    ]
    assert client.get("/flows/order_food").status_code == 200
    assert len(client.get("/flows").json()) == 1
    assert client.post(
        "/execution/plan",
        json={"intent": "ORDER_FOOD", "parameters": {}},
    ).status_code == 422
    assert client.post(
        "/execution/plan",
        json={"intent": "ORDER_FOOD", "flow_id": "missing", "parameters": {}},
    ).status_code == 404
    assert client.delete("/flows/order_food").json()["deleted"] is True
    assert client.get("/flows/order_food").status_code == 404


def test_api_synthesizes_and_persists_demonstrated_workflow(
    tmp_path, monkeypatch
) -> None:
    monkeypatch.setattr(flow_store, "STORE_PATH", tmp_path / "workflows.json")
    client = TestClient(create_app())

    response = client.post(
        "/flows/synthesize",
        json={"demonstration": ["Search", "Type pizza", "Set quantity 2", "Add to Cart"]},
    )

    assert response.status_code == 200
    assert response.json()["flow_id"] == "order_food"
    assert len(client.get("/flows").json()) == 1


def test_api_safety_and_ui_routes() -> None:
    client = TestClient(create_app())
    actions = client.post(
        "/safety/check",
        json={"actions": [{"action": "tap", "target": {"text": "Checkout"}}]},
    )
    ui_match = client.post(
        "/ui/match",
        json={
            "target": {"role": "button", "text": "Add to Cart"},
            "ui_tree": {
                "screen": "food_app",
                "elements": [{"id": "add", "role": "button", "text": "Add to Cart"}],
            },
        },
    )

    assert actions.status_code == 200
    assert actions.json()["requires_approval"] is True
    assert ui_match.status_code == 200
    assert ui_match.json()["matched"] is True
    assert ui_match.json()["node_id"] == "add"
    ui_recovery = client.post(
        "/ui/recover",
        json={
            "target": {"role": "button", "text": "Add to Cart"},
            "ui_tree": {
                "screen": "food_app",
                "elements": [{"id": "add", "role": "button", "text": "Add to Cart"}],
            },
        },
    )
    assert ui_recovery.status_code == 200
    assert ui_recovery.json()["requires_clarification"] is False