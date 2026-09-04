package com.kyro.ev

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

class GeminiClient(private val apiKey: String) {
    fun interpret(command: String): JSONObject {
        val prompt = """
You are E.V., a phone action planner. Convert the user's command into exactly one JSON object and no markdown.
Allowed actions: OPEN_YOUTUBE, SEARCH_YOUTUBE, PLAY, PAUSE, RESUME, NEXT, BACK, SCROLL_DOWN, NONE.
For SEARCH_YOUTUBE include a string field named query. For all other actions omit query.
User command: $command
""".trimIndent()

        val url = URL("https://generativelanguage.googleapis.com/v1beta/models/${BuildConfig.GEMINI_MODEL}:generateContent")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("x-goog-api-key", apiKey)
        connection.doOutput = true

        val body = JSONObject().apply {
            put(
                "contents",
                JSONArray().put(
                    JSONObject().put(
                        "parts",
                        JSONArray().put(JSONObject().put("text", prompt))
                    )
                )
            )
            put("generationConfig", JSONObject().put("temperature", 0.1))
        }.toString()

        connection.outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }
        val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
        val response = stream.bufferedReader().use { it.readText() }
        if (connection.responseCode !in 200..299) {
            error("Gemini HTTP ${connection.responseCode}: $response")
        }

        val root = JSONObject(response)
        val text = root.getJSONArray("candidates").getJSONObject(0)
            .getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
            .trim().removePrefix("```").removeSuffix("```").removePrefix("json").trim()
        return JSONObject(text)
    }
}
