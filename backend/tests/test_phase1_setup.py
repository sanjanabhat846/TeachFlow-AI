"""Basic smoke test for the Phase 1 backend scaffold."""

from backend.app.intent.classifier import classify_intent
from backend.app.intent.parameter_extractor import extract_parameters


def test_backend_imports_smoke() -> None:
    assert classify_intent("Order 2 pizzas") == "ORDER_FOOD"
    assert extract_parameters("Order 2 pizzas") == {
        "intent": "ORDER_FOOD",
        "parameters": {"item": "pizza", "quantity": 2},
    }
