"""Tests for the Phase 3 intent classifier."""

from backend.app.intent.classifier import classify_intent


def test_order_food_intent() -> None:
    assert classify_intent("Order 2 pizzas") == "ORDER_FOOD"
    assert classify_intent("Get me 3 burgers") == "ORDER_FOOD"


def test_shop_product_intent() -> None:
    assert classify_intent("Buy a headphones") == "SHOP_PRODUCT"
    assert classify_intent("I want to purchase a phone") == "SHOP_PRODUCT"


def test_travel_destination_intent() -> None:
    assert classify_intent("Book a flight to Paris") == "TRAVEL_DESTINATION"
    assert classify_intent("Travel to Tokyo") == "TRAVEL_DESTINATION"


def test_unknown_intent() -> None:
    assert classify_intent("Tell me a joke") == "UNKNOWN"
    assert classify_intent("Hello there") == "UNKNOWN"
