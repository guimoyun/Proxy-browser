package com.shadowbrowser.app

import android.app.Application

/**
 * 应用入口：初始化全局单例。
 * 这里只做轻量初始化，重活（代理进程/引擎）按需在运行时启动。
 */
class ShadowApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: ShadowApp
            private set
    }
}
