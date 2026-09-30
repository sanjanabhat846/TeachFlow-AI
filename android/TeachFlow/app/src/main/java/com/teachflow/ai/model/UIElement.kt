package com.teachflow.ai.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UIElement(
    @SerialName("id")
    val id: String,
    @SerialName("role")
    val role: String,
    @SerialName("class_name")
    val className: String = "",
    @SerialName("text")
    val text: String? = null,
    @SerialName("content_description")
    val contentDescription: String? = null,
    @SerialName("resource_id")
    val resourceId: String? = null,
    @SerialName("clickable")
    val clickable: Boolean = false,
    @SerialName("enabled")
    val enabled: Boolean = true,
    @SerialName("editable")
    val editable: Boolean = false,
    @SerialName("scrollable")
    val scrollable: Boolean = false,
    @SerialName("bounds")
    val bounds: String? = null
)
