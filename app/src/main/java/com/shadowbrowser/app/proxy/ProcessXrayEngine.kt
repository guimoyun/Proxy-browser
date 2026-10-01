package com.shadowbrowser.app.proxy

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * 进程式 Xray 引擎。
 *
 * 原理：把 xray-core 的 Android 二进制（assets/xray/xray）拷贝到应用私有目录，
 * 赋予可执行权限后用 ProcessBuilder 以子进程方式运行，配置写在 filesDir/xray/config.json。
 * 该方式不依赖 VPNService、不需要 root，绑定 127.0.0.1 本地端口即可。
 *
 * 就绪判定：轮询本地 HTTP 端口，端口真正打开才返回成功，避免“假连接”。
 * 日志：xray stderr 全部写入 ProxyLogStore（网络日志页可见），失败原因可排查。
 */
class ProcessXrayEngine(
    private val context: Context,
    override val localHttpPort: Int = 10809,
    override val localSocksPort: Int = 10808
) : ProxyEngine {

    private val current = AtomicReference(ProxyStatus.DISCONNECTED)
    private var process: Process? = null
    private val workDir = File(context.filesDir, "xray")
    private val binaryFile get() = File(workDir, "xray")
    private val configFile get() = File(workDir, "config.json")

    override val status: ProxyStatus get() = current.get()

    override fun start(configJson: String): Boolean {
        stop()
        if (!prepareBinary()) {
            ProxyLogStore.add("[xray] 未找到/无法准备 xray 二进制，请确认 APK 内含 assets/xray/xray")
            current.set(ProxyStatus.ERROR)
            return false
        }
        current.set(ProxyStatus.CONNECTING)
        return try {
            configFile.writeText(configJson, Charsets.UTF_8)
            val p = ProcessBuilder(binaryFile.absolutePath, "run", "-c", configFile.absolutePath)
                .directory(workDir)
                .redirectErrorStream(true)
                .start()
            process = p

            // 后台读取 xray 输出到日志（网络日志页可见）
            Thread {
                try {
                    p.inputStream.bufferedReader().forEachLine { line ->
                        if (line.isNotBlank()) ProxyLogStore.add("[xray] $line")
                    }
                } catch (_: Exception) {
                }
                if (current.get() == ProxyStatus.CONNECTED || current.get() == ProxyStatus.CONNECTING) {
                    ProxyLogStore.add("[xray] 进程已退出")
                    current.set(ProxyStatus.ERROR)
                }
                process = null
            }.start()

            // 轮询本地 HTTP 端口，真正就绪才算成功
            val deadline = System.currentTimeMillis() + 8000
            while (System.currentTimeMillis() < deadline) {
                if (!p.isAlive) {
                    ProxyLogStore.add("[xray] 启动后进程退出（多为配置错误），见上方日志")
                    current.set(ProxyStatus.ERROR)
                    return false
                }
                if (portOpen("127.0.0.1", localHttpPort)) {
                    ProxyLogStore.add("[xray] 本地代理就绪 127.0.0.1:$localHttpPort")
                    current.set(ProxyStatus.CONNECTED)
                    return true
                }
                Thread.sleep(200)
            }
            ProxyLogStore.add("[xray] 8 秒内本地端口未就绪，已停止")
            p.destroy()
            current.set(ProxyStatus.ERROR)
            false
        } catch (e: Exception) {
            ProxyLogStore.add("[xray] 启动异常: ${e.message ?: e.javaClass.simpleName}")
            Log.e("ProcessXrayEngine", "start failed", e)
            current.set(ProxyStatus.ERROR)
            false
        }
    }

    override fun stop() {
        try {
            process?.let {
                if (it.isAlive) {
                    it.destroy()
                    it.waitFor(800, TimeUnit.MILLISECONDS)
                    if (it.isAlive) it.destroyForcibly()
                }
            }
        } catch (_: Exception) {
        } finally {
            process = null
            current.set(ProxyStatus.DISCONNECTED)
        }
    }

    /** 把 assets 里的 xray 二进制拷贝到私有目录并赋予可执行权限 */
    private fun prepareBinary(): Boolean {
        return try {
            if (!workDir.exists()) workDir.mkdirs()
            if (!binaryFile.exists() || binaryFile.length() < 1_000_000L) {
                context.assets.open("xray/xray").use { input ->
                    FileOutputStream(binaryFile).use { out -> input.copyTo(out) }
                }
                ProxyLogStore.add("[xray] 已从 assets 释放 xray 二进制")
            }
            if (!binaryFile.setExecutable(true)) {
                Log.w("ProcessXrayEngine", "无法设置可执行权限")
            }
            binaryFile.exists() && binaryFile.length() > 1_000_000L
        } catch (e: Exception) {
            Log.e("ProcessXrayEngine", "未找到 xray 二进制(assets/xray/xray)，请按 README 准备", e)
            false
        }
    }

    private fun portOpen(host: String, port: Int, timeoutMs: Int = 300): Boolean {
        return try {
            Socket().use { s -> s.connect(InetSocketAddress(host, port), timeoutMs) }
            true
        } catch (_: Exception) {
            false
        }
    }
}
