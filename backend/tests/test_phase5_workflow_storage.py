"""Tests for local workflow storage."""

from backend.app.flow.flow_store import delete_flow, get_flow, list_flows, save_flow


def test_flow_store_crud() -> None:
    flow = {
        "flow_id": "order_food",
        "intent": "ORDER_FOOD",
        "parameters": ["item", "quantity"],
        "steps": [
            {"action": "search", "target": {"role": "edit_text"}, "value": "{{item}}"},
            {"action": "tap", "target": {"text": "Add to Cart"}},
        ],
    }

    save_flow(flow)
    stored = get_flow("order_food")
    assert stored is not None
    assert stored["flow_id"] == "order_food"
    assert len(list_flows()) >= 1

    delete_flow("order_food")
    assert get_flow("order_food") is None
