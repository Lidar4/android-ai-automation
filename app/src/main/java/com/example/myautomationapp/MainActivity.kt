package com.example.myautomationapp

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myautomationapp.ai.RemoteAiPlanner
import com.example.myautomationapp.capcut.CapCutController
import com.example.myautomationapp.media.MediaAnalyzer
import com.example.myautomationapp.planner.EditingPlan
import com.example.myautomationapp.planner.OfflineDraftPlanner
import com.example.myautomationapp.planner.PlanStep
import com.example.myautomationapp.security.ApiKeyStore
import com.example.myautomationapp.task.TaskManager

class MainActivity : AppCompatActivity() {
    private lateinit var status: TextView
    private lateinit var planPreview: TextView
    private lateinit var instruction: EditText
    private lateinit var apiKeyStore: ApiKeyStore
    private lateinit var taskManager: TaskManager
    private lateinit var capCut: CapCutController
    private var selectedMedia: Uri? = null
    private var currentPlan: EditingPlan? = null
    private val pickMedia = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        apiKeyStore = ApiKeyStore(this)
        taskManager = TaskManager()
        capCut = CapCutController(this)
        setContentView(buildDashboard())
        taskManager.addListener { task -> runOnUiThread { status.text = "Task ${task.state}: ${task.message}" } }
    }

    private fun buildDashboard(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 24, 32, 24)
        }
        val scroll = ScrollView(this).apply { addView(root) }
        root.addView(TextView(this).apply {
            text = "AI Video Editing Studio"
            textSize = 26f
            setTextIsSelectable(true)
        })
        root.addView(TextView(this).apply {
            text = "Bangla / English • Safe, approval-based editing"
            textSize = 15f
            setPadding(0, 4, 0, 20)
        })
        instruction = EditText(this).apply {
            hint = "Describe an edit in English or বাংলা…"
            minLines = 3
            gravity = Gravity.TOP
        }
        root.addView(instruction, fill())
        root.addView(actionButton("1. Select video, image, or audio") { selectMedia() })
        root.addView(actionButton("2. Create safe offline draft plan") { createDraftPlan() })
        root.addView(actionButton("Ask configured AI to create a validated plan") { chooseAiProvider() })
        planPreview = TextView(this).apply {
            text = "No plan created. The offline draft only analyzes media; provider-backed AI is not faked."
            setPadding(0, 18, 0, 18)
            setTextIsSelectable(true)
        }
        root.addView(planPreview)
        root.addView(actionButton("Approve and run current plan") { approvePlan() })
        root.addView(actionButton("Open CapCut (if installed)") { launchCapCut() })
        root.addView(actionButton("Provider API settings") { showProviderSettings() })
        root.addView(actionButton("Accessibility permissions") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        })
        status = TextView(this).apply {
            text = "Ready. No media has been uploaded."
            setPadding(0, 20, 0, 0)
            setTextIsSelectable(true)
        }
        root.addView(status)
        return scroll
    }

    private fun actionButton(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        setOnClickListener { action() }
    }

    private fun fill() = LinearLayout.LayoutParams(-1, LinearLayout.LayoutParams.WRAP_CONTENT)

    private fun selectMedia() {
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            addCategory(Intent.CATEGORY_OPENABLE)
        }, pickMedia)
    }

    @Deprecated("Use Activity Result APIs when the minimum SDK modernization is scheduled")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != pickMedia || resultCode != RESULT_OK || data == null) return
        selectedMedia = data.data
        selectedMedia?.let { uri ->
            runCatching {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            MediaAnalyzer(this).inspect(uri).onSuccess { info ->
                status.text = "Selected: ${info.displayName}\n${info.width}×${info.height}, ${info.durationMs / 1000}s, ${info.mimeType ?: "unknown type"}"
            }.onFailure { status.text = "Media selected, but local inspection failed: ${it.message}" }
        }
    }

    private fun createDraftPlan() {
        val plan = OfflineDraftPlanner.draft(instruction.text?.toString().orEmpty())
        currentPlan = plan
        planPreview.text = "PLAN PREVIEW (offline draft; not AI)\nGoal: ${plan.goal}\nTarget: ${plan.target}\nSteps:\n${plan.steps.joinToString("\n") { "• ${it.action}" }}\n\nApproval is required before execution."
        taskManager.submit(plan) { executeTask(it) }
    }

    private fun chooseAiProvider() {
        AlertDialog.Builder(this)
            .setTitle("Choose configured AI provider")
            .setItems(arrayOf("OpenAI", "Gemini")) { _, which -> requestAiPlan(if (which == 0) "openai" else "gemini") }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun requestAiPlan(provider: String) {
        status.text = "Requesting a structured plan from $provider…"
        RemoteAiPlanner(provider, apiKeyStore).createPlan(instruction.text?.toString().orEmpty()) { result ->
            runOnUiThread {
                result.onSuccess { plan ->
                    currentPlan = plan
                    planPreview.text = "PLAN PREVIEW ($provider; validated JSON)\nGoal: ${plan.goal}\nTarget: ${plan.target}\nSteps:\n${plan.steps.joinToString("\n") { "• ${it.action}" }}\n\nApproval is required before execution."
                    taskManager.submit(plan) { executeTask(it) }
                }.onFailure { status.text = "AI plan unavailable: ${it.message}" }
            }
        }
    }

    private fun approvePlan() {
        if (currentPlan == null) {
            Toast.makeText(this, "Create a plan first", Toast.LENGTH_SHORT).show()
            return
        }
        taskManager.approveAndRun { executeTask(it) }
    }

    private fun executeTask(task: TaskManager.Task) {
        taskManager.update(task, TaskManager.State.RUNNING, "Executing allowlisted steps")
        val unsupported = task.plan.steps.firstOrNull { it.action != PlanStep.Action.ANALYZE_MEDIA }
        if (unsupported != null) {
            taskManager.update(task, TaskManager.State.FAILED, "Unsupported action: ${unsupported.action}", "No false success reported")
            return
        }
        if (selectedMedia == null) {
            taskManager.update(task, TaskManager.State.FAILED, "Select media before analysis", "No input media")
            return
        }
        taskManager.update(task, TaskManager.State.VERIFYING, "Verifying local media metadata")
        MediaAnalyzer(this).inspect(selectedMedia!!).fold(
            onSuccess = { info -> taskManager.update(task, TaskManager.State.COMPLETED, "Verified ${info.displayName}; no external upload performed") },
            onFailure = { error -> taskManager.update(task, TaskManager.State.FAILED, "Media analysis failed", error.message) }
        )
    }

    private fun launchCapCut() {
        capCut.launch().onSuccess {
            status.text = "CapCut launched. UI automation is not claimed until installed-app testing is available."
        }.onFailure { status.text = "CapCut unavailable: ${it.message}" }
    }

    private fun showProviderSettings() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 8, 40, 0)
        }
        val provider = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, listOf("openai", "gemini"))
        }
        val key = EditText(this).apply {
            hint = "Paste API key; stored encrypted in Android Keystore"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val explanation = TextView(this).apply {
            text = "Optional. The selected provider may charge for API use. Keys never go into source, logs, or APK resources."
        }
        container.addView(explanation)
        container.addView(provider)
        container.addView(key)
        AlertDialog.Builder(this).setTitle("Provider API settings").setView(container)
            .setPositiveButton("Save securely") { _, _ ->
                runCatching { apiKeyStore.save(provider.selectedItem.toString(), key.text.toString()) }
                    .onSuccess { status.text = "${provider.selectedItem} key saved in Android Keystore." }
                    .onFailure { status.text = "Key not saved: ${it.message}" }
            }
            .setNeutralButton("Delete key") { _, _ ->
                apiKeyStore.delete(provider.selectedItem.toString())
                status.text = "${provider.selectedItem} key deleted."
            }
            .setNegativeButton("Cancel", null).show()
    }
}
