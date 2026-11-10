package com.example.myautomationapp

import com.example.myautomationapp.planner.EditingPlanCodec
import com.example.myautomationapp.planner.PlanValidator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditingPlanTest {
    @Test fun rejectsUnknownActions() {
        val result = EditingPlanCodec.fromJson("""{"schema_version":1,"project":"video_edit","goal":"x","steps":[{"action":"run_shell"}]}""")
        assertTrue(result.isFailure)
    }

    @Test fun acceptsValidatedAnalysisPlan() {
        val result = EditingPlanCodec.fromJson("""{"schema_version":1,"project":"video_edit","target":"local_engine","goal":"Analyze","steps":[{"action":"analyze_media"}],"requires_confirmation":true}""")
        assertTrue("decoded plan error: ${result.exceptionOrNull()?.message}", result.isSuccess)
        assertTrue(PlanValidator.validate(result.getOrThrow()).isSuccess)
    }

    @Test fun rejectsExportWithoutConfirmation() {
        val plan = com.example.myautomationapp.planner.EditingPlan(
            goal = "export", steps = listOf(com.example.myautomationapp.planner.PlanStep(com.example.myautomationapp.planner.PlanStep.Action.EXPORT)), requiresConfirmation = false
        )
        assertFalse(PlanValidator.validate(plan).isSuccess)
    }
}
