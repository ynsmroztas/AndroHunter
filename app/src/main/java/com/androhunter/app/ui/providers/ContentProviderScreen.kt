package com.androhunter.app.ui.providers

import android.content.pm.PackageManager
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
import com.androhunter.app.ui.common.HunterTopBar
import com.androhunter.app.ui.theme.*

@Composable
fun ContentProviderScreen(packageName: String, onBack: () -> Unit) {
    val context   = LocalContext.current
    val providers = remember {
        try {
            context.packageManager.getPackageInfo(packageName, PackageManager.GET_PROVIDERS)
                .providers?.toList() ?: emptyList()
        } catch (e: Exception) { emptyList() }
    }

    Column(Modifier.fillMaxSize().background(HunterBg)) {
        HunterTopBar("CONTENT PROVIDERS", onBack)
        if (providers.isEmpty()) {
            Text("Provider bulunamadı", color = HunterTextDim, fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(16.dp))
        } else {
            LazyColumn(contentPadding = PaddingValues(16.dp)) {
                items(providers) { p ->
                    Column(
                        Modifier.fillMaxWidth()
                            .background(HunterCard, RoundedCornerShape(4.dp))
                            .padding(12.dp)
                    ) {
                        Text(p.name.substringAfterLast("."), color = HunterGreen,
                            fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(p.authority ?: "?", color = HunterBlue, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        if (p.exported)
                            Text("EXPORTED", color = HunterRed, fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("R:${p.readPermission ?: "none"}  W:${p.writePermission ?: "none"}",
                            color = HunterTextDim, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}
