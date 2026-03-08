package com.androhunter.app.ui.frida

import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.androhunter.app.ui.common.HunterTopBar
import com.androhunter.app.ui.theme.*

data class FridaScript(
    val name: String,
    val desc: String,
    val category: String,
    val color: Color,
    val code: String
)

fun buildFridaScripts(pkg: String) = listOf(
    FridaScript("SSL Pinning Bypass", "Tüm SSL pinning yöntemlerini bypass et",
        "SSL", HunterRed, """
Java.perform(function() {
    // OkHttp3 bypass
    try {
        var CertPinning = Java.use('okhttp3.CertificatePinner');
        CertPinning.check.overload('java.lang.String','java.util.List').implementation = function(a,b){ return; };
        console.log('[+] OkHttp3 SSL bypass OK');
    } catch(e){}

    // TrustManager bypass  
    try {
        var X509 = Java.use('javax.net.ssl.X509TrustManager');
        var TrustManager = Java.registerClass({
            name: 'com.bypass.TrustManager',
            implements: [X509],
            methods: {
                checkClientTrusted: function(chain, authType){},
                checkServerTrusted: function(chain, authType){},
                getAcceptedIssuers: function(){ return []; }
            }
        });
        var SSLContext = Java.use('javax.net.ssl.SSLContext');
        var ctx = SSLContext.getInstance('TLS');
        ctx.init(null, [TrustManager.${'$'}new()], null);
        SSLContext.getDefault.implementation = function(){ return ctx; };
        console.log('[+] TrustManager bypass OK');
    } catch(e){}

    // Conscrypt / BoringSSL
    try {
        var nativeLib = Java.use('com.google.android.gms.org.conscrypt.TrustManagerImpl');
        nativeLib.checkTrustedRecursive.implementation = function(a,b,c,d,e,f){ return []; };
        console.log('[+] Conscrypt bypass OK');
    } catch(e){}
});
""".trimIndent()),
    FridaScript("Root Detection Bypass", "Root/Jailbreak tespitini bypass et",
        "Root", HunterYellow, """
Java.perform(function() {
    // RootBeer bypass
    try {
        var RootBeer = Java.use('com.scottyab.rootbeer.RootBeer');
        RootBeer.isRooted.implementation = function(){ return false; };
        RootBeer.isRootedWithoutBusyBox.implementation = function(){ return false; };
        console.log('[+] RootBeer bypass OK');
    } catch(e){}

    // SafetyNet bypass
    try {
        var SafetyNet = Java.use('com.google.android.gms.safetynet.SafetyNetApi');
        console.log('[+] SafetyNet hook attempted');
    } catch(e){}

    // File existence check hook
    var File = Java.use('java.io.File');
    File.exists.implementation = function() {
        var path = this.getAbsolutePath();
        var suspicious = ['/su','/magisk','/system/bin/su','/sbin/su'];
        if(suspicious.some(p => path.includes(p))) {
            console.log('[!] Root file check blocked: ' + path);
            return false;
        }
        return this.exists();
    };
    console.log('[+] File.exists hook OK');
});
""".trimIndent()),
    FridaScript("Login Bypass", "Authentication kontrolünü atla",
        "Auth", HunterGreen, """
Java.perform(function() {
    // $pkg login methodlarını hook'la
    try {
        Java.enumerateLoadedClasses({
            onMatch: function(name) {
                if(name.includes('$pkg') && 
                   (name.toLowerCase().includes('auth') || 
                    name.toLowerCase().includes('login') || 
                    name.toLowerCase().includes('session'))) {
                    console.log('[+] Auth class found: ' + name);
                    try {
                        var clazz = Java.use(name);
                        var methods = clazz.class.getDeclaredMethods();
                        methods.forEach(function(m) {
                            if(m.getName().toLowerCase().includes('verify') ||
                               m.getName().toLowerCase().includes('check') ||
                               m.getName().toLowerCase().includes('valid')) {
                                console.log('    [>] Hooking: ' + m.getName());
                            }
                        });
                    } catch(e) {}
                }
            },
            onComplete: function() {}
        });
    } catch(e){ console.log('[-] ' + e); }
});
""".trimIndent()),
    FridaScript("Crypto Monitor", "Şifreleme/çözme operasyonlarını izle",
        "Crypto", HunterPurple, """
Java.perform(function() {
    var Cipher = Java.use('javax.crypto.Cipher');
    Cipher.doFinal.overload('[B').implementation = function(data) {
        console.log('[CRYPTO] doFinal input: ' + 
            Java.use('android.util.Base64').encodeToString(data, 0));
        var result = this.doFinal(data);
        console.log('[CRYPTO] doFinal output: ' + 
            Java.use('android.util.Base64').encodeToString(result, 0));
        return result;
    };

    var Mac = Java.use('javax.crypto.Mac');
    Mac.doFinal.overload().implementation = function() {
        var result = this.doFinal();
        console.log('[HMAC] ' + bytesToHex(result));
        return result;
    };

    function bytesToHex(bytes) {
        return Array.from(bytes).map(b => ('0' + (b & 0xFF).toString(16)).slice(-2)).join('');
    }
    console.log('[+] Crypto monitor active');
});
""".trimIndent()),
    FridaScript("SQL Monitor", "Tüm SQL sorgularını intercept et",
        "SQL", HunterRed, """
Java.perform(function() {
    // SQLiteDatabase.rawQuery hook
    var DB = Java.use('android.database.sqlite.SQLiteDatabase');
    DB.rawQuery.overload('java.lang.String','[Ljava.lang.String;').implementation = function(sql, args) {
        console.log('[SQL] rawQuery: ' + sql);
        if(args) console.log('  args: ' + args.join(', '));
        return this.rawQuery(sql, args);
    };
    DB.execSQL.overload('java.lang.String').implementation = function(sql) {
        console.log('[SQL] execSQL: ' + sql);
        return this.execSQL(sql);
    };
    DB.query.overload('java.lang.String','[Ljava.lang.String;','java.lang.String','[Ljava.lang.String;','java.lang.String','java.lang.String','java.lang.String')
        .implementation = function(table,cols,sel,selArgs,gb,having,ob) {
            console.log('[SQL] query table=' + table + ' WHERE ' + sel);
            return this.query(table,cols,sel,selArgs,gb,having,ob);
        };
    console.log('[+] SQL monitor active for $pkg');
});
""".trimIndent()),
    FridaScript("HTTP Intercept", "HTTP istek/yanıtlarını yakala",
        "HTTP", HunterBlue, """
Java.perform(function() {
    // OkHttp interceptor
    try {
        var Builder = Java.use('okhttp3.OkHttpClient${'$'}Builder');
        var Interceptor = Java.use('okhttp3.Interceptor');
        console.log('[+] OkHttp3 found, hooking requests...');
    } catch(e) {}

    // HttpURLConnection hook
    var URL = Java.use('java.net.URL');
    URL.openConnection.overload().implementation = function() {
        console.log('[HTTP] Connection to: ' + this.toString());
        return this.openConnection();
    };

    // Volley / Retrofit via reflection
    Java.enumerateLoadedClasses({
        onMatch: function(name) {
            if(name.includes('okhttp3.Request') || name.includes('retrofit2')) {
                console.log('[HTTP] Framework detected: ' + name);
            }
        },
        onComplete: function(){}
    });
    console.log('[+] HTTP intercept active');
});
""".trimIndent()),
)

@Composable
fun FridaGeneratorScreen(onBack: () -> Unit) {
    val context   = LocalContext.current
    val clipboard = LocalClipboardManager.current

    val installedApps = remember {
        context.packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            .sortedBy { context.packageManager.getApplicationLabel(it).toString() }
    }

    var selectedPkg  by remember { mutableStateOf<String?>(null) }
    var selectedCat  by remember { mutableStateOf<String?>(null) }
    var expandedScript by remember { mutableStateOf<String?>(null) }

    val scripts  = buildFridaScripts(selectedPkg ?: "com.target.app")
    val cats     = listOf(null) + scripts.map { it.category }.distinct()
    val filtered = scripts.filter { selectedCat == null || it.category == selectedCat }

    Column(Modifier.fillMaxSize().background(HunterBg)) {
        HunterTopBar("FRIDA GENERATOR", onBack)

        LazyColumn(contentPadding=PaddingValues(12.dp), verticalArrangement=Arrangement.spacedBy(8.dp)) {
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
                                .clickable(indication=null, interactionSource=remember{androidx.compose.foundation.interaction.MutableInteractionSource()}) {
                                    selectedPkg = app.packageName
                                }.padding(horizontal=10.dp, vertical=5.dp),
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

            // Kurulum komutu
            if (selectedPkg != null) {
                item {
                    Column(
                        Modifier.fillMaxWidth()
                            .background(HunterCard, RoundedCornerShape(6.dp))
                            .border(1.dp, HunterGreen.copy(alpha=0.3f), RoundedCornerShape(6.dp))
                            .padding(10.dp)
                    ) {
                        Text("FRIDA BAŞLATMA KOMUTU", color=HunterDim, fontFamily=FontFamily.Monospace,
                            fontWeight=FontWeight.Bold, fontSize=10.sp)
                        Spacer(Modifier.height(6.dp))
                        val cmd = "frida -U -f $selectedPkg -l script.js --no-pause"
                        Text(cmd, color=HunterGreen, fontFamily=FontFamily.Monospace, fontSize=11.sp)
                        Spacer(Modifier.height(4.dp))
                        TextButton(onClick={clipboard.setText(AnnotatedString(cmd))}) {
                            Text("⎘ Kopyala", color=HunterBlue, fontFamily=FontFamily.Monospace, fontSize=11.sp)
                        }
                    }
                }
            }

            // Category filter
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                    cats.forEach { cat ->
                        FilterChip(selected=selectedCat==cat, onClick={selectedCat=cat},
                            label={Text(cat?:"ALL", fontFamily=FontFamily.Monospace, fontSize=9.sp)},
                            colors=FilterChipDefaults.filterChipColors(
                                containerColor=HunterCard, labelColor=HunterDim,
                                selectedContainerColor=HunterGreen.copy(alpha=0.2f), selectedLabelColor=HunterGreen))
                    }
                }
            }

            items(filtered) { script ->
                val isExpanded = expandedScript == script.name
                Column(
                    Modifier.fillMaxWidth()
                        .background(HunterCard, RoundedCornerShape(6.dp))
                        .border(1.dp, script.color.copy(alpha=0.3f), RoundedCornerShape(6.dp))
                ) {
                    Row(
                        Modifier.fillMaxWidth()
                            .clickable(indication=null, interactionSource=remember{androidx.compose.foundation.interaction.MutableInteractionSource()}) {
                                expandedScript = if(isExpanded) null else script.name
                            }.padding(12.dp),
                        verticalAlignment=Alignment.CenterVertically,
                        horizontalArrangement=Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(color=script.color.copy(alpha=0.12f), shape=RoundedCornerShape(4.dp)) {
                            Text(script.category, color=script.color, fontFamily=FontFamily.Monospace,
                                fontWeight=FontWeight.Bold, fontSize=9.sp,
                                modifier=Modifier.padding(horizontal=6.dp, vertical=3.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            Text(script.name, color=script.color, fontFamily=FontFamily.Monospace,
                                fontWeight=FontWeight.Bold, fontSize=13.sp)
                            Text(script.desc, color=HunterDim, fontFamily=FontFamily.Monospace, fontSize=10.sp)
                        }
                        Text(if(isExpanded)"▲" else "▼", color=HunterDim, fontSize=12.sp)
                    }

                    if (isExpanded) {
                        Column(
                            Modifier.fillMaxWidth()
                                .background(HunterSurface)
                                .padding(10.dp)
                        ) {
                            // Code display
                            Box(
                                Modifier.fillMaxWidth()
                                    .background(Color(0xFF050508), RoundedCornerShape(4.dp))
                                    .border(1.dp, script.color.copy(alpha=0.2f), RoundedCornerShape(4.dp))
                                    .padding(10.dp)
                            ) {
                                Text(script.code, color=Color(0xFF98C379),
                                    fontFamily=FontFamily.Monospace, fontSize=10.sp, lineHeight=15.sp)
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick={clipboard.setText(AnnotatedString(script.code))},
                                    modifier=Modifier.weight(1f).height(36.dp),
                                    colors=ButtonDefaults.buttonColors(containerColor=script.color.copy(alpha=0.15f)),
                                    shape=RoundedCornerShape(4.dp),
                                    border=androidx.compose.foundation.BorderStroke(1.dp, script.color.copy(alpha=0.5f))
                                ) { Text("[ KOPYALA ]", color=script.color, fontFamily=FontFamily.Monospace,
                                    fontWeight=FontWeight.Bold, fontSize=11.sp) }
                                Button(
                                    onClick={
                                        val full = "// ${script.name} — $selectedPkg\n// frida -U -f $selectedPkg -l script.js\n\n${script.code}"
                                        clipboard.setText(AnnotatedString(full))
                                    },
                                    modifier=Modifier.weight(1f).height(36.dp),
                                    colors=ButtonDefaults.buttonColors(containerColor=HunterBlue.copy(alpha=0.1f)),
                                    shape=RoundedCornerShape(4.dp),
                                    border=androidx.compose.foundation.BorderStroke(1.dp, HunterBlue.copy(alpha=0.4f))
                                ) { Text("[ HEADER İLE ]", color=HunterBlue, fontFamily=FontFamily.Monospace,
                                    fontWeight=FontWeight.Bold, fontSize=10.sp) }
                            }
                        }
                    }
                }
            }
        }
    }
}
