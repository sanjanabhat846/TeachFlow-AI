"""Deterministic semantic UI matching for the TeachFlow AI MVP."""

from __future__ import annotations

import re
from difflib import SequenceMatcher

SAFE_MATCH_THRESHOLD = 0.6
AMBIGUITY_MARGIN = 0.08


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


def _semantic_similarity(first: str, second: str) -> float:
    """Compare semantic labels while keeping weak lexical resemblance conservative."""
    first = _normalize(first)
    second = _normalize(second)
    if not first or not second:
        return 0.0
    if first == second:
        return 1.0
    if first in second or second in first:
        return 0.7

    first_tokens = set(first.split())
    second_tokens = set(second.split())
    overlap = len(first_tokens & second_tokens)
    if overlap:
        coverage = overlap / max(len(first_tokens), len(second_tokens))
        return 0.55 + 0.4 * coverage

    return 0.5 * _fuzzy_similarity(first, second)


def _score_candidate(target: dict, element: dict) -> float:
    """Score a candidate element against the target semantic description."""
    target_role = str(target.get("role", "")).lower()
    target_text = _normalize(str(target.get("text", "")))
    target_description = _normalize(str(target.get("content_description", "")))
    target_resource = str(target.get("resource_id", "")).lower()
    target_class = str(target.get("class_name", "")).lower()
    target_context = str(target.get("context", ""))

    role = str(element.get("role", "")).lower()
    text = _normalize(str(element.get("text", "")))
    description = _normalize(str(element.get("content_description", "")))
    resource = str(element.get("resource_id", "")).lower()
    class_name = str(element.get("class_name", "")).lower()
    context = str(element.get("context", ""))

    if not element.get("enabled", True):
        return 0.0
    if target.get("clickable") is True and not element.get("clickable", False):
        return 0.0

    score = 0.0

    if target_role and role:
        if role == target_role:
            score += 0.3
        elif target_role in role or role in target_role:
            score += 0.15
        else:
            role_labels = [description, context]
            score += 0.15 * max(
                (_semantic_similarity(target_role, label) for label in role_labels),
                default=0.0,
            )

    target_labels = [label for label in (target_text, target_description) if label]
    element_labels = [label for label in (text, description) if label]
    if target_labels and element_labels:
        score += 0.5 * max(
            _semantic_similarity(expected, actual)
            for expected in target_labels
            for actual in element_labels
        )

    if target_resource and resource:
        score += 0.15 * _semantic_similarity(target_resource, resource)

    if target_class and class_name:
        score += 0.1 * _semantic_similarity(target_class, class_name)

    if target_context and context:
        score += 0.25 * _semantic_similarity(target_context, context)

    if target.get("clickable") is True and element.get("clickable", False):
        score += 0.05

    return min(score, 1.0)


def _best_match(target: dict, ui_tree: dict) -> tuple[dict | None, float, bool]:
    if not isinstance(ui_tree, dict):
        return None, 0.0, False

    elements = ui_tree.get("elements", [])
    if not isinstance(elements, list) or not elements:
        return None, 0.0, False

    candidates = [
        (element, _score_candidate(target, element))
        for element in elements
        if isinstance(element, dict)
    ]
    candidates.sort(key=lambda candidate: candidate[1], reverse=True)
    if not candidates:
        return None, 0.0, False

    best_element, best_score = candidates[0]
    ambiguous = (
        len(candidates) > 1
        and best_score >= SAFE_MATCH_THRESHOLD
        and best_score - candidates[1][1] < AMBIGUITY_MARGIN
    )
    if best_score < SAFE_MATCH_THRESHOLD or ambiguous:
        return None, best_score, ambiguous
    return best_element, best_score, False


def match_ui_element(target: dict, ui_tree: dict) -> dict:
    """Find a UI element matching a semantic target using role/text/content metadata.

    Security principle: accept only a candidate with confident semantic similarity.
    """
    best_match, best_score, _ = _best_match(target, ui_tree)
    if best_match is not None:
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
    best_match, best_score, ambiguous = _best_match(target, ui_tree)
    match = {
        "matched": best_match is not None,
        "node_id": best_match.get("id") if best_match is not None else None,
        "confidence": round(best_score, 2),
    }
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
        "reason": (
            "Multiple similarly matching UI elements are ambiguous"
            if ambiguous
            else "No sufficiently confident semantic match found"
        ),
    }
