package com.kyro.ev

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
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
                if (clickNodeOrParent(node)) return true
            }
        }
        return false
    }

    fun searchCurrentApp(query: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val edit = findSearchField(root) ?: return false
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, query)
        }
        val set = edit.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        if (!set) return false
        if (clickSearchButton(root)) return true
        edit.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        return true
    }

    private fun findSearchField(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val cls = node.className?.toString().orEmpty()
        val text = node.text?.toString().orEmpty()
        val desc = node.contentDescription?.toString().orEmpty()
        val hint = node.hintText?.toString().orEmpty()
        val searchable = (text + " " + desc + " " + hint).lowercase()
        if (node.isEditable && (searchable.contains("search") || cls.contains("EditText", true))) return node
        for (i in 0 until node.childCount) {
            val found = node.getChild(i)?.let { findSearchField(it) }
            if (found != null) return found
        }
        return null
    }

    private fun clickSearchButton(root: AccessibilityNodeInfo): Boolean {
        val labels = listOf("search", "go", "submit", "enter")
        for (label in labels) {
            val nodes = root.findAccessibilityNodeInfosByText(label)
            for (node in nodes) {
                val n = node.text?.toString()?.trim()?.lowercase().orEmpty()
                val d = node.contentDescription?.toString()?.trim()?.lowercase().orEmpty()
                if ((n == label || d == label) && clickNodeOrParent(node)) return true
            }
        }
        return false
    }

    fun clickFirstVideoResult(): Boolean {
        val root = rootInActiveWindow ?: return false
        val titleIds = listOf("com.google.android.youtube:id/video_title", "com.google.android.youtube:id/title")
        for (id in titleIds) {
            val nodes = root.findAccessibilityNodeInfosByViewId(id)
            for (node in nodes) if (clickNodeOrParent(node)) return true
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
        val cls = node.className?.toString().orEmpty()
        val text = node.text?.toString()?.trim().orEmpty()
        if (node.isClickable && cls.contains("TextView", true) && text.length > 8 && !isNavigationText(text)) {
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
