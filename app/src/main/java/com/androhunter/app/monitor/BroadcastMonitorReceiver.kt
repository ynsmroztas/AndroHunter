package com.androhunter.app.monitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

object BroadcastLog {
    val entries = mutableListOf<BroadcastEntry>()
}

data class BroadcastEntry(
    val timestamp: String,
    val action: String,
    val extras: Map<String, String>
)

class BroadcastMonitorReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val extras = mutableMapOf<String, String>()
        intent.extras?.keySet()?.forEach { key ->
            extras[key] = intent.extras?.get(key)?.toString() ?: "null"
        }
        val entry = BroadcastEntry(
            timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                .format(java.util.Date()),
            action    = intent.action ?: "unknown",
            extras    = extras
        )
        BroadcastLog.entries.add(0, entry)
        Log.d("AndroHunter", "Broadcast: ${entry.action} extras=$extras")
    }
}
