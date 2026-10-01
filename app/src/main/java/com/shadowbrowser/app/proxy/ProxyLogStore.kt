package com.shadowbrowser.app.proxy

import java.util.concurrent.CopyOnWriteArrayList

/**
 * 代理引擎日志缓冲（内存），供“网络日志”页展示。
 * 线程安全，仅保留最近 N 条。
 */
object ProxyLogStore {

    private const val MAX = 300
    private val logs = CopyOnWriteArrayList<String>()

    fun add(line: String) {
        if (logs.size >= MAX) logs.removeAt(0)
        logs.add("%tT ".format(java.util.Date()) + line)
    }

    fun all(): List<String> = logs.toList()

    fun clear() = logs.clear()
}
