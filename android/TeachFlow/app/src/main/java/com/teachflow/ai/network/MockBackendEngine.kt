package com.teachflow.ai.network

import android.content.Context
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
        // Pre-populate default ORDER_FOOD workflow
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

    fun initPersistence(context: Context) {
        val loaded = WorkflowStore.loadWorkflows(context)
        if (loaded.isNotEmpty()) {
            loaded.forEach { savedWorkflows[it.intent] = it }
            _workflowsFlow.value = savedWorkflows.values.toList()
        }
    }

    fun learnWorkflow(context: Context?, prompt: String, capturedActions: List<CapturedAction>): Workflow {
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

        if (context != null) {
            WorkflowStore.saveWorkflows(context, savedWorkflows.values.toList())
        }

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

    fun extractIntentFromPrompt(prompt: String): String {
        val lower = prompt.lowercase()
        return when {
            lower.contains("order") || lower.contains("get") || lower.contains("want") || lower.contains("buy") || lower.contains("bring") -> "ORDER_FOOD"
            lower.contains("book") || lower.contains("ride") || lower.contains("cab") -> "BOOK_RIDE"
            lower.contains("send") || lower.contains("message") -> "SEND_MESSAGE"
            else -> "ORDER_FOOD"
        }
    }

    fun extractParametersFromPrompt(prompt: String): Map<String, String> {
        val params = mutableMapOf<String, String>()

        // 1. Extract quantity (supports digits and English word numbers)
        val numberRegex = Regex("\\b\\d+\\b")
        val matchNumber = numberRegex.find(prompt)
        if (matchNumber != null) {
            params["quantity"] = matchNumber.value
        } else {
            val lower = prompt.lowercase()
            when {
                lower.contains("one") || lower.contains("a ") || lower.contains("an ") -> params["quantity"] = "1"
                lower.contains("two") -> params["quantity"] = "2"
                lower.contains("three") -> params["quantity"] = "3"
                lower.contains("four") -> params["quantity"] = "4"
                lower.contains("five") -> params["quantity"] = "5"
                else -> params["quantity"] = "1"
            }
        }

        // 2. Extract item
        val lowerPrompt = prompt.lowercase()
        when {
            lowerPrompt.contains("pizza") -> params["item"] = "pizza"
            lowerPrompt.contains("burger") -> params["item"] = "burger"
            lowerPrompt.contains("taco") -> params["item"] = "taco"
            lowerPrompt.contains("sushi") -> params["item"] = "sushi"
            lowerPrompt.contains("coffee") -> params["item"] = "coffee"
            else -> {
                val words = prompt.split(" ").filter { it.isNotBlank() }
                params["item"] = words.lastOrNull()?.trim('.', ',', '!') ?: "burger"
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
