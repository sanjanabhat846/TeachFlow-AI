"""Tests for approval-required execution actions."""

import pytest

from backend.app.executor.safety_checker import check_safety


def test_safety_checker_requires_approval_for_sensitive_actions() -> None:
    plan = [
        {"action": "tap", "target": {"text": "Checkout"}},
        {"action": "type", "target": {"role": "edit_text", "text": "Password"}},
    ]

    result = check_safety(plan)

    assert result["requires_approval"] is True
    assert result["reason"] == "Sensitive action requires approval: checkout"


@pytest.mark.parametrize(
    ("target_text", "category"),
    [
        ("Pay now", "payment"),
        ("Sign in", "authentication"),
        ("Password", "password"),
        ("Enter OTP", "otp"),
    ],
)
def test_safety_checker_gates_sensitive_categories(target_text: str, category: str) -> None:
    result = check_safety([{"action": "tap", "target": {"text": target_text}}])

    assert result == {
        "requires_approval": True,
        "reason": f"Sensitive action requires approval: {category}",
    }


def test_safety_checker_allows_ordinary_actions() -> None:
    plan = [
        {"action": "search", "target": {"role": "edit_text"}, "value": "pizza"},
        {"action": "tap", "target": {"text": "Add to Cart"}},
    ]

    assert check_safety(plan) == {"requires_approval": False, "reason": None}