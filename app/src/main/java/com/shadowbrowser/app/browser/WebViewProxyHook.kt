package com.shadowbrowser.app.browser

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature

/**
 * WebView 代理覆盖（免 VPN 的关键）。
 *
 * 通过 androidx.webkit.ProxyController 把本应用所有 WebView 的 HTTP/HTTPS 流量
 * 重定向到本地代理端口，全程不占用 VPNService、不弹系统 VPN 图标。
 *
 * 注意：代理覆盖作用于整个进程的 WebView，断开时务必 clearProxyOverride 恢复直连。
 */
object WebViewProxyHook {

    @Volatile private var appContext: Context? = null

    /** 由 ProxyManager 初始化时注入 ApplicationContext */
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun supported(): Boolean =
        WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)

    fun setProxy(httpPort: Int, onApplied: () -> Unit) {
        val ctx = appContext ?: return
        if (!supported()) return
        val config = ProxyConfig.Builder()
            .addProxyRule("http://127.0.0.1:$httpPort")
            .addBypassRule("localhost;127.0.0.1")
            .build()
        ProxyController.getInstance().setProxyOverride(
            config,
            ContextCompat.getMainExecutor(ctx),
            onApplied
        )
    }

    fun clearProxy(onCleared: () -> Unit) {
        val ctx = appContext ?: return
        if (!supported()) return
        ProxyController.getInstance().clearProxyOverride(
            ContextCompat.getMainExecutor(ctx),
            onCleared
        )
    }
}
