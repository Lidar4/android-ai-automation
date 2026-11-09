package com.example.myautomationapp

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

data class AppEntry(val label: String, val packageName: String)

object AppCatalog {
    @Suppress("DEPRECATION")
    fun loadLaunchableApps(context: Context): List<AppEntry> {
        val intent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
        val results = if (Build.VERSION.SDK_INT >= 33) {
            context.packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            context.packageManager.queryIntentActivities(intent, 0)
        }
        return results.mapNotNull { info ->
            val activity = info.activityInfo ?: return@mapNotNull null
            if (activity.packageName == context.packageName) return@mapNotNull null
            val label = info.loadLabel(context.packageManager)?.toString()?.trim().orEmpty()
            if (label.isBlank()) null else AppEntry(label, activity.packageName)
        }.distinctBy { it.packageName }.sortedBy { it.label.lowercase() }
    }
}
