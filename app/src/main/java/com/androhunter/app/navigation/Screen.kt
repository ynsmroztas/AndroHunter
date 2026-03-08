package com.androhunter.app.navigation

import kotlinx.serialization.Serializable

sealed class Screen {
    @Serializable object AppList      : Screen()
    @Serializable object Terminal     : Screen()
    @Serializable object AdbManager   : Screen()
    @Serializable object BroadcastMon : Screen()
    @Serializable object WebTester    : Screen()
    @Serializable object TaskHijack   : Screen()
    @Serializable object AccessMon    : Screen()
    @Serializable object About        : Screen()

    @Serializable data class AppDetail(val packageName: String)        : Screen()
    @Serializable data class IntentFuzzer(val packageName: String)     : Screen()
    @Serializable data class ContentProviders(val packageName: String) : Screen()
    @Serializable data class FileProviders(val packageName: String)    : Screen()
    @Serializable data class AutoAdb(val packageName: String)          : Screen()
    @Serializable data class DexAnalyzer(val packageName: String)      : Screen()
    @Serializable data class ManifestViewer(val packageName: String)   : Screen()
    @Serializable data class PayloadTest(
        val packageName: String,
        val className: String,
        val extraKey: String,
        val dataUri: String = ""
    ) : Screen()

    @Serializable object SharedPrefs       : Screen()
    @Serializable object ActivityLauncher  : Screen()
    @Serializable object BroadcastFuzzer   : Screen()
    @Serializable object FridaGenerator    : Screen()
    @Serializable object SslBypass         : Screen()

}
