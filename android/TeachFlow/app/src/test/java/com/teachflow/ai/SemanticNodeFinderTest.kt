package com.teachflow.ai

import com.teachflow.ai.executor.SemanticNodeFinder
import com.teachflow.ai.model.TargetSpec
import com.teachflow.ai.model.UIElement
import org.junit.Assert.*
import org.junit.Test

class SemanticNodeFinderTest {

    @Test
    fun testExactResourceIdMatch() {
        val target = TargetSpec(
            role = "button",
            resourceId = "add_cart"
        )

        val element = UIElement(
            id = "node_1",
            role = "button",
            resourceId = "com.app:id/add_cart"
        )

        val matchScore = SemanticNodeFinder.scoreElementMatch(element, target)
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
        val elementV2 = UIElement(
            id = "node_2",
            role = "button",
            text = "Add",
            resourceId = "add_cart_btn"
        )

        val normalizedScore = SemanticNodeFinder.scoreElementMatch(elementV2, targetV1)

        assertTrue("Normalized text match should succeed", normalizedScore.score >= 0.75f)
        assertTrue(normalizedScore.reason.contains("Normalized text match"))
    }

    @Test
    fun testNoCoordinateDependency() {
        val target = TargetSpec(
            role = "edit_text",
            text = "Search"
        )

        val element = UIElement(
            id = "node_3",
            role = "edit_text",
            text = "Search",
            resourceId = "search_box"
        )

        val score = SemanticNodeFinder.scoreElementMatch(element, target)

        assertEquals(0.95f, score.score, 0.06f)
    }
}
