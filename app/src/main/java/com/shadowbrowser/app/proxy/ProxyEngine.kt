package com.shadowbrowser.app.proxy

/**
 * 代理引擎抽象：不同实现（内嵌进程 / 子进程二进制）可互换。
 * 核心目标：在 127.0.0.1 本地端口起 HTTP/SOCKS 入站，让浏览器流量走本地代理，
 * 而【不占用系统 VPNService】，即“无需开启系统 VPN 即可代理上网”。
 */
interface ProxyEngine {

    val status: ProxyStatus

    /** 启动代理；成功返回 true */
    fun start(configJson: String): Boolean

    /** 停止代理并释放本地端口 */
    fun stop()

    /** 本地 HTTP 入站端口（浏览器代理指向这里） */
    val localHttpPort: Int

    /** 本地 SOCKS 入站端口 */
    val localSocksPort: Int
}

enum class ProxyStatus { DISCONNECTED, CONNECTING, CONNECTED, ERROR }
