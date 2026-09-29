package com.teachflow.ai

import com.teachflow.ai.model.CapturedAction
import com.teachflow.ai.model.ExecutionResult
import com.teachflow.ai.model.TargetSpec
import com.teachflow.ai.model.Workflow
import com.teachflow.ai.model.WorkflowStep
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class ActionModelSerializationTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testCapturedActionSerialization() {
        val action = CapturedAction(
            action = "tap",
            target = TargetSpec(role = "button", text = "Add to Cart", resourceId = "add_cart")
        )

        val jsonStr = json.encodeToString(action)
        assertTrue(jsonStr.contains("\"action\":\"tap\""))
        assertTrue(jsonStr.contains("\"text\":\"Add to Cart\""))

        val deserialized = json.decodeFromString<CapturedAction>(jsonStr)
        assertEquals("tap", deserialized.action)
        assertEquals("Add to Cart", deserialized.target?.text)
    }

    @Test
    fun testExecutionResultSerialization() {
        val result = ExecutionResult(
            flowId = "order_food",
            success = true,
            completedSteps = 4,
            totalSteps = 5,
            step = 4,
            message = "Product selected",
            requiresApproval = true,
            approvalReason = "Payment"
        )

        val jsonStr = json.encodeToString(result)
        assertTrue(jsonStr.contains("\"flow_id\":\"order_food\""))
        assertTrue(jsonStr.contains("\"requires_approval\":true"))

        val deserialized = json.decodeFromString<ExecutionResult>(jsonStr)
        assertTrue(deserialized.success)
        assertEquals(4, deserialized.completedSteps)
        assertEquals("Payment", deserialized.approvalReason)
    }

    @Test
    fun testWorkflowContractSerialization() {
        val workflow = Workflow(
            flowId = "order_food",
            intent = "ORDER_FOOD",
            parameters = mapOf("item" to "burger", "quantity" to "3"),
            steps = listOf(
                WorkflowStep(
                    stepIndex = 1,
                    action = "type",
                    target = TargetSpec(role = "edit_text"),
                    value = "{{item}}"
                )
            )
        )

        val jsonStr = json.encodeToString(workflow)
        assertTrue(jsonStr.contains("\"intent\":\"ORDER_FOOD\""))
        assertTrue(jsonStr.contains("\"{{item}}\""))

        val deserialized = json.decodeFromString<Workflow>(jsonStr)
        assertEquals("ORDER_FOOD", deserialized.intent)
        assertEquals("burger", deserialized.parameters["item"])
    }
}
