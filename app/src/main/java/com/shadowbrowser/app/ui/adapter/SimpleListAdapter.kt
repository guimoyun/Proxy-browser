package com.shadowbrowser.app.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.shadowbrowser.app.R

/**
 * 通用两行列表（书签/历史用）：主标题 + 副标题(URL) + 可选右侧按钮。
 * 注意：onClick 放在参数最后，便于尾随 lambda 调用。
 */
class SimpleListAdapter(
    private val items: List<Pair<String, String>>,
    private val onDelete: ((Pair<String, String>) -> Unit)? = null,
    private val onClick: (Pair<String, String>) -> Unit
) : RecyclerView.Adapter<SimpleListAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val title: TextView = v.findViewById(R.id.listTitle)
        val url: TextView = v.findViewById(R.id.listUrl)
        val del: ImageButton = v.findViewById(R.id.listDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_list, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.title.text = item.first
        holder.url.text = item.second
        holder.itemView.setOnClickListener { onClick(item) }
        if (onDelete != null) {
            holder.del.visibility = View.VISIBLE
            holder.del.setOnClickListener { onDelete?.invoke(item) }
        } else holder.del.visibility = View.GONE
    }

    override fun getItemCount() = items.size
}
