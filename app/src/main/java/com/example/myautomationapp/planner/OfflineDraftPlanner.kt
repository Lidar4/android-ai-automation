package com.example.myautomationapp.planner

/**
 * Safe offline fallback, not an AI provider. It only creates an explicit analysis draft
 * and never claims that editing operations were executed.
 */
object OfflineDraftPlanner {
    fun draft(instruction: String): EditingPlan = EditingPlan(
        goal = instruction.trim().ifEmpty { "Analyze selected media" }.take(500),
        steps = listOf(PlanStep(PlanStep.Action.ANALYZE_MEDIA)),
        requiresConfirmation = true
    )
}
