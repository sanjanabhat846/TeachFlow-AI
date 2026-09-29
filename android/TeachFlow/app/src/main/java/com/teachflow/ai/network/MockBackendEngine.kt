package com.teachflow.ai.network

import com.teachflow.ai.model.CapturedAction
import com.teachflow.ai.model.TargetSpec
import com.teachflow.ai.model.Workflow
import com.teachflow.ai.model.WorkflowStep
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object MockBackendEngine {

    private val savedWorkflows = mutableMapOf<String, Workflow>()

    private val _workflowsFlow = MutableStateFlow<List<Workflow>>(emptyList())
    val workflowsFlow: StateFlow<List<Workflow>> = _workflowsFlow

    init {
        // Pre-populate with default ORDER_FOOD workflow
        val defaultOrderFood = Workflow(
            flowId = "order_food",
            intent = "ORDER_FOOD",
            description = "Searches for product item, selects item, adjusts quantity, adds to cart, and triggers checkout",
            parameters = mapOf("item" to "burger", "quantity" to "3"),
            steps = listOf(
                WorkflowStep(
                    stepIndex = 1,
                    action = "type",
                    target = TargetSpec(role = "edit_text", resourceId = "search_box", text = "Search"),
                    value = "{{item}}",
                    requiresApproval = false
                ),
                WorkflowStep(
                    stepIndex = 2,
                    action = "tap",
                    target = TargetSpec(role = "button", text = "{{item}}"),
                    requiresApproval = false
                ),
                WorkflowStep(
                    stepIndex = 3,
                    action = "set_quantity",
                    target = TargetSpec(role = "quantity_picker", resourceId = "quantity_picker"),
                    value = "{{quantity}}",
                    requiresApproval = false
                ),
                WorkflowStep(
                    stepIndex = 4,
                    action = "tap",
                    target = TargetSpec(role = "button", text = "Add to Cart", resourceId = "add_cart"),
                    requiresApproval = false
                ),
                WorkflowStep(
                    stepIndex = 5,
                    action = "tap",
                    target = TargetSpec(role = "button", text = "Checkout", resourceId = "checkout_button"),
                    requiresApproval = true,
                    approvalReason = "Payment / Sensitive Checkout Checkpoint"
                )
            )
        )
        savedWorkflows[defaultOrderFood.intent] = defaultOrderFood
        _workflowsFlow.value = savedWorkflows.values.toList()
    }

    fun learnWorkflow(prompt: String, capturedActions: List<CapturedAction>): Workflow {
        val extractedIntent = extractIntentFromPrompt(prompt)
        val defaultParams = extractParametersFromPrompt(prompt)

        val steps = if (capturedActions.isNotEmpty()) {
            capturedActions.mapIndexed { index, action ->
                WorkflowStep(
                    stepIndex = index + 1,
                    action = action.action,
                    target = action.target,
                    value = action.value ?: if (action.action == "set_quantity") "{{quantity}}" else if (action.action == "type") "{{item}}" else null,
                    requiresApproval = isActionSensitive(action),
                    approvalReason = if (isActionSensitive(action)) "Payment / Sensitive Checkpoint" else null
                )
            }
        } else {
            savedWorkflows["ORDER_FOOD"]?.steps ?: emptyList()
        }

        val synthesized = Workflow(
            flowId = extractedIntent.lowercase(),
            intent = extractedIntent,
            description = "Synthesized flow for '$prompt'",
            parameters = defaultParams,
            steps = steps
        )

        savedWorkflows[extractedIntent] = synthesized
        _workflowsFlow.value = savedWorkflows.values.toList()
        return synthesized
    }

    fun matchAndGeneratePlan(userVoiceCommand: String): Pair<Workflow, Map<String, String>> {
        val params = extractParametersFromPrompt(userVoiceCommand)
        val matchedWorkflow = savedWorkflows["ORDER_FOOD"] ?: savedWorkflows.values.firstOrNull() ?: Workflow(
            flowId = "order_food",
            intent = "ORDER_FOOD",
            description = "Fallback food ordering flow",
            parameters = params,
            steps = emptyList()
        )

        val updatedParams = matchedWorkflow.parameters.toMutableMap()
        updatedParams.putAll(params)

        return matchedWorkflow to updatedParams
    }

    private fun extractIntentFromPrompt(prompt: String): String {
        val lower = prompt.lowercase()
        return when {
            lower.contains("order") || lower.contains("get") || lower.contains("buy") -> "ORDER_FOOD"
            lower.contains("book") || lower.contains("ride") || lower.contains("cab") -> "BOOK_RIDE"
            lower.contains("send") || lower.contains("message") -> "SEND_MESSAGE"
            else -> "CUSTOM_WORKFLOW"
        }
    }

    fun extractParametersFromPrompt(prompt: String): Map<String, String> {
        val params = mutableMapOf<String, String>()
        val words = prompt.split(" ")

        // Extract quantity (number)
        val numberRegex = Regex("\\b\\d+\\b")
        val matchNumber = numberRegex.find(prompt)
        if (matchNumber != null) {
            params["quantity"] = matchNumber.value
        } else if (prompt.contains("two", ignoreCase = true)) {
            params["quantity"] = "2"
        } else if (prompt.contains("three", ignoreCase = true)) {
            params["quantity"] = "3"
        } else if (prompt.contains("one", ignoreCase = true)) {
            params["quantity"] = "1"
        } else {
            params["quantity"] = "1"
        }

        // Extract item (food / target object)
        val lower = prompt.lowercase()
        when {
            lower.contains("pizza") || lower.contains("pizzas") -> params["item"] = "pizza"
            lower.contains("burger") || lower.contains("burgers") -> params["item"] = "burger"
            lower.contains("taco") || lower.contains("tacos") -> params["item"] = "taco"
            lower.contains("sushi") -> params["item"] = "sushi"
            lower.contains("coffee") -> params["item"] = "coffee"
            else -> {
                // Heuristic fallback: word after quantity or second word
                val cleanWords = words.filter { it.isNotBlank() }
                if (cleanWords.size > 1) {
                    params["item"] = cleanWords.last().trim('.', ',', '!')
                } else {
                    params["item"] = "item"
                }
            }
        }

        return params
    }

    private fun isActionSensitive(action: CapturedAction): Boolean {
        val text = action.target?.text?.lowercase() ?: ""
        val res = action.target?.resourceId?.lowercase() ?: ""
        return text.contains("pay") || text.contains("checkout") || res.contains("checkout")
    }
}
