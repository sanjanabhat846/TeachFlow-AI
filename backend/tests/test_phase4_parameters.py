"""Tests for the Phase 4 parameter extractor."""

from backend.app.intent.parameter_extractor import extract_parameters


def test_order_food_parameters() -> None:
    assert extract_parameters("Order 2 pizzas") == {
        "intent": "ORDER_FOOD",
        "parameters": {"item": "pizza", "quantity": 2},
    }
    assert extract_parameters("Get me 3 burgers") == {
        "intent": "ORDER_FOOD",
        "parameters": {"item": "burger", "quantity": 3},
    }
    assert extract_parameters("I want 1 sandwich") == {
        "intent": "ORDER_FOOD",
        "parameters": {"item": "sandwich", "quantity": 1},
    }
    assert extract_parameters("Add 4 tacos") == {
        "intent": "ORDER_FOOD",
        "parameters": {"item": "taco", "quantity": 4},
    }


def test_unknown_parameters() -> None:
    assert extract_parameters("Hello there") == {"intent": "UNKNOWN", "parameters": {}}
