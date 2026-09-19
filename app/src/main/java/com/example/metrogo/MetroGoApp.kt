package com.example.metrogo

import android.app.Application

class MetroGoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        LanguageManager.init(this)
    }
}