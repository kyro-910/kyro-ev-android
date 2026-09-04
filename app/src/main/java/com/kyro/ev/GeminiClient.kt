package com.kyro.ev

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

class GeminiClient(private val apiKey: String) {
    private class GeminiHttpException(val code: Int, message: String) : Exception(message)

    fun interpret(command: String): JSONObject {
        val prompt = """
You are E.V., a phone action planner. Convert the user's command into exactly one JSON object and no markdown.
Allowed actions:
OPEN_APP: open any installed Android app. Include string field app.
SEARCH_APP: open any installed Android app and search inside it. Include string fields app and query. Use this for commands like 'open YouTube and search Minecraft', 'search Minecraft on YouTube', or 'open Instagram and search cats'.
OPEN_YOUTUBE, SEARCH_YOUTUBE, SEARCH_AND_PLAY_LATEST, PLAY, PAUSE, RESUME, NEXT, BACK, SCROLL_DOWN, NONE.
For SEARCH_YOUTUBE include query. For SEARCH_AND_PLAY_LATEST include query. For all other actions omit query unless required.
Use the app name exactly as the user naturally says it. Never invent a package name.
User command: $command
""".trimIndent()

        val models = linkedSetOf(BuildConfig.GEMINI_MODEL, "gemini-3.7-flash", "gemini-3.6-flash", "gemini-2.5-flash").filter { it.isNotBlank() }
        var lastError: Exception? = null
        for (model in models) {
            repeat(3) { attempt ->
                try { return request(model, prompt) }
                catch (e: GeminiHttpException) {
                    lastError = e
                    if (e.code != 503 && e.code != 429 && e.code < 500) throw e
                    if (attempt < 2) Thread.sleep(1500L * (attempt + 1))
                } catch (e: Exception) { lastError = e; throw e }
            }
        }
        throw lastError ?: IllegalStateException("Gemini request failed")
    }

    private fun request(model: String, prompt: String): JSONObject {
        val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent")
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("x-goog-api-key", apiKey)
            connection.doOutput = true
            val body = JSONObject().apply {
                put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))))
                put("generationConfig", JSONObject().put("temperature", 0.1))
            }.toString()
            connection.outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw GeminiHttpException(code, "Gemini HTTP $code: $response")
            val root = JSONObject(response)
            val text = root.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text").trim()
                .removePrefix("```").removeSuffix("```").removePrefix("json").trim()
            return JSONObject(text)
        } finally { connection.disconnect() }
    }
}
