package com.teachflow.ai.network

import com.teachflow.ai.model.CapturedAction
import com.teachflow.ai.model.ExecutionResult
import com.teachflow.ai.model.UITree
import com.teachflow.ai.model.Workflow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

object TeachFlowApiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    suspend fun sendIntent(userPrompt: String): Result<Pair<Workflow, Map<String, String>>> = withContext(Dispatchers.IO) {
        if (NetworkConfig.useMockBackend.value) {
            val (workflow, params) = MockBackendEngine.matchAndGeneratePlan(userPrompt)
            return@withContext Result.success(workflow to params)
        }

        try {
            val url = "${NetworkConfig.baseUrl.value}/api/v1/intent"
            val payload = json.encodeToString(mapOf("prompt" to userPrompt))
            val request = Request.Builder()
                .url(url)
                .post(payload.toRequestBody(JSON_MEDIA_TYPE))
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val workflow = json.decodeFromString<Workflow>(body)
                    Result.success(workflow to workflow.parameters)
                } else {
                    Result.failure(Exception("HTTP error ${response.code}: ${response.message}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendDemonstration(prompt: String, capturedActions: List<CapturedAction>): Result<Workflow> = withContext(Dispatchers.IO) {
        if (NetworkConfig.useMockBackend.value) {
            val workflow = MockBackendEngine.learnWorkflow(context = null, prompt = prompt, capturedActions = capturedActions)
            return@withContext Result.success(workflow)
        }

        try {
            val url = "${NetworkConfig.baseUrl.value}/api/v1/workflows/learn"
            val payloadMap = mapOf(
                "prompt" to prompt,
                "actions" to json.encodeToString(capturedActions)
            )
            val request = Request.Builder()
                .url(url)
                .post(json.encodeToString(payloadMap).toRequestBody(JSON_MEDIA_TYPE))
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val workflow = json.decodeFromString<Workflow>(body)
                    Result.success(workflow)
                } else {
                    Result.failure(Exception("HTTP error ${response.code}: ${response.message}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendUITree(tree: UITree): Result<Unit> = withContext(Dispatchers.IO) {
        if (NetworkConfig.useMockBackend.value) {
            return@withContext Result.success(Unit)
        }

        try {
            val url = "${NetworkConfig.baseUrl.value}/api/v1/ui/tree"
            val request = Request.Builder()
                .url(url)
                .post(json.encodeToString(tree).toRequestBody(JSON_MEDIA_TYPE))
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) Result.success(Unit)
                else Result.failure(Exception("HTTP error ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendExecutionResult(result: ExecutionResult): Result<Unit> = withContext(Dispatchers.IO) {
        if (NetworkConfig.useMockBackend.value) {
            return@withContext Result.success(Unit)
        }

        try {
            val url = "${NetworkConfig.baseUrl.value}/api/v1/execution/status"
            val request = Request.Builder()
                .url(url)
                .post(json.encodeToString(result).toRequestBody(JSON_MEDIA_TYPE))
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) Result.success(Unit)
                else Result.failure(Exception("HTTP error ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
