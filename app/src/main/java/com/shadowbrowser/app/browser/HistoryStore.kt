package com.shadowbrowser.app.browser

import android.content.Context
import com.google.gson.Gson
import java.io.File

/**
 * 历史记录存储（filesDir/history.json），最多保留 200 条。
 */
class HistoryStore(context: Context) {

    data class Entry(
        val title: String,
        val url: String,
        val visitedAt: Long = System.currentTimeMillis()
    )

    private val gson = Gson()
    private val file = File(context.filesDir, "history.json")

    @Volatile private var cache: List<Entry>? = null

    @Synchronized
    fun all(): List<Entry> {
        cache?.let { return it }
        val list = try {
            if (file.exists())
                gson.fromJson(file.readText(), Array<Entry>::class.java)?.toList() ?: emptyList()
            else emptyList()
        } catch (_: Exception) {
            emptyList()
        }
        cache = list
        return list
    }

    @Synchronized
    fun record(title: String, url: String) {
        if (url.isBlank() || !url.startsWith("http")) return
        val list = all().toMutableList()
        list.removeAll { it.url == url }
        list.add(0, Entry(title.ifBlank { url }, url))
        if (list.size > 200) list.subList(200, list.size).clear()
        cache = list
        persist(list)
    }

    @Synchronized
    fun clear() {
        cache = emptyList()
        persist(emptyList())
    }

    private fun persist(list: List<Entry>) {
        try {
            file.parentFile?.mkdirs()
            file.writeText(gson.toJson(list))
        } catch (_: Exception) {
        }
    }
}
