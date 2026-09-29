"""Semantic UI matching placeholder for the TeachFlow AI backend."""


def match_ui_element(target: dict, ui_tree: dict) -> dict:
    """Return a matched UI node or a failure payload."""
    return {"matched": False, "node_id": None, "confidence": 0.0}
