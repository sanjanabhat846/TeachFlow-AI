"""Deterministic workflow matching for the TeachFlow AI MVP."""

from __future__ import annotations

import re


FOOD_KEYWORDS = {
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

ORDER_WORDS = {"order", "orders", "get", "buy", "purchase", "need", "want", "add"}


def match_flow(command: str) -> dict:
    """Map a natural-language command to the learned order-food flow.

    The approach is intentionally deterministic and robust for the MVP: look for an
    order-like action plus a food keyword.
    """
    text = (command or "").strip().lower()
    if not text:
        return {"flow_id": "unknown", "confidence": 0.0}

    has_order_intent = any(word in text for word in ORDER_WORDS)
    has_food_keyword = any(keyword in text for keyword in FOOD_KEYWORDS)
    if not (has_order_intent and has_food_keyword):
        return {"flow_id": "unknown", "confidence": 0.0}

    # Strong simple heuristic: a command that looks like ordering food should map to
    # the same learned flow even if the exact product or quantity wording varies.
    if re.search(r"\b(order|orders|get|buy|purchase|need|want|add)\b", text):
        return {"flow_id": "order_food", "confidence": 0.94}

    return {"flow_id": "unknown", "confidence": 0.0}
