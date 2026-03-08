package com.androhunter.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.androhunter.app.monitor.AccessibilityMonitorScreen
import com.androhunter.app.monitor.BroadcastMonitorScreen
import com.androhunter.app.monitor.TaskHijackScreen
import com.androhunter.app.ui.about.AboutScreen
import com.androhunter.app.ui.adb.AdbScreen
import com.androhunter.app.ui.actlauncher.ActivityLauncherScreen
import com.androhunter.app.ui.appdetail.AppDetailScreen
import com.androhunter.app.ui.applist.AppListScreen
import com.androhunter.app.ui.autoadb.AutoAdbScreen
import com.androhunter.app.ui.broadcast.BroadcastFuzzerScreen
import com.androhunter.app.ui.dex.DexAnalyzerScreen
import com.androhunter.app.ui.fileprovider.FileProviderScreen
import com.androhunter.app.ui.frida.FridaGeneratorScreen
import com.androhunter.app.ui.intent.IntentFuzzerScreen
import com.androhunter.app.ui.manifest.ManifestViewerScreen
import com.androhunter.app.ui.payload.PayloadScreen
import com.androhunter.app.ui.prefs.SharedPrefsScreen
import com.androhunter.app.ui.providers.ContentProviderScreen
import com.androhunter.app.ui.ssl.SslBypassScreen
import com.androhunter.app.ui.terminal.TerminalScreen
import com.androhunter.app.web.WebTesterScreen

@Composable
fun AndroHunterNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(navController = navController, startDestination = Screen.AppList, modifier = modifier) {

        composable<Screen.AppList> {
            AppListScreen(onAppSelected = { pkg -> navController.navigate(Screen.AppDetail(pkg)) })
        }
        composable<Screen.AppDetail> { back ->
            val r = back.toRoute<Screen.AppDetail>()
            AppDetailScreen(
                packageName        = r.packageName,
                onIntentFuzzer     = { navController.navigate(Screen.IntentFuzzer(r.packageName)) },
                onContentProviders = { navController.navigate(Screen.ContentProviders(r.packageName)) },
                onFileProviders    = { navController.navigate(Screen.FileProviders(r.packageName)) },
                onAutoAdb          = { navController.navigate(Screen.AutoAdb(r.packageName)) },
                onDexAnalyzer      = { navController.navigate(Screen.DexAnalyzer(r.packageName)) },
                onManifestViewer   = { navController.navigate(Screen.ManifestViewer(r.packageName)) },
                onBack             = { navController.navigateUp() }
            )
        }
        composable<Screen.IntentFuzzer> { back ->
            val r = back.toRoute<Screen.IntentFuzzer>()
            IntentFuzzerScreen(
                packageName   = r.packageName,
                onPayloadTest = { pkg, cls, key, uri ->
                    navController.navigate(Screen.PayloadTest(pkg, cls, key, uri ?: ""))
                },
                onBack = { navController.navigateUp() }
            )
        }
        composable<Screen.PayloadTest> { back ->
            val r = back.toRoute<Screen.PayloadTest>()
            PayloadScreen(
                packageName = r.packageName,
                className   = r.className,
                extraKey    = r.extraKey,
                dataUri     = r.dataUri.ifBlank { null },
                onBack      = { navController.navigateUp() }
            )
        }
        composable<Screen.ContentProviders> { back ->
            ContentProviderScreen(
                packageName = back.toRoute<Screen.ContentProviders>().packageName,
                onBack      = { navController.navigateUp() })
        }
        composable<Screen.FileProviders> { back ->
            FileProviderScreen(
                packageName = back.toRoute<Screen.FileProviders>().packageName,
                onBack      = { navController.navigateUp() })
        }
        composable<Screen.AutoAdb> { back ->
            AutoAdbScreen(
                packageName = back.toRoute<Screen.AutoAdb>().packageName,
                onBack      = { navController.navigateUp() })
        }
        composable<Screen.DexAnalyzer> { back ->
            DexAnalyzerScreen(
                packageName = back.toRoute<Screen.DexAnalyzer>().packageName,
                onBack      = { navController.navigateUp() })
        }
        composable<Screen.ManifestViewer> { back ->
            ManifestViewerScreen(
                packageName = back.toRoute<Screen.ManifestViewer>().packageName,
                onBack      = { navController.navigateUp() })
        }
        composable<Screen.Terminal>     { TerminalScreen(onBack = { navController.navigateUp() }) }
        composable<Screen.AdbManager>   { AdbScreen(onBack = { navController.navigateUp() }) }
        composable<Screen.BroadcastMon> { BroadcastMonitorScreen(onBack = { navController.navigateUp() }) }
        composable<Screen.WebTester>    { WebTesterScreen(onBack = { navController.navigateUp() }) }
        composable<Screen.TaskHijack>   { TaskHijackScreen(onBack = { navController.navigateUp() }) }
        composable<Screen.AccessMon>    { AccessibilityMonitorScreen(onBack = { navController.navigateUp() }) }
        composable<Screen.About>        { AboutScreen(onBack = { navController.navigateUp() }) }

        // v4.0 yeni modüller
        composable<Screen.SharedPrefs>       { SharedPrefsScreen(onBack = { navController.navigateUp() }) }
        composable<Screen.ActivityLauncher>  { ActivityLauncherScreen(onBack = { navController.navigateUp() }) }
        composable<Screen.BroadcastFuzzer>   { BroadcastFuzzerScreen(onBack = { navController.navigateUp() }) }
        composable<Screen.FridaGenerator>    { FridaGeneratorScreen(onBack = { navController.navigateUp() }) }
        composable<Screen.SslBypass>         { SslBypassScreen(onBack = { navController.navigateUp() }) }
    }
}
