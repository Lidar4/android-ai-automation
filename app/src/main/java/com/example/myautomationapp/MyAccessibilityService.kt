package com.example.myautomationapp

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.util.Log

class MyAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "MyAccessibilityService"
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Accessibility events may contain sensitive screen text. Do not log or persist them.
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility Service Interrupted")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Accessibility Service Connected Successfully")
    }
}
