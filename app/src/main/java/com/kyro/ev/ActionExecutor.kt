package com.kyro.ev

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object ActionExecutor {
    fun execute(context: Context, plan: JSONObject): String {
        return when (plan.optString("action", "NONE")) {
            "OPEN_APP" -> {
                val app = plan.optString("app").trim()
                if (app.isEmpty()) return "I need an app name."
                if (openApp(context, app)) "Opening $app." else "I couldn't find $app on this phone."
            }
            "SEARCH_APP" -> {
                val app = plan.optString("app").trim()
                val query = plan.optString("query").trim()
                if (app.isEmpty() || query.isEmpty()) return "I need both an app and a search query."
                if (!openApp(context, app)) return "I couldn't find $app on this phone."
                EvAccessibilityService.instance?.runAfter(900L) {
                    EvAccessibilityService.instance?.searchCurrentApp(query)
                }
                "Opening $app and searching for $query."
            }
            "OPEN_YOUTUBE" -> { openYouTube(context); "Opening YouTube." }
            "SEARCH_YOUTUBE" -> { searchYouTube(context, plan.optString("query")); "Searching YouTube for ${plan.optString("query")}." }
            "SEARCH_AND_PLAY_LATEST" -> {
                val query = plan.optString("query").trim()
                if (query.isEmpty()) return "I need a YouTube search query."
                searchAndPlayLatest(context, query)
            }
            "PLAY" -> click("Play", "Play video", "Resume")
            "PAUSE" -> click("Pause", "Pause video")
            "RESUME" -> click("Play", "Resume")
            "NEXT" -> click("Next", "Next video")
            "BACK" -> { EvAccessibilityService.instance?.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK); "Going back." }
            "SCROLL_DOWN" -> { EvAccessibilityService.instance?.scrollDown() == true; "Scrolling down." }
            else -> "I couldn't map that command to an action yet."
        }
    }

    private fun click(vararg labels: String): String {
        val ok = EvAccessibilityService.instance?.clickText(*labels) == true
        return if (ok) "Done." else "I need the E.V. Android Control service enabled to control that button."
    }

    private fun openApp(context: Context, requestedName: String): Boolean {
        val pm = context.packageManager
        val wanted = normalize(requestedName)
        val candidates = pm.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
            PackageManager.MATCH_ALL
        )
        var best: android.content.pm.ResolveInfo? = null
        var bestScore = 0
        for (ri in candidates) {
            val label = ri.loadLabel(pm)?.toString()?.trim().orEmpty()
            val pkg = ri.activityInfo?.packageName.orEmpty()
            if (pkg == context.packageName || label.isEmpty()) continue
            val n = normalize(label)
            val score = when {
                n == wanted -> 100
                n.startsWith(wanted) -> 85
                n.contains(wanted) -> 70
                wanted.contains(n) && n.length > 2 -> 60
                normalize(pkg.substringAfterLast('.')).contains(wanted) -> 45
                else -> 0
            }
            if (score > bestScore) { bestScore = score; best = ri }
        }
        val ri = best ?: return false
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            setClassName(ri.activityInfo.packageName, ri.activityInfo.name)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return true
    }

    private fun normalize(value: String): String = value.lowercase().replace(Regex("[^a-z0-9]"), "")

    private fun openYouTube(context: Context) {
        val launch = context.packageManager.getLaunchIntentForPackage("com.google.android.youtube")
        if (launch != null) context.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        else context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun searchYouTube(context: Context, query: String) {
        val encoded = URLEncoder.encode(query, StandardCharsets.UTF_8.toString())
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=$encoded")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun searchAndPlayLatest(context: Context, query: String): String {
        searchYouTube(context, query)
        EvAccessibilityService.instance?.runAfter(1800L) {
            EvAccessibilityService.instance?.clickFirstVideoResult()
        }
        return "Searching YouTube for $query and opening the latest result."
    }
}
