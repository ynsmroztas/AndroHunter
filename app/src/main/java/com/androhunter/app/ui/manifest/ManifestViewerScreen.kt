package com.androhunter.app.ui.manifest

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.androhunter.app.ui.common.HunterTopBar
import com.androhunter.app.ui.theme.*

data class ManifestItem(
    val type: String,
    val name: String,
    val exported: Boolean,
    val permission: String?,
    val intentFilters: List<String>,
    val severity: String
)

@Composable
fun ManifestViewerScreen(packageName: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val pm      = context.packageManager

    val flags = PackageManager.GET_ACTIVITIES or PackageManager.GET_RECEIVERS or
                PackageManager.GET_SERVICES   or PackageManager.GET_PROVIDERS  or
                PackageManager.GET_PERMISSIONS or PackageManager.GET_META_DATA

    val packageInfo = remember {
        try { pm.getPackageInfo(packageName, flags) }
        catch (e: Exception) { null }
    }

    val components = remember(packageInfo) {
        buildList {
            packageInfo?.activities?.forEach { a ->
                val severity = if (a.exported && a.permission == null) "HIGH"
                               else if (a.exported) "MEDIUM" else "INFO"
                add(ManifestItem("Activity", a.name.substringAfterLast("."), a.exported,
                    a.permission, emptyList(), severity))
            }
            packageInfo?.receivers?.forEach { r ->
                val severity = if (r.exported && r.permission == null) "HIGH"
                               else if (r.exported) "MEDIUM" else "INFO"
                add(ManifestItem("Receiver", r.name.substringAfterLast("."), r.exported,
                    r.permission, emptyList(), severity))
            }
            packageInfo?.services?.forEach { s ->
                val severity = if (s.exported && s.permission == null) "HIGH"
                               else if (s.exported) "MEDIUM" else "INFO"
                add(ManifestItem("Service", s.name.substringAfterLast("."), s.exported,
                    s.permission, emptyList(), severity))
            }
            packageInfo?.providers?.forEach { p ->
                val severity = if (p.exported && p.readPermission == null) "HIGH"
                               else if (p.exported) "MEDIUM" else "INFO"
                add(ManifestItem("Provider", p.name.substringAfterLast("."), p.exported,
                    p.readPermission ?: p.writePermission, emptyList(), severity))
            }
        }.sortedWith(compareByDescending<ManifestItem> { it.exported }
            .thenBy { it.type })
    }

    val permissions = remember(packageInfo) {
        packageInfo?.requestedPermissions?.toList() ?: emptyList()
    }

    val dangerousPerms = remember(permissions) {
        permissions.filter { perm ->
            listOf("CAMERA", "MICROPHONE", "LOCATION", "CONTACTS", "STORAGE",
                   "PHONE", "SMS", "RECORD_AUDIO", "ACCESSIBILITY",
                   "BIND_", "INSTALL_PACKAGES", "SYSTEM_ALERT").any {
                perm.uppercase().contains(it)
            }
        }
    }

    val appInfo = packageInfo?.applicationInfo
    val isDebug    = (appInfo?.flags ?: 0) and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0
    val isBackup   = (appInfo?.flags ?: 0) and android.content.pm.ApplicationInfo.FLAG_ALLOW_BACKUP != 0
    val exportedCount = components.count { it.exported }

    var activeTab by remember { mutableStateOf(0) }
    val tabs = listOf("Components (${components.size})", "Permissions (${permissions.size})", "Info")

    Column(Modifier.fillMaxSize().background(HunterBg)) {
        HunterTopBar("MANIFEST", onBack)
        Text("  $packageName", color = HunterTextDim, fontFamily = FontFamily.Monospace,
            fontSize = 11.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))

        // Quick risk summary
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isDebug)  RiskChip("DEBUG",    HunterRed)
            if (isBackup) RiskChip("BACKUP",   HunterYellow)
            if (exportedCount > 3) RiskChip("$exportedCount EXPORTED", HunterRed)
            if (dangerousPerms.size > 5) RiskChip("${dangerousPerms.size} DANGEROUS PERMS", HunterYellow)
        }

        // Tabs
        TabRow(
            selectedTabIndex = activeTab,
            containerColor   = HunterSurface,
            contentColor     = HunterGreen,
            indicator        = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                    color = HunterGreen, height = 2.dp)
            }
        ) {
            tabs.forEachIndexed { i, title ->
                Tab(
                    selected = activeTab == i,
                    onClick  = { activeTab = i },
                    text     = {
                        Text(title, fontFamily = FontFamily.Monospace, fontSize = 11.sp,
                            color = if (activeTab == i) HunterGreen else HunterTextDim)
                    }
                )
            }
        }

        when (activeTab) {
            0 -> ComponentsTab(components)
            1 -> PermissionsTab(permissions, dangerousPerms)
            2 -> InfoTab(packageInfo, isDebug, isBackup)
        }
    }
}

@Composable
private fun ComponentsTab(components: List<ManifestItem>) {
    LazyColumn(contentPadding = PaddingValues(16.dp)) {
        items(components) { c ->
            val color = when (c.severity) {
                "HIGH"   -> HunterRed
                "MEDIUM" -> HunterYellow
                else     -> HunterBorder
            }
            val typeColor = when (c.type) {
                "Activity" -> HunterGreen
                "Receiver" -> HunterBlue
                "Service"  -> HunterPurple
                else       -> HunterYellow
            }
            Row(
                Modifier.fillMaxWidth()
                    .background(HunterCard, RoundedCornerShape(4.dp))
                    .border(1.dp, color, RoundedCornerShape(4.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(color = typeColor.copy(alpha = 0.15f), shape = RoundedCornerShape(3.dp)) {
                    Text(c.type.take(3).uppercase(), color = typeColor,
                        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 9.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                }
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(c.name, color = HunterGreen, fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    if (c.permission != null)
                        Text("perm: ${c.permission.substringAfterLast(".")}",
                            color = HunterTextDim, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                }
                if (c.exported) {
                    Surface(color = color.copy(alpha = 0.15f), shape = RoundedCornerShape(3.dp)) {
                        Text("EXPORTED", color = color, fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold, fontSize = 9.sp,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun PermissionsTab(all: List<String>, dangerous: List<String>) {
    LazyColumn(contentPadding = PaddingValues(16.dp)) {
        item {
            Text("⚠ TEHLİKELİ İZİNLER (${dangerous.size})", color = HunterRed,
                fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
        }
        items(dangerous) { perm ->
            PermRow(perm, HunterRed)
            Spacer(Modifier.height(4.dp))
        }
        item {
            Spacer(Modifier.height(12.dp))
            Text("TÜM İZİNLER (${all.size})", color = HunterTextDim,
                fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
        }
        items(all.filter { it !in dangerous }) { perm ->
            PermRow(perm, HunterTextDim)
            Spacer(Modifier.height(2.dp))
        }
    }
}

@Composable
private fun PermRow(perm: String, color: androidx.compose.ui.graphics.Color) {
    Row(
        Modifier.fillMaxWidth()
            .background(HunterCard, RoundedCornerShape(4.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("▸ ", color = color, fontFamily = FontFamily.Monospace)
        Text(perm.substringAfterLast("."), color = color,
            fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun InfoTab(info: PackageInfo?, isDebug: Boolean, isBackup: Boolean) {
    val ai = info?.applicationInfo
    LazyColumn(contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        item {
            InfoCard("Paket",     info?.packageName ?: "?")
            InfoCard("Versiyon",  info?.versionName ?: "?")
            InfoCard("VersionCode", info?.longVersionCode?.toString() ?: "?")
            InfoCard("Min SDK",   ai?.minSdkVersion?.toString() ?: "?")
            InfoCard("Target SDK", ai?.targetSdkVersion?.toString() ?: "?")
            InfoCard("Debug",     isDebug.toString(),  if (isDebug) HunterRed else HunterGreen)
            InfoCard("Backup",    isBackup.toString(), if (isBackup) HunterYellow else HunterGreen)
            InfoCard("APK Path",  ai?.sourceDir ?: "?")
        }
    }
}

@Composable
private fun InfoCard(label: String, value: String,
    valueColor: androidx.compose.ui.graphics.Color = HunterGreen) {
    Row(
        Modifier.fillMaxWidth()
            .background(HunterCard, RoundedCornerShape(4.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(label, color = HunterTextDim, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        Text(value, color = valueColor, fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold, fontSize = 12.sp,
            modifier = Modifier.widthIn(max = 220.dp))
    }
}

@Composable
private fun RiskChip(label: String, color: androidx.compose.ui.graphics.Color) {
    Surface(color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(4.dp))) {
        Text(label, color = color, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
            fontSize = 10.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
    }
}

