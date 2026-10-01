package com.shadowbrowser.app.browser

import android.content.Context
import com.google.gson.Gson
import java.io.File

/**
 * 书签存储（filesDir/bookmarks.json）。
 */
class BookmarkStore(context: Context) {

    data class Bookmark(val title: String, val url: String)

    private val gson = Gson()
    private val file = File(context.filesDir, "bookmarks.json")

    @Volatile private var cache: List<Bookmark>? = null

    @Synchronized
    fun all(): List<Bookmark> {
        cache?.let { return it }
        val list = try {
            if (file.exists())
                gson.fromJson(file.readText(), Array<Bookmark>::class.java)?.toList() ?: emptyList()
            else emptyList()
        } catch (_: Exception) {
            emptyList()
        }
        cache = list
        return list
    }

    @Synchronized
    fun add(title: String, url: String) {
        if (url.isBlank()) return
        val list = all().toMutableList()
        list.removeAll { it.url == url }
        list.add(0, Bookmark(title.ifBlank { url }, url))
        cache = list
        persist(list)
    }

    @Synchronized
    fun remove(url: String) {
        val list = all().filterNot { it.url == url }
        cache = list
        persist(list)
    }

    private fun persist(list: List<Bookmark>) {
        try {
            file.parentFile?.mkdirs()
            file.writeText(gson.toJson(list))
        } catch (_: Exception) {
        }
    }
}
