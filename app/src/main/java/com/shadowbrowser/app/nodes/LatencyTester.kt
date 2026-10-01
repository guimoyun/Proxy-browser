package com.shadowbrowser.app.nodes

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

/**
 * 节点连通性/延迟测试：TCP 连接握手耗时（毫秒）。
 * 仅是网络可达性参考，非代理链路真实延迟。
 */
object LatencyTester {

    suspend fun test(node: Node, timeoutMs: Int = 4000): Long? = withContext(Dispatchers.IO) {
        try {
            val start = System.nanoTime()
            Socket().use { s ->
                s.connect(InetSocketAddress(node.address, node.port), timeoutMs)
            }
            (System.nanoTime() - start) / 1_000_000
        } catch (_: Exception) {
            null
        }
    }
}
