package com.shadowbrowser.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.shadowbrowser.app.R
import com.shadowbrowser.app.databinding.FragmentNodesBinding
import com.shadowbrowser.app.nodes.LatencyTester
import com.shadowbrowser.app.nodes.Node
import com.shadowbrowser.app.nodes.SubscriptionManager
import com.shadowbrowser.app.proxy.ProxyManager
import com.shadowbrowser.app.ui.adapter.NodeListAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 代理节点页：列表 + 选择/连接/测延迟/编辑/删除 + 手动添加 + 订阅导入。
 */
class NodesFragment : Fragment() {

    private var _b: FragmentNodesBinding? = null
    private val b get() = _b!!
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val latency = HashMap<String, Long>()

    private lateinit var proxy: ProxyManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _b = FragmentNodesBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        proxy = ProxyManager.init(requireContext())
        b.toolbarBack.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
        b.toolbarTitle.text = getString(R.string.node_title)
        b.btnAdd.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, NodeEditFragment.newInstance())
                .addToBackStack(null)
                .commit()
        }
        b.btnImport.setOnClickListener { showImportDialog() }

        b.list.layoutManager = LinearLayoutManager(requireContext())
        refresh()
    }

    private fun refresh() {
        val nodes = proxy.nodeStore.nodes()
        b.emptyView.visibility = if (nodes.isEmpty()) View.VISIBLE else View.GONE
        b.list.visibility = if (nodes.isEmpty()) View.GONE else View.VISIBLE
        b.list.adapter = NodeListAdapter(
            nodes,
            proxy.nodeStore.selectedId(),
            latency,
            onSelect = { node ->
                proxy.nodeStore.setSelected(node.id)
                refresh()
            },
            onConnect = { node ->
                proxy.nodeStore.setSelected(node.id)
                proxy.start(node)
                requireActivity().onBackPressedDispatcher.onBackPressed()
                Toast.makeText(requireContext(), "正在连接 ${node.displayName()}", Toast.LENGTH_SHORT).show()
            },
            onLongPress = { node -> showActions(node) }
        )
    }

    private fun showActions(node: Node) {
        val items = arrayOf(
            getString(R.string.connect), getString(R.string.test_latency),
            getString(R.string.edit_node), getString(R.string.delete_node)
        )
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(node.displayName())
            .setItems(items) { _, which ->
                when (which) {
                    0 -> {
                        proxy.nodeStore.setSelected(node.id)
                        proxy.start(node)
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    }
                    1 -> test(node)
                    2 -> {
                        requireActivity().supportFragmentManager.beginTransaction()
                            .replace(R.id.fragmentContainer, NodeEditFragment.newInstance(node.id))
                            .addToBackStack(null)
                            .commit()
                    }
                    3 -> confirmDelete(node)
                }
            }
            .show()
    }

    private fun test(node: Node) {
        scope.launch {
            latency[node.id] = -1
            refresh()
            val ms = LatencyTester.test(node)
            latency[node.id] = ms ?: -1
            refresh()
            Toast.makeText(requireContext(),
                if (ms != null) "延迟 ${ms}ms" else "连接超时", Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmDelete(node: Node) {
        AlertDialog.Builder(requireContext())
            .setMessage("删除节点 ${node.displayName()}？")
            .setNegativeButton("取消", null)
            .setPositiveButton("删除") { _, _ ->
                proxy.nodeStore.deleteNode(node.id)
                if (proxy.isConnected() && proxy.activeNode.value?.id == node.id) proxy.stop()
                refresh()
            }
            .show()
    }

    private fun showImportDialog() {
        val input = android.widget.EditText(requireContext()).apply {
            hint = getString(R.string.subscription_url)
            setText(proxy.nodeStore.subscriptionUrl() ?: "")
            setSingleLine(true)
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.import_subscription))
            .setView(input)
            .setNegativeButton("取消", null)
            .setPositiveButton(getString(R.string.subscription_import)) { _, _ ->
                val url = input.text.toString().trim()
                if (url.isEmpty()) return@setPositiveButton
                importSubscription(url)
            }
            .show()
    }

    private fun importSubscription(url: String) {
        b.btnImport.isEnabled = false
        Toast.makeText(requireContext(), getString(R.string.subscription_fetching), Toast.LENGTH_SHORT).show()
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                try { SubscriptionManager().fetch(url) } catch (e: Exception) { null }
            }
            b.btnImport.isEnabled = true
            if (result == null || result.isEmpty()) {
                Toast.makeText(requireContext(), getString(R.string.subscription_fail), Toast.LENGTH_SHORT).show()
            } else {
                proxy.nodeStore.setSubscription(url)
                val added = proxy.nodeStore.importNodes(result)
                Toast.makeText(requireContext(),
                    getString(R.string.subscription_ok, added), Toast.LENGTH_SHORT).show()
                refresh()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
