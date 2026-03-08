package com.androhunter.app.ui.intent

import android.content.ComponentName
import android.content.Intent
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.androhunter.app.core.Strings
import com.androhunter.app.ui.common.HunterTopBar
import com.androhunter.app.ui.theme.*
import kotlinx.coroutines.*

data class ShellResult(val cmd: String, val output: String, val isError: Boolean)

@Composable
fun IntentFuzzerScreen(
    packageName: String,
    onPayloadTest: (pkg: String, cls: String, key: String, uri: String?) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val pm      = context.packageManager
    val s       = Strings.get()

    val components = remember {
        buildList {
            try {
                val info = pm.getPackageInfo(packageName,
                    PackageManager.GET_ACTIVITIES or PackageManager.GET_RECEIVERS or PackageManager.GET_SERVICES)
                info.activities?.forEach { add(Triple("Activity", it.name, it.exported)) }
                info.receivers?.forEach  { add(Triple("Receiver", it.name, it.exported)) }
                info.services?.forEach   { add(Triple("Service",  it.name, it.exported)) }
            } catch (e: Exception) {}
        }
    }

    var extraKey    by remember { mutableStateOf("data") }
    var extraValue  by remember { mutableStateOf("test") }
    var dataUri     by remember { mutableStateOf("") }
    var statusMsg   by remember { mutableStateOf("") }
    var expanded    by remember { mutableStateOf<String?>(null) }

    // Shell panel state
    var shellVisible  by remember { mutableStateOf(false) }
    var shellInput    by remember { mutableStateOf("") }
    val shellResults  = remember { mutableStateListOf<ShellResult>() }
    val shellState    = rememberLazyListState()
    val scope         = rememberCoroutineScope()

    fun runShell(cmd: String, targetPkg: String = "") {
        val fullCmd = if (targetPkg.isNotBlank()) {
            "am broadcast -a $cmd -p $targetPkg 2>&1; " +
            "am start -n $targetPkg/$cmd 2>&1; " +
            "run-as $targetPkg $cmd 2>&1"
        } else cmd

        scope.launch(Dispatchers.IO) {
            try {
                val proc   = Runtime.getRuntime().exec(arrayOf("sh", "-c", fullCmd))
                val out    = proc.inputStream.bufferedReader().readText()
                val err    = proc.errorStream.bufferedReader().readText()
                proc.waitFor()
                val result = (if (out.isNotBlank()) out else err).trim()
                withContext(Dispatchers.Main) {
                    shellResults.add(0, ShellResult(cmd, result.ifBlank { "(no output)" }, err.isNotBlank() && out.isBlank()))
                    if (shellResults.isNotEmpty()) scope.launch { shellState.scrollToItem(0) }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    shellResults.add(0, ShellResult(cmd, e.message ?: "Error", true))
                }
            }
        }
    }

    Column(Modifier.fillMaxSize().background(HunterBg)) {
        HunterTopBar(s.intentFuzzer, onBack)
        Text("  $packageName", color = HunterTextDim, fontFamily = FontFamily.Monospace, fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))

        // Config fields
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HunterField(s.extraKey,   extraKey,   Modifier.weight(1f)) { extraKey = it }
                HunterField(s.extraValue, extraValue, Modifier.weight(1f)) { extraValue = it }
            }
            HunterField(s.dataUri, dataUri, Modifier.fillMaxWidth()) { dataUri = it }
        }

        if (statusMsg.isNotEmpty()) {
            Text(statusMsg, color = HunterYellow, fontFamily = FontFamily.Monospace, fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        }

        // Quick shell commands for this package
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("SHELL:", color = HunterTextDim, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
            listOf(
                "pm dump $packageName" to "dump",
                "pm path $packageName" to "path",
                "dumpsys activity $packageName" to "activity",
            ).forEach { (cmd, label) ->
                FilterChip(
                    selected = false,
                    onClick  = { shellVisible = true; runShell(cmd) },
                    label    = { Text(label, fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                    colors   = FilterChipDefaults.filterChipColors(
                        containerColor = HunterCard, labelColor = HunterBlue)
                )
            }
            Spacer(Modifier.weight(1f))
            IconButton(
                onClick = { shellVisible = !shellVisible },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    if (shellVisible) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Shell",
                    tint = if (shellVisible) HunterGreen else HunterTextDim
                )
            }
        }

        // ── Inline Shell Panel ────────────────────────────
        AnimatedVisibility(
            visible = shellVisible,
            enter   = expandVertically(tween(300)),
            exit    = shrinkVertically(tween(300))
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(HunterSurface)
                    .border(1.dp, HunterBorder)
            ) {
                Row(
                    Modifier.fillMaxWidth().background(HunterCard).padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("● SHELL — $packageName", color = HunterGreen,
                        fontFamily = FontFamily.Monospace, fontSize = 10.sp, modifier = Modifier.weight(1f))
                    IconButton(onClick = { shellResults.clear() }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Delete, null, tint = HunterRed, modifier = Modifier.size(14.dp))
                    }
                }
                LazyColumn(
                    state   = shellState,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp, vertical = 4.dp),
                    reverseLayout = true
                ) {
                    items(shellResults) { r ->
                        Column(Modifier.padding(vertical = 2.dp)) {
                            Text("$ ${r.cmd}", color = HunterGreen, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                            Text(r.output, color = if (r.isError) HunterRed else Color(0xFFE6EDF3),
                                fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                        }
                    }
                }
                Row(
                    Modifier.fillMaxWidth().background(HunterCard).padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("$ ", color = HunterGreen, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = shellInput, onValueChange = { shellInput = it },
                        modifier = Modifier.weight(1f), singleLine = true,
                        placeholder = { Text("adb shell ...", color = HunterTextDim, fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HunterGreen, unfocusedBorderColor = HunterBorder,
                            focusedTextColor = HunterGreen, unfocusedTextColor = HunterGreen, cursorColor = HunterGreen),
                        shape = RoundedCornerShape(4.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    )
                    IconButton(onClick = { if (shellInput.isNotBlank()) { runShell(shellInput); shellInput = "" } }) {
                        Icon(Icons.Default.Send, null, tint = HunterGreen, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // ── Component List ────────────────────────────────
        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
            items(components) { (type, cls, exported) ->
                val short = cls.substringAfterLast(".")
                val typeColor = when (type) {
                    "Activity" -> HunterGreen
                    "Receiver" -> HunterBlue
                    else       -> HunterPurple
                }

                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(HunterCard, RoundedCornerShape(6.dp))
                        .border(
                            width = if (exported) 1.dp else 0.5.dp,
                            color = if (exported) HunterRed.copy(alpha = 0.4f) else HunterBorder,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .clickable { expanded = if (expanded == cls) null else cls }
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = typeColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(3.dp),
                            modifier = Modifier.width(36.dp)
                        ) {
                            Text(type.take(3).uppercase(), color = typeColor,
                                fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
                                fontSize = 9.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(short, color = HunterGreen, fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(cls, color = HunterTextDim, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                        }
                        if (exported) {
                            Surface(color = HunterRed.copy(alpha = 0.15f), shape = RoundedCornerShape(3.dp)) {
                                Text(s.exportedLabel, color = HunterRed, fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold, fontSize = 9.sp,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                            }
                        }
                        Icon(
                            if (expanded == cls) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            null, tint = HunterBorder, modifier = Modifier.size(16.dp)
                        )
                    }

                    AnimatedVisibility(visible = expanded == cls) {
                        Column {
                            Spacer(Modifier.height(10.dp))
                            HorizontalDivider(color = HunterBorder, thickness = 0.5.dp)
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ActionBtn(s.launchBtn, HunterGreen, Modifier.weight(1f)) {
                                    try {
                                        val intent = Intent().apply {
                                            component = ComponentName(packageName, cls)
                                            putExtra(extraKey, extraValue)
                                            if (dataUri.isNotBlank()) data = android.net.Uri.parse(dataUri)
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                        statusMsg = "✓ $short"
                                    } catch (e: Exception) { statusMsg = "✗ ${e.message}" }
                                }
                                ActionBtn(s.payloadBtn, HunterRed, Modifier.weight(1f)) {
                                    onPayloadTest(packageName, cls, extraKey, dataUri.ifBlank { null })
                                }
                                ActionBtn(s.shellBtn, HunterBlue, Modifier.weight(1f)) {
                                    shellVisible = true
                                    runShell("am start -n $packageName/$cls --es $extraKey '$extraValue'")
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun ActionBtn(label: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick, modifier = modifier.height(36.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(4.dp),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        Text(label, color = HunterBg, fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold, fontSize = 11.sp)
    }
}

@Composable
private fun HunterField(label: String, value: String, modifier: Modifier, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onChange,
        label = { Text(label, fontFamily = FontFamily.Monospace, color = HunterTextDim, fontSize = 10.sp) },
        modifier = modifier, singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = HunterGreen, unfocusedBorderColor = HunterBorder,
            focusedTextColor = HunterGreen, unfocusedTextColor = HunterGreen,
            cursorColor = HunterGreen, focusedLabelColor = HunterGreen),
        shape = RoundedCornerShape(4.dp),
        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
    )
}
