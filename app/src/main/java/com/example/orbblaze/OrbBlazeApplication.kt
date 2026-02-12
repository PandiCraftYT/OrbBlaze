package com.example.orbblaze

import android.app.Application
import com.google.android.gms.ads.MobileAds

class OrbBlazeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Mobile Ads once when the app starts.
        MobileAds.initialize(this) {}
    }
}
