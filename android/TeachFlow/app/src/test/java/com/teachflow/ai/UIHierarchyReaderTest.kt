package com.teachflow.ai

import com.teachflow.ai.accessibility.UIHierarchyReader
import com.teachflow.ai.model.UIElement
import com.teachflow.ai.model.UITree
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class UIHierarchyReaderTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testRoleMappingForEditText() {
        val role = UIHierarchyReader.mapClassToRole(
            className = "android.widget.EditText",
            text = "Search",
            resourceId = "com.app:id/search_box",
            isEditable = true
        )
        assertEquals("edit_text", role)
    }

    @Test
    fun testRoleMappingForButton() {
        val role = UIHierarchyReader.mapClassToRole(
            className = "android.widget.Button",
            text = "Add to Cart",
            resourceId = "com.app:id/add_cart",
            isEditable = false
        )
        assertEquals("button", role)
    }

    @Test
    fun testUITreeJsonSerialization() {
        val element = UIElement(
            id = "node_1",
            role = "button",
            className = "android.widget.Button",
            text = "Add to Cart",
            contentDescription = "Add selected item to cart",
            resourceId = "add_cart",
            clickable = true,
            enabled = true,
            editable = false,
            scrollable = false,
            bounds = "[100,500][980,620]"
        )

        val tree = UITree(
            screen = "food_app",
            timestamp = 1727625600000L,
            elements = listOf(element)
        )

        val serializedJson = json.encodeToString(tree)
        assertTrue(serializedJson.contains("\"role\":\"button\""))
        assertTrue(serializedJson.contains("\"text\":\"Add to Cart\""))
        assertTrue(serializedJson.contains("\"resource_id\":\"add_cart\""))

        val deserialized = json.decodeFromString<UITree>(serializedJson)
        assertEquals("food_app", deserialized.screen)
        assertEquals(1, deserialized.elements.size)
        assertEquals("Add to Cart", deserialized.elements[0].text)
    }
}
