"""Workflow matching placeholder for the TeachFlow AI backend."""


def match_flow(command: str) -> dict:
    """Return the best matching workflow for a natural-language command."""
    return {"flow_id": "unknown", "confidence": 0.0}
