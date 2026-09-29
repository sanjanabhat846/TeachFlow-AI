package com.teachflow.ai

import com.teachflow.ai.network.MockBackendEngine
import org.junit.Assert.*
import org.junit.Test

class MockBackendEngineTest {

    @Test
    fun testExtractParametersForThreeBurgers() {
        val prompt = "Get me 3 burgers"
        val params = MockBackendEngine.extractParametersFromPrompt(prompt)
        assertEquals("burger", params["item"])
        assertEquals("3", params["quantity"])
    }

    @Test
    fun testExtractParametersForTwoPizzas() {
        val prompt = "Order 2 pizzas"
        val params = MockBackendEngine.extractParametersFromPrompt(prompt)
        assertEquals("pizza", params["item"])
        assertEquals("2", params["quantity"])
    }

    @Test
    fun testMatchAndGeneratePlan() {
        val prompt = "Get me 3 burgers"
        val (workflow, params) = MockBackendEngine.matchAndGeneratePlan(prompt)

        assertEquals("ORDER_FOOD", workflow.intent)
        assertEquals("burger", params["item"])
        assertEquals("3", params["quantity"])
        assertTrue(workflow.steps.isNotEmpty())
    }
}
