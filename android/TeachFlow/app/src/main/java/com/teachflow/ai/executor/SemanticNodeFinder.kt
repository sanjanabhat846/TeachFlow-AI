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
        minConfidence: Float = 0.6f
    ): MatchResult {
        if (rootNode == null || target == null) {
            return MatchResult(null, null, 0f, "Root node or target is null")
        }

        val candidates = mutableListOf<Pair<AccessibilityNodeInfo, MatchScore>>()
        collectCandidates(rootNode, target, candidates)

        val rankedCandidates = candidates.sortedByDescending { it.second.score }
        val bestCandidate = rankedCandidates.firstOrNull()
        val ambiguous = bestCandidate != null && rankedCandidates.size > 1 &&
            bestCandidate.second.score - rankedCandidates[1].second.score < AMBIGUITY_MARGIN

        return if (bestCandidate != null && !ambiguous && bestCandidate.second.score >= minConfidence) {
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
                matchReason = when {
                    ambiguous -> "Multiple similarly matching UI elements are ambiguous"
                    else -> "No candidate exceeded confidence threshold $minConfidence"
                }
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
        val element = UIHierarchyReader.createUIElement(node, "temp_node")
        return scoreElementMatch(element, target)
    }

    fun scoreElementMatch(element: UIElement, target: TargetSpec): MatchScore {
        val nodeResId = element.resourceId?.lowercase() ?: ""
        val nodeText = element.text?.lowercase() ?: ""
        val nodeDesc = element.contentDescription?.lowercase() ?: ""
        val nodeRole = element.role.lowercase()

        val targetResId = target.resourceId?.lowercase() ?: ""
        val targetText = target.text?.lowercase() ?: ""
        val targetDesc = target.contentDescription?.lowercase() ?: ""
        val targetRole = target.role?.lowercase() ?: ""
        val targetClass = target.className?.lowercase() ?: ""

        if (!node.isEnabled) {
            return MatchScore(0f, "Disabled node")
        }
        if (target.clickable == true && !hasClickableAncestor(node)) {
            return MatchScore(0f, "Node is not clickable")
        }

        var score = 0f

        // Resource IDs are useful anchors but remain one of several semantic signals.
        if (targetResId.isNotBlank() && nodeResId.isNotBlank()) {
            score += when {
                nodeResId == targetResId -> 0.5f
                nodeResId.endsWith(targetResId) || targetResId.endsWith(nodeResId) -> 0.35f
                else -> 0f
            }
        }

        if (targetClass.isNotBlank() && nodeClass.lowercase() == targetClass) {
            score += 0.1f
        }

        if (targetRole.isNotBlank() && nodeRole == targetRole) {
            score += 0.2f
        }

        val textSimilarity = semanticSimilarity(targetText, nodeText)
        val descriptionSimilarity = semanticSimilarity(targetDesc, nodeDesc)
        val labelSimilarity = maxOf(textSimilarity, descriptionSimilarity)
        if (labelSimilarity > 0f) {
            score += 0.7f * labelSimilarity
        }

        val nodeContext = UIHierarchyReader.contextFor(node).lowercase()
        val contextSimilarity = semanticSimilarity(target.context.orEmpty().lowercase(), nodeContext)
        if (contextSimilarity > 0f) {
            score += 0.2f * contextSimilarity
        }

        if (target.clickable == true && hasClickableAncestor(node)) {
            score += 0.05f
        }

        val reason = when {
            textSimilarity == 1f -> "Exact text match"
            descriptionSimilarity == 1f -> "Exact content description match"
            targetResId.isNotBlank() && score >= 0.5f -> "Resource ID and semantic match"
            labelSimilarity > 0f && contextSimilarity > 0f -> "Label and hierarchy context match"
            labelSimilarity > 0f -> "Semantic label variation match"
            else -> "No match"
        }
        return MatchScore(score.coerceAtMost(1f), reason)
    }

    private fun semanticSimilarity(expected: String, actual: String): Float {
        val normalizedExpected = normalize(expected)
        val normalizedActual = normalize(actual)
        if (normalizedExpected.isBlank() || normalizedActual.isBlank()) return 0f
        if (normalizedExpected == normalizedActual) return 1f
        if (normalizedExpected.contains(normalizedActual) || normalizedActual.contains(normalizedExpected)) return 0.82f
        val expectedTokens = normalizedExpected.split(" ").toSet()
        val actualTokens = normalizedActual.split(" ").toSet()
        val overlap = expectedTokens.intersect(actualTokens).size
        if (overlap == 0) return 0f
        return 0.55f + 0.4f * (overlap.toFloat() / maxOf(expectedTokens.size, actualTokens.size))
    }

    private fun normalize(value: String): String =
        value.lowercase().replace(Regex("[^a-z0-9]+"), " ").trim()

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

    private const val AMBIGUITY_MARGIN = 0.08f
}
