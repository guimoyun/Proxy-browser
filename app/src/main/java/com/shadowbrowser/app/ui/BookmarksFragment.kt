package com.shadowbrowser.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.shadowbrowser.app.R
import com.shadowbrowser.app.browser.BookmarkStore
import com.shadowbrowser.app.databinding.FragmentListBinding
import com.shadowbrowser.app.ui.adapter.SimpleListAdapter

/**
 * 书签页。
 */
class BookmarksFragment : Fragment() {

    private var _b: FragmentListBinding? = null
    private val b get() = _b!!
    private lateinit var store: BookmarkStore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _b = FragmentListBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        store = BookmarkStore(requireContext())
        b.toolbarBack.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
        b.toolbarTitle.text = getString(R.string.m_bookmark)
        refresh()
    }

    private fun refresh() {
        val items = store.all().map { it.title to it.url }
        if (items.isEmpty()) {
            b.list.visibility = View.GONE
            b.emptyView.visibility = View.VISIBLE
            b.emptyView.text = "暂无书签"
        } else {
            b.list.visibility = View.VISIBLE
            b.emptyView.visibility = View.GONE
            b.list.layoutManager = LinearLayoutManager(requireContext())
            b.list.adapter = SimpleListAdapter(
                items,
                onDelete = { store.remove(it.second); refresh() },
                onClick = { open(it.second) }
            )
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
