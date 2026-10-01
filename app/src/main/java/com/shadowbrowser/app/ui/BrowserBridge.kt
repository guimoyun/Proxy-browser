package com.shadowbrowser.app.ui

/**
 * 浏览器桥：供功能页（书签/历史）把 URL 交给浏览器加载。
 * 通过 bind/unbind 绑定当前 BrowserFragment 实例。
 */
object BrowserBridge {

    private var browser: BrowserFragment? = null

    fun bind(frag: BrowserFragment) {
        browser = frag
    }

    fun unbind(frag: BrowserFragment) {
        if (browser === frag) browser = null
    }

    fun openUrl(url: String) {
        browser?.loadUrl(url)
    }
}
