"""Deterministic semantic UI matching for the TeachFlow AI MVP."""

from __future__ import annotations

import re
from difflib import SequenceMatcher


def _normalize(value: str) -> str:
    """Normalize text for semantic matching."""
    return re.sub(r"[^a-z0-9]+", " ", (value or "").lower()).strip()


def _fuzzy_similarity(a: str, b: str) -> float:
    """Return a similarity score between 0 and 1 using SequenceMatcher."""
    if not a and not b:
        return 1.0
    if not a or not b:
        return 0.0
    return SequenceMatcher(None, a, b).ratio()


def _score_candidate(target: dict, element: dict) -> float:
    """Score a candidate element against the target semantic description."""
    target_role = str(target.get("role", "")).lower()
    target_text = _normalize(str(target.get("text", "")))
    target_description = _normalize(str(target.get("content_description", "")))
    target_resource = str(target.get("resource_id", "")).lower()

    role = str(element.get("role", "")).lower()
    text = _normalize(str(element.get("text", "")))
    description = _normalize(str(element.get("content_description", "")))
    resource = str(element.get("resource_id", "")).lower()

    score = 0.0

    if target_role and role:
        if role == target_role:
            score += 0.4
        elif target_role in role or role in target_role:
            score += 0.25

    if target_text:
        if text == target_text:
            score += 0.45
        elif target_text in text or text in target_text:
            score += 0.25
        else:
            score += 0.25 * _fuzzy_similarity(target_text, text)

    if target_description:
        if description == target_description:
            score += 0.2
        elif target_description in description or description in target_description:
            score += 0.12
        else:
            score += 0.12 * _fuzzy_similarity(target_description, description)

    if target_resource and resource:
        if resource == target_resource:
            score += 0.15
        elif target_resource in resource or resource in target_resource:
            score += 0.08

    return score


def match_ui_element(target: dict, ui_tree: dict) -> dict:
    """Find a UI element matching a semantic target using role/text/content metadata.

    Security principle: accept only a candidate with confident semantic similarity.
    """
    if not isinstance(ui_tree, dict):
        return {"matched": False, "node_id": None, "confidence": 0.0}

    elements = ui_tree.get("elements", [])
    if not isinstance(elements, list) or not elements:
        return {"matched": False, "node_id": None, "confidence": 0.0}

    best_match = None
    best_score = 0.0

    for element in elements:
        if not isinstance(element, dict):
            continue
        score = _score_candidate(target, element)
        if score > best_score:
            best_score = score
            best_match = element

    if best_match is None:
        return {"matched": False, "node_id": None, "confidence": 0.0}

    if best_score >= 0.6:
        return {
            "matched": True,
            "node_id": best_match.get("id"),
            "confidence": round(best_score, 2),
        }

    return {"matched": False, "node_id": None, "confidence": round(best_score, 2)}


def recover_target(target: dict, ui_tree: dict) -> dict:
    """Safely recover a target from a current UI tree.

    If a confident semantic match exists, return it. Otherwise require user clarification
    instead of silently choosing a possibly unsafe or ambiguous target.
    """
    match = match_ui_element(target, ui_tree)
    if match["matched"]:
        return {
            "matched": True,
            "node_id": match["node_id"],
            "confidence": match["confidence"],
            "requires_clarification": False,
        }

    return {
        "matched": False,
        "node_id": None,
        "confidence": match["confidence"],
        "requires_clarification": True,
        "reason": "No sufficiently confident semantic match found",
    }
