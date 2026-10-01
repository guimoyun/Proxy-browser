package com.shadowbrowser.app.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.shadowbrowser.app.R
import com.shadowbrowser.app.nodes.Node

/**
 * 节点列表：标题 + 协议/地址 + 延迟胶囊；点击选中/连接，长按编辑，右侧连接按钮。
 */
class NodeListAdapter(
    private val nodes: List<Node>,
    private val selectedId: String?,
    private val latency: Map<String, Long>,
    private val onSelect: (Node) -> Unit,
    private val onConnect: (Node) -> Unit,
    private val onLongPress: (Node) -> Unit
) : RecyclerView.Adapter<NodeListAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val title: TextView = v.findViewById(R.id.nodeTitle)
        val sub: TextView = v.findViewById(R.id.nodeSubtitle)
        val latency: TextView = v.findViewById(R.id.nodeLatency)
        val connect: View = v.findViewById(R.id.nodeConnect)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_node, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val node = nodes[position]
        holder.title.text = node.displayName()
        holder.sub.text = "${node.protocolLabel()} · ${node.address}:${node.port}"
        holder.title.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
        val l = latency[node.id]
        holder.latency.text = when {
            l == null -> "—"
            l < 0 -> "超时"
            else -> "${l}ms"
        }
        holder.itemView.setBackgroundResource(
            if (node.id == selectedId) R.drawable.bg_card else R.drawable.bg_surface
        )
        holder.itemView.setOnClickListener { onSelect(node) }
        holder.itemView.setOnLongClickListener { onLongPress(node); true }
        holder.connect.setOnClickListener { onConnect(node) }
    }

    override fun getItemCount() = nodes.size
}
