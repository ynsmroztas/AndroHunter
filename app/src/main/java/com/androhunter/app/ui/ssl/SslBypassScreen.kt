package com.androhunter.app.ui.ssl

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.androhunter.app.ui.common.HunterTopBar
import com.androhunter.app.ui.theme.*

data class SslMethod(
    val name: String,
    val desc: String,
    val difficulty: String,
    val color: Color,
    val steps: List<String>,
    val command: String
)

private val SSL_METHODS = listOf(
    SslMethod(
        name="Frida SSL Kill Switch 2",
        desc="En kapsamlı SSL bypass — OkHttp, TrustManager, Conscrypt, Appcelerator, OpenSSL",
        difficulty="KOLAY",
        color=HunterGreen,
        steps=listOf(
            "1. frida-server'ı cihaza yükle (adb push frida-server /data/local/tmp/)",
            "2. frida-server'ı başlat: adb shell /data/local/tmp/frida-server &",
            "3. Aşağıdaki komutu PC'de çalıştır"
        ),
        command="frida --codeshare akabe4/frida-multiple-unpinning -U -f TARGET_PKG"
    ),
    SslMethod(
        name="objection SSL Bypass",
        desc="Otomatik SSL pinning bypass + runtime exploration",
        difficulty="KOLAY",
        color=HunterGreen,
        steps=listOf(
            "pip install objection",
            "objection -g TARGET_PKG explore",
            "android sslpinning disable"
        ),
        command="objection -g TARGET_PKG explore --startup-command 'android sslpinning disable'"
    ),
    SslMethod(
        name="Magisk TrustMeAlready",
        desc="Root + Magisk modülü ile sistem geneli SSL bypass",
        difficulty="ORTA",
        color=HunterYellow,
        steps=listOf(
            "Magisk Manager → Modules → TrustMeAlready ara ve yükle",
            "Cihazı yeniden başlat",
            "Tüm uygulamalarda SSL bypass aktif"
        ),
        command="# Magisk module: TrustMeAlready (sistem geneli)"
    ),
    SslMethod(
        name="Network Security Config",
        desc="APK repack: network_security_config.xml ekle",
        difficulty="ORTA",
        color=HunterYellow,
        steps=listOf(
            "apktool d app.apk",
            "res/xml/network_security_config.xml oluştur",
            "<trust-anchors><certificates src='user'/></trust-anchors> ekle",
            "apktool b app -o app_patched.apk && apksigner sign ..."
        ),
        command="apktool d app.apk && apktool b app_patched -o patched.apk"
    ),
    SslMethod(
        name="Xposed SSLUnpinning",
        desc="Xposed Framework modülü ile SSL bypass",
        difficulty="ZOR",
        color=HunterRed,
        steps=listOf(
            "Xposed Framework yükle (LSPosed tercih edilir)",
            "JustTrustMe veya SSLUnpinning modülünü yükle",
            "Hedef uygulamayı modüle ekle ve yeniden başlat"
        ),
        command="# LSPosed + JustTrustMe (Xposed module)"
    ),
    SslMethod(
        name="Burp Proxy + User CA",
        desc="Burp sertifikasını user store'a yükle + proxy",
        difficulty="KOLAY",
        color=HunterBlue,
        steps=listOf(
            "Burp Suite → Proxy → Import/Export CA Certificate → DER",
            "Android Ayarlar → Güvenlik → Sertifika Yükle → Burp cert",
            "Wi-Fi proxy: localhost:8080",
            "Android 7+ için: Network Security Config gerekebilir"
        ),
        command="adb push burp.der /sdcard/ && adb shell am start -a android.settings.SECURITY_SETTINGS"
    ),
)

@Composable
fun SslBypassScreen(onBack: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    var targetPkg by remember { mutableStateOf("") }
    var expanded  by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().background(HunterBg)) {
        HunterTopBar("SSL PINNING BYPASS", onBack)

        LazyColumn(contentPadding=PaddingValues(12.dp), verticalArrangement=Arrangement.spacedBy(8.dp)) {
            item {
                OutlinedTextField(
                    value=targetPkg, onValueChange={targetPkg=it},
                    label={Text("Hedef Package Name", fontFamily=FontFamily.Monospace, fontSize=11.sp)},
                    placeholder={Text("com.target.app", color=HunterDim, fontFamily=FontFamily.Monospace, fontSize=11.sp)},
                    modifier=Modifier.fillMaxWidth(), singleLine=true,
                    textStyle=androidx.compose.ui.text.TextStyle(color=HunterGreen, fontFamily=FontFamily.Monospace),
                    colors=OutlinedTextFieldDefaults.colors(
                        focusedBorderColor=HunterGreen, unfocusedBorderColor=HunterBorder,
                        focusedLabelColor=HunterGreen, unfocusedLabelColor=HunterDim)
                )
            }

            items(SSL_METHODS) { method ->
                val isExp = expanded == method.name
                val pkg   = targetPkg.ifBlank { "TARGET_PKG" }
                Column(
                    Modifier.fillMaxWidth()
                        .background(HunterCard, RoundedCornerShape(6.dp))
                        .border(1.dp, method.color.copy(alpha=0.35f), RoundedCornerShape(6.dp))
                ) {
                    Row(
                        Modifier.fillMaxWidth()
                            .clickable(indication=null, interactionSource=remember{androidx.compose.foundation.interaction.MutableInteractionSource()}) {
                                expanded = if(isExp) null else method.name
                            }.padding(12.dp),
                        verticalAlignment=Alignment.CenterVertically,
                        horizontalArrangement=Arrangement.spacedBy(8.dp)
                    ) {
                        Column(Modifier.weight(1f)) {
                            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                                Surface(color=method.color.copy(alpha=0.15f), shape=RoundedCornerShape(3.dp)) {
                                    Text(method.difficulty, color=method.color, fontFamily=FontFamily.Monospace,
                                        fontWeight=FontWeight.Bold, fontSize=8.sp,
                                        modifier=Modifier.padding(horizontal=5.dp, vertical=2.dp))
                                }
                            }
                            Spacer(Modifier.height(3.dp))
                            Text(method.name, color=method.color, fontFamily=FontFamily.Monospace,
                                fontWeight=FontWeight.Bold, fontSize=13.sp)
                            Text(method.desc, color=HunterDim, fontFamily=FontFamily.Monospace, fontSize=10.sp)
                        }
                        Text(if(isExp)"▲" else "▼", color=HunterDim, fontSize=12.sp)
                    }

                    if (isExp) {
                        Column(
                            Modifier.fillMaxWidth().background(HunterSurface).padding(12.dp),
                            verticalArrangement=Arrangement.spacedBy(6.dp)
                        ) {
                            Text("ADIMLAR", color=HunterDim, fontFamily=FontFamily.Monospace,
                                fontWeight=FontWeight.Bold, fontSize=10.sp)
                            method.steps.forEach { step ->
                                Text(step, color=HunterText, fontFamily=FontFamily.Monospace, fontSize=11.sp)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text("KOMUT", color=HunterDim, fontFamily=FontFamily.Monospace,
                                fontWeight=FontWeight.Bold, fontSize=10.sp)
                            Box(
                                Modifier.fillMaxWidth()
                                    .background(Color(0xFF050508), RoundedCornerShape(4.dp))
                                    .border(1.dp, method.color.copy(alpha=0.2f), RoundedCornerShape(4.dp))
                                    .padding(10.dp)
                            ) {
                                Text(method.command.replace("TARGET_PKG", pkg),
                                    color=HunterGreen, fontFamily=FontFamily.Monospace, fontSize=11.sp)
                            }
                            Button(
                                onClick={clipboard.setText(AnnotatedString(method.command.replace("TARGET_PKG",pkg)))},
                                modifier=Modifier.fillMaxWidth().height(36.dp),
                                colors=ButtonDefaults.buttonColors(containerColor=method.color.copy(alpha=0.15f)),
                                shape=RoundedCornerShape(4.dp),
                                border=androidx.compose.foundation.BorderStroke(1.dp, method.color.copy(alpha=0.5f))
                            ) { Text("[ KOPYALA ]", color=method.color, fontFamily=FontFamily.Monospace,
                                fontWeight=FontWeight.Bold, fontSize=12.sp) }
                        }
                    }
                }
            }
        }
    }
}
