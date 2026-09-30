package com.teachflow.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teachflow.ai.accessibility.TeachFlowAccessibilityService
import com.teachflow.ai.network.MockBackendEngine
import com.teachflow.ai.network.NetworkConfig
import com.teachflow.ai.ui.theme.AccentPurple
import com.teachflow.ai.ui.theme.EmeraldGreen
import com.teachflow.ai.ui.theme.GlowCyan

@Composable
fun HomeScreen(
    onNavigateToLearn: () -> Unit,
    onNavigateToReplay: () -> Unit,
    onNavigateToWorkflows: () -> Unit,
    onNavigateToAccessibility: () -> Unit
) {
    val isAccessibilityConnected by TeachFlowAccessibilityService.isServiceConnected.collectAsState()
    val savedWorkflows by MockBackendEngine.workflowsFlow.collectAsState()
    val isMockMode by NetworkConfig.useMockBackend.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Header Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(AccentPurple.copy(alpha = 0.25f), GlowCyan.copy(alpha = 0.25f))
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Logo",
                                tint = AccentPurple,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "TeachFlow AI",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "“Teach once. Reuse anywhere.”",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Medium,
                                color = GlowCyan
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Demonstrate UI workflows once. Let AI extract semantic actions and re-execute on demand with voice.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            // Accessibility & Service Status Bar
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToAccessibility() },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isAccessibilityConnected) EmeraldGreen.copy(alpha = 0.12f) else MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(if (isAccessibilityConnected) EmeraldGreen else MaterialTheme.colorScheme.error)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isAccessibilityConnected) "Accessibility Service Connected" else "Accessibility Service Required",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = if (isAccessibilityConnected) EmeraldGreen else MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = if (isAccessibilityConnected) "Ready to capture & execute UI trees" else "Tap here to grant permission in Settings",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Navigate",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Core Actions",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isMockMode) "Mock Engine" else "Live Backend",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Switch(
                        checked = isMockMode,
                        onCheckedChange = { NetworkConfig.setUseMockBackend(it) },
                        modifier = Modifier.scale(0.8f)
                    )
                }
            }
        }

        item {
            // Main Feature Grid / Cards
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionCard(
                    title = "Learn a Task",
                    subtitle = "Demonstrate UI steps (e.g., 'Order 2 pizzas') once and synthesize parameterized flow",
                    icon = Icons.Default.School,
                    badgeColor = AccentPurple,
                    onClick = onNavigateToLearn
                )

                ActionCard(
                    title = "Run a Learned Task",
                    subtitle = "Replay with new voice prompt (e.g., 'Get me 3 burgers') using semantic UI matching",
                    icon = Icons.Default.PlayArrow,
                    badgeColor = GlowCyan,
                    onClick = onNavigateToReplay
                )

                ActionCard(
                    title = "Learned Workflows (${savedWorkflows.size})",
                    subtitle = "Inspect, edit, or delete existing synthesized execution contracts",
                    icon = Icons.Default.ListAlt,
                    badgeColor = EmeraldGreen,
                    onClick = onNavigateToWorkflows
                )

                ActionCard(
                    title = "Accessibility Status",
                    subtitle = "Manage Android node permissions & UI tree inspector",
                    icon = Icons.Default.AccessibilityNew,
                    badgeColor = MaterialTheme.colorScheme.secondary,
                    onClick = onNavigateToAccessibility
                )
            }
        }
    }
}

@Composable
private fun ActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    badgeColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(badgeColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = badgeColor,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Open",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
