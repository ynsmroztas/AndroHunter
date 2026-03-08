package com.androhunter.app.ui.actlauncher

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.androhunter.app.ui.common.HunterTopBar
import com.androhunter.app.ui.theme.HunterBg
import com.androhunter.app.ui.theme.HunterBorder
import com.androhunter.app.ui.theme.HunterCard
import com.androhunter.app.ui.theme.HunterDim
import com.androhunter.app.ui.theme.HunterGreen
import com.androhunter.app.ui.theme.HunterRed
import com.androhunter.app.ui.theme.HunterText

private val GreenBg     = Color(0x1A00FF88)
private val GreenBorder = Color(0x8000FF88)
private val RedBg       = Color(0x0DFF2D55)
private val RedBorder   = Color(0x66FF2D55)
private val RedBadgeBg  = Color(0x26FF2D55)
private val GreenBadge  = Color(0x1A00FF88)
private val DimBg       = Color(0x26808080)

data class HunterActivityInfo(
    val pkg: String,
    val name: String,
    val exported: Boolean,
    val isLauncher: Boolean
)

@Composable
fun ActivityLauncherScreen(onBack: () -> Unit) {
    val context   = LocalContext.current
    val clipboard = LocalClipboardManager.current

    val installedApps = remember {
        context.packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { it.sourceDir.isNotBlank() }
            .sortedBy { context.packageManager.getApplicationLabel(it).toString() }
    }

    var selectedPkg by remember { mutableStateOf<String?>(null) }
    var activities  by remember { mutableStateOf<List<HunterActivityInfo>>(emptyList()) }
    var filterExp   by remember { mutableStateOf(false) }
    var lastResult  by remember { mutableStateOf("") }
    var extraData   by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }

    fun loadActivities(pkg: String) {
        try {
            val info = context.packageManager.getPackageInfo(pkg, PackageManager.GET_ACTIVITIES)
            activities = info.activities?.mapIndexed { i, act ->
                HunterActivityInfo(pkg, act.name, act.exported, i == 0)
            } ?: emptyList()
        } catch (e: Exception) {
            activities = listOf(HunterActivityInfo(pkg, "ERR: ${e.message?.take(60)}", false, false))
        }
    }

    fun launch(act: HunterActivityInfo) {
        try {
            val i = Intent().apply {
                component = ComponentName(act.pkg, act.name)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (extraData.isNotBlank()) { putExtra("data", extraData); putExtra("url", extraData) }
            }
            context.startActivity(i)
            lastResult = "✓ ${act.name.substringAfterLast(".")}"
        } catch (e: Exception) { lastResult = "✗ ${e.message?.take(80)}" }
    }

    fun adbCmd(act: HunterActivityInfo): String {
        val ex = if (extraData.isNotBlank()) " --es data \"$extraData\"" else ""
        return "adb shell am start -n ${act.pkg}/${act.name}$ex"
    }

    val filtered = activities
        .filter { !filterExp || it.exported }
        .filter { searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) }

    Column(Modifier.fillMaxSize().background(HunterBg)) {
        HunterTopBar("ACTIVITY LAUNCHER", onBack)

        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("UYGULAMA SEÇ", color = HunterDim, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            LazyColumn(Modifier.height(140.dp)) {
                items(installedApps.take(80)) { app ->
                    val label = context.packageManager.getApplicationLabel(app).toString()
                    val isSel = selectedPkg == app.packageName
                    val rowBg = if (isSel) GreenBg else HunterCard
                    Row(
                        Modifier.fillMaxWidth()
                            .background(rowBg, RoundedCornerShape(4.dp))
                            .clickable(indication = null,
                                interactionSource = remember { MutableInteractionSource() }) {
                                selectedPkg = app.packageName; loadActivities(app.packageName)
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            val nameColor = if (isSel) HunterGreen else HunterText
                            Text(label, color = nameColor, fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Text(app.packageName, color = HunterDim,
                                fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                        }
                        RadioButton(selected = isSel,
                            onClick = { selectedPkg = app.packageName; loadActivities(app.packageName) },
                            colors = RadioButtonDefaults.colors(selectedColor = HunterGreen))
                    }
                    Spacer(Modifier.height(2.dp))
                }
            }

            OutlinedTextField(
                value = extraData, onValueChange = { extraData = it },
                label = { Text("Extra / Deep Link", fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                placeholder = { Text("https://evil.com", color = HunterDim,
                    fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                textStyle = TextStyle(color = HunterGreen, fontFamily = FontFamily.Monospace),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = HunterGreen, unfocusedBorderColor = HunterBorder,
                    focusedLabelColor = HunterGreen, unfocusedLabelColor = HunterDim)
            )
        }

        if (activities.isNotEmpty()) {
            Column(Modifier.padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = RedBadgeBg, shape = RoundedCornerShape(4.dp)) {
                        Text("EXP ${activities.count { it.exported }}", color = HunterRed,
                            fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                    Surface(color = DimBg, shape = RoundedCornerShape(4.dp)) {
                        Text("ALL ${activities.size}", color = HunterDim,
                            fontFamily = FontFamily.Monospace, fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                    Spacer(Modifier.weight(1f))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = filterExp, onCheckedChange = { filterExp = it },
                            colors = CheckboxDefaults.colors(checkedColor = HunterRed))
                        Text("Sadece exported", color = HunterDim,
                            fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                    }
                }
                OutlinedTextField(
                    value = searchQuery, onValueChange = { searchQuery = it },
                    label = { Text("Ara...", fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    textStyle = TextStyle(color = HunterGreen, fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HunterGreen, unfocusedBorderColor = HunterBorder,
                        focusedLabelColor = HunterGreen, unfocusedLabelColor = HunterDim)
                )
                if (lastResult.isNotBlank()) {
                    val ok = lastResult.startsWith("✓")
                    Surface(color = if (ok) GreenBg else RedBg, shape = RoundedCornerShape(4.dp)) {
                        Text(lastResult, color = if (ok) HunterGreen else HunterRed,
                            fontFamily = FontFamily.Monospace, fontSize = 11.sp,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp))
                    }
                }
            }

            HorizontalDivider(color = HunterBorder, modifier = Modifier.padding(vertical = 4.dp))

            LazyColumn(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                items(filtered) { act ->
                    val rowBg     = if (act.exported) RedBg     else HunterCard
                    val rowBorder = if (act.exported) RedBorder  else HunterBorder
                    val nameColor = if (act.exported) HunterRed  else HunterText
                    Column(
                        Modifier.fillMaxWidth()
                            .background(rowBg, RoundedCornerShape(6.dp))
                            .border(1.dp, rowBorder, RoundedCornerShape(6.dp))
                            .padding(10.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (act.exported)
                                Surface(color = RedBadgeBg, shape = RoundedCornerShape(3.dp)) {
                                    Text("EXPORTED", color = HunterRed,
                                        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
                                        fontSize = 8.sp,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                                }
                            if (act.isLauncher)
                                Surface(color = GreenBadge, shape = RoundedCornerShape(3.dp)) {
                                    Text("LAUNCHER", color = HunterGreen,
                                        fontFamily = FontFamily.Monospace, fontSize = 8.sp,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                                }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(act.name.substringAfterLast("."), color = nameColor,
                            fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(act.name, color = HunterDim, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = { launch(act) },
                                modifier = Modifier.weight(1f).height(36.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GreenBg),
                                shape = RoundedCornerShape(4.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, GreenBorder)
                            ) {
                                Text("[ BAŞLAT ]", color = HunterGreen,
                                    fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                            Button(
                                onClick = { clipboard.setText(AnnotatedString(adbCmd(act))) },
                                modifier = Modifier.weight(1f).height(36.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = HunterCard),
                                shape = RoundedCornerShape(4.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, HunterBorder)
                            ) {
                                Text("[ ADB ]", color = HunterDim,
                                    fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}
