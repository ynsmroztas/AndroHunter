package com.androhunter.app.ui.dex

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

data class DexFinding(val category: String, val value: String, val severity: String)

private val PATTERNS = mapOf(
    "API Key / Token"    to listOf(
        Regex("""(?i)(api[_\-]?key|apikey|access_token|auth_token)[=:\s"']+[A-Za-z0-9_\-]{16,}"""),
        Regex("""(?i)(bearer)[=:\s"']+[A-Za-z0-9_\-.]{20,}""")
    ),
    "AWS Credentials"    to listOf(Regex("""AKIA[0-9A-Z]{16}""")),
    "Private Key"        to listOf(Regex("""-----BEGIN [A-Z ]*PRIVATE KEY-----""")),
    "Hardcoded Password" to listOf(
        Regex("""(?i)(password|passwd|pwd)[=:\s"']+[^\s"']{6,}""")
    ),
    "Firebase URL"       to listOf(Regex("""https://[a-z0-9\-]+\.firebaseio\.com""")),
    "HTTP Endpoint"      to listOf(Regex("""https?://[a-zA-Z0-9./_\-?=&%:]{15,}""")),
    "Internal IP"        to listOf(
        Regex("""(192\.168\.|10\.|172\.1[6-9]\.)\d+\.\d+(:\d+)?""")
    ),
    "Debug Flag"         to listOf(
        Regex("""(?i)(debug|staging|dev)[_\-]?(mode|url|host)[=:\s"']+true""")
    ),
    "SQL Query"          to listOf(Regex("""(?i)SELECT .{1,50} FROM """))
)

@Composable
fun DexAnalyzerScreen(packageName: String, onBack: () -> Unit) {
    val context   = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope     = rememberCoroutineScope()

    var findings  by remember { mutableStateOf<List<DexFinding>>(emptyList()) }
    var analyzing by remember { mutableStateOf(false) }
    var progress  by remember { mutableStateOf("") }
    var filterCat by remember { mutableStateOf<String?>(null) }

    fun analyze() {
        analyzing = true
        findings  = emptyList()
        scope.launch(Dispatchers.IO) {
            val found = mutableListOf<DexFinding>()
            try {
                val apkPath = context.packageManager.getApplicationInfo(packageName, 0).sourceDir
                withContext(Dispatchers.Main) { progress = "APK taranıyor..." }

                val proc  = Runtime.getRuntime().exec(arrayOf("sh", "-c",
                    "strings '$apkPath' 2>/dev/null | sort -u | head -8000"))
                val lines = proc.inputStream.bufferedReader().readLines()
                proc.waitFor()

                withContext(Dispatchers.Main) { progress = "${lines.size} string analiz ediliyor..." }

                lines.forEach { line ->
                    if (line.length < 8 || line.length > 500) return@forEach
                    PATTERNS.forEach { (category, patterns) ->
                        patterns.forEach { pattern ->
                            if (pattern.containsMatchIn(line) && found.none { it.value == line }) {
                                val sev = when (category) {
                                    "API Key / Token", "AWS Credentials",
                                    "Private Key", "Hardcoded Password" -> "HIGH"
                                    "Firebase URL", "Internal IP",
                                    "Debug Flag"                        -> "MEDIUM"
                                    else                                  -> "INFO"
                                }
                                found.add(DexFinding(category, line.take(200), sev))
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                found.add(DexFinding("ERROR", e.message ?: "Analiz hatası", "INFO"))
            }
            withContext(Dispatchers.Main) {
                findings  = found.sortedByDescending { it.severity }
                analyzing = false
                progress  = ""
            }
        }
    }

    val filtered   = findings.filter { filterCat == null || it.category == filterCat }
    val categories = findings.map { it.category }.distinct()
    val highCount  = findings.count { it.severity == "HIGH" }
    val medCount   = findings.count { it.severity == "MEDIUM" }

    Column(Modifier.fillMaxSize().background(HunterBg)) {
        HunterTopBar("DEX ANALYZER", onBack)
        Text("  $packageName", color = HunterTextDim, fontFamily = FontFamily.Monospace,
            fontSize = 11.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))

        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick  = { analyze() },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                enabled  = !analyzing,
                colors   = ButtonDefaults.buttonColors(containerColor = HunterGreen),
                shape    = RoundedCornerShape(4.dp)
            ) {
                if (analyzing) {
                    CircularProgressIndicator(color = HunterBg, strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(progress, color = HunterBg, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                } else {
                    Text("[ DEX TARA ]", color = HunterBg, fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold)
                }
            }

            if (findings.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SBox("HIGH $highCount",   HunterRed,    Modifier.weight(1f))
                    SBox("MED  $medCount",    HunterYellow, Modifier.weight(1f))
                    SBox("INFO ${findings.size - highCount - medCount}", HunterTextDim, Modifier.weight(1f))
                }
                TextButton(onClick = {
                    clipboard.setText(AnnotatedString(
                        findings.joinToString("\n") { "[${it.severity}] ${it.category}: ${it.value}" }
                    ))
                }) {
                    Icon(Icons.Default.Share, null, tint = HunterBlue, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Tüm bulgular kopyala", color = HunterBlue,
                        fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilterChip(selected = filterCat == null, onClick = { filterCat = null },
                        label = { Text("ALL", fontFamily = FontFamily.Monospace, fontSize = 9.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = HunterCard, labelColor = HunterTextDim,
                            selectedContainerColor = HunterGreen.copy(alpha = 0.2f),
                            selectedLabelColor = HunterGreen))
                    categories.take(4).forEach { cat ->
                        FilterChip(selected = filterCat == cat,
                            onClick = { filterCat = if (filterCat == cat) null else cat },
                            label = { Text(cat.take(8), fontFamily = FontFamily.Monospace, fontSize = 9.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = HunterCard, labelColor = HunterTextDim,
                                selectedContainerColor = HunterYellow.copy(alpha = 0.2f),
                                selectedLabelColor = HunterYellow))
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
            items(filtered) { f ->
                val color = when (f.severity) {
                    "HIGH" -> HunterRed; "MEDIUM" -> HunterYellow; else -> HunterTextDim
                }
                Column(
                    Modifier.fillMaxWidth()
                        .background(HunterCard, RoundedCornerShape(4.dp))
                        .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                        .padding(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(color = color.copy(alpha = 0.15f), shape = RoundedCornerShape(3.dp)) {
                            Text(f.severity, color = color, fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold, fontSize = 8.sp,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                        }
                        Text(f.category, color = color, fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { clipboard.setText(AnnotatedString(f.value)) },
                            modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Share, null, tint = HunterTextDim,
                                modifier = Modifier.size(12.dp))
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(f.value, color = HunterGreen, fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp)
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun SBox(label: String, color: Color, modifier: Modifier) {
    Surface(color = color.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp), modifier = modifier) {
        Text(label, color = color, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
            fontSize = 11.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp))
    }
}
