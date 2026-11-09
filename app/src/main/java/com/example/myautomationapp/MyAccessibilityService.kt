package com.example.myautomationapp

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.util.Log

class MyAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "MyAccessibilityService"
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Triggered when events occur on screen (e.g., app opens, text changes)
        val eventType = event?.eventType
        val packageName = event?.packageName?.toString()
        val textList = event?.text

        Log.d(TAG, "Event: $eventType | Package: $packageName | Text: $textList")
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility Service Interrupted")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Accessibility Service Connected Successfully")
    }
}
