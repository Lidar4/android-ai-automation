package com.example.myautomationapp

import java.util.Locale

enum class CommandKind { OPEN_APP, BACK, HOME, RECENT, SCROLL_DOWN, SCROLL_UP, UNKNOWN }
data class ParsedCommand(val kind: CommandKind, val app: AppEntry? = null)

object AutomationCommandEngine {
    private val knownPackages = mapOf(
        "youtube" to listOf("com.google.android.youtube"), "facebook" to listOf("com.facebook.katana"),
        "instagram" to listOf("com.instagram.android"), "tiktok" to listOf("com.zhiliaoapp.musically"),
        "whatsapp" to listOf("com.whatsapp"), "messenger" to listOf("com.facebook.orca"),
        "telegram" to listOf("org.telegram.messenger"), "gmail" to listOf("com.google.android.gm"),
        "chrome" to listOf("com.android.chrome", "com.chrome.beta"), "settings" to listOf("com.android.settings"),
        "photos" to listOf("com.google.android.apps.photos"),
        "camera" to listOf("com.android.camera2", "com.google.android.GoogleCamera", "com.android.camera"),
        "spotify" to listOf("com.spotify.music"), "maps" to listOf("com.google.android.apps.maps")
    )
    private val aliases = mapOf(
        "ইউটিউব" to "youtube", "ফেসবুক" to "facebook", "ইনস্টাগ্রাম" to "instagram",
        "টিকটক" to "tiktok", "হোয়াটসঅ্যাপ" to "whatsapp", "হোয়াটসঅ্যাপ" to "whatsapp",
        "মেসেঞ্জার" to "messenger", "টেলিগ্রাম" to "telegram", "জিমেইল" to "gmail",
        "ক্রোম" to "chrome", "সেটিংস" to "settings", "গ্যালারি" to "photos",
        "ফটোস" to "photos", "ক্যামেরা" to "camera"
    )
    fun parse(input: String, apps: List<AppEntry>): ParsedCommand {
        val text = input.lowercase(Locale.ROOT).trim()
        if (text.contains("scroll down") || text.contains("নিচে স্ক্রল") || text.contains("নিচে যাও")) return ParsedCommand(CommandKind.SCROLL_DOWN)
        if (text.contains("scroll up") || text.contains("উপরে স্ক্রল") || text.contains("উপরে যাও")) return ParsedCommand(CommandKind.SCROLL_UP)
        if (text == "back" || text.contains("go back") || text.contains("পেছনে যাও") || text.contains("ব্যাক")) return ParsedCommand(CommandKind.BACK)
        if (text == "home" || text.contains("go home") || text.contains("হোমে যাও")) return ParsedCommand(CommandKind.HOME)
        if (text.contains("recent apps") || text.contains("রিসেন্ট অ্যাপ")) return ParsedCommand(CommandKind.RECENT)
        val cleaned = text
            .replace(Regex("^(open|launch|start|খোলো|খুলে দাও|চালু করো|ওপেন করো)\\s*"), "")
            .replace(Regex("(খোলো|খুলে দাও|চালু করো|ওপেন করো|open|please)$"), "")
            .trim()
        val target = aliases.entries.firstOrNull { cleaned.contains(it.key) }?.value ?: cleaned
        val known = knownPackages[target]
        if (known != null) return ParsedCommand(CommandKind.OPEN_APP, apps.firstOrNull { it.packageName in known })
        val match = apps.firstOrNull { it.label.equals(cleaned, true) || (it.label.lowercase(Locale.ROOT).contains(cleaned) && cleaned.length >= 3) }
        if (match != null) return ParsedCommand(CommandKind.OPEN_APP, match)
        if (text.startsWith("open ") || text.startsWith("launch ") || text.startsWith("খোলো") || text.contains("চালু করো") || text.contains("ওপেন করো")) return ParsedCommand(CommandKind.OPEN_APP, null)
        return ParsedCommand(CommandKind.UNKNOWN)
    }
}
