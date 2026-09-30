package com.teachflow.ai.executor

import android.view.accessibility.AccessibilityNodeInfo
import com.teachflow.ai.accessibility.UIHierarchyReader
import com.teachflow.ai.model.TargetSpec
import com.teachflow.ai.model.UIElement

data class MatchResult(
    val node: AccessibilityNodeInfo?,
    val element: UIElement?,
    val confidence: Float,
    val matchReason: String
)

object SemanticNodeFinder {

    fun findBestMatch(
        rootNode: AccessibilityNodeInfo?,
        target: TargetSpec?,
        minConfidence: Float = 0.5f
    ): MatchResult {
        if (rootNode == null || target == null) {
            return MatchResult(null, null, 0f, "Root node or target is null")
        }

        val candidates = mutableListOf<Pair<AccessibilityNodeInfo, MatchScore>>()
        collectCandidates(rootNode, target, candidates)

        val bestCandidate = candidates.maxByOrNull { it.second.score }

        return if (bestCandidate != null && bestCandidate.second.score >= minConfidence) {
            val element = UIHierarchyReader.createUIElement(bestCandidate.first, "matched_node")
            MatchResult(
                node = bestCandidate.first,
                element = element,
                confidence = bestCandidate.second.score,
                matchReason = bestCandidate.second.reason
            )
        } else {
            MatchResult(
                node = null,
                element = null,
                confidence = bestCandidate?.second?.score ?: 0f,
                matchReason = "No candidate exceeded confidence threshold $minConfidence"
            )
        }
    }

    private fun collectCandidates(
        node: AccessibilityNodeInfo?,
        target: TargetSpec,
        candidates: MutableList<Pair<AccessibilityNodeInfo, MatchScore>>
    ) {
        if (node == null) return

        try {
            val score = scoreNodeMatch(node, target)
            if (score.score > 0f) {
                candidates.add(node to score)
            }

            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                collectCandidates(child, target, candidates)
            }
        } catch (e: Exception) {
            // Guard against node hierarchy mutation
        }
    }

    fun scoreNodeMatch(node: AccessibilityNodeInfo, target: TargetSpec): MatchScore {
        val nodeResId = node.viewIdResourceName?.lowercase() ?: ""
        val nodeText = node.text?.toString()?.lowercase() ?: ""
        val nodeDesc = node.contentDescription?.toString()?.lowercase() ?: ""
        val nodeClass = node.className?.toString() ?: ""
        val nodeRole = UIHierarchyReader.mapClassToRole(nodeClass, nodeText, nodeResId, node.isEditable)

        val targetResId = target.resourceId?.lowercase() ?: ""
        val targetText = target.text?.lowercase() ?: ""
        val targetDesc = target.contentDescription?.lowercase() ?: ""
        val targetRole = target.role?.lowercase() ?: ""
        val targetClass = target.className?.lowercase() ?: ""

        if (target.enabled == true && !node.isEnabled) {
            return MatchScore(0f, "Disabled node")
        }
        if (target.clickable == true && !hasClickableAncestor(node)) {
            return MatchScore(0f, "Node is not clickable")
        }

        // 1. Resource ID exact or endsWith match
        if (targetResId.isNotBlank() && nodeResId.isNotBlank()) {
            if (nodeResId == targetResId || nodeResId.endsWith(targetResId) || targetResId.endsWith(nodeResId)) {
                return MatchScore(1.0f, "Resource ID match ($nodeResId)")
            }
        }

        if (targetClass.isNotBlank() && nodeClass.lowercase() == targetClass &&
            targetText.isBlank() && targetDesc.isBlank() && targetResId.isBlank()
        ) {
            return MatchScore(0.70f, "Accessibility class match ($nodeClass)")
        }

        // 2. Exact text match
        if (targetText.isNotBlank() && nodeText == targetText) {
            val roleBonus = if (targetRole.isBlank() || nodeRole == targetRole) 0.05f else 0f
            return MatchScore(0.95f + roleBonus, "Exact text match ($nodeText)")
        }

        // 3. Exact content description match
        if (targetDesc.isNotBlank() && nodeDesc == targetDesc) {
            return MatchScore(0.90f, "Exact content description match ($nodeDesc)")
        }

        // 4. Normalized / Substring text match (handles UI text variants e.g. "Add to Cart" vs "Add")
        if (targetText.isNotBlank() && nodeText.isNotBlank()) {
            if (nodeText.contains(targetText) || targetText.contains(nodeText)) {
                return MatchScore(0.82f, "Normalized text match ($nodeText ~ $targetText)")
            }
        }

        // 5. Content description substring match
        if (targetDesc.isNotBlank() && nodeDesc.isNotBlank()) {
            if (nodeDesc.contains(targetDesc) || targetDesc.contains(nodeDesc)) {
                return MatchScore(0.78f, "Content description substring match")
            }
        }

        // 6. Role match + partial text match
        if (targetRole.isNotBlank() && nodeRole == targetRole) {
            if (targetText.isBlank() && targetDesc.isBlank() && targetResId.isBlank()) {
                return MatchScore(0.60f, "Role only match ($nodeRole)")
            }
            if (targetText.isNotBlank() && (nodeText.contains(targetText) || targetText.contains(nodeText))) {
                return MatchScore(0.85f, "Role + partial text match")
            }
        }

        return MatchScore(0.0f, "No match")
    }

    private fun hasClickableAncestor(node: AccessibilityNodeInfo): Boolean {
        var current: AccessibilityNodeInfo? = node
        while (current != null) {
            if (current.isClickable && current.isEnabled) return true
            current = current.parent
        }
        return false
    }

    data class MatchScore(
        val score: Float,
        val reason: String
    )
}
