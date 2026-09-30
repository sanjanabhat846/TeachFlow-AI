package com.teachflow.ai.network

import com.teachflow.ai.model.CapturedAction
import com.teachflow.ai.model.ExecutionResult
import com.teachflow.ai.model.TargetSpec
import com.teachflow.ai.model.UITree
import com.teachflow.ai.model.Workflow
import com.teachflow.ai.model.WorkflowStep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

@Serializable
data class UiMatchReply(
    val matched: Boolean,
    @SerialName("node_id") val nodeId: String? = null,
    val confidence: Float = 0f
)

@Serializable
data class UiRecoveryReply(
    val matched: Boolean,
    @SerialName("node_id") val nodeId: String? = null,
    val confidence: Float = 0f,
    @SerialName("requires_clarification") val requiresClarification: Boolean,
    val reason: String? = null
)

@Serializable
data class SafetyCheckReply(
    @SerialName("requires_approval") val requiresApproval: Boolean,
    val reason: String? = null
)

object TeachFlowApiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun sendIntent(userPrompt: String): Result<Pair<Workflow, Map<String, String>>> =
        withContext(Dispatchers.IO) {
            if (NetworkConfig.useMockBackend.value) {
                val (workflow, parameters) = MockBackendEngine.matchAndGeneratePlan(userPrompt)
                return@withContext Result.success(workflow to parameters)
            }

            runCatching {
                val intent = json.decodeFromString<IntentReply>(
                    post("/intent/classify", json.encodeToString(TextRequest(userPrompt)))
                ).intent
                val parameters = json.decodeFromString<ParameterReply>(
                    post(
                        "/intent/parameters",
                        json.encodeToString(TextRequest(userPrompt, intent))
                    )
                ).parameters
                val match = json.decodeFromString<FlowMatchReply>(
                    post("/flows/match", json.encodeToString(TextRequest(userPrompt)))
                )
                if (match.flowId == "unknown") {
                    throw IOException("No learned workflow matches this command")
                }

                val flowId = URLEncoder.encode(match.flowId, Charsets.UTF_8.name())
                val workflow = json.decodeFromString<BackendWorkflow>(get("/flows/$flowId"))
                val plan = json.decodeFromString<ExecutionPlanReply>(
                    post(
                        "/execution/plan",
                        json.encodeToString(ExecutionPlanRequest(intent, parameters, match.flowId))
                    )
                )
                val parameterValues = parameters.mapValues { (_, value) -> value.asText() }
                val executableWorkflow = Workflow(
                    flowId = workflow.flowId,
                    intent = workflow.intent,
                    description = "Backend workflow (${match.confidence} match confidence)",
                    parameters = parameterValues,
                    steps = plan.actions.mapIndexed { index, action ->
                        WorkflowStep(
                            stepIndex = index + 1,
                            action = action.action,
                            target = action.target,
                            value = action.value?.asText()
                        )
                    }
                )
                executableWorkflow to parameterValues
            }
        }

    suspend fun sendDemonstration(
        prompt: String,
        capturedActions: List<CapturedAction>
    ): Result<Workflow> = withContext(Dispatchers.IO) {
        if (NetworkConfig.useMockBackend.value) {
            return@withContext runCatching {
                MockBackendEngine.learnWorkflow(prompt, capturedActions)
            }
        }

        runCatching {
            val intent = json.decodeFromString<IntentReply>(
                post("/intent/classify", json.encodeToString(TextRequest(prompt)))
            ).intent
            val parameters = json.decodeFromString<ParameterReply>(
                post("/intent/parameters", json.encodeToString(TextRequest(prompt, intent)))
            ).parameters
            val demonstration = buildList {
                add(prompt)
                capturedActions.forEach { action ->
                    val description = when (action.action.lowercase()) {
                        "set_quantity" -> "Quantity ${action.value.orEmpty()}"
                        "type", "search" -> action.value
                        else -> action.target?.text ?: action.target?.contentDescription
                    }
                    description?.takeIf(String::isNotBlank)?.let(::add)
                }
            }
            val synthesized = json.decodeFromString<BackendWorkflow>(
                post(
                    "/flows/synthesize",
                    json.encodeToString(SynthesisRequest(demonstration))
                )
            )

            if (capturedActions.isNotEmpty()) {
                val refinedSteps = synthesized.steps.map { step ->
                    val capturedTarget = when (step.action) {
                        "search" -> capturedActions.firstOrNull {
                            it.action.equals("type", ignoreCase = true) ||
                                it.action.equals("search", ignoreCase = true)
                        }
                        "select" -> capturedActions.firstOrNull {
                            it.action.equals("tap", ignoreCase = true) &&
                                isDemonstratedItemTarget(
                                    it.target,
                                    parameters["item"]?.asText()
                                )
                        }
                        "set_quantity" -> capturedActions.lastOrNull {
                            it.action.equals("set_quantity", ignoreCase = true)
                        }
                        "tap" -> capturedActions.firstOrNull {
                            it.action.equals("tap", ignoreCase = true) &&
                                it.target?.text?.contains("cart", ignoreCase = true) == true
                        }
                        else -> null
                    }?.target

                    val parameterizedTarget = if (step.action == "select") {
                        parameterizeDemonstratedTarget(capturedTarget)
                    } else {
                        capturedTarget
                    }

                    LearnStep(
                        action = step.action,
                        target = parameterizedTarget ?: step.target,
                        value = when (step.action) {
                            "search" -> JsonPrimitive("{{item}}")
                            "set_quantity" -> JsonPrimitive("{{quantity}}")
                            else -> step.value
                        }
                    )
                }
                val safetySteps = capturedActions.filter(::isSensitiveCapture).map { action ->
                    LearnStep(
                        action = action.action,
                        target = action.target,
                        value = action.value?.let(::JsonPrimitive)
                    )
                }
                post(
                    "/flows/learn",
                    json.encodeToString(
                        LearnWorkflowRequest(
                            flowId = synthesized.flowId,
                            intent = intent,
                            parameters = parameters.keys.toList(),
                            steps = refinedSteps + safetySteps
                        )
                    )
                )
            }

            val parameterValues = parameters.mapValues { (_, value) -> value.asText() }
            Workflow(
                flowId = synthesized.flowId,
                intent = intent,
                description = "Synthesized from Android demonstration",
                parameters = parameterValues,
                steps = synthesized.steps.mapIndexed { index, step ->
                    WorkflowStep(
                        stepIndex = index + 1,
                        action = step.action,
                        target = step.target,
                        value = step.value?.asText()
                    )
                }
            )
        }
    }

    suspend fun matchUiTarget(target: TargetSpec, tree: UITree): Result<UiMatchReply> =
        withContext(Dispatchers.IO) {
            if (NetworkConfig.useMockBackend.value) {
                return@withContext Result.success(UiMatchReply(true, "mock", 1f))
            }
            runCatching {
                json.decodeFromString<UiMatchReply>(
                    post(
                        "/ui/match",
                        json.encodeToString(UiMatchRequest(target, tree))
                    )
                )
            }
        }

    suspend fun recoverUiTarget(target: TargetSpec, tree: UITree): Result<UiRecoveryReply> =
        withContext(Dispatchers.IO) {
            if (NetworkConfig.useMockBackend.value) {
                return@withContext Result.success(
                    UiRecoveryReply(true, "mock", 1f, false)
                )
            }
            runCatching {
                json.decodeFromString<UiRecoveryReply>(
                    post(
                        "/ui/recover",
                        json.encodeToString(UiMatchRequest(target, tree))
                    )
                )
            }
        }

    suspend fun checkSafety(step: WorkflowStep): Result<SafetyCheckReply> =
        withContext(Dispatchers.IO) {
            if (NetworkConfig.useMockBackend.value) {
                val sensitive = step.requiresApproval || isSensitiveAction(step)
                return@withContext Result.success(
                    SafetyCheckReply(sensitive, step.approvalReason)
                )
            }
            runCatching {
                json.decodeFromString<SafetyCheckReply>(
                    post(
                        "/safety/check",
                        json.encodeToString(
                            SafetyCheckRequest(
                                listOf(
                                    BackendAction(
                                        action = step.action,
                                        target = step.target,
                                        value = step.value?.let(::JsonPrimitive)
                                    )
                                )
                            )
                        )
                    )
                )
            }
        }

    suspend fun sendExecutionResult(result: ExecutionResult): Result<Unit> =
        withContext(Dispatchers.IO) {
            if (NetworkConfig.useMockBackend.value) {
                return@withContext Result.success(Unit)
            }
            runCatching {
                post(
                    "/execution/result",
                    json.encodeToString(
                        ExecutionResultRequest(
                            success = result.success,
                            step = result.step,
                            message = result.message,
                            error = result.error
                        )
                    )
                )
                Unit
            }
        }

    private fun isSensitiveAction(step: WorkflowStep): Boolean {
        val searchable = listOf(
            step.action,
            step.target?.text,
            step.target?.contentDescription,
            step.target?.resourceId,
            step.value
        ).filterNotNull().joinToString(" ")
        return SENSITIVE_PATTERN.containsMatchIn(searchable)
    }

    private fun isCartOrSensitiveTarget(target: TargetSpec?): Boolean {
        val text = listOf(target?.text, target?.contentDescription, target?.resourceId)
            .filterNotNull()
            .joinToString(" ")
        return CART_OR_SENSITIVE_PATTERN.containsMatchIn(text)
    }

    private fun isSensitiveCapture(action: CapturedAction): Boolean {
        val searchable = listOf(
            action.action,
            action.target?.text,
            action.target?.contentDescription,
            action.target?.resourceId,
            action.value
        ).filterNotNull().joinToString(" ")
        return SENSITIVE_PATTERN.containsMatchIn(searchable)
    }

    private fun post(path: String, payload: String): String {
        val request = Request.Builder()
            .url("${NetworkConfig.baseUrl.value}$path")
            .post(payload.toRequestBody(jsonMediaType))
            .build()
        return execute(request)
    }

    private fun get(path: String): String {
        val request = Request.Builder()
            .url("${NetworkConfig.baseUrl.value}$path")
            .get()
            .build()
        return execute(request)
    }

    private fun execute(request: Request): String = client.newCall(request).execute().use { response ->
        val body = response.body?.string().orEmpty()
        if (!response.isSuccessful) {
            throw IOException("HTTP ${response.code}: ${body.ifBlank { response.message }}")
        }
        body
    }

    private fun JsonElement.asText(): String =
        if (this == JsonNull) "" else jsonPrimitive.content

    private val SENSITIVE_PATTERN =
        Regex("\\b(checkout|pay(?:ment)?|otp|password|passcode|pin|authenticate|authentication|login|sign in|verification code)\\b", RegexOption.IGNORE_CASE)
    private val CART_OR_SENSITIVE_PATTERN =
        Regex("\\b(cart|checkout|pay|payment|otp|password|passcode|pin|login|sign in)\\b", RegexOption.IGNORE_CASE)

    @Serializable
    private data class TextRequest(val text: String, val intent: String? = null)

    @Serializable
    private data class IntentReply(val intent: String)

    @Serializable
    private data class ParameterReply(val parameters: Map<String, JsonElement>)

    @Serializable
    private data class FlowMatchReply(
        @SerialName("flow_id") val flowId: String,
        val confidence: Float
    )

    @Serializable
    private data class BackendWorkflow(
        @SerialName("flow_id") val flowId: String,
        val intent: String,
        val parameters: List<String> = emptyList(),
        val steps: List<BackendStep> = emptyList()
    )

    @Serializable
    private data class BackendStep(
        val action: String,
        val target: TargetSpec? = null,
        val value: JsonElement? = null
    )

    @Serializable
    private data class ExecutionPlanRequest(
        val intent: String,
        val parameters: Map<String, JsonElement>,
        @SerialName("flow_id") val flowId: String
    )

    @Serializable
    private data class ExecutionPlanReply(val actions: List<BackendStep>)

    @Serializable
    private data class SynthesisRequest(val demonstration: List<String>)

    @Serializable
    private data class LearnWorkflowRequest(
        @SerialName("flow_id") val flowId: String,
        val intent: String,
        val parameters: List<String>,
        val steps: List<LearnStep>
    )

    @Serializable
    private data class LearnStep(
        val action: String,
        val target: TargetSpec? = null,
        val value: JsonElement? = null
    )

    @Serializable
    private data class UiMatchRequest(val target: TargetSpec, @SerialName("ui_tree") val uiTree: UITree)

    @Serializable
    private data class SafetyCheckRequest(val actions: List<BackendAction>)

    @Serializable
    private data class BackendAction(
        val action: String,
        val target: TargetSpec? = null,
        val value: JsonElement? = null
    )

    @Serializable
    private data class ExecutionResultRequest(
        val success: Boolean,
        val step: Int,
        val message: String? = null,
        val error: String? = null
    )
}

internal fun parameterizeDemonstratedTarget(target: TargetSpec?): TargetSpec? {
    if (target == null) return null
    val demonstratedText = target.text ?: target.contentDescription
    if (demonstratedText.isNullOrBlank()) return target

    val parameterizedDescription = target.contentDescription?.replace(
        oldValue = demonstratedText,
        newValue = "{{item}}",
        ignoreCase = true
    )
    return target.copy(
        text = "{{item}}",
        contentDescription = parameterizedDescription
    )
}

internal fun isDemonstratedItemTarget(target: TargetSpec?, item: String?): Boolean {
    if (target == null || item.isNullOrBlank()) return false
    val targetText = listOf(target.text, target.contentDescription)
        .filterNotNull()
        .joinToString(" ")
    if (CART_OR_CHECKOUT_PATTERN.containsMatchIn(targetText)) return false
    return targetText.contains(item, ignoreCase = true)
}

private val CART_OR_CHECKOUT_PATTERN = Regex("\\b(cart|checkout|pay|payment)\\b", RegexOption.IGNORE_CASE)