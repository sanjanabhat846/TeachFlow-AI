"""Approval checks for potentially sensitive execution actions."""

from __future__ import annotations

import re
from typing import Any


_SENSITIVE_CATEGORIES = (
    ("checkout", re.compile(r"\bcheckout\b", re.IGNORECASE)),
    ("payment", re.compile(r"\bpay(?:ment|ments)?\b", re.IGNORECASE)),
    ("authentication", re.compile(r"\b(?:auth(?:entication|enticate|orization)?|log[ -]*in|sign[ -]*in)\b", re.IGNORECASE)),
    ("password", re.compile(r"\b(?:password|passcode|pin)\b", re.IGNORECASE)),
    ("OTP", re.compile(r"\b(?:otp|one[ -]time (?:password|code)|verification code)\b", re.IGNORECASE)),
)


def _text_values(value: Any) -> list[str]:
    if isinstance(value, dict):
        return [text for nested in value.values() for text in _text_values(nested)]
    if isinstance(value, list):
        return [text for nested in value for text in _text_values(nested)]
    if isinstance(value, str):
        return [value]
    return []


def check_safety(execution_plan: list[dict]) -> dict:
    """Require human approval before an action involving sensitive workflows."""
    if not isinstance(execution_plan, list):
        return {
            "requires_approval": True,
            "reason": "Unable to verify execution plan",
        }

    for action in execution_plan:
        if not isinstance(action, dict):
            return {
                "requires_approval": True,
                "reason": "Unable to verify execution plan",
            }

        action_text = " ".join(_text_values(action))
        for category, pattern in _SENSITIVE_CATEGORIES:
            if pattern.search(action_text):
                return {
                    "requires_approval": True,
                    "reason": f"Sensitive action requires approval: {category.lower()}",
                }

    return {"requires_approval": False, "reason": None}