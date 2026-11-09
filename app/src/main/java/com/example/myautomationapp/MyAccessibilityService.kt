package com.example.myautomationapp

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class MyAccessibilityService : AccessibilityService() {
    companion object {
        private const val TAG = "ProtonAccessibility"
        @Volatile var instance: MyAccessibilityService? = null
            private set
    }
    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "Proton AI accessibility service connected")
    }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Screen text and event contents are intentionally not logged or stored.
    }
    override fun onInterrupt() { Log.i(TAG, "Accessibility service interrupted") }
    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }
    fun performCommand(command: ParsedCommand): Boolean = when (command.kind) {
        CommandKind.BACK -> performGlobalAction(GLOBAL_ACTION_BACK)
        CommandKind.HOME -> performGlobalAction(GLOBAL_ACTION_HOME)
        CommandKind.RECENT -> performGlobalAction(GLOBAL_ACTION_RECENTS)
        CommandKind.SCROLL_DOWN -> performScroll(down = true)
        CommandKind.SCROLL_UP -> performScroll(down = false)
        CommandKind.TAP_TEXT -> command.value?.let { clickVisibleText(it) } ?: false
        CommandKind.TYPE_TEXT -> command.value?.let { typeIntoFocusedField(it) } ?: false
        else -> false
    }

    private fun clickVisibleText(target: String): Boolean {
        val blocked = setOf("send", "post", "publish", "pay", "purchase", "buy", "delete", "remove", "confirm", "পাঠান", "পোস্ট", "প্রকাশ", "পেমেন্ট", "কিনুন", "মুছে ফেলুন", "নিশ্চিত করুন")
        if (target.trim().lowercase() in blocked) return false
        val root = rootInActiveWindow ?: return false
        val match = findMatchingNode(root, target.trim()) ?: return false
        var clickable: AccessibilityNodeInfo? = match
        while (clickable != null && !clickable.isClickable) clickable = clickable.parent
        return clickable?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true
    }

    private fun findMatchingNode(node: AccessibilityNodeInfo, target: String): AccessibilityNodeInfo? {
        if (!node.isVisibleToUser) return null
        val text = node.text?.toString()?.trim().orEmpty()
        val description = node.contentDescription?.toString()?.trim().orEmpty()
        if (text.equals(target, true) || description.equals(target, true)) return node
        for (index in 0 until node.childCount) {
            val child = node.getChild(index) ?: continue
            val found = findMatchingNode(child, target)
            if (found != null) return found
        }
        return null
    }

    private fun typeIntoFocusedField(value: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val field = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return false
        if (!field.isEditable || !field.isFocused) return false
        val args = Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value) }
        return field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }
    private fun performScroll(down: Boolean): Boolean {
        val width = resources.displayMetrics.widthPixels.toFloat()
        val height = resources.displayMetrics.heightPixels.toFloat()
        val x = width * 0.5f
        val startY = if (down) height * 0.72f else height * 0.32f
        val endY = if (down) height * 0.30f else height * 0.74f
        val path = Path().apply { moveTo(x, startY); lineTo(x, endY) }
        val gesture = GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(path, 0, 420)).build()
        return dispatchGesture(gesture, null, null)
    }
}
