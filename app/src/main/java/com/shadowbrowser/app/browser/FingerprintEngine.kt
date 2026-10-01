package com.shadowbrowser.app.browser

import android.content.Context
import android.content.SharedPreferences
import org.mozilla.geckoview.GeckoSession

/**
 * 指纹伪装（GeckoView 可改项）。
 *
 * 诚实边界：完整指纹伪造（Canvas/WebGL/字体精确伪装）需要修改引擎源码，
 * 本工程不做。GeckoView 提供：
 *  - UA 覆盖（默认/随机/自定义/电脑模式），逐标签生效；
 *  - 隐私模式（不保留历史/cookie）；
 *  - 「基线指纹保护」（Firefox FPP：字体/画布噪声/WebRTC IP 保护等，
 *    通过配置文件写入 prefs，重启应用后生效）。
 */
class FingerprintEngine(context: Context) {

    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences("fingerprint", Context.MODE_PRIVATE)

    companion object {
        private const val K_RANDOM_UA = "random_ua"
        private const val K_DESKTOP = "desktop"
        private const val K_WEBRTC = "webrtc_off"
        private const val K_FPP = "fpp"
        private const val K_CUSTOM_UA = "custom_ua"

        val DESKTOP_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:156.0) " +
            "Gecko/20100101 Firefox/156.0"
    }

    fun effectiveUserAgent(): String {
        if (prefs.getBoolean(K_RANDOM_UA, false)) return randomAndroidUa()
        if (prefs.getBoolean(K_DESKTOP, false)) return DESKTOP_UA
        val custom = prefs.getString(K_CUSTOM_UA, "").orEmpty()
        if (custom.isNotBlank()) return custom
        return defaultGeckoUa()
    }

    private fun defaultGeckoUa(): String =
        "Mozilla/5.0 (Android 14; Mobile; rv:156.0) Gecko/156.0 Firefox/156.0"

    fun desktopMode(): Boolean = prefs.getBoolean(K_DESKTOP, false)
    fun setDesktopMode(on: Boolean) = prefs.edit().putBoolean(K_DESKTOP, on).apply()

    fun randomUaEnabled(): Boolean = prefs.getBoolean(K_RANDOM_UA, false)
    fun setRandomUa(on: Boolean) = prefs.edit().putBoolean(K_RANDOM_UA, on).apply()

    fun customUa(): String = prefs.getString(K_CUSTOM_UA, "").orEmpty()
    fun setCustomUa(s: String) = prefs.edit().putString(K_CUSTOM_UA, s).apply()

    /** 基线指纹保护（Firefox FPP，写入 gecko 配置 prefs，重启应用后生效） */
    fun fppEnabled(): Boolean = prefs.getBoolean(K_FPP, true)
    fun setFpp(on: Boolean) = prefs.edit().putBoolean(K_FPP, on).apply()

    fun webrtcDisabled(): Boolean = prefs.getBoolean(K_WEBRTC, false)
    fun setWebrtcDisabled(on: Boolean) = prefs.edit().putBoolean(K_WEBRTC, on).apply()

    /** 应用到 Gecko 会话（UA 立即生效；FPP/WebRTC 需重启应用） */
    fun applyTo(session: GeckoSession) {
        session.settings.setUserAgentOverride(effectiveUserAgent())
    }

    private fun randomAndroidUa(): String {
        val ver = listOf("132", "136", "140", "144", "148", "152", "156").random()
        val device = listOf(
            "Pixel 8",
            "SM-S918B",
            "M2012K11AG",
            "2201123C"
        ).random()
        return "Mozilla/5.0 (Android 14; Mobile; $device; rv:$ver.0) Gecko/$ver.0 Firefox/$ver.0"
    }
}
