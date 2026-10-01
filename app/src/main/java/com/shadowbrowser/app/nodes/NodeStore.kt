package com.shadowbrowser.app.nodes

import android.content.Context
import com.google.gson.Gson
import java.io.File

/**
 * 轻量持久化：节点列表 + 当前选中节点 + 订阅地址。
 * 使用 filesDir 下的 JSON 文件保存，避免引入数据库依赖。
 */
class NodeStore(context: Context) {

    private val gson = Gson()
    private val file = File(context.filesDir, "nodes.json")

    private data class Wrapper(
        val nodes: List<Node> = emptyList(),
        val selectedId: String? = null,
        val subscriptionUrl: String? = null
    )

    @Volatile private var cache: Wrapper? = null

    @Synchronized
    private fun load(): Wrapper {
        cache?.let { return it }
        val w = try {
            if (file.exists()) gson.fromJson(file.readText(), Wrapper::class.java) ?: Wrapper()
            else Wrapper()
        } catch (_: Exception) {
            Wrapper()
        }
        cache = w
        return w
    }

    @Synchronized
    private fun save(w: Wrapper) {
        cache = w
        try {
            file.parentFile?.mkdirs()
            file.writeText(gson.toJson(w))
        } catch (_: Exception) {
        }
    }

    fun nodes(): List<Node> = load().nodes

    fun selectedId(): String? = load().selectedId

    fun selectedNode(): Node? = load().nodes.firstOrNull { it.id == load().selectedId }

    fun subscriptionUrl(): String? = load().subscriptionUrl

    fun addNode(node: Node) {
        val w = load()
        val list = w.nodes.toMutableList()
        list.add(node)
        save(w.copy(nodes = list))
    }

    fun updateNode(node: Node) {
        val w = load()
        val list = w.nodes.toMutableList()
        val idx = list.indexOfFirst { it.id == node.id }
        if (idx >= 0) list[idx] = node else list.add(node)
        save(w.copy(nodes = list))
    }

    fun deleteNode(id: String) {
        val w = load()
        val list = w.nodes.filterNot { it.id == id }
        val sel = if (w.selectedId == id) null else w.selectedId
        save(w.copy(nodes = list, selectedId = sel))
    }

    fun setSelected(id: String?) {
        save(load().copy(selectedId = id))
    }

    fun setSubscription(url: String?) {
        save(load().copy(subscriptionUrl = url))
    }

    /** 批量导入（来自订阅），保留已有节点，按 地址+端口+协议 去重 */
    fun importNodes(newNodes: List<Node>): Int {
        val w = load()
        val existing = w.nodes.toMutableList()
        val existingKeys = existing.map { it.address + ":" + it.port + ":" + it.protocol }.toHashSet()
        var added = 0
        for (n in newNodes) {
            val key = n.address + ":" + n.port + ":" + n.protocol
            if (existingKeys.add(key)) {
                existing.add(n)
                added++
            }
        }
        save(w.copy(nodes = existing))
        return added
    }
}
