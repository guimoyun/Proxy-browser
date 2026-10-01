package com.shadowbrowser.app.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.shadowbrowser.app.R
import com.shadowbrowser.app.browser.TabInfo

/**
 * 标签页列表（标签面板用）：标题 + URL + 关闭按钮，点击切换。
 */
class TabListAdapter(
    private val tabs: List<TabInfo>,
    private val currentId: Long,
    private val onOpen: (TabInfo) -> Unit,
    private val onClose: (TabInfo) -> Unit
) : RecyclerView.Adapter<TabListAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val title: TextView = v.findViewById(R.id.tabTitle)
        val url: TextView = v.findViewById(R.id.tabUrl)
        val close: View = v.findViewById(R.id.tabClose)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_tab, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val tab = tabs[position]
        holder.title.text = if (tab.title.isBlank()) "新标签页" else tab.title
        holder.url.text = tab.url
        holder.itemView.setBackgroundResource(
            if (tab.id == currentId) R.drawable.bg_card else R.drawable.bg_surface
        )
        holder.itemView.setOnClickListener { onOpen(tab) }
        holder.close.setOnClickListener { onClose(tab) }
    }

    override fun getItemCount() = tabs.size
}
