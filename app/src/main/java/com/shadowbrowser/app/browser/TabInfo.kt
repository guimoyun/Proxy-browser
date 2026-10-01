package com.shadowbrowser.app.browser

import android.webkit.WebView

/** 单个标签页：持有其 WebView 实例与展示信息 */
class TabInfo(
    val id: Long,
    val webView: WebView
) {
    var url: String = ""
    var title: String = ""
    var isIncognito: Boolean = false
}
