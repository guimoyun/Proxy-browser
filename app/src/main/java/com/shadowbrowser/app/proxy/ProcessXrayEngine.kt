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
 * 注意：构建前需把 xray 二进制放入 app/src/main/assets/xray/xray（见 README）。
 * 缺失时 start() 返回 false 并记录明确错误。
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

            Thread.sleep(700)
            if (!p.isAlive) {
                current.set(ProxyStatus.ERROR)
                return false
            }
            if (!portOpen("127.0.0.1", localHttpPort)) {
                Log.w("ProcessXrayEngine", "本地端口 $localHttpPort 未就绪，但进程存活")
            }
            // 后台兜底读取日志，进程退出时更新状态
            Thread {
                try {
                    p.inputStream.bufferedReader().forEachLine { line ->
                        if (line.isNotBlank()) Log.d("ProcessXrayEngine", line)
                    }
                } catch (_: Exception) {
                }
                if (current.get() == ProxyStatus.CONNECTED || current.get() == ProxyStatus.CONNECTING) {
                    current.set(ProxyStatus.ERROR)
                }
                process = null
            }.start()

            current.set(ProxyStatus.CONNECTED)
            true
        } catch (e: Exception) {
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
