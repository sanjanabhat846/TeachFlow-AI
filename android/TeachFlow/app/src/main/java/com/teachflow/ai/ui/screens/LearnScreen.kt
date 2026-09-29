package com.teachflow.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.teachflow.ai.accessibility.TeachFlowAccessibilityService
import com.teachflow.ai.model.CapturedAction
import com.teachflow.ai.network.TeachFlowApiClient
import com.teachflow.ai.ui.components.DemoAppSimulator
import com.teachflow.ai.ui.theme.AccentPurple
import com.teachflow.ai.ui.theme.EmeraldGreen
import kotlinx.coroutines.launch

@Composable
fun LearnScreen() {
    var taskPrompt by remember { mutableStateOf("Order 2 pizzas") }
    var isRecording by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Waiting for demonstration...") }
    var learnedWorkflowSummary by remember { mutableStateOf<String?>(null) }
    val capturedActions by TeachFlowAccessibilityService.capturedActions.collectAsState()
    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Teach a New Task",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Demonstrate the workflow once. TeachFlow extracts node semantics and synthesizes a reusable template.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Tell me what you want to teach:",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = taskPrompt,
                        onValueChange = { taskPrompt = it },
                        placeholder = { Text("e.g. Order 2 pizzas") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (!isRecording) {
                        Button(
                            onClick = {
                                isRecording = true
                                statusText = "Recording live demonstration... Perform actions below."
                                learnedWorkflowSummary = null
                                TeachFlowAccessibilityService.startRecording()
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.FiberManualRecord, contentDescription = "Record", tint = androidx.compose.ui.graphics.Color.Red)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Start Demonstration", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    } else {
                        Button(
                            onClick = {
                                isRecording = false
                                val actions = TeachFlowAccessibilityService.stopRecording()
                                statusText = "Synthesizing parameterized workflow..."
                                scope.launch {
                                    val result = TeachFlowApiClient.sendDemonstration(taskPrompt, actions)
                                    result.onSuccess { workflow ->
                                        statusText = "Workflow learned successfully!"
                                        learnedWorkflowSummary = "Synthesized Workflow: ${workflow.intent}(${workflow.parameters.keys.joinToString(", ")})"
                                    }.onFailure { err ->
                                        statusText = "Synthesis warning: ${err.message}. Defaulting to mock synthesis."
                                        learnedWorkflowSummary = "Workflow learned: ORDER_FOOD(item, quantity)"
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = "Stop")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Stop Demonstration", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }
        }

        item {
            // Status Section
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Status:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = if (statusText.contains("successfully")) EmeraldGreen else MaterialTheme.colorScheme.primary
                        )
                    )
                    if (learnedWorkflowSummary != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = learnedWorkflowSummary!!,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = EmeraldGreen
                        )
                    }
                }
            }
        }

        item {
            // Embedded Interactive Demo Target App
            DemoAppSimulator(
                onSimulatedAction = { actionType, label, resId ->
                    if (isRecording) {
                        // Capture action in service memory
                    }
                }
            )
        }

        if (capturedActions.isNotEmpty()) {
            item {
                Text(
                    text = "Captured Actions (${capturedActions.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            items(capturedActions) { action ->
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = AccentPurple.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = action.action.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = AccentPurple,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Target: ${action.target?.text ?: action.target?.resourceId ?: action.target?.role ?: "UI Node"}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            if (action.value != null) {
                                Text(
                                    text = "Value: ${action.value}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
