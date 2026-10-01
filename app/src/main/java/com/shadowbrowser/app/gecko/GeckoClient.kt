package com.shadowbrowser.app.gecko

import android.app.Activity
import android.util.Log
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession

/**
 * Gecko 会话委托：导航 / 内容 / 进度 统一转发到界面。
 * JS 弹窗等其余回调使用 GeckoView 默认行为（自动确认/关闭），避免版本差异。
 */
class GeckoClient(
    private val activity: Activity,
    private val listener: Listener
) : GeckoSession.NavigationDelegate,
    GeckoSession.ContentDelegate,
    GeckoSession.ProgressDelegate {

    interface Listener {
        fun onPageStart(url: String?)
        fun onPageStop(url: String?)
        fun onPageTitle(title: String?, url: String?)
        fun onProgress(progress: Int)
        fun onExternalUrl(url: String)
    }

    @Volatile private var lastUrl: String? = null

    // ---------------- ProgressDelegate ----------------
    override fun onProgressChange(session: GeckoSession, progress: Int) {
        listener.onProgress(progress)
    }

    override fun onPageStart(session: GeckoSession, url: String) {
        lastUrl = url
        listener.onPageStart(url)
    }

    override fun onPageStop(session: GeckoSession, success: Boolean) {
        listener.onPageStop(lastUrl)
    }

    // ---------------- ContentDelegate ----------------
    override fun onTitleChange(session: GeckoSession, title: String?) {
        listener.onPageTitle(title, lastUrl)
    }

    // ---------------- NavigationDelegate ----------------
    override fun onLocationChange(
        session: GeckoSession,
        url: String?,
        perms: MutableList<GeckoSession.PermissionDelegate.ContentPermission>,
        hasUserGesture: Boolean
    ) {
        if (url != null) lastUrl = url
    }

    override fun onLoadRequest(
        session: GeckoSession,
        request: GeckoSession.NavigationDelegate.LoadRequest
    ): GeckoResult<AllowOrDeny> {
        val url = request.uri ?: return GeckoResult.fromValue(AllowOrDeny.ALLOW)
        val ok = url.startsWith("http://") || url.startsWith("https://") ||
            url.startsWith("about:") || url.startsWith("data:") || url.startsWith("view-source:")
        if (!ok) {
            Log.i("GeckoClient", "外部链接 -> 系统: $url")
            listener.onExternalUrl(url)
            return GeckoResult.fromValue(AllowOrDeny.DENY)
        }
        return GeckoResult.fromValue(AllowOrDeny.ALLOW)
    }
}
