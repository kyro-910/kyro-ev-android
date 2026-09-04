package com.kyro.ev

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val executor = Executors.newSingleThreadExecutor()
    private val actionExecutor = Executors.newSingleThreadExecutor()
    private lateinit var status: TextView
    private lateinit var input: TextView
    private var recognizer: SpeechRecognizer? = null
    private val prefs by lazy { getSharedPreferences("ev_settings", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 10)
        }
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 48, 32, 32)
            gravity = Gravity.CENTER_HORIZONTAL
        }
        val title = TextView(this).apply { text = "E.V."; textSize = 34f; gravity = Gravity.CENTER }
        input = TextView(this).apply { text = "Say something…"; textSize = 20f; setPadding(0, 40, 0, 24) }
        status = TextView(this).apply { text = "Ready. Enable Android Control for YouTube buttons."; textSize = 16f }
        val talk = Button(this).apply { text = "🎙 TALK TO E.V."; setOnClickListener { listen() } }
        val apiKey = Button(this).apply { text = "Set Gemini API Key"; setOnClickListener { configureGeminiKey() } }
        val accessibility = Button(this).apply {
            text = "Enable Android Control"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        }
        root.addView(title)
        root.addView(input)
        root.addView(talk)
        root.addView(apiKey)
        root.addView(accessibility)
        root.addView(status)
        setContentView(root)
    }

    private fun listen() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            toast("Speech recognition isn't available on this phone.")
            return
        }
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : android.speech.RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { status.text = "Listening…" }
                override fun onBeginningOfSpeech() { }
                override fun onRmsChanged(rmsdB: Float) { }
                override fun onBufferReceived(buffer: ByteArray?) { }
                override fun onEndOfSpeech() { status.text = "Thinking…" }
                override fun onError(error: Int) { status.text = "Voice error: $error" }
                override fun onResults(results: Bundle?) {
                    val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                    input.text = text
                    handleCommand(text)
                }
                override fun onPartialResults(partialResults: Bundle?) { }
                override fun onEvent(eventType: Int, params: Bundle?) { }
            })
            startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            })
        }
    }

    private fun getGeminiApiKey(): String =
        BuildConfig.GEMINI_API_KEY.ifBlank { prefs.getString("gemini_api_key", "").orEmpty() }.trim()

    private fun configureGeminiKey(onSaved: (() -> Unit)? = null) {
        val edit = EditText(this).apply {
            hint = "Paste your Gemini API key"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            setSingleLine(true)
            setText(prefs.getString("gemini_api_key", ""))
        }
        android.app.AlertDialog.Builder(this)
            .setTitle("Gemini API Key")
            .setMessage("Your key is stored only on this phone. Do not share it with anyone.")
            .setView(edit)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                val key = edit.text.toString().trim()
                if (key.isBlank()) {
                    status.text = "Gemini API key is empty."
                } else {
                    prefs.edit().putString("gemini_api_key", key).apply()
                    status.text = "Gemini API key saved on this phone."
                    onSaved?.invoke()
                }
            }
            .show()
    }

    private fun handleCommand(text: String) {
        val apiKey = getGeminiApiKey()
        if (apiKey.isBlank()) {
            status.text = "Gemini API key needed. Tap Set Gemini API Key."
            configureGeminiKey()
            return
        }
        executor.execute {
            try {
                val plan = GeminiClient(apiKey).interpret(text)
                val result = ActionExecutor.execute(this, plan)
                runOnUiThread { status.text = result }
            } catch (e: Exception) {
                runOnUiThread { status.text = "E.V. error: ${e.message}" }
            }
        }
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    override fun onDestroy() {
        recognizer?.destroy()
        executor.shutdownNow()
        actionExecutor.shutdownNow()
        super.onDestroy()
    }
}
