package com.androhunter.app.monitor

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent

object AccessLog {
    val entries = mutableListOf<AccessEntry>()
}

data class AccessEntry(
    val time: String,
    val packageName: String,
    val eventType: String,
    val text: String
)

class AccessibilityMonitorService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val entry = AccessEntry(
            time        = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date()),
            packageName = event.packageName?.toString() ?: "?",
            eventType   = AccessibilityEvent.eventTypeToString(event.eventType),
            text        = event.text?.joinToString(" ") ?: ""
        )
        AccessLog.entries.add(0, entry)
        if (AccessLog.entries.size > 500) AccessLog.entries.removeAt(500)
    }

    override fun onInterrupt() {}
}
