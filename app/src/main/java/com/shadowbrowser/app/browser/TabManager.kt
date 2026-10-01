package com.shadowbrowser.app.browser

import android.annotation.SuppressLint
import android.content.Context
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * 标签页管理器：负责创建/切换/关闭 WebView 实例，并应用指纹伪装。
 */
class TabManager(
    private val context: Context,
    private val fingerprint: FingerprintEngine,
    private val client: WebViewClient,
    private val chrome: WebChromeClient
) {
    private val tabs = ArrayList<TabInfo>()
    private var nextId = 1L

    var current: TabInfo? = null
        private set

    val count: Int get() = tabs.size

    fun createTab(incognito: Boolean = false): TabInfo {
        val wv = ShadowWebView(context, fingerprint, incognito)
        wv.webViewClient = client
        wv.webChromeClient = chrome
        return TabInfo(nextId++, wv).apply { isIncognito = incognito }.also { tabs.add(it) }
    }

    /** 激活某标签，返回被切走的旧标签（用于从容器移除） */
    fun activate(tab: TabInfo): TabInfo? {
        val old = current
        current = tab
        return old
    }

    fun close(tab: TabInfo): TabInfo? {
        val idx = tabs.indexOf(tab)
        if (idx < 0) return null
        tabs.removeAt(idx)
        // 必须先从父容器移除再 destroy，否则 "WebView.destroy() while attached" 崩溃
        (tab.webView.parent as? ViewGroup)?.removeView(tab.webView)
        try {
            tab.webView.destroy()
        } catch (_: Exception) {
        }
        if (current === tab) {
            current = if (tabs.isNotEmpty()) tabs[minOf(idx, tabs.size - 1)] else null
        }
        return current
    }

    fun list(): List<TabInfo> = tabs.toList()

    fun closeAll() {
        tabs.forEach {
            try {
                (it.webView.parent as? ViewGroup)?.removeView(it.webView)
                it.webView.destroy()
            } catch (_: Exception) {
            }
        }
        tabs.clear()
        current = null
    }

    fun closeCurrent(): TabInfo? {
        val cur = current ?: return null
        return close(cur)
    }
}

/**
 * WebView 封装：创建时即应用指纹伪装。
 */
@SuppressLint("SetJavaScriptEnabled")
class ShadowWebView(
    context: Context,
    private val fingerprint: FingerprintEngine,
    val incognito: Boolean = false
) : WebView(context) {

    init {
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
            builtInZoomControls = true
            displayZoomControls = false
            setSupportZoom(true)
            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(false)
            mediaPlaybackRequiresUserGesture = true
            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
        }
        if (incognito) {
            settings.domStorageEnabled = false
            settings.databaseEnabled = false
            settings.cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
            clearCache(true)
        }
        fingerprint.applyTo(this)
    }

    override fun onResume() {
        super.onResume()
        resumeTimers()
    }

    override fun onPause() {
        super.onPause()
        pauseTimers()
    }
}
