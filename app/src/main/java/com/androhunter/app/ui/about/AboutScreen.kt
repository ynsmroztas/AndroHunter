package com.androhunter.app.ui.about

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.androhunter.app.core.Strings
import com.androhunter.app.ui.common.HunterTopBar
import com.androhunter.app.ui.theme.*

private val ASCII = """
    _              _        _   _             _
   / \   _ __   __| |_ __ | | | |_   _ _ __ | |_ ___ _ __
  / _ \ | '_ \ / _` | '__|| | | | | | | '_ \| __/ _ \ '__|
 / ___ \| | | | (_| | |   | |_| | |_| | | | | ||  __/ |
/_/   \_\_| |_|\__,_|_|    \___/ \__,_|_| |_|\__\___|_|
""".trimIndent()

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val s = Strings.get()
    Column(Modifier.fillMaxSize().background(HunterBg).verticalScroll(rememberScrollState())) {
        HunterTopBar(s.navAbout.uppercase(), onBack)
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(ASCII, color = HunterGreen, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
            Text("Android Security Toolkit v2.0",
                color = HunterTextDim, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            InfoLine(s.aboutDeveloper, "ynsmroztas")
            InfoLine("Twitter/X",      "x.com/ynsmroztas")
            InfoLine(s.aboutTeam,      "MITSEC")
            InfoLine(s.aboutPurpose,   s.aboutPurposeVal)
            Spacer(Modifier.height(16.dp))
            Text(s.aboutModules, color = HunterTextDim, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            Spacer(Modifier.height(4.dp))
            listOf(
                "App Scanner"       to "appsFound / ${s.totalApps}",
                "Intent Fuzzer"     to "Activity / Receiver / Service",
                "Payload Engine"    to "SQLi, XSS, LFI, IDOR, Template, CMDi",
                "Terminal"          to s.terminalTitle,
                "ADB Manager"       to s.adbTitle,
                "Broadcast Monitor" to s.broadcastTitle,
                "Web Tester"        to "${s.assetLinks} + HTTP",
                "Task Hijacking"    to s.hijackTitle,
                "Accessibility Mon" to s.accessTitle,
                "Content Providers" to s.contentProviders,
                "File Providers"    to s.fileProviders,
            ).forEach { (mod, desc) ->
                Row {
                    Text("▸ ", color = HunterGreen, fontFamily = FontFamily.Monospace)
                    Column {
                        Text(mod,  color = HunterGreen,   fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(desc, color = HunterTextDim, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row {
        Text("$label: ", color = HunterTextDim, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
        Text(value, color = HunterGreen, fontFamily = FontFamily.Monospace,
            fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}
