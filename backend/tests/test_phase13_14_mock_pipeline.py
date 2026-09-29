"""End-to-end backend pipeline tests using mock demonstrations and UI trees."""

from fastapi.testclient import TestClient

from backend.app.flow import flow_store
from backend.app.main import create_app


def test_mock_pipeline_reuses_learned_flow_and_recovers_ui_variation(
    tmp_path, monkeypatch
) -> None:
    monkeypatch.setattr(flow_store, "STORE_PATH", tmp_path / "workflows.json")
    client = TestClient(create_app())

    first_command = "Order 2 pizzas"
    first_intent = client.post("/intent/classify", json={"text": first_command}).json()
    first_extraction = client.post(
        "/intent/parameters", json={"text": first_command}
    ).json()

    assert first_intent["intent"] == "ORDER_FOOD"
    assert first_extraction == {
        "intent": "ORDER_FOOD",
        "parameters": {"item": "pizza", "quantity": 2},
    }

    learned = client.post(
        "/flows/synthesize",
        json={
            "demonstration": ["Search", "Pizza", "Quantity 2", "Add to Cart"]
        },
    )
    assert learned.status_code == 200
    learned_workflow = learned.json()
    assert learned_workflow["flow_id"] == "order_food"
    assert learned_workflow["parameters"] == ["item", "quantity"]
    assert learned_workflow["steps"][0]["value"] == "{{item}}"
    assert learned_workflow["steps"][2]["value"] == 2
    assert client.get("/flows").json()[0]["flow_id"] == "order_food"

    second_command = "Get me 3 burgers"
    second_intent = client.post(
        "/intent/classify", json={"text": second_command}
    ).json()
    second_extraction = client.post(
        "/intent/parameters", json={"text": second_command}
    ).json()
    matched_flow = client.post("/flows/match", json={"text": second_command}).json()

    assert second_intent["intent"] == "ORDER_FOOD"
    assert second_extraction == {
        "intent": "ORDER_FOOD",
        "parameters": {"item": "burger", "quantity": 3},
    }
    assert matched_flow["flow_id"] == learned.json()["flow_id"]
    assert len(client.get("/flows").json()) == 1

    plan_response = client.post(
        "/execution/plan",
        json={
            "intent": second_extraction["intent"],
            "parameters": second_extraction["parameters"],
            "flow_id": matched_flow["flow_id"],
        },
    )
    assert plan_response.status_code == 200
    actions = plan_response.json()["actions"]
    assert actions[0]["value"] == "burger"
    assert actions[2]["value"] == 3

    changed_ui = {
        "screen": "food_app",
        "elements": [{"id": "add_button", "role": "button", "text": "Add"}],
    }
    semantic_target = {"role": "button", "text": "Add to Cart"}
    ui_match = client.post(
        "/ui/match",
        json={"target": semantic_target, "ui_tree": changed_ui},
    ).json()
    recovery = client.post(
        "/ui/recover",
        json={"target": semantic_target, "ui_tree": changed_ui},
    ).json()

    assert ui_match["matched"] is True
    assert ui_match["node_id"] == "add_button"
    assert recovery["matched"] is True
    assert recovery["node_id"] == "add_button"
    assert recovery["requires_clarification"] is False

    routine_safety = client.post("/safety/check", json={"actions": actions}).json()
    checkout_safety = client.post(
        "/safety/check",
        json={"actions": [{"action": "tap", "target": {"text": "Checkout"}}]},
    ).json()
    assert routine_safety["requires_approval"] is False
    assert checkout_safety["requires_approval"] is True