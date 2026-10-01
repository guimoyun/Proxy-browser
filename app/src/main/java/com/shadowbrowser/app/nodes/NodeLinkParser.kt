package com.shadowbrowser.app.nodes

import android.util.Base64
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/**
 * 分享链接 / 订阅解析器。支持：
 *  - vless://uuid@host:port?query#remark
 *  - trojan://password@host:port?query#remark
 *  - vmess://  base64(JSON)
 *  - ss://    SIP002 或 base64(method:password@host:port)
 */
object NodeLinkParser {

    /** 解析单条链接；无法识别返回 null */
    fun parse(link: String): Node? {
        val s = link.trim()
        return when {
            s.startsWith("vless://") -> parseVless(s)
            s.startsWith("trojan://") -> parseTrojan(s)
            s.startsWith("vmess://") -> parseVmess(s)
            s.startsWith("ss://") -> parseSs(s)
            else -> null
        }
    }

    /** 解析文本块（可能是多行、空格分隔、或整体 base64 的订阅内容），返回节点列表 */
    fun parseMany(raw: String): List<Node> {
        var text = raw.trim()
        // 订阅常为整体 base64
        if (text.length > 40 && text.matches(Regex("[A-Za-z0-9+/=_-]+")) && text.contains("://").not()) {
            text = safeBase64Decode(text) ?: raw
        }
        val links = text.split(Regex("\\s+")).filter { it.startsWith("http") || it.contains("://") }
        return links.mapNotNull { parse(it) }
    }

    // ---------------- vless ----------------
    private fun parseVless(link: String): Node? {
        val core = link.removePrefix("vless://")
        val remark = fragment(core)
        val q = query(core)
        val (auth, hostPort) = splitAuthHost(core)
        val (host, port) = splitHostPort(hostPort) ?: return null
        return Node(
            protocol = "vless",
            remark = remark,
            address = host,
            port = port,
            uuid = auth,
            encryption = q["encryption"] ?: "none",
            flow = q["flow"] ?: "",
            network = q["type"] ?: "tcp",
            tls = when (q["security"]) {
                "tls" -> "tls"
                "reality" -> "reality"
                else -> "none"
            },
            sni = q["sni"] ?: q["host"] ?: "",
            alpn = q["alpn"] ?: "",
            fingerprint = q["fp"] ?: "",
            publicKey = q["pbk"] ?: "",
            shortId = q["sid"] ?: "",
            spiderX = q["spx"] ?: "",
            host = q["host"] ?: "",
            path = q["path"] ?: "",
            serviceName = q["serviceName"] ?: ""
        )
    }

    // ---------------- trojan ----------------
    private fun parseTrojan(link: String): Node? {
        val core = link.removePrefix("trojan://")
        val remark = fragment(core)
        val q = query(core)
        val (auth, hostPort) = splitAuthHost(core)
        val (host, port) = splitHostPort(hostPort) ?: return null
        return Node(
            protocol = "trojan",
            remark = remark,
            address = host,
            port = port,
            password = auth,
            network = q["type"] ?: "tcp",
            tls = if (q["security"] == "reality") "reality" else if ((q["security"] ?: "tls") == "tls") "tls" else "none",
            sni = q["sni"] ?: q["host"] ?: "",
            alpn = q["alpn"] ?: "",
            fingerprint = q["fp"] ?: "",
            publicKey = q["pbk"] ?: "",
            shortId = q["sid"] ?: "",
            spiderX = q["spx"] ?: "",
            host = q["host"] ?: "",
            path = q["path"] ?: "",
            serviceName = q["serviceName"] ?: ""
        )
    }

    // ---------------- vmess ----------------
    private fun parseVmess(link: String): Node? {
        val b64 = link.removePrefix("vmess://")
        val json = safeBase64Decode(b64) ?: return null
        return try {
            val obj = org.json.JSONObject(json)
            Node(
                protocol = "vmess",
                remark = obj.optString("ps", ""),
                address = obj.optString("add", ""),
                port = obj.optInt("port", 443),
                uuid = obj.optString("id", ""),
                security = obj.optString("scy", "auto").ifBlank { "auto" },
                network = obj.optString("net", "tcp").ifBlank { "tcp" },
                tls = if (obj.optString("tls", "") == "tls") "tls" else "none",
                sni = obj.optString("sni", ""),
                host = obj.optString("host", ""),
                path = obj.optString("path", ""),
                alpn = obj.optString("alpn", ""),
                fingerprint = obj.optString("fp", "")
            )
        } catch (_: Exception) {
            null
        }
    }

    // ---------------- shadowsocks ----------------
    private fun parseSs(link: String): Node? {
        val core = link.removePrefix("ss://")
        val remark = fragment(core)
        // SIP002: method:password@host:port#remark
        if (core.contains("@")) {
            val (creds, hostPort) = splitAuthHost(core)
            val (method, password) = creds.split(":", limit = 2).let {
                it[0] to if (it.size > 1) it[1] else ""
            }
            val (host, port) = splitHostPort(hostPort) ?: return null
            return Node(
                protocol = "shadowsocks", remark = remark, address = host, port = port,
                method = method, password = password
            )
        }
        // 老式 base64
        val b64 = core.substringBefore("#")
        val decoded = safeBase64Decode(b64) ?: return null
        if (!decoded.contains("@")) return null
        val (creds, hostPort) = splitAuthHost(decoded)
        val (method, password) = creds.split(":", limit = 2).let {
            it[0] to if (it.size > 1) it[1] else ""
        }
        val (host, port) = splitHostPort(hostPort) ?: return null
        return Node(
            protocol = "shadowsocks", remark = remark, address = host, port = port,
            method = method, password = password
        )
    }

    // ---------------- 工具 ----------------
    private fun fragment(s: String): String {
        val idx = s.lastIndexOf('#')
        return if (idx >= 0) URLDecoder.decode(s.substring(idx + 1), StandardCharsets.UTF_8.name()) else ""
    }

    private fun query(s: String): Map<String, String> {
        val q = s.substringAfter('?').substringBefore('#')
        return q.split("&").mapNotNull {
            val kv = it.split("=", limit = 2)
            if (kv.size == 2) kv[0] to URLDecoder.decode(kv[1], StandardCharsets.UTF_8.name()) else null
        }.toMap()
    }

    /** 去掉 query 与 fragment，返回 [auth, host:port] */
    private fun splitAuthHost(s: String): Pair<String, String> {
        val clean = s.substringBefore('?').substringBefore('#')
        val at = clean.lastIndexOf('@')
        return if (at >= 0) clean.substring(0, at) to clean.substring(at + 1)
        else "" to clean
    }

    private fun splitHostPort(hp: String): Pair<String, Int>? {
        val h = hp.substringBefore(':')
        val p = hp.substringAfter(':', "").toIntOrNull()
        return if (h.isNotBlank() && p != null) h to p else null
    }

    private fun safeBase64Decode(s: String): String? {
        val cleaned = s.trim().replace("\n", "").replace("\r", "")
        var b64 = cleaned
        // 补充 padding
        while (b64.length % 4 != 0) b64 += "="
        return try {
            val raw = Base64.decode(b64, Base64.URL_SAFE or Base64.NO_WRAP)
            String(raw, StandardCharsets.UTF_8)
        } catch (_: Exception) {
            try {
                val raw = Base64.decode(cleaned, Base64.DEFAULT)
                String(raw, StandardCharsets.UTF_8)
            } catch (_: Exception) {
                null
            }
        }
    }
}
