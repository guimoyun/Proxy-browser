package com.shadowbrowser.app.browser

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
import com.shadowbrowser.app.proxy.ProxyLogStore

/**
 * WebView 代理覆盖（免 VPN 的关键）。
 *
 * 通过 androidx.webkit.ProxyController 把本应用所有 WebView 的 HTTP/HTTPS 流量
 * 重定向到本地代理端口，全程不占用 VPNService、不弹系统 VPN 图标。
 *
 * 注意：
 *  - 依赖系统 WebView ≥ 91（PROXY_OVERRIDE 特性）；不支持时 apply() 不生效并记录原因。
 *  - 重复设置会抛异常，因此 apply() 内部先清除旧覆盖再设置。
 */
object WebViewProxyHook {

    @Volatile private var appContext: Context? = null
    @Volatile private var active = false

    /** 由 ProxyManager 初始化时注入 ApplicationContext */
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun supported(): Boolean =
        WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)

    fun isActive(): Boolean = active

    /** 先清旧覆盖、再设置新覆盖；onApplied 在主线程回调 */
    fun apply(httpPort: Int, onApplied: () -> Unit) {
        val ctx = appContext ?: return
        if (!supported()) {
            ProxyLogStore.add("[proxy] 系统 WebView 不支持 PROXY_OVERRIDE（需 WebView ≥ 91），代理覆盖未生效")
            return
        }
        val executor = ContextCompat.getMainExecutor(ctx)
        val config = ProxyConfig.Builder()
            .addProxyRule("http://127.0.0.1:$httpPort")
            .addBypassRule("localhost;127.0.0.1")
            .build()
        val doSet = {
            try {
                ProxyController.getInstance().setProxyOverride(config, executor) {
                    active = true
                    ProxyLogStore.add("[proxy] 已应用本地代理 127.0.0.1:$httpPort")
                    onApplied()
                }
            } catch (e: Exception) {
                ProxyLogStore.add("[proxy] 设置代理覆盖失败: ${e.message ?: e.javaClass.simpleName}")
            }
        }
        if (active) {
            try {
                ProxyController.getInstance().clearProxyOverride(executor) { doSet() }
            } catch (e: Exception) {
                ProxyLogStore.add("[proxy] 清除旧覆盖失败: ${e.message ?: e.javaClass.simpleName}")
                doSet()
            }
        } else doSet()
    }

    fun clear(onCleared: () -> Unit) {
        val ctx = appContext ?: return
        if (!supported()) {
            active = false
            onCleared()
            return
        }
        try {
            ProxyController.getInstance().clearProxyOverride(ContextCompat.getMainExecutor(ctx)) {
                active = false
                ProxyLogStore.add("[proxy] 已清除代理覆盖")
                onCleared()
            }
        } catch (e: Exception) {
            ProxyLogStore.add("[proxy] 清除代理覆盖异常: ${e.message ?: e.javaClass.simpleName}")
            active = false
            onCleared()
        }
    }
}
