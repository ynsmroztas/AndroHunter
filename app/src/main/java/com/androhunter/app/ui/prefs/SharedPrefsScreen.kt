package com.androhunter.app.ui.prefs

import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.androhunter.app.ui.common.HunterTopBar
import com.androhunter.app.ui.theme.*
import kotlinx.coroutines.*

data class PrefEntry(
    val file: String,
    val key: String,
    val value: String,
    val isSensitive: Boolean
)

private val SENSITIVE_KEYS = listOf(
    "token","password","passwd","secret","key","auth","session",
    "credential","api","jwt","bearer","cookie","hash","pin","code"
)

@Composable
fun SharedPrefsScreen(onBack: () -> Unit) {
    val context   = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope     = rememberCoroutineScope()

    val installedApps = remember {
        context.packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { it.sourceDir.isNotBlank() }
            .sortedBy { context.packageManager.getApplicationLabel(it).toString() }
    }

    var selectedPkg by remember { mutableStateOf<String?>(null) }
    var scanning    by remember { mutableStateOf(false) }
    var entries     by remember { mutableStateOf<List<PrefEntry>>(emptyList()) }
    var filterSens  by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    fun readPrefs(pkg: String) {
        scanning = true; entries = emptyList()
        scope.launch(Dispatchers.IO) {
            val found = mutableListOf<PrefEntry>()
            try {
                // Try reading via run-as (debug apps) or dumpsys
                val proc = Runtime.getRuntime().exec(arrayOf("sh", "-c",
                    "run-as $pkg find /data/data/$pkg/shared_prefs -name '*.xml' 2>/dev/null | head -20"
                ))
                val files = proc.inputStream.bufferedReader().readLines()
                proc.waitFor()

                if (files.isNotEmpty()) {
                    files.forEach { filePath ->
                        val catProc = Runtime.getRuntime().exec(arrayOf("sh","-c",
                            "run-as $pkg cat '$filePath' 2>/dev/null"))
                        val xml = catProc.inputStream.bufferedReader().readText()
                        catProc.waitFor()
                        val fname = filePath.substringAfterLast("/")
                        // Parse XML entries
                        Regex("""<(string|boolean|int|long|float)\s+name="([^"]+)"[^>]*>?([^<]*)<""")
                            .findAll(xml).forEach { m ->
                                val key   = m.groupValues[2]
                                val value = m.groupValues[3].trim()
                                val sens  = SENSITIVE_KEYS.any { key.contains(it, ignoreCase = true) }
                                found.add(PrefEntry(fname, key, value, sens))
                            }
                    }
                } else {
                    // Fallback: dumpsys
                    val d = Runtime.getRuntime().exec(arrayOf("sh","-c",
                        "dumpsys package $pkg 2>/dev/null | grep -i 'pref\\|token\\|session' | head -30"))
                    val lines = d.inputStream.bufferedReader().readLines()
                    d.waitFor()
                    lines.forEach { line ->
                        if (line.contains("=")) {
                            val (k,v) = line.trim().split("=",limit=2).let{
                                it[0].trim() to (it.getOrNull(1)?.trim() ?: "")
                            }
                            val sens = SENSITIVE_KEYS.any { k.contains(it, ignoreCase=true) }
                            found.add(PrefEntry("dumpsys", k, v, sens))
                        }
                    }
                    if (found.isEmpty())
                        found.add(PrefEntry("info","status",
                            "Uygulama debug değil veya izin gerekiyor. Root gerekebilir.", false))
                }
            } catch (e: Exception) {
                found.add(PrefEntry("error","exception", e.message ?: "Unknown", false))
            }
            withContext(Dispatchers.Main) { entries = found; scanning = false }
        }
    }

    val filtered = entries.filter {
        (!filterSens || it.isSensitive) &&
        (searchQuery.isBlank() || it.key.contains(searchQuery,true) || it.value.contains(searchQuery,true))
    }
    val sensCount = entries.count { it.isSensitive }

    Column(Modifier.fillMaxSize().background(HunterBg)) {
        HunterTopBar("SHARED PREFS", onBack)

        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // App selector
            Text("UYGULAMA SEÇ", color = HunterDim, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            LazyColumn(Modifier.height(160.dp)) {
                items(installedApps.take(80)) { app ->
                    val label = context.packageManager.getApplicationLabel(app).toString()
                    val isSel = selectedPkg == app.packageName
                    Row(
                        Modifier.fillMaxWidth()
                            .background(if(isSel) HunterGreen.copy(alpha=0.1f) else HunterCard, RoundedCornerShape(4.dp))
                            .clickable(indication=null, interactionSource=remember{androidx.compose.foundation.interaction.MutableInteractionSource()}) {
                                selectedPkg = app.packageName
                            }.padding(horizontal=10.dp, vertical=6.dp),
                        horizontalArrangement=Arrangement.SpaceBetween,
                        verticalAlignment=Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(label, color=if(isSel)HunterGreen else HunterText,
                                fontFamily=FontFamily.Monospace, fontWeight=FontWeight.Bold, fontSize=11.sp)
                            Text(app.packageName, color=HunterDim, fontFamily=FontFamily.Monospace, fontSize=9.sp)
                        }
                        RadioButton(selected=isSel, onClick={selectedPkg=app.packageName},
                            colors=RadioButtonDefaults.colors(selectedColor=HunterGreen))
                    }
                    Spacer(Modifier.height(2.dp))
                }
            }

            Button(
                onClick = { selectedPkg?.let { readPrefs(it) } },
                enabled = selectedPkg != null && !scanning,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor=HunterGreen.copy(alpha=0.15f)),
                shape = RoundedCornerShape(4.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, HunterGreen.copy(alpha=0.5f))
            ) {
                if (scanning) {
                    CircularProgressIndicator(color=HunterGreen, strokeWidth=2.dp, modifier=Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text(if(scanning) "OKUNUYOR..." else "[ OKU ]", color=HunterGreen,
                    fontFamily=FontFamily.Monospace, fontWeight=FontWeight.Bold, fontSize=12.sp)
            }
        }

        if (entries.isNotEmpty()) {
            Column(Modifier.padding(horizontal=12.dp), verticalArrangement=Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp), verticalAlignment=Alignment.CenterVertically) {
                    Surface(color=HunterRed.copy(alpha=0.15f), shape=RoundedCornerShape(4.dp)) {
                        Text("⚠ $sensCount HASSASİYET", color=HunterRed, fontFamily=FontFamily.Monospace,
                            fontWeight=FontWeight.Bold, fontSize=10.sp,
                            modifier=Modifier.padding(horizontal=8.dp, vertical=4.dp))
                    }
                    Surface(color=HunterDim.copy(alpha=0.15f), shape=RoundedCornerShape(4.dp)) {
                        Text("TOPLAM ${entries.size}", color=HunterDim, fontFamily=FontFamily.Monospace,
                            fontSize=10.sp, modifier=Modifier.padding(horizontal=8.dp, vertical=4.dp))
                    }
                }
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Checkbox(checked=filterSens, onCheckedChange={filterSens=it},
                        colors=CheckboxDefaults.colors(checkedColor=HunterRed))
                    Text("Sadece hassas", color=HunterDim, fontFamily=FontFamily.Monospace, fontSize=11.sp)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick={
                        clipboard.setText(AnnotatedString(
                            entries.joinToString("\n"){"[${it.file}] ${it.key}=${it.value}"}
                        ))
                    }) { Text("Kopyala", color=HunterBlue, fontFamily=FontFamily.Monospace, fontSize=11.sp) }
                }
                OutlinedTextField(
                    value=searchQuery, onValueChange={searchQuery=it},
                    label={Text("Ara...", fontFamily=FontFamily.Monospace, fontSize=11.sp)},
                    modifier=Modifier.fillMaxWidth(),
                    singleLine=true,
                    textStyle=androidx.compose.ui.text.TextStyle(color=HunterGreen, fontFamily=FontFamily.Monospace),
                    colors=OutlinedTextFieldDefaults.colors(
                        focusedBorderColor=HunterGreen, unfocusedBorderColor=HunterBorder,
                        focusedLabelColor=HunterGreen, unfocusedLabelColor=HunterDim)
                )
            }

            HorizontalDivider(color=HunterBorder, modifier=Modifier.padding(vertical=6.dp))

            LazyColumn(contentPadding=PaddingValues(horizontal=12.dp, vertical=4.dp)) {
                items(filtered) { e ->
                    val color = if(e.isSensitive) HunterRed else HunterGreen
                    Column(
                        Modifier.fillMaxWidth()
                            .background(if(e.isSensitive) HunterRed.copy(alpha=0.06f) else HunterCard, RoundedCornerShape(6.dp))
                            .border(1.dp, color.copy(alpha=0.3f), RoundedCornerShape(6.dp))
                            .padding(10.dp)
                    ) {
                        Row(verticalAlignment=Alignment.CenterVertically,
                            horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                            if(e.isSensitive) Surface(color=HunterRed.copy(alpha=0.15f), shape=RoundedCornerShape(3.dp)) {
                                Text("⚠ HASSASİYET", color=HunterRed, fontFamily=FontFamily.Monospace,
                                    fontWeight=FontWeight.Bold, fontSize=8.sp,
                                    modifier=Modifier.padding(horizontal=5.dp,vertical=2.dp))
                            }
                            Surface(color=HunterSurface, shape=RoundedCornerShape(3.dp)) {
                                Text(e.file, color=HunterDim, fontFamily=FontFamily.Monospace, fontSize=8.sp,
                                    modifier=Modifier.padding(horizontal=4.dp,vertical=2.dp))
                            }
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick={clipboard.setText(AnnotatedString("${e.key}=${e.value}"))},
                                modifier=Modifier.size(20.dp)) {
                                Text("⎘", color=HunterDim, fontSize=14.sp)
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(e.key, color=color, fontFamily=FontFamily.Monospace,
                            fontWeight=FontWeight.Bold, fontSize=12.sp)
                        Text(e.value.ifBlank{"(boş)"}, color=color.copy(alpha=0.8f),
                            fontFamily=FontFamily.Monospace, fontSize=11.sp)
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}
