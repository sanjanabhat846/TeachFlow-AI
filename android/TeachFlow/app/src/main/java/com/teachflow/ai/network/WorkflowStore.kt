package com.teachflow.ai.network

import android.content.Context
import com.teachflow.ai.model.Workflow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

object WorkflowStore {

    private const val FILE_NAME = "saved_workflows.json"
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    fun saveWorkflows(context: Context, workflows: List<Workflow>) {
        try {
            val file = File(context.filesDir, FILE_NAME)
            val content = json.encodeToString(workflows)
            file.writeText(content)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadWorkflows(context: Context): List<Workflow> {
        return try {
            val file = File(context.filesDir, FILE_NAME)
            if (file.exists()) {
                val content = file.readText()
                json.decodeFromString<List<Workflow>>(content)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
