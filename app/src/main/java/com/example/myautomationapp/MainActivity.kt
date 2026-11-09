package com.example.myautomationapp

import android.Manifest
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.content.pm.PackageManager
import android.provider.Settings
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private val bg = Color.rgb(8, 13, 25)
    private val panel = Color.rgb(20, 29, 47)
    private val accent = Color.rgb(93, 164, 255)
    private val muted = Color.rgb(157, 173, 198)
    private lateinit var commandInput: EditText
    private lateinit var statusText: TextView
    private lateinit var resultText: TextView
    private lateinit var appList: LinearLayout
    private var installedApps: List<AppEntry> = emptyList()
    private var speechRecognizer: SpeechRecognizer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        installedApps = AppCatalog.loadLaunchableApps(this)
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(26), dp(20), dp(28))
            setBackgroundColor(bg)
        }
        val scroll = ScrollView(this).apply { isFillViewport = true; addView(page) }
        page.addView(label("PROTON AI", 13, accent, true))
        page.addView(label("Phone Automation", 29, Color.WHITE, true).withTop(5))
        page.addView(label("Give a command in Bangla or English. Proton will perform supported actions on your phone.", 14, muted).withTop(7))

        val statusCard = card().withTop(22)
        statusCard.addView(label("ACCESSIBILITY STATUS", 10, muted, true))
        statusText = label("Checking permission…", 17, Color.WHITE, true).withTop(7) as TextView
        statusCard.addView(statusText)
        statusCard.addView(label("Enable Proton AI in Android Accessibility settings to use navigation, scrolling, visible button taps and focused-field text entry.", 12, muted).withTop(6))
        val permissionButton = button("Open accessibility settings")
        permissionButton.setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        statusCard.addView(permissionButton.withTop(12))
        page.addView(statusCard)

        page.addView(sectionTitle("Ask Proton").withTop(24))
        commandInput = EditText(this).apply {
            hint = "Example: YouTube খোলো / Open WhatsApp"
            setHintTextColor(muted)
            setTextColor(Color.WHITE)
            textSize = 15f
            minLines = 2
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = rounded(panel, 14)
        }
        page.addView(commandInput.withTop(12))
        val controls = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val runButton = button("Run command")
        runButton.layoutParams = LinearLayout.LayoutParams(0, dp(50), 1f).apply { marginEnd = dp(8) }
        runButton.setOnClickListener { runCommand(commandInput.text.toString()) }
        val voiceButton = button("Voice")
        voiceButton.layoutParams = LinearLayout.LayoutParams(0, dp(50), 1f).apply { marginStart = dp(8) }
        voiceButton.setOnClickListener { startVoiceInput() }
        controls.addView(runButton)
        controls.addView(voiceButton)
        page.addView(controls.withTop(10))
        resultText = label("Ready. Try “Open YouTube” or “নিচে স্ক্রল করো”.", 13, muted).withTop(12) as TextView
        page.addView(resultText)

        page.addView(sectionTitle("Quick app access").withTop(26))
        page.addView(label("Tap an app to open it. The list is based on apps installed on this device.", 12, muted).withTop(5))
        appList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        page.addView(appList.withTop(10))
        renderAppList()
        page.addView(label("PROTON AI • ON-DEVICE COMMAND MODE", 10, muted, true).apply { gravity = Gravity.CENTER }.withTop(26))
        setContentView(scroll)
    }

    override fun onResume() { super.onResume(); refreshAccessibilityStatus() }
    @Deprecated("Use the Activity Result API in future revisions")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 410 && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) startVoiceInput()
        else if (requestCode == 410) showResult("ভয়েস কমান্ডের জন্য Microphone permission প্রয়োজন।")
    }
    override fun onDestroy() { speechRecognizer?.destroy(); speechRecognizer = null; super.onDestroy() }

    private fun runCommand(raw: String) {
        if (raw.isBlank()) { showResult("একটি নির্দেশ লিখে দাও।"); return }
        val parsed = AutomationCommandEngine.parse(raw, installedApps)
        when (parsed.kind) {
            CommandKind.OPEN_APP -> {
                val entry = parsed.app
                if (entry == null) { showResult("অ্যাপটি পাওয়া যায়নি। ইনস্টল আছে কি না দেখো, অথবা তালিকা থেকে খোলো।"); return }
                val launchIntent = packageManager.getLaunchIntentForPackage(entry.packageName)
                if (launchIntent == null) showResult("${entry.label} সরাসরি খোলা যাচ্ছে না।")
                else { launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(launchIntent); showResult("${entry.label} খোলা হচ্ছে।") }
            }
            CommandKind.BACK, CommandKind.HOME, CommandKind.RECENT, CommandKind.SCROLL_DOWN, CommandKind.SCROLL_UP, CommandKind.TAP_TEXT, CommandKind.TYPE_TEXT -> {
                val service = MyAccessibilityService.instance
                if (service == null) showResult("এই নির্দেশের জন্য Accessibility Service চালু করো। উপরের সেটিংস বাটন ব্যবহার করো।")
                else showResult(if (service.performCommand(parsed)) "নির্দেশ সম্পন্ন হয়েছে।" else "Android এই কাজটি সম্পাদন করতে দেয়নি।")
            }
            CommandKind.UNKNOWN -> showResult("এই কমান্ডটি এখনো সমর্থিত নয়। অ্যাপ খোলা, Back, Home, Recent Apps, Scroll, tap এবং type ব্যবহার করো।")
        }
    }

    private fun startVoiceInput() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 410)
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) { showResult("এই ফোনে speech recognition service পাওয়া যায়নি।"); return }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Proton-কে নির্দেশ দিন")
        }
        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).also { recognizer ->
                recognizer.setRecognitionListener(object : android.speech.RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) { showResult("শুনছি…") }
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() { showResult("নির্দেশ প্রক্রিয়া করা হচ্ছে…") }
                    override fun onError(error: Int) { showResult("ভয়েস বোঝা যায়নি। আবার চেষ্টা করো অথবা লিখে দাও।") }
                    override fun onResults(results: Bundle?) {
                        val spoken = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                        commandInput.setText(spoken)
                        runCommand(spoken)
                    }
                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
                recognizer.startListening(intent)
            }
        } catch (_: Exception) { showResult("ভয়েস ইনপুট চালু করা যায়নি।") }
    }

    private fun renderAppList() {
        appList.removeAllViews()
        val preferred = listOf("YouTube", "Facebook", "Instagram", "TikTok", "WhatsApp", "Messenger", "Telegram", "Gmail", "Chrome", "Settings", "Photos", "Camera")
        val ordered = installedApps.sortedWith(compareBy<AppEntry>({ preferred.indexOfFirst { p -> it.label.equals(p, true) }.let { if (it < 0) 999 else it } }, { it.label.lowercase(Locale.ROOT) }))
        ordered.take(60).forEach { entry ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(14), dp(12), dp(14), dp(12))
                background = rounded(panel, 12)
                isClickable = true
                isFocusable = true
                addView(label(entry.label, 14, Color.WHITE, true).apply { layoutParams = LinearLayout.LayoutParams(0, -2, 1f) })
                addView(label("OPEN  ›", 11, accent, true))
                setOnClickListener { runCommand("open ${entry.label}") }
            }
            appList.addView(row, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(7) })
        }
        if (ordered.isEmpty()) appList.addView(label("No launchable apps found.", 13, muted))
    }

    private fun refreshAccessibilityStatus() {
        val enabled = MyAccessibilityService.instance != null || isAccessibilityServiceEnabled()
        statusText.text = if (enabled) "Enabled • Ready for supported phone actions" else "Not enabled • App launching still works"
        statusText.setTextColor(if (enabled) Color.rgb(115, 220, 165) else Color.WHITE)
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val enabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES).orEmpty()
        return enabled.contains("$packageName/$packageName.MyAccessibilityService", true)
    }
    private fun showResult(message: String) { resultText.text = message; Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }
    private fun label(text: String, size: Int, color: Int, bold: Boolean = false) = TextView(this).apply {
        this.text = text; textSize = size.toFloat(); setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
        setLineSpacing(dp(2).toFloat(), 1f)
    }
    private fun sectionTitle(text: String) = label(text, 19, Color.WHITE, true)
    private fun card() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(17), dp(17), dp(17), dp(17)); background = rounded(panel, 18) }
    private fun button(text: String) = Button(this).apply {
        this.text = text; isAllCaps = false; textSize = 14f; setTextColor(Color.WHITE)
        background = rounded(Color.rgb(42, 91, 160), 13); minHeight = dp(48)
    }
    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat() }
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
    private fun <T : View> T.withTop(margin: Int): T {
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(margin) }
        return this
    }
}
