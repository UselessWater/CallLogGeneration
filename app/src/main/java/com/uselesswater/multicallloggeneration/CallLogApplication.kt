package com.uselesswater.multicallloggeneration

import android.app.Application
import com.jakewharton.threetenabp.AndroidThreeTen

class CallLogApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidThreeTen.init(this)
    }
}