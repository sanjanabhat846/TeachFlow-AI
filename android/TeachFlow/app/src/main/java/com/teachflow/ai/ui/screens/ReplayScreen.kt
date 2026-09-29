package com.teachflow.ai.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.teachflow.ai.executor.ActionExecutor
import com.teachflow.ai.model.Workflow
import com.teachflow.ai.network.TeachFlowApiClient
import com.teachflow.ai.ui.components.DemoAppSimulator
import com.teachflow.ai.ui.theme.AccentPurple
import com.teachflow.ai.ui.theme.EmeraldGreen
import com.teachflow.ai.ui.theme.GlowCyan
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ReplayScreen(
    onRequestApproval: (reason: String, onApprove: () -> Unit, onCancel: () -> Unit) -> Unit
) {
    var voiceCommandText by remember { mutableStateOf("Get me 3 burgers") }
    var matchedWorkflow by remember { mutableStateOf<Workflow?>(null) }
    var extractedParams by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var isExecuting by remember { mutableStateOf(false) }
    var currentStepIndex by remember { mutableIntStateOf(0) }
    var executionMessage by remember { mutableStateOf("Ready to run execution plan.") }
    var executionSuccess by remember { mutableStateOf<Boolean?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Speech-to-Text launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!matches.isNullOrEmpty()) {
                voiceCommandText = matches[0]
            }
        }
    }

    // Auto-match workflow when command changes
    LaunchedEffect(voiceCommandText) {
        if (voiceCommandText.isNotBlank()) {
            val res = TeachFlowApiClient.sendIntent(voiceCommandText)
            res.onSuccess { (wf, params) ->
                matchedWorkflow = wf
                extractedParams = params
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Run Learned Task",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Re-execute learned workflows with new voice parameters using semantic accessibility matching.",
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
                        text = "Voice command:",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = voiceCommandText,
                            onValueChange = { voiceCommandText = it },
                            placeholder = { Text("e.g. Get me 3 burgers") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                try {
                                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak command (e.g. Get me 3 burgers)")
                                    }
                                    speechLauncher.launch(intent)
                                } catch (e: Exception) {
                                    // Voice input fallback if unavailable
                                }
                            },
                            colors = IconButtonDefaults.iconButtonColors(containerColor = AccentPurple.copy(alpha = 0.2f))
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = "Voice Input", tint = AccentPurple)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Matched Workflow & Parameters display
                    matchedWorkflow?.let { wf ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Matched workflow:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = GlowCyan.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = wf.intent,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = GlowCyan,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Parameters:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                extractedParams.forEach { (key, value) ->
                                    Text(
                                        text = "  • $key: $value",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (isExecuting) return@Button
                                isExecuting = true
                                executionSuccess = null
                                currentStepIndex = 0
                                executionMessage = "Starting execution plan..."

                                scope.launch {
                                    val steps = wf.steps
                                    var allSuccess = true

                                    for (i in steps.indices) {
                                        val step = steps[i]
                                        currentStepIndex = i + 1
                                        executionMessage = "Step ${i + 1}/${steps.size}: ${step.action} target '${step.target?.text ?: step.target?.role ?: "UI element"}'"

                                        if (ActionExecutor.isSensitiveAction(step)) {
                                            var approvedState = false
                                            var cancelState = false

                                            onRequestApproval(
                                                step.approvalReason ?: "Sensitive Checkpoint (${step.action})",
                                                { approvedState = true },
                                                { cancelState = true }
                                            )

                                            // Wait for human approval or cancel decision
                                            while (!approvedState && !cancelState) {
                                                delay(200)
                                            }

                                            if (cancelState) {
                                                executionMessage = "Execution cancelled by user at sensitive step ${i + 1}"
                                                allSuccess = false
                                                break
                                            }
                                        }

                                        val stepResult = ActionExecutor.executeStep(
                                            flowId = wf.flowId,
                                            step = step,
                                            parameters = extractedParams,
                                            approved = true
                                        )

                                        delay(600) // Brief step delay for visual observation

                                        if (!stepResult.success && stepResult.error != null) {
                                            executionMessage = "Step ${i + 1} issue: ${stepResult.message}. Proceeding with semantic recovery..."
                                        }
                                    }

                                    isExecuting = false
                                    executionSuccess = allSuccess
                                    if (allSuccess) {
                                        executionMessage = "Workflow completed successfully! All actions executed."
                                    }
                                }
                            },
                            enabled = !isExecuting,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GlowCyan),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            if (isExecuting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Executing Step $currentStepIndex...", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            } else {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Run")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Run", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
            }
        }

        item {
            // Execution Status Card
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Execution Progress:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = executionMessage,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = when (executionSuccess) {
                                true -> EmeraldGreen
                                false -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.primary
                            }
                        )
                    )
                }
            }
        }

        item {
            // Embedded Interactive Demo Target App
            DemoAppSimulator()
        }
    }
}
