package com.androhunter.app.web

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.androhunter.app.ui.common.HunterCard
import com.androhunter.app.ui.common.HunterTopBar
import com.androhunter.app.ui.theme.*
import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.URL

@Composable
fun WebTesterScreen(onBack: () -> Unit) {
    var targetUrl  by remember { mutableStateOf("https://") }
    var assetHost  by remember { mutableStateOf("") }
    var output     by remember { mutableStateOf("") }
    var running    by remember { mutableStateOf(false) }
    val scope      = rememberCoroutineScope()

    fun fetch(url: String, label: String): String {
        return try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 6000; conn.readTimeout = 6000
            val code = conn.responseCode
            val body = try { conn.inputStream.bufferedReader().readText() } catch (e: Exception) {
                conn.errorStream?.bufferedReader()?.readText() ?: ""
            }
            conn.disconnect()
            "[$label] HTTP $code\n${body.take(500)}\n"
        } catch (e: Exception) { "[$label] HATA: ${e.message}\n" }
    }

    Column(Modifier.fillMaxSize().background(HunterBg).verticalScroll(rememberScrollState())) {
        HunterTopBar("WEB TESTER", onBack)

        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

            HunterCard {
                Text("ASSET LINKS CHECKER", color = HunterGreen,
                    fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = assetHost, onValueChange = { assetHost = it },
                    label = { Text("Alan adı (örn: example.com)", fontFamily = FontFamily.Monospace, color = HunterTextDim) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HunterGreen, unfocusedBorderColor = HunterBorder,
                        focusedTextColor = HunterGreen, unfocusedTextColor = HunterGreen, cursorColor = HunterGreen),
                    shape = RoundedCornerShape(4.dp))
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        running = true; output = ""
                        scope.launch(Dispatchers.IO) {
                            val host = assetHost.trim().removePrefix("https://").removePrefix("http://")
                            val sb = StringBuilder()
                            sb.append(fetch("https://$host/.well-known/assetlinks.json", "Asset Links"))
                            sb.append(fetch("https://$host/.well-known/statements.json", "Statements"))
                            sb.append(fetch("https://$host/.well-known/apple-app-site-association", "AASA"))
                            withContext(Dispatchers.Main) { output = sb.toString(); running = false }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = HunterGreen),
                    shape = RoundedCornerShape(4.dp), enabled = !running
                ) { Text("[ ASSET LINKS TARA ]", color = HunterBg, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) }
            }

            HunterCard {
                Text("HTTP PROBE", color = HunterBlue,
                    fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = targetUrl, onValueChange = { targetUrl = it },
                    label = { Text("URL", fontFamily = FontFamily.Monospace, color = HunterTextDim) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HunterBlue, unfocusedBorderColor = HunterBorder,
                        focusedTextColor = HunterBlue, unfocusedTextColor = HunterGreen, cursorColor = HunterBlue),
                    shape = RoundedCornerShape(4.dp))
                Spacer(Modifier.height(8.dp))

                // Quick checks
                val quickPaths = listOf(
                    "/robots.txt", "/.git/config", "/.env", "/admin", "/api/v1",
                    "/swagger-ui.html", "/actuator", "/phpinfo.php", "/.htaccess",
                    "/backup.zip", "/config.json", "/debug"
                )
                Text("HIZLI PATH TARAMA:", color = HunterTextDim, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = {
                        running = true; output = ""
                        scope.launch(Dispatchers.IO) {
                            val base = targetUrl.trimEnd('/')
                            val sb   = StringBuilder()
                            quickPaths.forEach { path ->
                                sb.append(fetch("$base$path", path))
                            }
                            withContext(Dispatchers.Main) { output = sb.toString(); running = false }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors   = ButtonDefaults.buttonColors(containerColor = HunterBlue),
                    shape    = RoundedCornerShape(4.dp), enabled = !running
                ) { Text("[ PATH TARA ]", color = HunterBg, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) }
            }

            if (running) LinearProgressIndicator(modifier = Modifier.fillMaxWidth(),
                color = HunterGreen, trackColor = HunterBorder)

            if (output.isNotBlank()) {
                HunterCard {
                    Text("SONUÇ", color = HunterTextDim, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(output, color = HunterGreen, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                }
            }
        }
    }
}
