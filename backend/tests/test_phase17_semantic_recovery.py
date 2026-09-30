"""Regression tests for safe semantic UI variation and recovery."""

from backend.app.semantic.ui_matcher import match_ui_element, recover_target


def test_add_to_cart_matches_shorter_label_with_cart_context() -> None:
    target = {
        "role": "button",
        "text": "Add to Cart",
        "context": "Cart actions",
        "clickable": True,
    }
    ui_tree = {
        "elements": [
            {
                "id": "add",
                "role": "button",
                "text": "Add",
                "content_description": "Add item",
                "resource_id": "cart_add",
                "context": "Cart actions",
                "clickable": True,
                "enabled": True,
            }
        ]
    }

    result = recover_target(target, ui_tree)

    assert result["matched"] is True
    assert result["node_id"] == "add"
    assert result["requires_clarification"] is False


def test_search_matches_expanded_text_label() -> None:
    result = match_ui_element(
        {"role": "edit_text", "text": "Search"},
        {"elements": [{"id": "search", "role": "edit_text", "text": "Search products", "enabled": True}]},
    )

    assert result["matched"] is True
    assert result["node_id"] == "search"


def test_quantity_control_recovers_across_role_and_layout_variation() -> None:
    result = recover_target(
        {
            "role": "quantity_picker",
            "text": "Quantity",
            "context": "Order quantity selector",
        },
        {
            "elements": [
                {
                    "id": "decrease_quantity",
                    "role": "button",
                    "content_description": "Decrease quantity",
                    "context": "Order quantity selector",
                    "clickable": True,
                    "enabled": True,
                }
            ]
        },
    )

    assert result["matched"] is True
    assert result["node_id"] == "decrease_quantity"


def test_content_description_and_resource_id_variations_use_context() -> None:
    result = match_ui_element(
        {
            "role": "button",
            "text": "Add to Cart",
            "content_description": "Add to Cart",
            "resource_id": "food:add_cart",
            "context": "Cart actions",
            "clickable": True,
        },
        {
            "elements": [
                {
                    "id": "add_action",
                    "role": "button",
                    "text": "Add",
                    "content_description": "Add products",
                    "resource_id": "food:cart_add_action",
                    "context": "Cart actions",
                    "clickable": True,
                    "enabled": True,
                }
            ]
        },
    )

    assert result["matched"] is True
    assert result["node_id"] == "add_action"


def test_disabled_match_is_rejected_and_enabled_alternative_is_used() -> None:
    result = match_ui_element(
        {"role": "button", "text": "Add to Cart", "clickable": True},
        {
            "elements": [
                {
                    "id": "disabled_add",
                    "role": "button",
                    "text": "Add to Cart",
                    "clickable": True,
                    "enabled": False,
                },
                {
                    "id": "enabled_add",
                    "role": "button",
                    "text": "Add",
                    "content_description": "Add to Cart",
                    "clickable": True,
                    "enabled": True,
                },
            ]
        },
    )

    assert result["matched"] is True
    assert result["node_id"] == "enabled_add"


def test_missing_target_requires_clarification() -> None:
    result = recover_target(
        {"role": "button", "text": "Add to Cart"},
        {"elements": [{"id": "help", "role": "button", "text": "Help", "enabled": True}]},
    )

    assert result["matched"] is False
    assert result["requires_clarification"] is True


def test_ambiguous_add_candidates_require_clarification() -> None:
    target = {"role": "button", "text": "Add to Cart", "clickable": True}
    ui_tree = {
        "elements": [
            {"id": "add_first", "role": "button", "text": "Add", "clickable": True, "enabled": True},
            {"id": "add_second", "role": "button", "text": "Add", "clickable": True, "enabled": True},
        ]
    }

    match = match_ui_element(target, ui_tree)
    recovery = recover_target(target, ui_tree)

    assert match["matched"] is False
    assert recovery["matched"] is False
    assert recovery["requires_clarification"] is True
    assert "ambiguous" in recovery["reason"].lower()


def test_context_disambiguates_similarly_labeled_add_candidates() -> None:
    result = match_ui_element(
        {
            "role": "button",
            "text": "Add to Cart",
            "context": "Cart actions",
            "clickable": True,
        },
        {
            "elements": [
                {
                    "id": "add_address",
                    "role": "button",
                    "text": "Add",
                    "context": "Address actions",
                    "clickable": True,
                    "enabled": True,
                },
                {
                    "id": "add_cart",
                    "role": "button",
                    "text": "Add",
                    "content_description": "Add products to cart",
                    "context": "Cart actions",
                    "clickable": True,
                    "enabled": True,
                },
            ]
        },
    )

    assert result["matched"] is True
    assert result["node_id"] == "add_cart"


def test_weak_checkout_candidate_requires_clarification() -> None:
    result = recover_target(
        {"role": "button", "text": "Checkout", "context": "Cart actions"},
        {
            "elements": [
                {
                    "id": "continue",
                    "role": "text_view",
                    "text": "Continue",
                    "context": "Cart actions",
                    "enabled": True,
                }
            ]
        },
    )

    assert result["matched"] is False
    assert result["requires_clarification"] is True


def test_low_confidence_recovery_has_no_executable_node() -> None:
    result = recover_target(
        {"role": "button", "text": "Checkout"},
        {
            "elements": [
                {"id": "next", "role": "text_view", "text": "Next", "enabled": True}
            ]
        },
    )

    assert result["matched"] is False
    assert result["node_id"] is None
    assert result["requires_clarification"] is True


def test_low_confidence_target_is_not_reported_as_matched() -> None:
    result = match_ui_element(
        {"role": "button", "text": "Checkout"},
        {"elements": [{"id": "next", "role": "text_view", "text": "Next", "enabled": True}]},
    )

    assert result["matched"] is False
    assert result["node_id"] is None
