package com.shadowbrowser.app.browser

import android.app.Activity
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebView

/**
 * Chrome 客户端：进度、标题、JS 弹窗、全屏。
 */
class ChromeClient(
    private val activity: Activity,
    private val listener: Listener
) : WebChromeClient() {

    interface Listener {
        fun onTitle(title: String?, url: String?)
        fun onProgress(progress: Int)
        fun onShowFullscreen()
        fun onHideFullscreen()
    }

    override fun onReceivedTitle(view: WebView, title: String?) {
        listener.onTitle(title, view.url)
        super.onReceivedTitle(view, title)
    }

    override fun onProgressChanged(view: WebView, newProgress: Int) {
        listener.onProgress(newProgress)
        super.onProgressChanged(view, newProgress)
    }

    override fun onJsAlert(view: WebView, url: String, message: String, result: JsResult): Boolean {
        android.app.AlertDialog.Builder(activity)
            .setMessage(message)
            .setPositiveButton("确定") { _, _ -> result.confirm() }
            .setOnCancelListener { result.cancel() }
            .show()
        return true
    }

    override fun onShowCustomView(view: android.view.View, callback: CustomViewCallback) {
        listener.onShowFullscreen()
        super.onShowCustomView(view, callback)
    }

    override fun onHideCustomView() {
        listener.onHideFullscreen()
        super.onHideCustomView()
    }
}
