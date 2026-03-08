package com.androhunter.app.monitor

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
fun AccessibilityMonitorScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val logs    = remember { mutableStateListOf<AccessEntry>() }
    var filter  by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            logs.clear()
            logs.addAll(if (filter.isBlank()) AccessLog.entries
                        else AccessLog.entries.filter { it.packageName.contains(filter, true) })
            kotlinx.coroutines.delay(1000)
        }
    }

    Column(Modifier.fillMaxSize().background(HunterBg)) {
        HunterTopBar("ACCESSIBILITY MON", onBack)

        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            HunterCard {
                Text("Servisi etkinleştirmek için Erişilebilirlik Ayarları'nı açın",
                    color = HunterYellow, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                    colors  = ButtonDefaults.buttonColors(containerColor = HunterYellow),
                    shape   = RoundedCornerShape(4.dp), modifier = Modifier.fillMaxWidth()
                ) { Text("[ ERİŞİLEBİLİRLİK AYARLARI ]", color = HunterBg, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) }
            }

            OutlinedTextField(
                value = filter, onValueChange = { filter = it },
                label = { Text("Paket filtrele", fontFamily = FontFamily.Monospace, color = HunterTextDim) },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = HunterGreen, unfocusedBorderColor = HunterBorder,
                    focusedTextColor = HunterGreen, unfocusedTextColor = HunterGreen, cursorColor = HunterGreen),
                shape = RoundedCornerShape(4.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("EVENTS (${logs.size})", color = HunterTextDim, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                TextButton(onClick = { AccessLog.entries.clear(); logs.clear() }) {
                    Text("Temizle", color = HunterRed, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
            }
        }

        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
            items(logs) { e ->
                Column(
                    Modifier.fillMaxWidth().background(HunterCard, RoundedCornerShape(4.dp)).padding(10.dp)
                ) {
                    Row {
                        Text(e.time, color = HunterTextDim, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(e.packageName, color = HunterGreen, fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(e.eventType, color = HunterBlue, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                    if (e.text.isNotBlank())
                        Text(e.text, color = HunterYellow, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}
