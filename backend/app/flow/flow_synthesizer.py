"""Workflow synthesis for the TeachFlow AI MVP."""

from __future__ import annotations

import re


def synthesize_flow(demonstration: list[str]) -> dict:
    """Convert a demonstration into a reusable semantic ORDER_FOOD workflow.

    The implementation intentionally stays simple and deterministic: it recognizes a
    common food-order sequence and turns it into a parameterized template.
    """
    normalized = [step.strip() for step in demonstration if str(step).strip()]
    if not normalized:
        return {
            "flow_id": "order_food",
            "intent": "ORDER_FOOD",
            "parameters": ["item", "quantity"],
            "steps": [],
        }

    item = None
    quantity = None
    for step in normalized:
        lowered = step.lower()
        match = re.search(r"\b(pizza|burgers?|sandwich(es)?|tacos?|salad|fries|meal|coffee|drink|milkshake)\b", lowered)
        if match:
            item = match.group(1).rstrip("s")
            if item == "burgers":
                item = "burger"
            if item == "pizzas":
                item = "pizza"
            if item == "sandwiches":
                item = "sandwich"
            if item == "tacos":
                item = "taco"
            break

    quantity_match = re.search(r"\b(\d+)\b", " ".join(normalized).lower())
    if quantity_match:
        quantity = int(quantity_match.group(1))

    steps = [
        {
            "action": "search",
            "target": {"role": "edit_text"},
            "value": "{{item}}",
        },
        {
            "action": "select",
            "target": {"role": "product"},
        },
        {
            "action": "set_quantity",
            "value": quantity if quantity is not None else 1,
        },
        {
            "action": "tap",
            "target": {"text": "Add to Cart"},
        },
    ]

    return {
        "flow_id": "order_food",
        "intent": "ORDER_FOOD",
        "parameters": ["item", "quantity"],
        "steps": steps,
    }
