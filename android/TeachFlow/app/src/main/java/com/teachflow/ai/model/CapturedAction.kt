package com.teachflow.ai.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TargetSpec(
    @SerialName("role")
    val role: String? = null,
    @SerialName("text")
    val text: String? = null,
    @SerialName("content_description")
    val contentDescription: String? = null,
    @SerialName("resource_id")
    val resourceId: String? = null,
    @SerialName("class_name")
    val className: String? = null,
    @SerialName("clickable")
    val clickable: Boolean? = null,
    @SerialName("enabled")
    val enabled: Boolean? = null
)

@Serializable
data class CapturedAction(
    @SerialName("action")
    val action: String, // tap, type, scroll, set_quantity
    @SerialName("target")
    val target: TargetSpec? = null,
    @SerialName("value")
    val value: String? = null,
    @SerialName("direction")
    val direction: String? = null, // down, up, left, right
    @SerialName("timestamp")
    val timestamp: Long = System.currentTimeMillis()
)
