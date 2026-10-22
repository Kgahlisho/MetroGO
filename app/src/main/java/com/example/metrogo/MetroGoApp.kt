package com.example.metrogo

import android.app.Application
import org.maplibre.android.MapLibre

class MetroGoApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize MapLibre. No access token required.
        MapLibre.getInstance(this)

        LanguageManager.init(this)
    }
}