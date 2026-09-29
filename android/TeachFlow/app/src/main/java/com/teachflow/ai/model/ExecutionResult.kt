package com.teachflow.ai.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExecutionResult(
    @SerialName("flow_id")
    val flowId: String,
    @SerialName("success")
    val success: Boolean,
    @SerialName("completed_steps")
    val completedSteps: Int,
    @SerialName("total_steps")
    val totalSteps: Int,
    @SerialName("step")
    val step: Int = completedSteps,
    @SerialName("message")
    val message: String = "",
    @SerialName("requires_approval")
    val requiresApproval: Boolean = false,
    @SerialName("approval_reason")
    val approvalReason: String? = null,
    @SerialName("error")
    val error: String? = null
)
