package com.teachflow.ai.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UITree(
    @SerialName("screen")
    val screen: String = "unknown_screen",
    @SerialName("timestamp")
    val timestamp: Long = System.currentTimeMillis(),
    @SerialName("elements")
    val elements: List<UIElement> = emptyList()
)
