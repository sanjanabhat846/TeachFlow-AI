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

NUMBER_WORDS = {
    "zero": 0,
    "one": 1,
    "two": 2,
    "three": 3,
    "four": 4,
    "five": 5,
    "six": 6,
    "seven": 7,
    "eight": 8,
    "nine": 9,
    "ten": 10,
}
COMMAND_WORDS = {
    "a", "an", "add", "buy", "can", "could", "get", "i", "like", "me",
    "need", "of", "order", "orders", "please", "purchase", "the", "to",
    "want", "would", "you",
}
SIZE_WORDS = {"extra", "large", "medium", "small"}
NUMBER_PATTERN = re.compile(r"(?<![\w.])[+-]?\d+(?:\.\d+)?(?![\w.])")


def _extract_quantity(text: str, item_start: int) -> tuple[int | None, bool]:
    """Return a positive quantity, or flag quantity text that cannot be trusted."""
    quantity_prefix = text[:item_start]
    if re.search(r"\b(?:negative|minus)\b|(?<!\w)-\s*\d", quantity_prefix):
        return None, True

    numeric_matches = list(NUMBER_PATTERN.finditer(text))
    word_matches = [
        (match, value)
        for word, value in NUMBER_WORDS.items()
        for match in re.finditer(rf"\b{word}\b", text)
    ]

    if numeric_matches:
        if len(numeric_matches) != 1 or word_matches:
            return None, True
        value = numeric_matches[0].group()
        if not re.fullmatch(r"\+?\d+", value):
            return None, True
        quantity = int(value)
        return (quantity, False) if quantity > 0 else (None, True)

    if word_matches:
        if len(word_matches) != 1:
            return None, True
        quantity = word_matches[0][1]
        return (quantity, False) if quantity > 0 else (None, True)

    prefix_words = re.findall(r"[a-z]+", quantity_prefix.lower())
    unexplained_words = [
        word for word in prefix_words
        if word not in COMMAND_WORDS and word not in SIZE_WORDS
    ]
    if unexplained_words:
        return None, True

    return 1, False


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

    item = None
    lowered = normalized.lower()
    item_match = None
    for candidate, canonical in FOOD_ITEMS.items():
        item_match = re.search(rf"\b{re.escape(candidate)}\b", lowered)
        if item_match:
            item = canonical
            break

    if item is None or item_match is None:
        return {"intent": intent, "parameters": {}}

    quantity, invalid_quantity = _extract_quantity(lowered, item_match.start())
    if invalid_quantity or quantity is None:
        return {"intent": intent, "parameters": {}}

    return {"intent": intent, "parameters": {"item": item, "quantity": quantity}}
