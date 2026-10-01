package com.shadowbrowser.app.browser

import android.graphics.Bitmap
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * 浏览器客户端：页面加载、JS 指纹注入、外链处理。事件经回调转发给界面。
 */
class BrowserClient(
    private val fingerprint: FingerprintEngine,
    private val listener: Listener
) : WebViewClient() {

    interface Listener {
        fun onPageStarted(url: String?)
        fun onPageFinished(url: String?, title: String?)
        fun onExternalUrl(url: String)
    }

    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
        listener.onPageStarted(url)
        super.onPageStarted(view, url, favicon)
    }

    override fun onPageFinished(view: WebView, url: String?) {
        // 注入指纹 JS（覆盖 navigator 属性、时区、WebRTC）
        view.evaluateJavascript(fingerprint.jsInjection(), null)
        listener.onPageFinished(url, view.title)
        super.onPageFinished(view, url)
    }

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val url = request.url?.toString() ?: return false
        if (!url.startsWith("http://") && !url.startsWith("https://") && !url.startsWith("about:")) {
            listener.onExternalUrl(url)
            return true
        }
        return false
    }
}
