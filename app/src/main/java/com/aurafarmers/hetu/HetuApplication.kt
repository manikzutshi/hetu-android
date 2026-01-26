package com.aurafarmers.hetu

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class HetuApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        // Initialize RunAnywhere SDK here if needed
    }
}
