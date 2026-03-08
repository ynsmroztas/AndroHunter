package com.androhunter.app.ui.appdetail

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.androhunter.app.ui.common.HunterTopBar
import com.androhunter.app.ui.common.HunterCard
import com.androhunter.app.ui.theme.*

@Composable
fun AppDetailScreen(
    packageName: String,
    onIntentFuzzer: () -> Unit,
    onContentProviders: () -> Unit,
    onFileProviders: () -> Unit,
    onAutoAdb: () -> Unit,
    onDexAnalyzer: () -> Unit,
    onManifestViewer: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val pm      = context.packageManager

    val packageInfo = remember {
        try { pm.getPackageInfo(packageName,
            PackageManager.GET_ACTIVITIES or PackageManager.GET_RECEIVERS or
            PackageManager.GET_PROVIDERS  or PackageManager.GET_SERVICES  or
            PackageManager.GET_PERMISSIONS) }
        catch (e: Exception) { null }
    }

    val ai       = packageInfo?.applicationInfo
    val isDebug  = (ai?.flags ?: 0) and ApplicationInfo.FLAG_DEBUGGABLE != 0
    val isBackup = (ai?.flags ?: 0) and ApplicationInfo.FLAG_ALLOW_BACKUP != 0

    val exportedCount = remember(packageInfo) {
        (packageInfo?.activities?.count { it.exported } ?: 0) +
        (packageInfo?.receivers?.count  { it.exported } ?: 0) +
        (packageInfo?.services?.count   { it.exported } ?: 0)
    }

    Column(Modifier.fillMaxSize().background(HunterBg).verticalScroll(rememberScrollState())) {
        HunterTopBar(packageName.substringAfterLast(".").uppercase(), onBack)

        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

            // Risk indicators
            if (isDebug || isBackup || exportedCount > 2) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isDebug)          RiskTag("⚡ DEBUG MODE",          HunterRed)
                    if (isBackup)         RiskTag("📦 BACKUP ENABLED",       HunterYellow)
                    if (exportedCount > 2) RiskTag("🎯 $exportedCount EXPORTED", HunterRed)
                }
            }

            HunterCard {
                Text("PAKET BİLGİSİ", color = HunterTextDim, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                Spacer(Modifier.height(8.dp))
                InfoRow("Paket",       packageName)
                InfoRow("Versiyon",    packageInfo?.versionName ?: "?")
                InfoRow("Min SDK",     ai?.minSdkVersion?.toString() ?: "?")
                InfoRow("Target SDK",  ai?.targetSdkVersion?.toString() ?: "?")
                InfoRow("Debug",       isDebug.toString(),  if (isDebug) HunterRed else HunterGreen)
                InfoRow("Backup",      isBackup.toString(), if (isBackup) HunterYellow else HunterGreen)
                InfoRow("Activities",  packageInfo?.activities?.size?.toString() ?: "0")
                InfoRow("Receivers",   packageInfo?.receivers?.size?.toString()  ?: "0")
                InfoRow("Services",    packageInfo?.services?.size?.toString()   ?: "0")
                InfoRow("Providers",   packageInfo?.providers?.size?.toString()  ?: "0")
                InfoRow("Exported",    exportedCount.toString(),
                    if (exportedCount > 0) HunterRed else HunterGreen)
            }

            // Action grid
            Text("ARAÇLAR", color = HunterTextDim, fontFamily = FontFamily.Monospace, fontSize = 11.sp)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionBtn("INTENT FUZZER",   HunterGreen,  Modifier.weight(1f), onIntentFuzzer)
                    ActionBtn("AUTO ADB",         HunterRed,    Modifier.weight(1f), onAutoAdb)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionBtn("MANIFEST",         HunterBlue,   Modifier.weight(1f), onManifestViewer)
                    ActionBtn("DEX ANALYZER",     HunterYellow, Modifier.weight(1f), onDexAnalyzer)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionBtn("CONTENT PROV",     HunterPurple, Modifier.weight(1f), onContentProviders)
                    ActionBtn("FILE PROV",         HunterTextDim,Modifier.weight(1f), onFileProviders)
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, valueColor: Color = HunterGreen) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = HunterTextDim, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        Text(value, color = valueColor,   fontFamily = FontFamily.Monospace, fontSize = 12.sp,
            fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ActionBtn(label: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Button(
        onClick  = onClick,
        modifier = modifier.height(52.dp),
        colors   = ButtonDefaults.buttonColors(containerColor = color.copy(alpha = 0.15f)),
        shape    = RoundedCornerShape(6.dp),
        border   = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Text("[ $label ]", color = color, fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold, fontSize = 11.sp)
    }
}

@Composable
private fun RiskTag(label: String, color: Color) {
    Surface(color = color.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
        Text(label, color = color, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
            fontSize = 11.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
    }
}
