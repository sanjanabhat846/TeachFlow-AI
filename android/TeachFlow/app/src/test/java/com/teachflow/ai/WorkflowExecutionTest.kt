package com.teachflow.ai

import com.teachflow.ai.executor.ActionExecutor
import com.teachflow.ai.model.TargetSpec
import com.teachflow.ai.model.WorkflowStep
import org.junit.Assert.*
import org.junit.Test

class WorkflowExecutionTest {

    @Test
    fun testParameterTemplateResolution() {
        val template = "{{item}}"
        val params = mapOf("item" to "burger", "quantity" to "3")
        val resolved = ActionExecutor.resolveValue(template, params)
        assertEquals("burger", resolved)
    }

    @Test
    fun testMultipleParametersResolution() {
        val template = "Ordering {{quantity}} x {{item}}"
        val params = mapOf("item" to "pizzas", "quantity" to "2")
        val resolved = ActionExecutor.resolveValue(template, params)
        assertEquals("Ordering 2 x pizzas", resolved)
    }

    @Test
    fun testSensitiveActionDetectionForPayment() {
        val sensitiveStep = WorkflowStep(
            stepIndex = 5,
            action = "tap",
            target = TargetSpec(text = "Checkout & Pay"),
            requiresApproval = true
        )

        assertTrue(ActionExecutor.isSensitiveAction(sensitiveStep))
    }

    @Test
    fun testSensitiveActionDetectionByKeyword() {
        val keywordStep = WorkflowStep(
            stepIndex = 4,
            action = "tap",
            target = TargetSpec(text = "Confirm Order & Pay Now"),
            requiresApproval = false
        )

        assertTrue(ActionExecutor.isSensitiveAction(keywordStep))
    }

    @Test
    fun testNonSensitiveAction() {
        val normalStep = WorkflowStep(
            stepIndex = 1,
            action = "type",
            target = TargetSpec(role = "edit_text", text = "Search"),
            requiresApproval = false
        )

        assertFalse(ActionExecutor.isSensitiveAction(normalStep))
    }
}
