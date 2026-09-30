"""Tests for the Phase 4 parameter extractor."""

import pytest

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


@pytest.mark.parametrize(
    "text",
    [
        "Order 0 pizzas",
        "Order -2 pizzas",
        "Order negative 2 pizzas",
        "Order 1.5 pizzas",
        "Order many pizzas",
        "Order foo pizzas",
    ],
)
def test_invalid_quantities_do_not_produce_parameters(text: str) -> None:
    assert extract_parameters(text) == {"intent": "ORDER_FOOD", "parameters": {}}


def test_spoken_integer_quantity_is_parsed_without_falling_back_to_one() -> None:
    assert extract_parameters("Order two pizzas") == {
        "intent": "ORDER_FOOD",
        "parameters": {"item": "pizza", "quantity": 2},
    }


def test_missing_quantity_keeps_default_of_one() -> None:
    assert extract_parameters("Order pizzas") == {
        "intent": "ORDER_FOOD",
        "parameters": {"item": "pizza", "quantity": 1},
    }
