package com.shadowbrowser.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.shadowbrowser.app.R
import com.shadowbrowser.app.databinding.FragmentNodeEditBinding
import com.shadowbrowser.app.nodes.Node
import com.shadowbrowser.app.nodes.NodeLinkParser
import com.shadowbrowser.app.nodes.NodeStore
import com.shadowbrowser.app.proxy.ProxyManager

/**
 * 手动添加 / 编辑节点。顶部支持粘贴分享链接一键填充，其余字段可手动编辑。
 */
class NodeEditFragment : Fragment() {

    private var _b: FragmentNodeEditBinding? = null
    private val b get() = _b!!

    private var editId: String? = null

    companion object {
        fun newInstance(id: String? = null): NodeEditFragment =
            NodeEditFragment().apply { arguments = Bundle().apply { putString("id", id) } }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _b = FragmentNodeEditBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        editId = arguments?.getString("id")
        val store = ProxyManager.init(requireContext()).nodeStore

        b.toolbarBack.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
        b.toolbarTitle.text = if (editId == null) getString(R.string.add_node) else getString(R.string.edit_node)

        setupSpinners()
        b.btnFillLink.setOnClickListener { fillFromLink(store) }
        if (editId != null) {
            store.nodes().firstOrNull { it.id == editId }?.let { fillNode(it) }
        }
        b.btnCancel.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
        b.btnSave.setOnClickListener { save(store) }
    }

    private fun setupSpinners() {
        val ctx = requireContext()
        b.spProtocol.setAdapter(ArrayAdapter(ctx, android.R.layout.simple_list_item_1,
            arrayOf("vless", "vmess", "trojan", "shadowsocks")))
        b.spNetwork.setAdapter(ArrayAdapter(ctx, android.R.layout.simple_list_item_1,
            arrayOf("tcp", "ws", "grpc", "h2")))
        b.spTls.setAdapter(ArrayAdapter(ctx, android.R.layout.simple_list_item_1,
            arrayOf("none", "tls", "reality")))
        b.spSecurity.setAdapter(ArrayAdapter(ctx, android.R.layout.simple_list_item_1,
            arrayOf("auto", "aes-128-gcm", "chacha20-poly1305", "none")))
    }

    private fun fillFromLink(store: NodeStore) {
        val link = b.pasteLink.text?.toString()?.trim().orEmpty()
        if (link.isEmpty()) {
            Toast.makeText(requireContext(), "请先粘贴分享链接", Toast.LENGTH_SHORT).show(); return
        }
        val n = NodeLinkParser.parse(link)
        if (n == null) { Toast.makeText(requireContext(), "链接无法解析", Toast.LENGTH_SHORT).show(); return }
        fillNode(n)
        Toast.makeText(requireContext(), "已按链接填充，可修改后保存", Toast.LENGTH_SHORT).show()
    }

    private fun fillNode(n: Node) {
        b.etRemark.setText(n.remark)
        (b.spProtocol as? MaterialAutoCompleteTextView)?.setText(n.protocol, false)
        b.etAddress.setText(n.address)
        b.etPort.setText(n.port.toString())
        b.etUuid.setText(n.uuid)
        b.etPassword.setText(n.password)
        (b.spNetwork as? MaterialAutoCompleteTextView)?.setText(n.network, false)
        (b.spTls as? MaterialAutoCompleteTextView)?.setText(n.tls, false)
        (b.spSecurity as? MaterialAutoCompleteTextView)?.setText(n.security, false)
        b.etMethod.setText(n.method)
        b.etSni.setText(n.sni)
        b.etHost.setText(n.host)
        b.etPath.setText(n.path)
        b.etService.setText(n.serviceName)
        b.etFlow.setText(n.flow)
        b.etPublicKey.setText(n.publicKey)
        b.etShortId.setText(n.shortId)
    }

    private fun save(store: NodeStore) {
        val proto = (b.spProtocol as? MaterialAutoCompleteTextView)?.text?.toString()?.trim().orEmpty()
        val addr = b.etAddress.text?.toString()?.trim().orEmpty()
        val port = b.etPort.text?.toString()?.trim()?.toIntOrNull()
        if (proto.isEmpty() || addr.isEmpty() || port == null) {
            Toast.makeText(requireContext(), "协议、地址、端口为必填", Toast.LENGTH_SHORT).show()
            return
        }
        val node = store.nodes().firstOrNull { it.id == editId }?.copy() ?: Node()
        node.protocol = proto
        node.remark = b.etRemark.text?.toString()?.trim().orEmpty()
        node.address = addr
        node.port = port
        node.uuid = b.etUuid.text?.toString()?.trim().orEmpty()
        node.password = b.etPassword.text?.toString()?.trim().orEmpty()
        node.network = (b.spNetwork as? MaterialAutoCompleteTextView)?.text?.toString()?.trim().orEmpty().ifBlank { "tcp" }
        node.tls = (b.spTls as? MaterialAutoCompleteTextView)?.text?.toString()?.trim().orEmpty().ifBlank { "none" }
        node.security = (b.spSecurity as? MaterialAutoCompleteTextView)?.text?.toString()?.trim().orEmpty().ifBlank { "auto" }
        node.method = b.etMethod.text?.toString()?.trim().orEmpty().ifBlank { "aes-256-gcm" }
        node.sni = b.etSni.text?.toString()?.trim().orEmpty()
        node.host = b.etHost.text?.toString()?.trim().orEmpty()
        node.path = b.etPath.text?.toString()?.trim().orEmpty()
        node.serviceName = b.etService.text?.toString()?.trim().orEmpty()
        node.flow = b.etFlow.text?.toString()?.trim().orEmpty()
        node.publicKey = b.etPublicKey.text?.toString()?.trim().orEmpty()
        node.shortId = b.etShortId.text?.toString()?.trim().orEmpty()

        store.updateNode(node)
        Toast.makeText(requireContext(), "已保存", Toast.LENGTH_SHORT).show()
        requireActivity().onBackPressedDispatcher.onBackPressed()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
