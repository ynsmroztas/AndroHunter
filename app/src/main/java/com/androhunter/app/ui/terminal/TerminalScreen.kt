package com.androhunter.app.ui.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.androhunter.app.ui.common.HunterTopBar
import com.androhunter.app.ui.theme.*
import kotlinx.coroutines.*

data class TerminalLine(val text: String, val type: LineType)
enum class LineType { COMMAND, OUTPUT, ERROR, INFO }

@Composable
fun TerminalScreen(onBack: () -> Unit) {
    var input      by remember { mutableStateOf("") }
    val lines      = remember { mutableStateListOf<TerminalLine>() }
    val listState  = rememberLazyListState()
    val scope      = rememberCoroutineScope()

    val quickCmds = listOf("id", "whoami", "uname -a", "env", "ifconfig",
                            "netstat -an", "ps", "ls /data", "cat /proc/version",
                            "getprop ro.build.version.release")

    fun runCmd(cmd: String) {
        if (cmd.isBlank()) return
        lines.add(TerminalLine("$ $cmd", LineType.COMMAND))
        scope.launch(Dispatchers.IO) {
            try {
                val proc   = Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd))
                val stdout = proc.inputStream.bufferedReader().readText()
                val stderr = proc.errorStream.bufferedReader().readText()
                proc.waitFor()
                withContext(Dispatchers.Main) {
                    if (stdout.isNotBlank()) stdout.lines().forEach { lines.add(TerminalLine(it, LineType.OUTPUT)) }
                    if (stderr.isNotBlank()) stderr.lines().forEach { lines.add(TerminalLine(it, LineType.ERROR)) }
                    if (stdout.isBlank() && stderr.isBlank()) lines.add(TerminalLine("(çıktı yok)", LineType.INFO))
                    if (lines.isNotEmpty()) listState.animateScrollToItem(lines.size - 1)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    lines.add(TerminalLine("HATA: ${e.message}", LineType.ERROR))
                }
            }
        }
    }

    Column(Modifier.fillMaxSize().background(HunterBg).imePadding().navigationBarsPadding()) {
        HunterTopBar("TERMINAL", onBack)

        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp).padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            quickCmds.take(5).forEach { cmd ->
                FilterChip(
                    selected = false, onClick = { runCmd(cmd) },
                    label = { Text(cmd, fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = HunterCard, labelColor = HunterGreen)
                )
            }
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp).padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            quickCmds.drop(5).forEach { cmd ->
                FilterChip(
                    selected = false, onClick = { runCmd(cmd) },
                    label = { Text(cmd, fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = HunterCard, labelColor = HunterGreen)
                )
            }
        }

        LazyColumn(
            state   = listState,
            modifier = Modifier.weight(1f).fillMaxWidth()
                .background(HunterSurface).padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (lines.isEmpty()) {
                item {
                    Text("AndroHunter Terminal — komut girin",
                        color = HunterTextDim, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                }
            }
            items(lines) { line ->
                val color: Color = when (line.type) {
                    LineType.COMMAND -> HunterGreen
                    LineType.OUTPUT  -> Color(0xFFE6EDF3)
                    LineType.ERROR   -> HunterRed
                    LineType.INFO    -> HunterTextDim
                }
                Text(line.text, color = color, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            }
        }

        Row(
            Modifier.fillMaxWidth().background(HunterCard).padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("$ ", color = HunterGreen, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = input, onValueChange = { input = it },
                modifier = Modifier.weight(1f), singleLine = true,
                placeholder = { Text("komut...", color = HunterTextDim, fontFamily = FontFamily.Monospace) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = HunterGreen, unfocusedBorderColor = HunterBorder,
                    focusedTextColor = HunterGreen, unfocusedTextColor = HunterGreen, cursorColor = HunterGreen),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { runCmd(input); input = "" }),
                shape = RoundedCornerShape(4.dp)
            )
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = { runCmd(input); input = "" }) {
                Icon(Icons.Default.Send, contentDescription = "Çalıştır", tint = HunterGreen)
            }
            IconButton(onClick = { lines.clear() }) {
                Icon(Icons.Default.Delete, contentDescription = "Temizle", tint = HunterRed)
            }
        }
    }
}
