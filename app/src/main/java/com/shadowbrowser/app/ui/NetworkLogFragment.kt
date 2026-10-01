package com.shadowbrowser.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.shadowbrowser.app.R
import com.shadowbrowser.app.databinding.FragmentNetworkLogBinding
import com.shadowbrowser.app.proxy.ProxyLogStore

/**
 * 网络日志页：展示代理引擎与状态日志（内存缓冲）。
 */
class NetworkLogFragment : Fragment() {

    private var _b: FragmentNetworkLogBinding? = null
    private val b get() = _b!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _b = FragmentNetworkLogBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        b.toolbarBack.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
        b.toolbarTitle.text = getString(R.string.m_netlog)
        b.btnClear.setOnClickListener {
            ProxyLogStore.clear()
            render()
        }
        render()
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        val logs = ProxyLogStore.all()
        b.logView.text = if (logs.isEmpty()) "暂无日志" else logs.joinToString("\n")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
