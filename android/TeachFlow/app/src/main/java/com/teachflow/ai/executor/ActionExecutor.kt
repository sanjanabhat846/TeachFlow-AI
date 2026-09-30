package com.teachflow.ai.executor

import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.teachflow.ai.accessibility.TeachFlowAccessibilityService
import com.teachflow.ai.model.ExecutionResult
import com.teachflow.ai.model.WorkflowStep
import kotlinx.coroutines.delay

object ActionExecutor {

    private const val TAG = "ActionExecutor"

    private val SENSITIVE_KEYWORDS = listOf(
        "pay", "payment", "checkout", "buy", "order now",
        "confirm purchase", "authenticate", "otp", "password", "credit card",
        "login", "log in", "log-in", "sign in", "sign-in"
    )

    fun isSensitiveAction(step: WorkflowStep): Boolean {
        if (step.requiresApproval) return true

        val targetText = step.target?.text?.lowercase() ?: ""
        val targetRes = step.target?.resourceId?.lowercase() ?: ""
        val actionName = step.action.lowercase()

        return SENSITIVE_KEYWORDS.any { keyword ->
            targetText.contains(keyword) || targetRes.contains(keyword) || actionName.contains(keyword)
        }
    }

    suspend fun executeStep(
        flowId: String,
        step: WorkflowStep,
        parameters: Map<String, String>,
        approved: Boolean = false,
        activeRootNode: AccessibilityNodeInfo? = null
    ): ExecutionResult {
        // Substitute parameters into template string e.g. {{item}} -> "burger"
        val resolvedValue = resolveValue(step.value, parameters)
        val resolvedTarget = step.target?.copy(
            text = resolveValue(step.target.text, parameters)
        )

        val resolvedStep = step.copy(
            value = resolvedValue,
            target = resolvedTarget
        )

        // Human-in-the-loop check
        if (isSensitiveAction(resolvedStep) && !approved) {
            Log.w(TAG, "Step ${step.stepIndex} requires explicit human approval before execution.")
            return ExecutionResult(
                flowId = flowId,
                success = false,
                completedSteps = step.stepIndex - 1,
                totalSteps = step.stepIndex,
                step = step.stepIndex,
                message = "Execution paused for user approval",
                requiresApproval = true,
                approvalReason = step.approvalReason ?: "Sensitive Action (${step.action} on ${resolvedTarget?.text ?: "target"})",
                error = null
            )
        }

        // Get root window from AccessibilityService or test provider
        val rootNode = activeRootNode ?: TeachFlowAccessibilityService.getRootNode()
        if (rootNode == null) {
            return ExecutionResult(
                flowId = flowId,
                success = false,
                completedSteps = step.stepIndex - 1,
                totalSteps = step.stepIndex,
                step = step.stepIndex,
                message = "Accessibility root node unavailable",
                error = "Target not found: Root window unavailable"
            )
        }

        // Semantic target resolution
        var matchResult = SemanticNodeFinder.findBestMatch(rootNode, resolvedTarget)

        // Recovery attempt if target not immediately found
        if (matchResult.node == null) {
            delay(500) // Brief pause to allow UI transitions to settle
            val reReadRoot = TeachFlowAccessibilityService.getRootNode() ?: rootNode
            matchResult = SemanticNodeFinder.findBestMatch(reReadRoot, resolvedTarget, minConfidence = 0.6f)
        }

        val targetNode = matchResult.node
        if (targetNode == null) {
            return ExecutionResult(
                flowId = flowId,
                success = false,
                completedSteps = step.stepIndex - 1,
                totalSteps = step.stepIndex,
                step = step.stepIndex,
                message = "Failed to locate target element semantically",
                error = "Target not found"
            )
        }

        // Action execution dispatch
        val success = when (resolvedStep.action.lowercase()) {
            "tap", "click", "select" -> performClick(targetNode)
            "type", "search" -> performType(targetNode, resolvedStep.value ?: "")
            "scroll" -> performScroll(targetNode, resolvedStep.value ?: "down")
            "set_quantity" -> performSetQuantity(targetNode, resolvedStep.value ?: "1", rootNode)
            else -> performClick(targetNode)
        }

        return if (success) {
            ExecutionResult(
                flowId = flowId,
                success = true,
                completedSteps = step.stepIndex,
                totalSteps = step.stepIndex,
                step = step.stepIndex,
                message = "Executed ${resolvedStep.action} on target successfully (${matchResult.matchReason})"
            )
        } else {
            ExecutionResult(
                flowId = flowId,
                success = false,
                completedSteps = step.stepIndex - 1,
                totalSteps = step.stepIndex,
                step = step.stepIndex,
                message = "Action execution failed on target element",
                error = "Action execution failed"
            )
        }
    }

    private fun performClick(node: AccessibilityNodeInfo): Boolean {
        var current: AccessibilityNodeInfo? = node
        while (current != null) {
            if (current.isClickable) {
                return current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            current = current.parent
        }
        return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    private fun performType(node: AccessibilityNodeInfo, text: String): Boolean {
        val arguments = Bundle()
        arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        val result = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        if (!result) {
            // Fallback to click if set_text is not directly supported
            performClick(node)
        }
        return result
    }

    private fun performScroll(node: AccessibilityNodeInfo, direction: String): Boolean {
        val action = if (direction.lowercase() == "up") {
            AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        } else {
            AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
        }
        return node.performAction(action)
    }

    private fun performSetQuantity(
        node: AccessibilityNodeInfo,
        quantity: String,
        rootNode: AccessibilityNodeInfo
    ): Boolean {
        if (node.isEditable) return performType(node, quantity)

        val requestedQuantity = quantity.toIntOrNull() ?: return false
        val currentQuantity = findQuantityValue(node)?.toIntOrNull() ?: return false
        val difference = requestedQuantity - currentQuantity
        if (difference == 0) return true
        if (kotlin.math.abs(difference) > 50) return false

        val controlDescription = if (difference > 0) "increase quantity" else "decrease quantity"
        val control = findNode(rootNode) {
            it.contentDescription?.toString()?.contains(controlDescription, ignoreCase = true) == true
        } ?: return false

        repeat(kotlin.math.abs(difference)) {
            if (!control.isEnabled || !control.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                return false
            }
        }
        return true
    }

    private fun findQuantityValue(node: AccessibilityNodeInfo): String? {
        val nodeText = node.text?.toString().orEmpty()
        Regex("\\b\\d+\\b").find(nodeText)?.value?.let { return it }
        for (index in 0 until node.childCount) {
            val child = node.getChild(index) ?: continue
            findQuantityValue(child)?.let { return it }
        }
        return null
    }

    private fun findNode(
        node: AccessibilityNodeInfo?,
        predicate: (AccessibilityNodeInfo) -> Boolean
    ): AccessibilityNodeInfo? {
        if (node == null) return null
        if (predicate(node)) return node
        for (index in 0 until node.childCount) {
            findNode(node.getChild(index), predicate)?.let { return it }
        }
        return null
    }

    fun resolveValue(template: String?, parameters: Map<String, String>): String? {
        if (template == null) return null
        var resolvedStr: String = template
        parameters.forEach { (key, value) ->
            resolvedStr = resolvedStr.replace("{{$key}}", value)
                .replace("{$key}", value)
        }
        return resolvedStr
    }
}
