"""Minimal rule-based intent classifier for the TeachFlow AI backend."""

from __future__ import annotations


FOOD_KEYWORDS = {
    "pizza",
    "pizzas",
    "burger",
    "burgers",
    "sandwich",
    "sandwiches",
    "taco",
    "tacos",
    "fries",
    "meal",
    "food",
    "order food",
    "restaurant",
    "coffee",
    "drink",
    "milkshake",
    "salad",
}

SHOP_KEYWORDS = {
    "headphones",
    "phone",
    "phones",
    "laptop",
    "laptops",
    "tablet",
    "tablets",
    "watch",
    "watches",
    "camera",
    "shoes",
    "product",
    "item",
}

TRAVEL_KEYWORDS = {
    "flight",
    "flights",
    "travel",
    "trip",
    "hotel",
    "destination",
    "airport",
    "train",
    "booking",
    "book a flight",
    "to paris",
    "to tokyo",
    "to london",
}

ORDER_ACTIONS = {"order", "orders", "get", "buy", "add", "want", "purchase", "need"}
SHOP_ACTIONS = {"buy", "purchase", "shop", "get", "need", "want"}
TRAVEL_ACTIONS = {"travel", "book", "reserve", "schedule", "fly"}


def classify_intent(text: str) -> str:
    """Classify a short user command into a supported intent.

    The MVP intentionally uses a small rule-based approach instead of a heavy ML model.
    """
    normalized = (text or "").lower().strip()
    if not normalized:
        return "UNKNOWN"

    if any(keyword in normalized for keyword in FOOD_KEYWORDS) or any(
        action in normalized for action in ORDER_ACTIONS
    ) and any(keyword in normalized for keyword in FOOD_KEYWORDS):
        return "ORDER_FOOD"

    if any(keyword in normalized for keyword in SHOP_KEYWORDS) and any(
        action in normalized for action in SHOP_ACTIONS
    ):
        return "SHOP_PRODUCT"

    if any(keyword in normalized for keyword in TRAVEL_KEYWORDS) or any(
        action in normalized for action in TRAVEL_ACTIONS
    ) and any(keyword in normalized for keyword in TRAVEL_KEYWORDS):
        return "TRAVEL_DESTINATION"

    return "UNKNOWN"
