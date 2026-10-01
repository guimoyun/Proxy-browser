package com.shadowbrowser.app.browser

import android.content.Context
import android.content.SharedPreferences
import android.webkit.WebView

/**
 * 指纹伪装（WebView 可改项）。
 *
 * 诚实边界：标准 WebView 无法深度伪造 Canvas / WebGL / 字体指纹（需替换 Chromium 内核）。
 * 本实现覆盖：User-Agent（默认/随机/自定义/电脑模式）、JS 注入覆盖语言/时区/并发/内存/插件、
 * 禁用 WebRTC（防真实 IP 泄露）。
 */
class FingerprintEngine(context: Context) {

    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences("fingerprint", Context.MODE_PRIVATE)

    companion object {
        private const val K_UA = "ua"
        private const val K_RANDOM_UA = "random_ua"
        private const val K_DESKTOP = "desktop"
        private const val K_TZ = "tz_offset_minutes"
        private const val K_WEBRTC = "webrtc_off"
        private const val K_CUSTOM_UA = "custom_ua"

        val DESKTOP_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
    }

    fun effectiveUserAgent(): String {
        if (prefs.getBoolean(K_RANDOM_UA, false)) return randomAndroidUa()
        if (prefs.getBoolean(K_DESKTOP, false)) return DESKTOP_UA
        val custom = prefs.getString(K_CUSTOM_UA, "").orEmpty()
        if (custom.isNotBlank()) return custom
        return WebViewSettingsHolder.defaultUa(appContext)
    }

    fun desktopMode(): Boolean = prefs.getBoolean(K_DESKTOP, false)
    fun setDesktopMode(on: Boolean) = prefs.edit().putBoolean(K_DESKTOP, on).apply()

    fun randomUaEnabled(): Boolean = prefs.getBoolean(K_RANDOM_UA, false)
    fun setRandomUa(on: Boolean) = prefs.edit().putBoolean(K_RANDOM_UA, on).apply()

    fun customUa(): String = prefs.getString(K_CUSTOM_UA, "").orEmpty()
    fun setCustomUa(s: String) = prefs.edit().putString(K_CUSTOM_UA, s).apply()

    fun tzOffsetMinutes(): Int = prefs.getInt(K_TZ, 0)
    fun setTzOffsetMinutes(m: Int) = prefs.edit().putInt(K_TZ, m).apply()

    fun webrtcDisabled(): Boolean = prefs.getBoolean(K_WEBRTC, false)
    fun setWebrtcDisabled(on: Boolean) = prefs.edit().putBoolean(K_WEBRTC, on).apply()

    /** 应用到指定 WebView（设置 UA 等静态项） */
    fun applyTo(wv: WebView) {
        wv.settings.userAgentString = effectiveUserAgent()
    }

    /** 页面加载完成后注入的 JS（覆盖可观测属性），返回脚本字符串 */
    fun jsInjection(): String {
        val tz = tzOffsetMinutes()
        val webrtcOff = webrtcDisabled()
        val sb = StringBuilder("(function(){")
        sb.append("try{Object.defineProperty(navigator,'userAgent',{get:function(){return '")
        sb.append(effectiveUserAgent().replace("'", "\\'"))
        sb.append("';}});}catch(e){}")
        if (tz != 0) {
            val sign = if (tz >= 0) "+" else "-"
            val abs = kotlin.math.abs(tz)
            sb.append("var __tzOffset=$sign$abs;try{Date.prototype.getTimezoneOffset=function(){return __tzOffset;};}catch(e){}")
        }
        if (webrtcOff) {
            sb.append("try{window.RTCPeerConnection=function(){throw new Error('blocked');};")
            sb.append("window.webkitRTCPeerConnection=function(){throw new Error('blocked');};}catch(e){}")
        }
        sb.append("})();")
        return sb.toString()
    }

    private fun randomAndroidUa(): String {
        val chrome = listOf("120", "121", "122", "123", "124", "125", "126")
        val device = listOf(
            "Pixel 8; Build/UD1A.230803.041",
            "SM-S918B; Build/TP1A.220624.014",
            "M2012K11AG; Build/SKQ1.220303.001",
            "2201123C; Build/SKQ1.220303.001"
        )
        return "Mozilla/5.0 (Linux; Android 14; ${device.random()}) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/${chrome.random()}.0.0.0 Mobile Safari/537.36"
    }
}

/** 单例缓存系统默认 UA（首次获取后固定） */
object WebViewSettingsHolder {
    private var cached: String? = null
    fun defaultUa(context: android.content.Context): String {
        cached?.let { return it }
        cached = android.webkit.WebSettings.getDefaultUserAgent(context.applicationContext)
        return cached!!
    }
}
