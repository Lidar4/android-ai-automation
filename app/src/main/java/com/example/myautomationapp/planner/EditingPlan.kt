package com.example.myautomationapp.planner

import org.json.JSONArray
import org.json.JSONObject

/** Versioned, provider-independent plan exchanged between AI providers and the executor. */
data class EditingPlan(
    val schemaVersion: Int = 1,
    val project: String = "video_edit",
    val target: Target = Target.LOCAL_ENGINE,
    val goal: String,
    val steps: List<PlanStep>,
    val requiresConfirmation: Boolean = true
) {
    enum class Target { LOCAL_ENGINE, CAPCUT }
}

data class PlanStep(
    val action: Action,
    val arguments: Map<String, String> = emptyMap()
) {
    enum class Action {
        ANALYZE_MEDIA,
        TRIM,
        MERGE,
        RESIZE,
        ADD_SUBTITLES,
        ADD_BACKGROUND_MUSIC,
        LAUNCH_CAPCUT,
        OPEN_DRAFT,
        EXPORT
    }
}

object EditingPlanCodec {
    private val allowedKeys = setOf("schema_version", "project", "target", "goal", "steps", "requires_confirmation")

    fun fromJson(json: String): Result<EditingPlan> = runCatching {
        val root = JSONObject(json)
        val keys = root.keys().asSequence().toSet()
        require(keys.all { it in allowedKeys }) { "Plan contains unsupported fields" }
        require(root.optInt("schema_version", 1) == 1) { "Unsupported plan schema version" }
        require(root.optString("project") == "video_edit") { "Unsupported project type" }
        val target = when (root.optString("target", "local_engine")) {
            "local_engine" -> EditingPlan.Target.LOCAL_ENGINE
            "capcut" -> EditingPlan.Target.CAPCUT
            else -> error("Unsupported target")
        }
        val stepsJson = root.optJSONArray("steps") ?: error("steps is required")
        require(stepsJson.length() in 1..20) { "Plan must contain 1 to 20 steps" }
        val steps = (0 until stepsJson.length()).map { index ->
            val item = stepsJson.getJSONObject(index)
            val action = PlanStep.Action.entries.firstOrNull { it.name.lowercase() == item.optString("action") }
                ?: error("Unsupported action at step ${index + 1}")
            val args = mutableMapOf<String, String>()
            item.optJSONObject("arguments")?.keys()?.forEach { key ->
                require(key.length <= 64) { "Argument name too long" }
                args[key] = item.get("arguments").let { item.getJSONObject("arguments").optString(key) }
            }
            PlanStep(action, args)
        }
        EditingPlan(
            schemaVersion = 1,
            target = target,
            goal = root.optString("goal").trim().also { require(it.isNotEmpty()) { "goal is required" } },
            steps = steps,
            requiresConfirmation = root.optBoolean("requires_confirmation", true)
        ).also { PlanValidator.validate(it).getOrThrow() }
    }

    fun toJson(plan: EditingPlan): String = JSONObject().apply {
        put("schema_version", plan.schemaVersion)
        put("project", plan.project)
        put("target", if (plan.target == EditingPlan.Target.CAPCUT) "capcut" else "local_engine")
        put("goal", plan.goal)
        put("requires_confirmation", plan.requiresConfirmation)
        put("steps", JSONArray().apply {
            plan.steps.forEach { step ->
                put(JSONObject().apply {
                    put("action", step.action.name.lowercase())
                    put("arguments", JSONObject(step.arguments))
                })
            }
        })
    }.toString()
}

object PlanValidator {
    fun validate(plan: EditingPlan): Result<Unit> = runCatching {
        require(plan.schemaVersion == 1) { "Unsupported plan schema" }
        require(plan.project == "video_edit") { "Unsupported project" }
        require(plan.goal.length in 1..500) { "Goal length is invalid" }
        require(plan.steps.isNotEmpty() && plan.steps.size <= 20) { "Invalid step count" }
        plan.steps.forEach { step ->
            require(step.arguments.keys.none { it.contains("command", true) || it.contains("shell", true) }) {
                "Arbitrary command arguments are not allowed"
            }
            if (step.action == PlanStep.Action.EXPORT) require(plan.requiresConfirmation) {
                "Export must require confirmation"
            }
        }
    }
}
