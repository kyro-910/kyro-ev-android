package com.kyro.ev

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityEvent

class EvAccessibilityService : AccessibilityService() {
    companion object {
        var instance: EvAccessibilityService? = null
    }

    override fun onServiceConnected() { instance = this }
    override fun onInterrupt() {}
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onDestroy() { if (instance === this) instance = null; super.onDestroy() }

    fun clickText(vararg labels: String): Boolean {
        val root = rootInActiveWindow ?: return false
        for (label in labels) {
            val nodes = root.findAccessibilityNodeInfosByText(label)
            for (node in nodes) {
                if (node.isClickable && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
                var parent = node.parent
                repeat(3) {
                    if (parent != null && parent.isClickable && parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
                    parent = parent?.parent
                }
            }
        }
        return false
    }

    fun clickFirstVideoResult(): Boolean {
        val root = rootInActiveWindow ?: return false
        val titleIds = listOf(
            "com.google.android.youtube:id/video_title",
            "com.google.android.youtube:id/title"
        )
        for (id in titleIds) {
            val nodes = root.findAccessibilityNodeInfosByViewId(id)
            for (node in nodes) {
                if (clickNodeOrParent(node)) return true
            }
        }
        return clickFirstLikelyVideo(root)
    }

    private fun clickNodeOrParent(node: AccessibilityNodeInfo): Boolean {
        if (node.isClickable && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
        var parent = node.parent
        repeat(6) {
            if (parent != null && parent.isClickable && parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
            parent = parent?.parent
        }
        return false
    }

    private fun clickFirstLikelyVideo(node: AccessibilityNodeInfo): Boolean {
        val className = node.className?.toString().orEmpty()
        val text = node.text?.toString()?.trim().orEmpty()
        if (node.isClickable && className.contains("TextView", ignoreCase = true) && text.length > 8 && !isNavigationText(text)) {
            if (node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (clickFirstLikelyVideo(child)) return true
        }
        return false
    }

    private fun isNavigationText(text: String): Boolean = when (text.lowercase()) {
        "home", "shorts", "subscriptions", "library", "you", "search", "back", "menu" -> true
        else -> false
    }

    fun runAfter(delayMs: Long, action: () -> Unit) {
        android.os.Handler(mainLooper).postDelayed(action, delayMs)
    }

    fun scrollDown(): Boolean {
        val root = rootInActiveWindow ?: return false
        return scrollNode(root)
    }

    private fun scrollNode(node: AccessibilityNodeInfo): Boolean {
        if (node.isScrollable && node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)) return true
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (scrollNode(child)) return true
        }
        return false
    }
}
