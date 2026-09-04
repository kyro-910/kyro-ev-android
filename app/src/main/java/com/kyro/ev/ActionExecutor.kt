package com.kyro.ev

import android.content.Context
import android.content.Intent
import android.net.Uri
import org.json.JSONObject
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object ActionExecutor {
    fun execute(context: Context, plan: JSONObject): String {
        return when (plan.optString("action", "NONE")) {
            "OPEN_YOUTUBE" -> { openYoutube(context); "Opening YouTube." }
            "SEARCH_YOUTUBE" -> { searchYoutube(context, plan.optString("query")); "Searching YouTube for ${plan.optString("query")}." }
            "SEARCH_AND_PLAY_LATEST" -> {
                val query = plan.optString("query").trim()
                if (query.isEmpty()) return "I need a YouTube search query."
                searchAndPlayLatest(context, query)
            }
            "PLAY" -> click("Play", "Play video", "Resume")
            "PAUSE" -> click("Pause", "Pause video")
            "RESUME" -> click("Play", "Resume")
            "NEXT" -> click("Next", "Next video")
            "BACK" -> { EvAccessibilityService.instance?.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK) == true; "Going back." }
            "SCROLL_DOWN" -> { EvAccessibilityService.instance?.scrollDown() == true; "Scrolling down." }
            else -> "I couldn't map that to an action yet."
        }
    }

    private fun click(vararg labels: String): String {
        val ok = EvAccessibilityService.instance?.clickText(*labels) == true
        return if (ok) "Done." else "I need the E.V. Android Control service enabled to control that button."
    }

    private fun openYoutube(context: Context) {
        val launch = context.packageManager.getLaunchIntentForPackage("com.google.android.youtube")
        if (launch != null) context.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        else context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun searchYoutube(context: Context, query: String) {
        val encoded = URLEncoder.encode(query, StandardCharsets.UTF_8.toString())
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=$encoded"))
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun searchAndPlayLatest(context: Context, query: String): String {
        val encoded = URLEncoder.encode(query, StandardCharsets.UTF_8.toString())
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=$encoded"))
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        EvAccessibilityService.instance?.runAfter(1800L) {
            EvAccessibilityService.instance?.clickFirstVideoResult()
        }
        return "Searching YouTube for $query and opening the latest result."
    }
}
