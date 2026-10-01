package com.shadowbrowser.app.gecko

import android.content.Context
import com.shadowbrowser.app.browser.FingerprintEngine
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import java.io.File

/**
 * GeckoView 运行时（整个进程唯一）。
 *
 * 关键设计（免 VPN 代理上网）：
 *  - Gecko 通过配置文件（geckoview-config.yaml）预设网络代理 prefs，
 *    所有请求固定走本地 xray HTTP 入站 127.0.0.1:10809；
 *  - xray 常驻运行：未连接节点时以「直连模式」兜底，连接节点后切换到节点出站；
 *  - 因此 Gecko 侧代理设置一次性生效，切换节点只重启 xray 进程，无需重建浏览器。
 */
object GeckoEngine {

    @Volatile private var runtime: GeckoRuntime? = null

    fun runtime(context: Context): GeckoRuntime {
        runtime?.let { return it }
        synchronized(this) {
            runtime?.let { return it }
            val ctx = context.applicationContext
            val cfg = writeConfig(ctx)
            val settings = GeckoRuntimeSettings.Builder()
                .configFilePath(cfg.absolutePath)
                .remoteDebuggingEnabled(false)
                .build()
            val r = GeckoRuntime.create(ctx, settings)
            runtime = r
            return r
        }
    }

    /** 把代理/指纹 prefs 写入配置文件（Gecko 启动时读取，重启应用后生效新值） */
    private fun writeConfig(context: Context): File {
        val dir = File(context.filesDir, "gecko")
        if (!dir.exists()) dir.mkdirs()
        val f = File(dir, "geckoview-config.yaml")

        val sb = StringBuilder("prefs:\n")
        // 固定走本地代理（免 VPN 核心）：HTTP/HTTPS → 127.0.0.1:10809
        sb.append("  network.proxy.type: 1\n")
        sb.append("  network.proxy.http: \"127.0.0.1\"\n")
        sb.append("  network.proxy.http_port: 10809\n")
        sb.append("  network.proxy.ssl: \"127.0.0.1\"\n")
        sb.append("  network.proxy.ssl_port: 10809\n")
        sb.append("  network.proxy.share_proxy_settings: true\n")
        sb.append("  network.proxy.no_proxies_on: \"localhost, 127.0.0.1\"\n")

        // 指纹相关（重启应用后生效）
        val fp = FingerprintEngine(context)
        if (fp.fppEnabled()) {
            sb.append("  privacy.fingerprintingProtection: true\n")
        }
        if (fp.webrtcDisabled()) {
            sb.append("  media.peerconnection.enabled: false\n")
        }

        f.writeText(sb.toString())
        return f
    }
}
