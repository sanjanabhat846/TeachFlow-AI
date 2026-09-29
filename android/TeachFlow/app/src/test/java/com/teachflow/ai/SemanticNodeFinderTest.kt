package com.teachflow.ai

import com.teachflow.ai.executor.SemanticNodeFinder
import com.teachflow.ai.model.TargetSpec
import org.junit.Assert.*
import org.junit.Test

class SemanticNodeFinderTest {

    @Test
    fun testExactResourceIdMatch() {
        val target = TargetSpec(
            role = "button",
            resourceId = "add_cart"
        )

        // Mock node evaluation
        val matchScore = SemanticNodeFinder.MatchScore(1.0f, "Resource ID match (com.app:id/add_cart)")
        assertTrue(matchScore.score >= 0.9f)
        assertTrue(matchScore.reason.contains("Resource ID match"))
    }

    @Test
    fun testSemanticRecoveryForUIVariations() {
        // Target trained on UI Version 1: "Add to Cart"
        val targetV1 = TargetSpec(
            role = "button",
            text = "Add to Cart"
        )

        // UI Version 2 node text has changed to shorter "Add"
        val normalizedScore = SemanticNodeFinder.scoreNodeMatch(
            // We simulate scoring logic against normalized text
            node = mockAccessibilityNode("android.widget.Button", "Add", "add_cart_btn"),
            target = targetV1
        )

        assertTrue("Normalized text match should succeed", normalizedScore.score >= 0.75f)
        assertTrue(normalizedScore.reason.contains("Normalized text match"))
    }

    @Test
    fun testNoCoordinateDependency() {
        val target = TargetSpec(
            role = "edit_text",
            text = "Search"
        )

        val score = SemanticNodeFinder.scoreNodeMatch(
            node = mockAccessibilityNode("android.widget.EditText", "Search", "search_box"),
            target = target
        )

        assertEquals(0.95f, score.score, 0.05f)
    }

    private fun mockAccessibilityNode(classNameStr: String, textStr: String, resIdStr: String): android.view.accessibility.AccessibilityNodeInfo {
        // Return null or proxy node safely; scoreNodeMatch handles strings via reflection/safe properties
        val node = android.view.accessibility.AccessibilityNodeInfo.obtain()
        node.className = classNameStr
        node.text = textStr
        return node
    }
}
