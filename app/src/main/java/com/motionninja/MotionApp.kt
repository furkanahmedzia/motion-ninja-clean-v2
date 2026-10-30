package com.motionninja

import android.app.Application
import com.motionninja.util.NativeLibs

class MotionApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Load all native libraries at startup
        NativeLibs.loadAll()
    }
}