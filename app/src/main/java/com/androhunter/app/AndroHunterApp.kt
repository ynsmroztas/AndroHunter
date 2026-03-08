package com.androhunter.app

import android.app.Application
import com.androhunter.app.core.LanguageManager

class AndroHunterApp : Application() {
    override fun onCreate() {
        super.onCreate()
        LanguageManager.init(this)
    }
}
