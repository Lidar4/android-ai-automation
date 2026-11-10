package com.example.myautomationapp.ai

import com.example.myautomationapp.planner.EditingPlan
import com.example.myautomationapp.planner.EditingPlanCodec
import com.example.myautomationapp.security.ApiKeyStore
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class RemoteAiPlanner(private val provider: String, private val keyStore: ApiKeyStore) : AiPlanner {
    override val providerName: String = provider
    private val executor = Executors.newSingleThreadExecutor()

    override fun createPlan(instruction: String, callback: (Result<EditingPlan>) -> Unit) {
        val key = keyStore.read(provider)
        if (key == null) {
            callback(Result.failure(IllegalStateException("$provider is not configured. Add its API key in Provider Settings.")))
            return
        }
        if (instruction.trim().isEmpty()) {
            callback(Result.failure(IllegalArgumentException("Describe the desired edit first")))
            return
        }
        executor.execute {
            callback(runCatching { requestPlan(key, instruction) })
        }
    }

    private fun requestPlan(key: String, instruction: String): EditingPlan {
        val schema = """Return only JSON with schema_version=1, project=video_edit, target either local_engine or capcut, goal string, steps array, requires_confirmation=true. Each step action must be one of analyze_media, trim, merge, resize, add_subtitles, add_background_music, launch_capcut, open_draft, export. Never add shell commands or unsupported UI controls."""
        val response = when (provider) {
            "openai" -> postOpenAi(key, schema, instruction)
            "gemini" -> postGemini(key, schema, instruction)
            else -> error("Unsupported provider")
        }
        val json = extractJson(response)
        return EditingPlanCodec.fromJson(json).getOrThrow()
    }

    private fun postOpenAi(key: String, schema: String, instruction: String): String {
        val body = JSONObject().apply {
            put("model", "gpt-4o-mini")
            put("temperature", 0.1)
            put("messages", JSONArray().apply {
                put(JSONObject().put("role", "system").put("content", schema))
                put(JSONObject().put("role", "user").put("content", instruction))
            })
            put("response_format", JSONObject().put("type", "json_object"))
        }
        val json = post("https://api.openai.com/v1/chat/completions", key, body.toString(), null)
        return JSONObject(json).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
    }

    private fun postGemini(key: String, schema: String, instruction: String): String {
        val body = JSONObject().put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", "$schema\nUser request: $instruction")))))
        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$key"
        val json = post(endpoint, null, body.toString(), null)
        return JSONObject(json).getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
    }

    private fun post(endpoint: String, bearer: String?, body: String, unused: String?): String {
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 45_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            if (bearer != null) setRequestProperty("Authorization", "Bearer $bearer")
        }
        connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
        val text = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
        if (connection.responseCode !in 200..299) error("$provider request failed (${connection.responseCode})")
        return text
    }

    private fun extractJson(text: String): String {
        val fenced = Regex("```(?:json)?\\s*(\\{.*?})\\s*```", RegexOption.DOT_MATCHES_ALL).find(text)?.groupValues?.get(1)
        return (fenced ?: text).trim()
    }
}
