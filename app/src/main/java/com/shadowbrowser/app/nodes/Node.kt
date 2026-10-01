package com.shadowbrowser.app.nodes

/**
 * 单个代理节点配置。字段覆盖 vless / vmess / trojan / shadowsocks，
 * 以及 tcp / ws / grpc / h2 传输与 tls / reality 加密。
 */
data class Node(
    var id: String = java.util.UUID.randomUUID().toString(),
    var remark: String = "",
    var protocol: String = "vless",          // vless | vmess | trojan | shadowsocks
    var address: String = "",
    var port: Int = 443,

    // 身份
    var uuid: String = "",                    // vless / vmess
    var password: String = "",                // trojan / shadowsocks
    var security: String = "auto",            // vmess: auto|aes-128-gcm|chacha20-poly1305|none
    var method: String = "aes-256-gcm",       // shadowsocks 加密
    var encryption: String = "none",          // vless: none
    var flow: String = "",                    // vless: xtls-rprx-vision 等

    // 传输层
    var network: String = "tcp",              // tcp | ws | grpc | h2
    var tls: String = "none",                 // none | tls | reality
    var sni: String = "",
    var alpn: String = "",
    var fingerprint: String = "",
    var publicKey: String = "",               // reality
    var shortId: String = "",                 // reality
    var spiderX: String = "",
    var host: String = "",                    // ws header host / h2 host
    var path: String = "",                    // ws path
    var serviceName: String = "",             // grpc service name

    var udp: Boolean = true
) {
    /** 展示用名称 */
    fun displayName(): String =
        if (remark.isNotBlank()) remark
        else "${protocol.uppercase()} ${address}:${port}"

    fun protocolLabel(): String = when (protocol) {
        "vmess" -> "VMess"
        "trojan" -> "Trojan"
        "shadowsocks" -> "SS"
        else -> "VLESS"
    }
}
