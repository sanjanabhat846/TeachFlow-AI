"""Execution plan generation for the TeachFlow AI backend."""

from __future__ import annotations

import re
from typing import Any


_PARAMETER_PATTERN = re.compile(r"{{\s*([a-zA-Z_]\w*)\s*}}")


def _resolve_value(value: Any, parameters: dict[str, Any]) -> Any:
    """Resolve parameter placeholders in nested step data."""
    if isinstance(value, dict):
        return {key: _resolve_value(nested, parameters) for key, nested in value.items()}
    if isinstance(value, list):
        return [_resolve_value(item, parameters) for item in value]
    if not isinstance(value, str):
        return value

    exact_match = _PARAMETER_PATTERN.fullmatch(value)
    if exact_match:
        name = exact_match.group(1)
        if name not in parameters:
            raise ValueError(f"Missing execution parameter: {name}")
        return parameters[name]

    def replace(match: re.Match[str]) -> str:
        name = match.group(1)
        if name not in parameters:
            raise ValueError(f"Missing execution parameter: {name}")
        return str(parameters[name])

    return _PARAMETER_PATTERN.sub(replace, value)


def build_execution_plan(flow: dict, parameters: dict) -> list[dict]:
    """Create ordered semantic actions from a workflow and its runtime parameters."""
    if not isinstance(flow, dict):
        raise ValueError("Workflow must be an object")

    steps = flow.get("steps", [])
    if not isinstance(steps, list):
        raise ValueError("Workflow steps must be a list")
    if not isinstance(parameters, dict):
        raise ValueError("Execution parameters must be an object")

    actions = []
    for step in steps:
        if not isinstance(step, dict) or not isinstance(step.get("action"), str):
            raise ValueError("Each workflow step must include an action")

        action = _resolve_value(step, parameters)
        if action["action"] == "set_quantity" and "quantity" in parameters:
            action["value"] = parameters["quantity"]
        actions.append(action)

    return actions
