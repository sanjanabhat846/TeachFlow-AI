package com.teachflow.ai.accessibility

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.teachflow.ai.model.UIElement
import com.teachflow.ai.model.UITree

object UIHierarchyReader {

    fun extractUITree(rootNode: AccessibilityNodeInfo?, screenName: String = "active_screen"): UITree {
        if (rootNode == null) {
            return UITree(screen = screenName, timestamp = System.currentTimeMillis(), elements = emptyList())
        }

        val elementsList = mutableListOf<UIElement>()
        var counter = 1
        traverseNode(rootNode, elementsList, counterRef = { counter++ })

        return UITree(
            screen = screenName,
            timestamp = System.currentTimeMillis(),
            elements = elementsList
        )
    }

    private fun traverseNode(
        node: AccessibilityNodeInfo?,
        elements: MutableList<UIElement>,
        counterRef: () -> Int
    ) {
        if (node == null) return

        try {
            val element = createUIElement(node, "node_${counterRef()}")
            // Include node if it carries text, content description, resource ID, or is interactive
            if (isMeaningfulElement(element)) {
                elements.add(element)
            }

            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                traverseNode(child, elements, counterRef)
            }
        } catch (e: Exception) {
            // Protect against unexpected node state changes or recycling exceptions
        }
    }

    fun createUIElement(node: AccessibilityNodeInfo, idStr: String): UIElement {
        val classNameStr = node.className?.toString() ?: ""
        val textStr = node.text?.toString()?.takeIf { it.isNotBlank() }
        val descStr = node.contentDescription?.toString()?.takeIf { it.isNotBlank() }
        val resIdStr = node.viewIdResourceName?.takeIf { it.isNotBlank() }

        val boundsRect = Rect()
        node.getBoundsInScreen(boundsRect)
        val boundsStr = "[${boundsRect.left},${boundsRect.top}][${boundsRect.right},${boundsRect.bottom}]"

        val roleStr = mapClassToRole(classNameStr, textStr, resIdStr, node.isEditable)

        return UIElement(
            id = idStr,
            role = roleStr,
            className = classNameStr,
            text = textStr,
            contentDescription = descStr,
            resourceId = resIdStr,
            clickable = node.isClickable,
            enabled = node.isEnabled,
            editable = node.isEditable,
            scrollable = node.isScrollable,
            bounds = boundsStr,
            context = contextFor(node)
        )
    }

    fun contextFor(node: AccessibilityNodeInfo): String {
        val labels = mutableListOf<String>()
        var ancestor = node.parent
        while (ancestor != null && labels.size < 3) {
            val label = ancestor.text?.toString()?.takeIf { it.isNotBlank() }
                ?: ancestor.contentDescription?.toString()?.takeIf { it.isNotBlank() }
            if (label != null) labels.add(label)
            ancestor = ancestor.parent
        }
        return labels.asReversed().joinToString(" > ")
    }

    private fun isMeaningfulElement(element: UIElement): Boolean {
        return !element.text.isNullOrBlank() ||
                !element.contentDescription.isNullOrBlank() ||
                !element.resourceId.isNullOrBlank() ||
                element.clickable ||
                element.editable
    }

    fun mapClassToRole(className: String, text: String?, resourceId: String?, isEditable: Boolean): String {
        val lowerClass = className.lowercase()
        val lowerRes = resourceId?.lowercase() ?: ""

        return when {
            isEditable || lowerClass.contains("edittext") || lowerRes.contains("search") || lowerRes.contains("input") -> "edit_text"
            lowerClass.contains("button") -> "button"
            lowerClass.contains("checkbox") || lowerClass.contains("switch") || lowerClass.contains("toggle") -> "checkbox"
            lowerClass.contains("image") -> "image"
            lowerClass.contains("seekbar") || lowerClass.contains("picker") -> "quantity_picker"
            !text.isNullOrBlank() -> "text_view"
            else -> "container"
        }
    }
}
