"""Local JSON-backed workflow storage for the TeachFlow AI backend."""

from __future__ import annotations

import json
from pathlib import Path


STORE_PATH = Path(__file__).resolve().parents[2] / "data" / "workflows.json"


def _load_store() -> dict:
    """Load the workflow JSON store, creating it if missing."""
    if not STORE_PATH.exists():
        STORE_PATH.parent.mkdir(parents=True, exist_ok=True)
        STORE_PATH.write_text("{\n  \"flows\": []\n}\n", encoding="utf-8")

    with STORE_PATH.open("r", encoding="utf-8") as handle:
        try:
            data = json.load(handle)
        except json.JSONDecodeError:
            data = {"flows": []}

    if not isinstance(data, dict):
        data = {"flows": []}
    if "flows" not in data or not isinstance(data["flows"], list):
        data["flows"] = []

    return data


def _save_store(data: dict) -> None:
    """Write the store back to disk."""
    STORE_PATH.parent.mkdir(parents=True, exist_ok=True)
    with STORE_PATH.open("w", encoding="utf-8") as handle:
        json.dump(data, handle, indent=2)
        handle.write("\n")


def save_flow(flow: dict) -> dict:
    """Persist a learned workflow, replacing an existing one by flow_id."""
    data = _load_store()
    flow_id = flow.get("flow_id")
    if not flow_id:
        raise ValueError("flow_id is required to save a workflow")

    flows = data["flows"]
    for index, existing in enumerate(flows):
        if existing.get("flow_id") == flow_id:
            flows[index] = flow
            _save_store(data)
            return flow

    flows.append(flow)
    _save_store(data)
    return flow


def get_flow(flow_id: str) -> dict | None:
    """Load a workflow by ID."""
    data = _load_store()
    for flow in data.get("flows", []):
        if flow.get("flow_id") == flow_id:
            return flow
    return None


def list_flows() -> list[dict]:
    """List all stored workflows."""
    data = _load_store()
    return list(data.get("flows", []))


def delete_flow(flow_id: str) -> bool:
    """Delete a stored workflow by ID."""
    data = _load_store()
    before = len(data.get("flows", []))
    data["flows"] = [flow for flow in data.get("flows", []) if flow.get("flow_id") != flow_id]
    if len(data["flows"]) != before:
        _save_store(data)
        return True
    return False
