package com.example.metrogo

import android.app.Application
import com.mapbox.maps.MapboxOptions

class MetroGoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        MapboxOptions.accessToken = getString(R.string.mapbox_access_token)
        LanguageManager.init(this)
    }
}