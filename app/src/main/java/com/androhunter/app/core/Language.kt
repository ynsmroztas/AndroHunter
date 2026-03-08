package com.androhunter.app.core

import android.content.Context
import android.content.SharedPreferences

enum class AppLanguage(val code: String, val displayName: String, val flag: String) {
    TURKISH("tr", "Türkçe", "🇹🇷"),
    ENGLISH("en", "English", "🇬🇧")
}

object LanguageManager {
    private const val PREF_NAME = "androhunter_prefs"
    private const val KEY_LANG  = "selected_language"
    private const val KEY_FIRST = "is_first_launch"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun isFirstLaunch(): Boolean = prefs.getBoolean(KEY_FIRST, true)

    fun setFirstLaunchDone() = prefs.edit().putBoolean(KEY_FIRST, false).apply()

    fun getLanguage(): AppLanguage {
        val code = prefs.getString(KEY_LANG, AppLanguage.TURKISH.code)
        return AppLanguage.values().find { it.code == code } ?: AppLanguage.TURKISH
    }

    fun setLanguage(lang: AppLanguage) = prefs.edit().putString(KEY_LANG, lang.code).apply()

    fun isTurkish() = getLanguage() == AppLanguage.TURKISH
}
