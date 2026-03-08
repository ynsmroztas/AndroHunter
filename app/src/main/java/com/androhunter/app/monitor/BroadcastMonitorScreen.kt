package com.androhunter.app.monitor

import android.content.Intent
import android.content.IntentFilter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.androhunter.app.ui.common.HunterCard
import com.androhunter.app.ui.common.HunterTopBar
import com.androhunter.app.ui.theme.*

@Composable
fun BroadcastMonitorScreen(onBack: () -> Unit) {
    val context   = LocalContext.current
    var listening by remember { mutableStateOf(false) }
    val logs      = remember { mutableStateListOf<BroadcastEntry>() }
    val receiver  = remember { BroadcastMonitorReceiver() }

    val commonActions = listOf(
        "android.intent.action.BOOT_COMPLETED",
        "android.intent.action.PACKAGE_ADDED",
        "android.intent.action.PACKAGE_REMOVED",
        "android.net.conn.CONNECTIVITY_CHANGE",
        "android.intent.action.BATTERY_CHANGED",
        "android.intent.action.SCREEN_ON",
        "android.intent.action.SCREEN_OFF",
        "android.intent.action.USER_PRESENT",
    )

    DisposableEffect(listening) {
        if (listening) {
            val filter = IntentFilter()
            commonActions.forEach { filter.addAction(it) }
            context.registerReceiver(receiver, filter)
        }
        onDispose {
            if (listening) {
                try { context.unregisterReceiver(receiver) } catch (e: Exception) {}
            }
        }
    }

    // Refresh logs from global log
    LaunchedEffect(Unit) {
        while (true) {
            logs.clear()
            logs.addAll(BroadcastLog.entries)
            kotlinx.coroutines.delay(1000)
        }
    }

    Column(Modifier.fillMaxSize().background(HunterBg)) {
        HunterTopBar("BROADCAST MONITOR", onBack)

        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            HunterCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("BROADCAST DİNLEYİCİ", color = HunterGreen,
                            fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        Text(if (listening) "● AKTİF — ${logs.size} event" else "○ KAPALI",
                            color = if (listening) HunterGreen else HunterTextDim,
                            fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    }
                    Switch(
                        checked         = listening,
                        onCheckedChange = { listening = it },
                        colors          = SwitchDefaults.colors(
                            checkedThumbColor   = HunterBg,
                            checkedTrackColor   = HunterGreen,
                            uncheckedTrackColor = HunterBorder
                        )
                    )
                }
            }

            // Send custom broadcast
            HunterCard {
                var customAction by remember { mutableStateOf("") }
                Text("ÖZEL BROADCAST GÖNDER", color = HunterYellow,
                    fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = customAction, onValueChange = { customAction = it },
                    label = { Text("Action", fontFamily = FontFamily.Monospace, color = HunterTextDim) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HunterYellow, unfocusedBorderColor = HunterBorder,
                        focusedTextColor = HunterYellow, unfocusedTextColor = HunterGreen, cursorColor = HunterYellow),
                    shape = RoundedCornerShape(4.dp))
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (customAction.isNotBlank()) {
                            context.sendBroadcast(Intent(customAction))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors   = ButtonDefaults.buttonColors(containerColor = HunterYellow),
                    shape    = RoundedCornerShape(4.dp)
                ) { Text("[ GÖNDER ]", color = HunterBg, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) }
            }

            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("KAYITLAR (${logs.size})", color = HunterTextDim, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                TextButton(onClick = { BroadcastLog.entries.clear(); logs.clear() }) {
                    Text("Temizle", color = HunterRed, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
            }
        }

        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
            items(logs) { entry ->
                Column(
                    Modifier.fillMaxWidth()
                        .background(HunterCard, RoundedCornerShape(4.dp))
                        .padding(10.dp)
                ) {
                    Row {
                        Text(entry.timestamp, color = HunterTextDim, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(entry.action, color = HunterGreen, fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    if (entry.extras.isNotEmpty()) {
                        entry.extras.forEach { (k, v) ->
                            Text("  $k = $v", color = HunterBlue, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}
