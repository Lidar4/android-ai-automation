package com.example.myautomationapp.capcut

import android.content.Context
import android.content.Intent

class CapCutController(private val context: Context) {
    private val packageCandidates = listOf("com.lemon.lvoverseas", "com.lemon.lv")

    fun installedPackage(): String? = packageCandidates.firstOrNull { packageName ->
        runCatching { context.packageManager.getLaunchIntentForPackage(packageName) != null }.getOrDefault(false)
    }

    fun launch(): Result<Unit> = runCatching {
        val packageName = installedPackage() ?: error("CapCut is not installed or is not visible")
        val intent: Intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: error("CapCut launcher unavailable")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
