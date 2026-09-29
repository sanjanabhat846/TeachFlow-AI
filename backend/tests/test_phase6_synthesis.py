"""Tests for workflow synthesis from a demonstration."""

from backend.app.flow.flow_synthesizer import synthesize_flow


def test_order_food_workflow_synthesis() -> None:
    demonstration = [
        "Search",
        "Type pizza",
        "Select pizza",
        "Set quantity 2",
        "Add to Cart",
    ]

    flow = synthesize_flow(demonstration)

    assert flow["flow_id"] == "order_food"
    assert flow["intent"] == "ORDER_FOOD"
    assert flow["parameters"] == ["item", "quantity"]
    assert flow["steps"][0]["action"] == "search"
    assert flow["steps"][0]["target"]["role"] == "edit_text"
    assert flow["steps"][0]["value"] == "{{item}}"
    assert flow["steps"][1]["action"] == "select"
    assert flow["steps"][2]["action"] == "set_quantity"
    assert flow["steps"][2]["value"] == 2
    assert flow["steps"][3]["action"] == "tap"
    assert flow["steps"][3]["target"]["text"] == "Add to Cart"
