"""Minimal parameter extractor for TeachFlow AI order flows."""

from __future__ import annotations

import re

from backend.app.intent.classifier import classify_intent

FOOD_ITEMS = {
    "pizza": "pizza",
    "pizzas": "pizza",
    "burger": "burger",
    "burgers": "burger",
    "sandwich": "sandwich",
    "sandwiches": "sandwich",
    "taco": "taco",
    "tacos": "taco",
    "salad": "salad",
    "fries": "fries",
    "meal": "meal",
    "coffee": "coffee",
    "drink": "drink",
    "milkshake": "milkshake",
}


def extract_parameters(text: str) -> dict:
    """Extract the main product and quantity from recognized intent text.

    Supported MVP: ORDER_FOOD(item, quantity)
    """
    normalized = (text or "").strip()
    if not normalized:
        return {"intent": "UNKNOWN", "parameters": {}}

    intent = classify_intent(normalized)
    if intent != "ORDER_FOOD":
        return {"intent": intent, "parameters": {}}

    quantity = None
    match = re.search(r"\b(\d+)\b", normalized)
    if match:
        quantity = int(match.group(1))

    item = None
    lowered = normalized.lower()
    for candidate, canonical in FOOD_ITEMS.items():
        if re.search(rf"\b{re.escape(candidate)}\b", lowered):
            item = canonical
            break

    if item is None:
        return {"intent": intent, "parameters": {}}

    if quantity is None:
        quantity = 1

    return {"intent": intent, "parameters": {"item": item, "quantity": quantity}}
