package com.shadowbrowser.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.shadowbrowser.app.R
import com.shadowbrowser.app.browser.HistoryStore
import com.shadowbrowser.app.databinding.FragmentSettingsBinding
import com.shadowbrowser.app.proxy.ProxyManager

/**
 * 设置页（参考 Via 的分组列表）：代理 / 数据 / 关于。
 */
class SettingsFragment : Fragment() {

    private var _b: FragmentSettingsBinding? = null
    private val b get() = _b!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _b = FragmentSettingsBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        b.toolbarBack.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
        b.toolbarTitle.text = getString(R.string.settings_title)

        val container = b.container
        container.addView(section("代理"))
        container.addView(row("代理节点管理", "导入/编辑/连接节点") { open(NodesFragment()) })
        container.addView(row("指纹伪装", "UA / 时区 / WebRTC") { open(IdentityFragment()) })
        val statusText = TextView(requireContext()).apply {
            text = "当前状态：" + proxyStatus()
            setPadding(48, 8, 16, 8)
            textSize = 14f
        }
        container.addView(statusText)

        container.addView(section("数据"))
        container.addView(row("清除浏览数据", "书签/历史/缓存") {
            HistoryStore(requireContext()).clear()
            Toast.makeText(requireContext(), getString(R.string.cleared), Toast.LENGTH_SHORT).show()
        })

        container.addView(section("关于"))
        container.addView(row("版本", "1.0.0") {})
        container.addView(TextView(requireContext()).apply {
            text = getString(R.string.about_text)
            setPadding(48, 12, 48, 24)
            textSize = 13f
            setTextColor(resources.getColor(R.color.text_secondary, null))
        })
    }

    private fun proxyStatus(): String {
        return when (ProxyManager.init(requireContext()).status.value) {
            com.shadowbrowser.app.proxy.ProxyStatus.CONNECTED -> "已连接"
            com.shadowbrowser.app.proxy.ProxyStatus.CONNECTING -> "连接中"
            com.shadowbrowser.app.proxy.ProxyStatus.ERROR -> "连接失败"
            else -> "未连接"
        }
    }

    private fun section(title: String): View {
        val tv = TextView(requireContext()).apply {
            text = title
            setTextColor(resources.getColor(R.color.text_secondary, null))
            textSize = 13f
            setPadding(48, 24, 16, 6)
        }
        return tv
    }

    private fun row(title: String, sub: String, onClick: () -> Unit): View {
        val v = layoutInflater.inflate(R.layout.item_setting_row, b.container, false)
        v.findViewById<TextView>(R.id.rowTitle).text = title
        v.findViewById<TextView>(R.id.rowSub).text = sub
        v.findViewById<View>(R.id.rowChevron).visibility = View.VISIBLE
        v.setOnClickListener { onClick() }
        return v
    }

    private fun open(frag: Fragment) {
        requireActivity().supportFragmentManager.beginTransaction()
            .add(R.id.fragmentContainer, frag)
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
