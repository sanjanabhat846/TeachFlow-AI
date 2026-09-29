"""Tests for workflow matching."""

from backend.app.flow.flow_matcher import match_flow


def test_order_food_flow_matching() -> None:
    result_1 = match_flow("Order two pizzas")
    result_2 = match_flow("Get me three burgers")
    result_3 = match_flow("I want two pizzas")

    assert result_1["flow_id"] == "order_food"
    assert result_2["flow_id"] == "order_food"
    assert result_3["flow_id"] == "order_food"
    assert result_1["confidence"] >= 0.8
    assert result_2["confidence"] >= 0.8
    assert result_3["confidence"] >= 0.8


def test_unknown_flow_matching() -> None:
    result = match_flow("Tell me a joke")
    assert result["flow_id"] == "unknown"
    assert result["confidence"] == 0.0
