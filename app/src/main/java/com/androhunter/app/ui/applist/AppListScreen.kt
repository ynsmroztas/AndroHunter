package com.androhunter.app.ui.applist

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.androhunter.app.core.LanguageManager
import com.androhunter.app.core.Strings
import com.androhunter.app.ui.theme.*

@Composable
fun AppListScreen(onAppSelected: (String) -> Unit) {
    val context = LocalContext.current
    val s       = Strings.get()
    var query   by remember { mutableStateOf("") }
    var showSystem by remember { mutableStateOf(false) }

    val allApps = remember {
        context.packageManager
            .getInstalledPackages(PackageManager.GET_META_DATA)
            .sortedBy { it.packageName }
    }

    val exportedCount = remember {
        allApps.sumOf { pkg ->
            try {
                val info = context.packageManager.getPackageInfo(pkg.packageName,
                    PackageManager.GET_ACTIVITIES or PackageManager.GET_RECEIVERS)
                (info.activities?.count { it.exported } ?: 0) +
                (info.receivers?.count  { it.exported } ?: 0)
            } catch (e: Exception) { 0 }
        }
    }

    val debugCount = remember {
        allApps.count { (it.applicationInfo?.flags ?: 0) and ApplicationInfo.FLAG_DEBUGGABLE != 0 }
    }

    val filtered = allApps.filter {
        (showSystem || (it.applicationInfo?.flags ?: 0) and ApplicationInfo.FLAG_SYSTEM == 0) &&
        it.packageName.contains(query, ignoreCase = true)
    }

    Column(Modifier.fillMaxSize().background(HunterBg)) {

        // ── Header ──────────────────────────────────────
        Column(Modifier.background(HunterSurface).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(s.appTitle, color = HunterGreen, fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text(s.appSubtitle, color = HunterTextDim, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                }
                // Lang toggle
                val currentLang = LanguageManager.getLanguage()
                TextButton(onClick = {
                    LanguageManager.setLanguage(
                        if (LanguageManager.isTurkish()) com.androhunter.app.core.AppLanguage.ENGLISH
                        else com.androhunter.app.core.AppLanguage.TURKISH
                    )
                }) {
                    Text(currentLang.flag + " " + currentLang.code.uppercase(),
                        color = HunterGreen, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(12.dp))

            // Stats row
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard(allApps.size.toString(), s.totalApps,     HunterGreen,  Modifier.weight(1f))
                StatCard(exportedCount.toString(), s.exportedComponents, HunterRed,  Modifier.weight(1f))
                StatCard(debugCount.toString(),   s.debugApps,     HunterYellow, Modifier.weight(1f))
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── Search + Filter ──────────────────────────────
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value         = query,
                onValueChange = { query = it },
                placeholder   = { Text(s.searchHint, fontFamily = FontFamily.Monospace, color = HunterTextDim, fontSize = 12.sp) },
                leadingIcon   = { Icon(Icons.Default.Search, null, tint = HunterGreen) },
                modifier      = Modifier.weight(1f),
                singleLine    = true,
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = HunterGreen, unfocusedBorderColor = HunterBorder,
                    focusedTextColor     = HunterGreen, unfocusedTextColor   = HunterGreen,
                    cursorColor          = HunterGreen),
                shape = RoundedCornerShape(4.dp)
            )
            FilterChip(
                selected = showSystem,
                onClick  = { showSystem = !showSystem },
                label    = { Text("SYS", fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                colors   = FilterChipDefaults.filterChipColors(
                    containerColor         = HunterCard,
                    labelColor             = HunterTextDim,
                    selectedContainerColor = HunterRed.copy(alpha = 0.2f),
                    selectedLabelColor     = HunterRed
                )
            )
        }

        Spacer(Modifier.height(4.dp))
        Text(
            "${filtered.size} ${s.appsFound}",
            color = HunterTextDim, fontFamily = FontFamily.Monospace, fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(4.dp))

        // ── App List ─────────────────────────────────────
        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
            items(filtered, key = { it.packageName }) { pkg ->
                val appName = remember(pkg.packageName) {
                    try { context.packageManager.getApplicationLabel(pkg.applicationInfo!!).toString() }
                    catch (e: Exception) { pkg.packageName }
                }
                val icon: Drawable? = remember(pkg.packageName) {
                    try { context.packageManager.getApplicationIcon(pkg.packageName) }
                    catch (e: Exception) { null }
                }
                val isDebug  = (pkg.applicationInfo?.flags ?: 0) and ApplicationInfo.FLAG_DEBUGGABLE != 0
                val isSystem = (pkg.applicationInfo?.flags ?: 0) and ApplicationInfo.FLAG_SYSTEM != 0

                AnimatedVisibility(
                    visible = true,
                    enter   = fadeIn() + expandVertically()
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onAppSelected(pkg.packageName) }
                            .background(HunterCard, RoundedCornerShape(6.dp))
                            .border(
                                width = if (isDebug) 1.dp else 0.5.dp,
                                color = if (isDebug) HunterYellow.copy(alpha = 0.5f) else HunterBorder,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // App icon
                        Box(
                            Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
                                .background(HunterSurface),
                            contentAlignment = Alignment.Center
                        ) {
                            if (icon != null) {
                                Image(
                                    bitmap = icon.toBitmap(40, 40).asImageBitmap(),
                                    contentDescription = appName,
                                    modifier = Modifier.size(36.dp)
                                )
                            } else {
                                Text(appName.take(1).uppercase(),
                                    color = HunterGreen, fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(Modifier.weight(1f)) {
                            Text(appName, color = HunterGreen, fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                            Text(pkg.packageName, color = HunterTextDim, fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp, maxLines = 1)
                            Text("v${pkg.versionName ?: "?"}", color = HunterBlue,
                                fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                        }

                        // Badges
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (isDebug) Badge(HunterYellow, "DBG")
                            if (isSystem) Badge(HunterTextDim, "SYS")
                        }
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Default.ArrowForward, null, tint = HunterBorder, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun StatCard(value: String, label: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    // Animate number on appear
    val animValue by animateIntAsState(
        targetValue = value.toIntOrNull() ?: 0,
        animationSpec = tween(1000),
        label = "stat"
    )
    Column(
        modifier
            .background(HunterCard, RoundedCornerShape(6.dp))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(animValue.toString(), color = color, fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Text(label, color = HunterTextDim, fontFamily = FontFamily.Monospace,
            fontSize = 9.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines = 2)
    }
}

@Composable
private fun Badge(color: androidx.compose.ui.graphics.Color, label: String) {
    Surface(color = color.copy(alpha = 0.15f), shape = RoundedCornerShape(3.dp)) {
        Text(label, color = color, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
            fontSize = 8.sp, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
    }
}
