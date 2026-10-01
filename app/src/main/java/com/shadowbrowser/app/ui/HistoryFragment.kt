package com.shadowbrowser.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.shadowbrowser.app.R
import com.shadowbrowser.app.browser.HistoryStore
import com.shadowbrowser.app.databinding.FragmentListBinding
import com.shadowbrowser.app.ui.adapter.SimpleListAdapter

/**
 * 历史记录页。
 */
class HistoryFragment : Fragment() {

    private var _b: FragmentListBinding? = null
    private val b get() = _b!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _b = FragmentListBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val store = HistoryStore(requireContext())
        b.toolbarBack.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
        b.toolbarTitle.text = getString(R.string.m_history)

        val items = store.all().map { it.title to it.url }
        if (items.isEmpty()) {
            b.list.visibility = View.GONE
            b.emptyView.visibility = View.VISIBLE
            b.emptyView.text = "暂无历史"
        } else {
            b.list.visibility = View.VISIBLE
            b.emptyView.visibility = View.GONE
            b.list.layoutManager = LinearLayoutManager(requireContext())
            b.list.adapter = SimpleListAdapter(items) { open(it.second) }
        }
    }

    private fun open(url: String) {
        requireActivity().supportFragmentManager.popBackStackImmediate()
        BrowserBridge.openUrl(url)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
