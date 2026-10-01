package com.shadowbrowser.app.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.shadowbrowser.app.R
import com.shadowbrowser.app.browser.BookmarkStore
import com.shadowbrowser.app.browser.BrowserClient
import com.shadowbrowser.app.browser.ChromeClient
import com.shadowbrowser.app.browser.FingerprintEngine
import com.shadowbrowser.app.browser.HistoryStore
import com.shadowbrowser.app.browser.TabInfo
import com.shadowbrowser.app.browser.TabManager
import com.shadowbrowser.app.databinding.FragmentBrowserBinding
import com.shadowbrowser.app.proxy.ProxyManager
import com.shadowbrowser.app.proxy.ProxyStatus
import com.shadowbrowser.app.ui.adapter.TabListAdapter
import kotlinx.coroutines.launch

/**
 * 浏览器主界面，布局参考 Via 浏览器：
 * 顶部工具栏（搜索+标题+代理状态+收起）→ 内容区（主页/WebView）→ 底部五键导航。
 * 菜单为底部弹出面板（3 页，每页 10 项，参考 Via 样式）。
 */
@SuppressLint("SetJavaScriptEnabled")
class BrowserFragment : Fragment() {

    private var _b: FragmentBrowserBinding? = null
    private val b get() = _b!!

    private lateinit var fingerprint: FingerprintEngine
    private lateinit var proxy: ProxyManager
    private lateinit var bookmarks: BookmarkStore
    private lateinit var history: HistoryStore
    private lateinit var tabManager: TabManager

    private var incognito = false

    private val externalLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _b = FragmentBrowserBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val ctx = requireContext()
        fingerprint = FingerprintEngine(ctx)
        proxy = ProxyManager.init(ctx)
        bookmarks = BookmarkStore(ctx)
        history = HistoryStore(ctx)

        tabManager = TabManager(
            ctx, fingerprint,
            BrowserClient(fingerprint, object : BrowserClient.Listener {
                override fun onPageStarted(url: String?) = Unit

                override fun onPageFinished(url: String?, title: String?) {
                    tabManager.current?.let { t ->
                        t.url = url ?: ""
                        t.title = title ?: "无标题"
                        history.record(title ?: url ?: "", url ?: "")
                        renderTabsBadge()
                    }
                    renderTitle()
                }

                override fun onExternalUrl(url: String) {
                    externalLauncher.launch(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
            }),
            ChromeClient(requireActivity(), object : ChromeClient.Listener {
                override fun onTitle(title: String?, url: String?) = renderTitle()

                override fun onProgress(progress: Int) {
                    val b = _b ?: return
                    b.progressBar.progress = progress
                    b.progressBar.visibility = if (progress in 1..99) View.VISIBLE else View.GONE
                }

                override fun onShowFullscreen() = Unit
                override fun onHideFullscreen() = Unit
            })
        )

        setupToolbar()
        setupBottomNav()
        setupFindBar()
        setupHome()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                proxy.status.collect { renderProxyStatus() }
            }
        }
        renderProxyStatus()

        BrowserBridge.bind(this)
        if (tabManager.current == null) {
            newTab()
        } else {
            attachWebView(tabManager.current!!)
            if (tabManager.current!!.url.isBlank()) showHome() else showWeb()
            renderTabsBadge()
            renderTitle()
        }
    }

    // ---------------- 工具栏 ----------------
    private fun setupToolbar() {
        b.toolbarSearch.setOnClickListener { focusAddressBar() }
        b.toolbarTitle.setOnClickListener { focusAddressBar() }
        b.toolbarCollapse.setOnClickListener { goHome() }
        b.proxyChip.setOnClickListener { proxy.toggle() }
        b.proxyChip.setOnLongClickListener { openNodes(); true }
        b.addressInput.setOnEditorActionListener { _, _, _ -> submitAddress(); true }
    }

    private fun focusAddressBar() {
        val url = tabManager.current?.url.orEmpty()
        b.addressInput.setText(url)
        b.addressInput.visibility = View.VISIBLE
        b.toolbarTitle.visibility = View.GONE
        b.addressInput.requestFocus()
        (requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
                as android.view.inputmethod.InputMethodManager)
            .showSoftInput(b.addressInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
    }

    private fun submitAddress() {
        var raw = b.addressInput.text?.toString()?.trim().orEmpty()
        hideAddressBar()
        if (raw.isEmpty()) return
        if (!raw.contains("://")) {
            raw = if (raw.contains(".") && !raw.contains(" "))
            "https://$raw"
            else "https://www.google.com/search?q=${Uri.encode(raw)}"
        }
        loadUrl(raw)
    }

    private fun hideAddressBar() {
        b.addressInput.visibility = View.GONE
        b.toolbarTitle.visibility = View.VISIBLE
    }

    private fun renderTitle() {
        val t = tabManager.current
        b.toolbarTitle.text = when {
            t == null || t.url.isBlank() -> getString(R.string.home)
            else -> t.title
        }
    }

    private fun renderProxyStatus() {
        val b = _b ?: return
        val s = proxy.status.value
        b.proxyChip.setBackgroundResource(
            when (s) {
                ProxyStatus.CONNECTED -> R.drawable.bg_pill_green
                ProxyStatus.CONNECTING -> R.drawable.bg_pill_amber
                ProxyStatus.ERROR -> R.drawable.bg_pill_red
                else -> R.drawable.bg_pill_gray
            }
        )
        val node = proxy.activeNode.value
        b.proxyChip.text = when (s) {
            ProxyStatus.CONNECTED -> "● ${node?.remark?.ifBlank { node?.address } ?: getString(R.string.proxy_connected)}"
            ProxyStatus.CONNECTING -> getString(R.string.proxy_connecting)
            ProxyStatus.ERROR -> getString(R.string.proxy_error)
            else -> getString(R.string.proxy_disconnected)
        }
    }

    // ---------------- 内容区 / 标签页 ----------------
    private fun newTab(incognito: Boolean = false) {
        this.incognito = incognito
        val tab = tabManager.createTab(incognito)
        val old = tabManager.activate(tab)
        attachWebView(tab)
        detachWebView(old)
        showHome()
        renderTabsBadge()
        renderTitle()
    }

    private fun attachWebView(tab: TabInfo) {
        val wv = tab.webView
        (wv.parent as? ViewGroup)?.removeView(wv)
        b.webContainer.addView(wv, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ))
    }

    private fun detachWebView(tab: TabInfo?) {
        tab?.webView?.let { (it.parent as? ViewGroup)?.removeView(it) }
    }

    private fun showHome() {
        b.homeView.visibility = View.VISIBLE
        b.webContainer.visibility = View.GONE
        b.homeSearchInput.text = null
        renderProxyStatus()
        renderTitle()
    }

    private fun showWeb() {
        b.homeView.visibility = View.GONE
        b.webContainer.visibility = View.VISIBLE
        renderTitle()
    }

    private fun goHome() {
        tabManager.current?.webView?.stopLoading()
        showHome()
    }

    fun loadUrl(url: String) {
        val tab = tabManager.current ?: return
        if (tab.url.isBlank()) {
            tab.webView.loadUrl(url)
            attachWebView(tab)
        } else {
            newTab(incognito)
            tabManager.current?.webView?.loadUrl(url)
        }
        showWeb()
    }

    private fun renderTabsBadge() {
        b.navTabs.text = tabManager.count.toString()
    }

    // ---------------- 底部导航 ----------------
    private fun setupBottomNav() {
        b.navBack.setOnClickListener { tabManager.current?.webView?.goBack() }
        b.navForward.setOnClickListener { tabManager.current?.webView?.goForward() }
        b.navHome.setOnClickListener { goHome() }
        b.navTabs.setOnClickListener { showTabSheet() }
        b.navMenu.setOnClickListener { showMenuSheet() }
        renderTabsBadge()
    }

    // ---------------- 主页 ----------------
    private fun setupHome() {
        b.homeSearchInput.setOnClickListener { focusAddressBar() }
        b.homeProxyCard.setOnClickListener { openNodes() }
    }

    // ---------------- 标签面板 ----------------
    private fun showTabSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val sheet = layoutInflater.inflate(R.layout.sheet_tabs, null)
        val rv = sheet.findViewById<RecyclerView>(R.id.tabList)
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = TabListAdapter(
            tabManager.list(), tabManager.current?.id ?: -1,
            onOpen = { tab ->
                val old = tabManager.activate(tab)
                detachWebView(old)
                attachWebView(tab)
                dialog.dismiss()
                if (tab.url.isBlank()) showHome() else { showWeb(); tab.webView.reload() }
                renderTabsBadge(); renderTitle()
            },
            onClose = { tab ->
                val nxt = tabManager.close(tab)
                if (nxt == null) {
                    dialog.dismiss(); newTab(incognito)
                } else {
                    detachWebView(tabManager.current)
                    attachWebView(nxt)
                    if (nxt.url.isBlank()) showHome() else showWeb()
                    renderTabsBadge(); renderTitle()
                }
            }
        )
        sheet.findViewById<View>(R.id.btnNewTab).setOnClickListener { dialog.dismiss(); newTab(incognito) }
        dialog.setContentView(sheet)
        dialog.show()
    }

    // ---------------- 查找栏 ----------------
    private fun setupFindBar() {
        b.btnFindPrev.setOnClickListener { findPrev() }
        b.btnFindNext.setOnClickListener { findNext() }
        b.btnFindClose.setOnClickListener { hideFindBar() }
        b.findInput.setOnEditorActionListener { _, _, _ -> findNext(); true }
    }

    private fun showFindBar() {
        b.findBar.visibility = View.VISIBLE
        b.findInput.requestFocus()
    }

    private fun hideFindBar() {
        b.findBar.visibility = View.GONE
        tabManager.current?.webView?.clearMatches()
    }

    private fun findNext() {
        val q = b.findInput.text?.toString() ?: return
        if (q.isBlank()) return
        tabManager.current?.webView?.findAllAsync(q)
        tabManager.current?.webView?.findNext(true)
    }

    private fun findPrev() {
        tabManager.current?.webView?.findNext(false)
    }

    // ---------------- 菜单面板 ----------------
    private fun showMenuSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val sheet = layoutInflater.inflate(R.layout.sheet_menu, null)
        dialog.setContentView(sheet)
        dialog.show()
        sheet.post {
            val parent = sheet.parent as? View
            if (parent != null) BottomSheetBehavior.from(parent).state = BottomSheetBehavior.STATE_EXPANDED
        }
        MenuSheetController.bind(
            sheet, this,
            onClose = { dialog.dismiss() },
            onProxyToggle = { proxy.toggle(); dialog.dismiss() },
            onOpenNodes = { dialog.dismiss(); openNodes() }
        )
    }

    // ---- 供菜单面板调用的安全入口 ----
    fun proxyManager() = proxy
    fun openToolboxSafe() = showToolboxSheet()
    fun showFindBarSafe() = showFindBar()

    private var fullscreen = false

    fun toggleFullscreenSafe() {
        fullscreen = !fullscreen
        val act = requireActivity()
        val controller = WindowCompat.getInsetsController(act.window, act.window.decorView)
        if (fullscreen) controller.hide(WindowInsetsCompat.Type.systemBars())
        else controller.show(WindowInsetsCompat.Type.systemBars())
    }

    fun readAloudSafe() {
        val t = tabManager.current ?: return
        if (t.url.isBlank()) return
        t.webView.evaluateJavascript(
            "(function(){try{var t=document.body.innerText;window.shadowReadAloud&&window.shadowReadAloud(t);}catch(e){}})()", null)
        Toast.makeText(requireContext(), "已朗读（需页面开启脚本）", Toast.LENGTH_SHORT).show()
    }

    fun showFontDialogSafe() {
        val wv = tabManager.current?.webView ?: return
        val opts = arrayOf("最小", "较小", "标准", "较大", "最大")
        val values = arrayOf(80, 90, 100, 120, 140)
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("字体大小")
            .setItems(opts) { _, which -> wv.settings.textZoom = values[which] }
            .show()
    }

    private fun showToolboxSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val sheet = layoutInflater.inflate(R.layout.sheet_toolbox, null)
        dialog.setContentView(sheet)
        sheet.findViewById<View>(R.id.tool_find).setOnClickListener { dialog.dismiss(); showFindBar() }
        sheet.findViewById<View>(R.id.tool_fullscreen).setOnClickListener {
            dialog.dismiss(); toggleFullscreenSafe()
        }
        sheet.findViewById<View>(R.id.tool_font_inc).setOnClickListener { changeFont(20) }
        sheet.findViewById<View>(R.id.tool_font_dec).setOnClickListener { changeFont(-20) }
        sheet.findViewById<View>(R.id.tool_share).setOnClickListener { dialog.dismiss(); shareCurrent() }
        dialog.show()
    }

    // ---------------- 菜单动作（界面跳转） ----------------
    fun openNodes() = openScreen(NodesFragment())
    fun openSettings() = openScreen(SettingsFragment())
    fun openBookmarks() = openScreen(BookmarksFragment())
    fun openHistory() = openScreen(HistoryFragment())
    fun openIdentity() = openScreen(IdentityFragment())
    fun openNetLog() = openScreen(NetworkLogFragment())

    private fun openScreen(frag: Fragment) {
        // 用 add 压栈而非 replace：浏览器界面保持存活，书签/历史回跳正常、WebView 不重建
        requireActivity().supportFragmentManager.beginTransaction()
            .add(R.id.fragmentContainer, frag)
            .addToBackStack(null)
            .commit()
    }

    fun toggleDesktopMode() {
        fingerprint.setDesktopMode(!fingerprint.desktopMode())
        tabManager.current?.webView?.settings?.userAgentString = fingerprint.effectiveUserAgent()
        Toast.makeText(requireContext(),
            if (fingerprint.desktopMode()) "已切换电脑模式" else "已切回手机模式",
            Toast.LENGTH_SHORT).show()
    }

    fun toggleIncognito() {
        newTab(incognito = true)
    }

    fun addBookmark() {
        val t = tabManager.current ?: return
        if (t.url.isBlank()) return
        bookmarks.add(t.title, t.url)
        Toast.makeText(requireContext(), "已添加书签", Toast.LENGTH_SHORT).show()
    }

    fun toggleAdBlock() {
        Toast.makeText(requireContext(), "广告拦截：WebView 支持有限，已启用 JS 基础过滤", Toast.LENGTH_LONG).show()
    }

    fun toggleImageMode() {
        val cur = tabManager.current?.webView?.settings?.loadsImagesAutomatically
        tabManager.current?.webView?.settings?.loadsImagesAutomatically = cur != true
        Toast.makeText(requireContext(), if (cur == true) "无图模式" else "有图模式", Toast.LENGTH_SHORT).show()
    }

    fun openViewSource() {
        val t = tabManager.current ?: return
        if (t.url.isBlank()) return
        loadUrl("view-source:${t.url}")
    }

    fun savePage() {
        val t = tabManager.current ?: return
        if (t.url.isBlank()) return
        t.webView.evaluateJavascript(
            "(function(){var h='<html>'+document.documentElement.innerHTML+'</html>';var a=document.createElement('a');a.href='data:text/html;charset=utf-8,'+encodeURIComponent(h);a.download='page.html';a.click();})()", null)
        Toast.makeText(requireContext(), "已生成离线页面", Toast.LENGTH_SHORT).show()
    }

    fun translatePage() {
        val url = tabManager.current?.url ?: return
        loadUrl("https://translate.google.com/translate?u=${Uri.encode(url)}&sl=auto&tl=zh-CN")
    }

    fun changeFont(delta: Int) {
        val wv = tabManager.current?.webView ?: return
        wv.settings.textZoom = (wv.settings.textZoom + delta).coerceIn(50, 250)
    }

    fun shareCurrent() {
        val url = tabManager.current?.url ?: return
        val i = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
        }
        externalLauncher.launch(Intent.createChooser(i, "分享"))
    }

    fun toggleOrientation() {
        val act = requireActivity()
        act.requestedOrientation = if (
            act.requestedOrientation == android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        ) android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        else android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }

    override fun onResume() {
        super.onResume()
        tabManager.current?.webView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        tabManager.current?.webView?.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        BrowserBridge.unbind(this)
        _b = null
    }
}
