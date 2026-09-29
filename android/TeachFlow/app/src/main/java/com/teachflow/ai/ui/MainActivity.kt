package com.teachflow.ai.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.teachflow.ai.ui.components.ApprovalDialog
import com.teachflow.ai.ui.screens.*
import com.teachflow.ai.ui.theme.TeachFlowTheme

enum class ScreenTab(val title: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    LEARN("Learn", Icons.Default.School),
    REPLAY("Replay", Icons.Default.PlayArrow),
    WORKFLOWS("Workflows", Icons.Default.ListAlt),
    ACCESSIBILITY("Status", Icons.Default.AccessibilityNew)
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TeachFlowTheme {
                var currentTab by remember { mutableStateOf(ScreenTab.HOME) }
                var showApprovalDialog by remember { mutableStateOf(false) }
                var approvalReasonText by remember { mutableStateOf("") }
                var onApproveCallback by remember { mutableStateOf<(() -> Unit)?>(null) }
                var onCancelCallback by remember { mutableStateOf<(() -> Unit)?>(null) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Scaffold(
                        bottomBar = {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface
                            ) {
                                ScreenTab.values().forEach { tab ->
                                    NavigationBarItem(
                                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                                        label = { Text(tab.title) },
                                        selected = currentTab == tab,
                                        onClick = { currentTab = tab }
                                    )
                                }
                            }
                        }
                    ) { paddingValues ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                        ) {
                            when (currentTab) {
                                ScreenTab.HOME -> HomeScreen(
                                    onNavigateToLearn = { currentTab = ScreenTab.LEARN },
                                    onNavigateToReplay = { currentTab = ScreenTab.REPLAY },
                                    onNavigateToWorkflows = { currentTab = ScreenTab.WORKFLOWS },
                                    onNavigateToAccessibility = { currentTab = ScreenTab.ACCESSIBILITY }
                                )
                                ScreenTab.LEARN -> LearnScreen()
                                ScreenTab.REPLAY -> ReplayScreen(
                                    onRequestApproval = { reason, onApprove, onCancel ->
                                        approvalReasonText = reason
                                        onApproveCallback = {
                                            showApprovalDialog = false
                                            onApprove()
                                        }
                                        onCancelCallback = {
                                            showApprovalDialog = false
                                            onCancel()
                                        }
                                        showApprovalDialog = true
                                    }
                                )
                                ScreenTab.WORKFLOWS -> WorkflowsScreen(
                                    onSelectWorkflowToRun = { currentTab = ScreenTab.REPLAY }
                                )
                                ScreenTab.ACCESSIBILITY -> AccessibilityStatusScreen()
                            }

                            if (showApprovalDialog) {
                                ApprovalDialog(
                                    actionReason = approvalReasonText,
                                    onApprove = { onApproveCallback?.invoke() },
                                    onCancel = { onCancelCallback?.invoke() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
