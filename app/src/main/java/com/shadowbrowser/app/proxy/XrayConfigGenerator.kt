package com.shadowbrowser.app.proxy

import com.shadowbrowser.app.nodes.Node
import org.json.JSONArray
import org.json.JSONObject

/**
 * 把单个节点翻译成 xray 的 config.json。
 * 入站：本地 HTTP(127.0.0.1:httpPort) + SOCKS(127.0.0.1:socksPort)
 * 出站：指向用户配置的远端节点
 */
object XrayConfigGenerator {

    fun generate(node: Node, httpPort: Int, socksPort: Int): String {
        val root = JSONObject()

        root.put("log", JSONObject().put("loglevel", "warning"))

        // 入站（本地代理，无需 VPNService）
        val inbounds = JSONArray()
        inbounds.put(
            JSONObject()
                .put("port", httpPort)
                .put("listen", "127.0.0.1")
                .put("protocol", "http")
                .put("settings", JSONObject())
        )
        inbounds.put(
            JSONObject()
                .put("port", socksPort)
                .put("listen", "127.0.0.1")
                .put("protocol", "socks")
                .put("settings", JSONObject().put("udp", node.udp))
        )
        root.put("inbounds", inbounds)

        root.put("outbounds", JSONArray().put(buildOutbound(node)))
        return root.toString()
    }

    private fun buildOutbound(node: Node): JSONObject {
        val out = JSONObject()
        out.put("protocol", node.protocol)
        out.put("settings", buildSettings(node))
        out.put("streamSettings", buildStream(node))
        out.put("tag", "proxy")
        return out
    }

    private fun buildSettings(node: Node): JSONObject {
        return when (node.protocol) {
            "vless" -> {
                val user = JSONObject()
                    .put("id", node.uuid)
                    .put("encryption", node.encryption.ifBlank { "none" })
                if (node.flow.isNotBlank()) user.put("flow", node.flow)
                JSONObject().put("vnext", JSONArray().put(
                    JSONObject()
                        .put("address", node.address)
                        .put("port", node.port)
                        .put("users", JSONArray().put(user))
                ))
            }
            "vmess" -> JSONObject().put("vnext", JSONArray().put(
                JSONObject()
                    .put("address", node.address)
                    .put("port", node.port)
                    .put("users", JSONArray().put(
                        JSONObject()
                            .put("id", node.uuid)
                            .put("alterId", 0)
                            .put("security", node.security.ifBlank { "auto" })
                    ))
            ))
            "trojan" -> JSONObject().put("servers", JSONArray().put(
                JSONObject()
                    .put("address", node.address)
                    .put("port", node.port)
                    .put("password", node.password)
                    .put("level", 0)
            ))
            else -> JSONObject().put("servers", JSONArray().put( // shadowsocks
                JSONObject()
                    .put("address", node.address)
                    .put("port", node.port)
                    .put("method", node.method)
                    .put("password", node.password)
            ))
        }
    }

    private fun buildStream(node: Node): JSONObject {
        val stream = JSONObject()
        stream.put("network", node.network)

        when (node.tls) {
            "tls" -> stream
                .put("security", "tls")
                .put("tlsSettings", JSONObject()
                    .put("serverName", node.sni.ifBlank { node.address })
                    .put("allowInsecure", false)
                    .put("fingerprint", node.fingerprint.ifBlank { "chrome" })
                    .put("alpn", if (node.alpn.isBlank()) JSONArray() else JSONArray().put(node.alpn)))
            "reality" -> stream
                .put("security", "reality")
                .put("realitySettings", JSONObject()
                    .put("serverName", node.sni.ifBlank { node.address })
                    .put("fingerprint", node.fingerprint.ifBlank { "chrome" })
                    .put("publicKey", node.publicKey)
                    .put("shortId", node.shortId)
                    .put("spiderX", node.spiderX))
            else -> stream.put("security", "none")
        }

        when (node.network) {
            "ws" -> stream.put("wsSettings", JSONObject()
                .put("path", node.path)
                .put("headers", if (node.host.isBlank()) JSONObject() else JSONObject().put("Host", node.host)))
            "grpc" -> {
                val g = JSONObject()
                if (node.serviceName.isNotBlank()) g.put("serviceName", node.serviceName)
                stream.put("grpcSettings", g)
            }
            "h2" -> {
                val h = JSONObject()
                if (node.host.isNotBlank()) h.put("host", node.host)
                if (node.path.isNotBlank()) h.put("path", node.path)
                stream.put("httpSettings", h)
            }
        }
        return stream
    }
}
