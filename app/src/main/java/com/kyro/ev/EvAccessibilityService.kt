package com.kyro.ev

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityEvent

class EvAccessibilityService : AccessibilityService() {
    companion object { var instance: EvAccessibilityService? = null }

    override fun onServiceConnected() { instance = this }
    override fun onInterrupt() { }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) { }
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

    fun scrollDown(): Boolean {
        val root = rootInActiveWindow ?: return false
        return scrollNode(root)
    }

    private fun scrollNode(node: AccessibilityNodeInfo): Boolean {
        if (node.isScrollable && node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)) return true
        for (i in 0 until node.childCount) if (node.getChild(i)?.let { scrollNode(it) } == true) return true
        return false
    }
}
