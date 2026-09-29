package com.teachflow.ai.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WorkflowStep(
    @SerialName("step_index")
    val stepIndex: Int = 1,
    @SerialName("action")
    val action: String, // tap, type, search, select, set_quantity, scroll
    @SerialName("target")
    val target: TargetSpec? = null,
    @SerialName("value")
    val value: String? = null,
    @SerialName("requires_approval")
    val requiresApproval: Boolean = false,
    @SerialName("approval_reason")
    val approvalReason: String? = null
)

@Serializable
data class Workflow(
    @SerialName("flow_id")
    val flowId: String,
    @SerialName("intent")
    val intent: String,
    @SerialName("description")
    val description: String = "",
    @SerialName("parameters")
    val parameters: Map<String, String> = emptyMap(),
    @SerialName("steps")
    val steps: List<WorkflowStep> = emptyList()
)
