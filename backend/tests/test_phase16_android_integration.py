"""End-to-end API contract exercised by the Android demo flow."""

from fastapi.testclient import TestClient

from backend.app.flow import flow_store
from backend.app.main import create_app


def test_android_learn_and_replay_api_sequence(tmp_path, monkeypatch) -> None:
    monkeypatch.setattr(flow_store, "STORE_PATH", tmp_path / "workflows.json")
    client = TestClient(create_app())

    intent = client.post("/intent/classify", json={"text": "Order 2 pizzas"})
    parameters = client.post(
        "/intent/parameters",
        json={"text": "Order 2 pizzas", "intent": intent.json()["intent"]},
    )
    assert intent.json()["intent"] == "ORDER_FOOD"
    assert parameters.json()["parameters"] == {"item": "pizza", "quantity": 2}

    synthesized = client.post(
        "/flows/synthesize",
        json={"demonstration": ["Search", "pizza", "Quantity 2", "Add to Cart"]},
    )
    assert synthesized.status_code == 200

    learned = client.post(
        "/flows/learn",
        json={
            "flow_id": "order_food",
            "intent": "ORDER_FOOD",
            "parameters": ["item", "quantity"],
            "steps": [
                {
                    "action": "search",
                    "target": {
                        "role": "edit_text",
                        "text": "Search food",
                        "class_name": "android.widget.EditText",
                    },
                    "value": "{{item}}",
                },
                {
                    "action": "select",
                    "target": {
                        "role": "product",
                        "text": "{{item}}",
                        "content_description": "{{item}}",
                        "clickable": True,
                    },
                },
                {
                    "action": "tap",
                    "target": {"role": "button", "text": "Add to Cart"},
                },
            ],
        },
    )
    assert learned.status_code == 200

    match = client.post("/flows/match", json={"text": "Get me 3 burgers"})
    plan = client.post(
        "/execution/plan",
        json={
            "intent": "ORDER_FOOD",
            "flow_id": match.json()["flow_id"],
            "parameters": {"item": "burger", "quantity": 3},
        },
    )
    assert match.json()["flow_id"] == "order_food"
    assert plan.json()["actions"] == [
        {
            "action": "search",
            "target": {
                "role": "edit_text",
                "text": "Search food",
                "class_name": "android.widget.EditText",
            },
            "value": "burger",
        },
        {
            "action": "select",
            "target": {
                "role": "product",
                "text": "burger",
                "content_description": "burger",
                "clickable": True,
            },
        },
        {"action": "tap", "target": {"role": "button", "text": "Add to Cart"}},
    ]

    ui_match = client.post(
        "/ui/match",
        json={
            "target": plan.json()["actions"][0]["target"],
            "ui_tree": {
                "screen": "food_app",
                "timestamp": 1727625600000,
                "elements": [
                    {
                        "id": "search_input",
                        "role": "edit_text",
                        "class_name": "android.widget.EditText",
                        "text": "Search food",
                        "content_description": "Search food items",
                        "resource_id": "food:id/search_box",
                        "clickable": True,
                        "enabled": True,
                        "editable": True,
                        "scrollable": False,
                        "bounds": "[0,100][1080,200]",
                    }
                ],
            },
        },
    )
    assert ui_match.json()["matched"] is True
    assert ui_match.json()["node_id"] == "search_input"

    quantity_match = client.post(
        "/ui/match",
        json={
            "target": {"role": "quantity_picker", "content_description": "quantity_picker"},
            "ui_tree": {
                "elements": [
                    {
                        "id": "quantity_row",
                        "role": "container",
                        "content_description": "quantity_picker",
                        "enabled": True,
                    }
                ]
            },
        },
    )
    assert quantity_match.json()["matched"] is True

    unsafe_match = client.post(
        "/ui/match",
        json={
            "target": {"role": "button", "text": "Checkout", "clickable": True},
            "ui_tree": {
                "elements": [
                    {
                        "id": "disabled_checkout",
                        "role": "button",
                        "text": "Checkout",
                        "clickable": False,
                        "enabled": False,
                    }
                ]
            },
        },
    )
    assert unsafe_match.json()["matched"] is False

    safety = client.post(
        "/safety/check",
        json={"actions": [{"action": "tap", "target": {"text": "Checkout & Pay"}}]},
    )
    assert safety.json()["requires_approval"] is True