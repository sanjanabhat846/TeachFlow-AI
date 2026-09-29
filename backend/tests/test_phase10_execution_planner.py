"""Tests for semantic execution-plan generation."""

import pytest

from backend.app.executor.execution_planner import build_execution_plan


def test_execution_plan_substitutes_parameters_and_preserves_step_order() -> None:
    flow = {
        "steps": [
            {"action": "search", "target": {"role": "edit_text"}, "value": "{{item}}"},
            {"action": "set_quantity", "value": 1},
            {"action": "tap", "target": {"text": "Add to Cart"}},
        ]
    }

    plan = build_execution_plan(flow, {"item": "pizza", "quantity": 3})

    assert plan == [
        {"action": "search", "target": {"role": "edit_text"}, "value": "pizza"},
        {"action": "set_quantity", "value": 3},
        {"action": "tap", "target": {"text": "Add to Cart"}},
    ]
    assert flow["steps"][0]["value"] == "{{item}}"


def test_execution_plan_rejects_missing_template_parameters() -> None:
    flow = {"steps": [{"action": "search", "value": "{{item}}"}]}

    with pytest.raises(ValueError, match="item"):
        build_execution_plan(flow, {})