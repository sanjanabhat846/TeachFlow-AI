package com.teachflow.ai.accessibility

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.teachflow.ai.model.CapturedAction
import com.teachflow.ai.model.TargetSpec
import com.teachflow.ai.model.UITree
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class TeachFlowAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isServiceConnected.value = true
        Log.i(TAG, "TeachFlow Accessibility Service Connected successfully.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        try {
            val root = rootInActiveWindow
            if (root != null) {
                _latestUITree.value = UIHierarchyReader.extractUITree(root, event.packageName?.toString() ?: "unknown")
            }

            if (_isRecordingDemonstration.value) {
                captureActionFromEvent(event)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling accessibility event", e)
        }
    }

    private fun captureActionFromEvent(event: AccessibilityEvent) {
        val node = event.source ?: return
        val text = node.text?.toString() ?: event.text.joinToString(" ")
        val desc = node.contentDescription?.toString()
        val resId = node.viewIdResourceName
        val className = node.className?.toString() ?: ""
        val role = UIHierarchyReader.mapClassToRole(className, text, resId, node.isEditable)

        val target = TargetSpec(
            role = role,
            text = text.takeIf { it.isNotBlank() },
            contentDescription = desc?.takeIf { it.isNotBlank() },
            resourceId = resId?.takeIf { it.isNotBlank() },
            className = className,
            clickable = node.isClickable,
            enabled = node.isEnabled,
            context = UIHierarchyReader.contextFor(node)
        )

        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                val action = CapturedAction(
                    action = "tap",
                    target = target
                )
                addCapturedAction(action)
            }
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                val action = CapturedAction(
                    action = "type",
                    target = target,
                    value = text
                )
                addCapturedAction(action)
            }
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                val action = CapturedAction(
                    action = "scroll",
                    direction = "down"
                )
                addCapturedAction(action)
            }
        }
    }

    private fun addCapturedAction(action: CapturedAction) {
        val currentList = _capturedActions.value.toMutableList()
        currentList.add(action)
        _capturedActions.value = currentList
        Log.d(TAG, "Captured demonstration action: ${action.action} on target: ${action.target}")
    }

    override fun onInterrupt() {
        Log.w(TAG, "TeachFlow Accessibility Service Interrupted.")
    }

    override fun onDestroy() {
        super.onDestroy()
        _isServiceConnected.value = false
        if (instance == this) {
            instance = null
        }
        Log.i(TAG, "TeachFlow Accessibility Service Destroyed.")
    }

    companion object {
        private const val TAG = "TeachFlowAccService"
        var instance: TeachFlowAccessibilityService? = null
            private set

        private val _isServiceConnected = MutableStateFlow(false)
        val isServiceConnected: StateFlow<Boolean> = _isServiceConnected

        private val _latestUITree = MutableStateFlow(UITree())
        val latestUITree: StateFlow<UITree> = _latestUITree

        private val _isRecordingDemonstration = MutableStateFlow(false)
        val isRecordingDemonstration: StateFlow<Boolean> = _isRecordingDemonstration

        private val _capturedActions = MutableStateFlow<List<CapturedAction>>(emptyList())
        val capturedActions: StateFlow<List<CapturedAction>> = _capturedActions

        fun startRecording() {
            _capturedActions.value = emptyList()
            _isRecordingDemonstration.value = true
        }

        fun stopRecording(): List<CapturedAction> {
            _isRecordingDemonstration.value = false
            return _capturedActions.value
        }

        fun getRootNode(): AccessibilityNodeInfo? {
            return instance?.rootInActiveWindow
        }
    }
}
