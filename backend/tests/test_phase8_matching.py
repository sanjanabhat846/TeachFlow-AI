"""Tests for semantic UI matching."""

from backend.app.semantic.ui_matcher import match_ui_element


def test_exact_semantic_ui_match() -> None:
    target = {"role": "button", "text": "Add to Cart"}
    ui_tree = {
        "screen": "food_app",
        "elements": [
            {"id": "node_1", "role": "button", "text": "Add to Cart", "content_description": "", "resource_id": "add_cart"},
            {"id": "node_2", "role": "button", "text": "Checkout", "content_description": "Checkout", "resource_id": "checkout_btn"},
        ],
    }

    result = match_ui_element(target, ui_tree)
    assert result["matched"] is True
    assert result["node_id"] == "node_1"
    assert result["confidence"] >= 0.8


def test_fuzzy_semantic_ui_match() -> None:
    target = {"role": "button", "text": "Add to Cart"}
    ui_tree = {
        "screen": "food_app",
        "elements": [
            {"id": "node_3", "role": "button", "text": "Add", "content_description": "Add product to cart", "resource_id": "add_btn"},
        ],
    }

    result = match_ui_element(target, ui_tree)
    assert result["matched"] is True
    assert result["node_id"] == "node_3"
    assert result["confidence"] >= 0.55


def test_no_match_when_not_confident() -> None:
    target = {"role": "button", "text": "Add to Cart"}
    ui_tree = {
        "screen": "food_app",
        "elements": [
            {"id": "node_4", "role": "button", "text": "Help", "content_description": "Support", "resource_id": "help_btn"},
        ],
    }

    result = match_ui_element(target, ui_tree)
    assert result["matched"] is False
    assert result["node_id"] is None
