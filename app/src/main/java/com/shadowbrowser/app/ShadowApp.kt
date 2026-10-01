package com.shadowbrowser.app

import android.app.Application
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 应用入口：初始化全局单例 + 全局崩溃捕获。
 *
 * 崩溃捕获：任何未捕获异常都会写入 filesDir/crash.log，
 * 下次启动 MainActivity 会弹窗展示，便于定位设备上的闪退原因。
 */
class ShadowApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this

        val prev = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, e ->
            try {
                val sb = StringBuilder()
                sb.append("time=")
                    .append(SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()))
                    .append('\n')
                sb.append("thread=").append(thread.name).append('\n')
                sb.append(Log.getStackTraceString(e)).append('\n')
                File(filesDir, CRASH_LOG).writeText(sb.toString())
            } catch (_: Exception) {
            }
            prev?.uncaughtException(thread, e)
                ?: run { android.os.Process.killProcess(android.os.Process.myPid()) }
        }
    }

    companion object {
        lateinit var instance: ShadowApp
            private set

        const val CRASH_LOG = "crash.log"
    }
}
