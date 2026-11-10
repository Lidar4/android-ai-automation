package com.example.myautomationapp.ai

import com.example.myautomationapp.planner.EditingPlan
import com.example.myautomationapp.security.ApiKeyStore

interface AiPlanner {
    val providerName: String
    fun createPlan(instruction: String, callback: (Result<EditingPlan>) -> Unit)
}

class UnconfiguredPlanner(private val provider: String, private val keyStore: ApiKeyStore) : AiPlanner {
    override val providerName: String = provider
    override fun createPlan(instruction: String, callback: (Result<EditingPlan>) -> Unit) {
        if (!keyStore.isConfigured(provider)) {
            callback(Result.failure(IllegalStateException("$provider is not configured. Add your API key in Provider Settings.")))
        } else {
            callback(Result.failure(UnsupportedOperationException("$provider integration is not enabled in this build yet.")))
        }
    }
}
