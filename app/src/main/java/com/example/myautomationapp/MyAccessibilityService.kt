package com.example.myautomationapp

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
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
    fun performCommand(kind: CommandKind): Boolean = when (kind) {
        CommandKind.BACK -> performGlobalAction(GLOBAL_ACTION_BACK)
        CommandKind.HOME -> performGlobalAction(GLOBAL_ACTION_HOME)
        CommandKind.RECENT -> performGlobalAction(GLOBAL_ACTION_RECENTS)
        CommandKind.SCROLL_DOWN -> performScroll(down = true)
        CommandKind.SCROLL_UP -> performScroll(down = false)
        else -> false
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
