package com.shadowbrowser.app.nodes

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * 订阅拉取：请求订阅地址 → 解析返回的链接列表 → 解析成节点。
 */
class SubscriptionManager {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /** @return 解析出的节点列表；失败抛异常由调用方处理 */
    suspend fun fetch(url: String): List<Node> = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(url).header("User-Agent", "ShadowBrowser/1.0").build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw RuntimeException("HTTP ${resp.code}")
            val body = resp.body?.string().orEmpty()
            NodeLinkParser.parseMany(body)
        }
    }
}
