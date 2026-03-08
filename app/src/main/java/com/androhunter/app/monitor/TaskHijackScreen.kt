package com.androhunter.app.monitor

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
fun TaskHijackScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val am      = context.getSystemService(ActivityManager::class.java)
    var tasks   by remember { mutableStateOf(listOf<ActivityManager.RunningTaskInfo>()) }
    var result  by remember { mutableStateOf("") }
    var targetPkg by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            @Suppress("DEPRECATION")
            tasks = am.getRunningTasks(20) ?: emptyList()
            kotlinx.coroutines.delay(2000)
        }
    }

    Column(Modifier.fillMaxSize().background(HunterBg)) {
        HunterTopBar("TASK HIJACKING", onBack)

        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {

            HunterCard {
                Text("⚠ TASK HIJACKING PoC", color = HunterRed,
                    fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(Modifier.height(4.dp))
                Text("Hedef uygulamanın görev yığınına AndroHunter aktivitesi enjekte eder.",
                    color = HunterTextDim, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = targetPkg, onValueChange = { targetPkg = it },
                    label = { Text("Hedef Paket", fontFamily = FontFamily.Monospace, color = HunterTextDim) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HunterRed, unfocusedBorderColor = HunterBorder,
                        focusedTextColor = HunterRed, unfocusedTextColor = HunterGreen, cursorColor = HunterRed),
                    shape = RoundedCornerShape(4.dp))
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        try {
                            val intent = Intent().apply {
                                component = ComponentName(
                                    context.packageName,
                                    "${context.packageName}.MainActivity"
                                )
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                addFlags(Intent.FLAG_ACTIVITY_TASK_ON_HOME)
                                putExtra("hijack_target", targetPkg)
                            }
                            context.startActivity(intent)
                            result = "✓ Hijack denemesi gönderildi → $targetPkg"
                        } catch (e: Exception) { result = "✗ ${e.message}" }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors   = ButtonDefaults.buttonColors(containerColor = HunterRed),
                    shape    = RoundedCornerShape(4.dp)
                ) { Text("[ TASK HIJACK DENE ]", color = HunterBg, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) }
                if (result.isNotBlank())
                    Text(result, color = HunterYellow, fontFamily = FontFamily.Monospace, fontSize = 11.sp,
                        modifier = Modifier.padding(top = 8.dp))
            }

            Text("ÇALIŞAN GÖREVLER (${tasks.size})", color = HunterTextDim,
                fontFamily = FontFamily.Monospace, fontSize = 11.sp)
        }

        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
            items(tasks) { task ->
                Row(
                    Modifier.fillMaxWidth()
                        .background(HunterCard, RoundedCornerShape(4.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(task.topActivity?.packageName ?: "?",
                            color = HunterGreen, fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(task.topActivity?.className?.substringAfterLast(".") ?: "?",
                            color = HunterTextDim, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                    }
                    IconButton(onClick = { targetPkg = task.topActivity?.packageName ?: "" }) {
                        Text("→", color = HunterYellow, fontFamily = FontFamily.Monospace)
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}
