package com.qchat.android

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class QchatApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // 初始化
    }
}
