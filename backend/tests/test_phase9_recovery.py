"""Tests for recovery and re-matching."""

from backend.app.semantic.ui_matcher import recover_target


def test_recovery_recovers_matching_target_when_confident() -> None:
    target = {"role": "button", "text": "Add to Cart"}
    ui_tree = {
        "screen": "food_app",
        "elements": [
            {"id": "node_1", "role": "button", "text": "Add", "content_description": "Add product to cart", "resource_id": "add_btn"},
            {"id": "node_2", "role": "button", "text": "Checkout", "content_description": "Checkout", "resource_id": "checkout_btn"},
        ],
    }

    result = recover_target(target, ui_tree)
    assert result["matched"] is True
    assert result["node_id"] == "node_1"


def test_recovery_requires_user_clarification_if_unconfident() -> None:
    target = {"role": "button", "text": "Add to Cart"}
    ui_tree = {
        "screen": "food_app",
        "elements": [
            {"id": "node_5", "role": "button", "text": "Help", "content_description": "Support", "resource_id": "help_btn"},
        ],
    }

    result = recover_target(target, ui_tree)
    assert result["matched"] is False
    assert result["requires_clarification"] is True
