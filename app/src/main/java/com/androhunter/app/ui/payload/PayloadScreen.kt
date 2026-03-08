package com.androhunter.app.ui.payload

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.androhunter.app.ui.common.HunterTopBar
import com.androhunter.app.ui.theme.*
import kotlinx.coroutines.*

private val VULN_PATTERNS = mapOf(
    "SQL_INJECTION"      to listOf("sql", "syntax error", "mysql", "sqlite", "ORA-", "SQLSTATE"),
    "XSS"                to listOf("<script>", "onerror=", "alert("),
    "LFI"                to listOf("root:x:0:0", "/bin/bash", "PHP Version"),
    "OPEN_REDIRECT"      to listOf("evil.com", "302", "301"),
    "TEMPLATE_INJECTION" to listOf("49", "7777777"),
    "COMMAND_INJECTION"  to listOf("uid=", "gid=", "groups="),
    "IDOR"               to listOf("user_id", "email", "token", "secret")
)

data class PayloadResult(
    val payload: String,
    val typeName: String,
    val status: ResultStatus,
    val detail: String,
    val logcatHit: String = ""
)

enum class ResultStatus { VULNERABLE, SUSPICIOUS, SAFE, ERROR }

@Composable
fun PayloadScreen(
    packageName: String,
    className: String,
    extraKey: String,
    dataUri: String?,
    onBack: () -> Unit
) {
    val context   = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope     = rememberCoroutineScope()
    val results   = remember { mutableStateListOf<PayloadResult>() }
    val listState = rememberLazyListState()

    var running        by remember { mutableStateOf(false) }
    var progress       by remember { mutableStateOf(0f) }
    var currentPayload by remember { mutableStateOf("") }
    var logcatEnabled  by remember { mutableStateOf(true) }
    val selected       = remember { mutableStateListOf<PayloadType>() }

    val vulnCount  = results.count { it.status == ResultStatus.VULNERABLE }
    val suspCount  = results.count { it.status == ResultStatus.SUSPICIOUS }
    val errorCount = results.count { it.status == ResultStatus.ERROR }
    val safeCount  = results.count { it.status == ResultStatus.SAFE }

    fun captureLogcat(): String = try {
        val proc = Runtime.getRuntime().exec(arrayOf("sh", "-c",
            "logcat -d -v brief 2>/dev/null | grep -i '${packageName.takeLast(15)}\\|exception\\|crash' | tail -5"))
        val out = proc.inputStream.bufferedReader().readText().trim()
        proc.waitFor(); out.take(200)
    } catch (e: Exception) { "" }

    fun clearLogcat() {
        try { Runtime.getRuntime().exec(arrayOf("sh", "-c", "logcat -c")).waitFor() }
        catch (_: Exception) {}
    }

    fun firePayload(payload: String, type: PayloadType): PayloadResult {
        return try {
            val intent = Intent().apply {
                component = android.content.ComponentName(packageName, className)
                putExtra(extraKey, payload); putExtra("data", payload)
                putExtra("url", payload);    putExtra("path", payload)
                putExtra("query", payload)
                if (!dataUri.isNullOrBlank())
                    data = Uri.parse(dataUri.replace("PAYLOAD", Uri.encode(payload)))
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Thread.sleep(600)
            val logcat  = if (logcatEnabled) captureLogcat() else ""
            val crashed = logcat.contains("FATAL EXCEPTION", ignoreCase = true)
            val patterns = VULN_PATTERNS[type.name] ?: emptyList()
            val logVuln  = patterns.any { logcat.contains(it, ignoreCase = true) }
            val status = when { crashed -> ResultStatus.VULNERABLE; logVuln -> ResultStatus.SUSPICIOUS; else -> ResultStatus.SAFE }
            val detail = when { crashed -> "CRASH / FATAL EXCEPTION"; logVuln -> "Şüpheli logcat"; else -> "Intent gönderildi" }
            PayloadResult(payload, type.label, status, detail, logcat.take(120))
        } catch (e: Exception) {
            PayloadResult(payload, type.label, ResultStatus.ERROR, e.message ?: "Hata")
        }
    }

    Column(Modifier.fillMaxSize().background(HunterBg)) {
        HunterTopBar("PAYLOAD TEST v3", onBack)

        Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            Text("pkg: $packageName", color = HunterTextDim, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
            Text("cls: ${className.substringAfterLast(".")}", color = HunterTextDim, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
            Text("key: $extraKey", color = HunterTextDim, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
        }

        Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = logcatEnabled, onCheckedChange = { logcatEnabled = it },
                colors = CheckboxDefaults.colors(checkedColor = HunterGreen, checkmarkColor = HunterBg))
            Text("Logcat İzle", color = HunterGreen, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { clearLogcat() }) {
                Text("Logcat Temizle", color = HunterTextDim, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
            }
        }

        // Payload type selector
        Column(
            Modifier.padding(horizontal = 16.dp)
                .background(HunterCard, RoundedCornerShape(6.dp))
                .border(1.dp, HunterBorder, RoundedCornerShape(6.dp))
                .padding(12.dp)
        ) {
            Text("PAYLOAD TÜRLERİ", color = HunterTextDim, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            Spacer(Modifier.height(6.dp))
            val typeList = PayloadType.values().toList()
            val mid = (typeList.size + 1) / 2
            listOf(typeList.take(mid), typeList.drop(mid)).forEach { rowTypes ->
                Row(Modifier.fillMaxWidth()) {
                    rowTypes.forEach { type ->
                        val isSel = type in selected
                        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = isSel,
                                onCheckedChange = { if (it) selected.add(type) else selected.remove(type) },
                                colors = CheckboxDefaults.colors(checkedColor = HunterGreen, checkmarkColor = HunterBg)
                            )
                            Text(type.label, color = if (isSel) HunterGreen else HunterTextDim,
                                fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        }
                    }
                    if (rowTypes.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                if (selected.isEmpty()) return@Button
                running = true; results.clear(); clearLogcat()
                scope.launch {
                    val types = selected.toList()
                    val total = types.sumOf { it.payloads.size }.toFloat()
                    var done  = 0
                    types.forEach { type ->
                        type.payloads.forEach { payload ->
                            currentPayload = payload
                            val r = withContext(Dispatchers.IO) { firePayload(payload, type) }
                            results.add(r); done++; progress = done / total
                            if (results.isNotEmpty()) listState.scrollToItem(results.size - 1)
                        }
                    }
                    running = false; currentPayload = ""
                }
            },
            modifier = Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 16.dp),
            enabled  = !running && selected.isNotEmpty(),
            colors   = ButtonDefaults.buttonColors(containerColor = HunterGreen),
            shape    = RoundedCornerShape(4.dp)
        ) {
            if (running) {
                CircularProgressIndicator(color = HunterBg, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(currentPayload.take(30), color = HunterBg, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            } else {
                Text("[ PAYLOAD BAŞLAT ]", color = HunterBg, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
        }

        if (running) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                color = HunterGreen, trackColor = HunterBorder
            )
        }

        AnimatedVisibility(visible = results.isNotEmpty()) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PBox("VULN $vulnCount",  HunterRed,     Modifier.weight(1f))
                    PBox("SUSP $suspCount",  HunterYellow,  Modifier.weight(1f))
                    PBox("SAFE $safeCount",  HunterGreen,   Modifier.weight(1f))
                    PBox("ERR  $errorCount", HunterTextDim, Modifier.weight(1f))
                }
                if (!running) {
                    val summary = buildString {
                        appendLine("=== AndroHunter Payload Report ===")
                        appendLine("Target: $packageName :: ${className.substringAfterLast(".")}")
                        appendLine("Key: $extraKey | Total: ${results.size}")
                        appendLine("VULN:$vulnCount SUSP:$suspCount SAFE:$safeCount ERR:$errorCount")
                        appendLine("")
                        results.filter { it.status != ResultStatus.SAFE }.forEach {
                            appendLine("[${it.status}] ${it.typeName}: ${it.payload}")
                            appendLine("  → ${it.detail}")
                        }
                    }
                    TextButton(onClick = { clipboard.setText(AnnotatedString(summary)) },
                        modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Share, null, tint = HunterBlue, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Raporu Kopyala", color = HunterBlue, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    }
                }
            }
        }

        LazyColumn(state = listState, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
            items(results) { r ->
                PayloadResultRow(r)
                Spacer(Modifier.height(3.dp))
            }
        }
    }
}

@Composable
private fun PayloadResultRow(r: PayloadResult) {
    val color = when (r.status) {
        ResultStatus.VULNERABLE -> HunterRed
        ResultStatus.SUSPICIOUS -> HunterYellow
        ResultStatus.SAFE       -> HunterGreen
        ResultStatus.ERROR      -> HunterTextDim
    }
    val pulse = rememberInfiniteTransition(label = "p")
    val alpha by pulse.animateFloat(
        initialValue = 1f,
        targetValue  = if (r.status == ResultStatus.VULNERABLE) 0.3f else 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "a"
    )

    Column(
        Modifier.fillMaxWidth()
            .background(
                if (r.status == ResultStatus.VULNERABLE) HunterRed.copy(alpha = 0.07f) else HunterCard,
                RoundedCornerShape(4.dp))
            .border(1.dp,
                if (r.status == ResultStatus.VULNERABLE) color.copy(alpha = alpha) else HunterBorder,
                RoundedCornerShape(4.dp))
            .padding(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Surface(color = color.copy(alpha = 0.15f), shape = RoundedCornerShape(3.dp)) {
                Text(r.status.name.take(4), color = color, fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold, fontSize = 9.sp,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
            }
            Text(r.typeName, color = HunterTextDim, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
        }
        Spacer(Modifier.height(3.dp))
        Text(r.payload, color = color, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
        Text(r.detail,  color = HunterTextDim, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
        if (r.logcatHit.isNotBlank()) {
            Spacer(Modifier.height(2.dp))
            Text("logcat: ${r.logcatHit}", color = HunterBlue,
                fontFamily = FontFamily.Monospace, fontSize = 9.sp,
                modifier = Modifier.background(HunterSurface, RoundedCornerShape(3.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp))
        }
    }
}

@Composable
private fun PBox(label: String, color: Color, modifier: Modifier) {
    Surface(color = color.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp), modifier = modifier) {
        Text(label, color = color, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
            fontSize = 11.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp))
    }
}
