package com.shadowbrowser.app.gecko

import android.content.Context
import android.view.ViewGroup
import com.shadowbrowser.app.browser.FingerprintEngine
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.GeckoView

/**
 * Gecko 标签页管理器：每个标签一个 GeckoSession + GeckoView（真实内核）。
 */
class GeckoTabManager(
    private val context: Context,
    private val fingerprint: FingerprintEngine,
    private val client: GeckoClient
) {

    class Tab(
        val id: Long,
        val session: GeckoSession,
        val view: GeckoView,
        var title: String = "",
        var url: String = "",
        var isIncognito: Boolean = false
    )

    private val tabs = ArrayList<Tab>()
    private var nextId = 1L

    var current: Tab? = null
        private set

    val count: Int get() = tabs.size

    fun createTab(incognito: Boolean = false): Tab {
        val settings = GeckoSessionSettings.Builder()
            .usePrivateMode(incognito)
            .build()
        val session = GeckoSession(settings)
        session.setContentDelegate(client)
        session.setNavigationDelegate(client)
        session.setProgressDelegate(client)
        // UA 覆盖（GeckoSessionSettings.setUserAgentOverride 为公开 setter）
        session.settings.setUserAgentOverride(fingerprint.effectiveUserAgent())

        val view = GeckoView(context)
        session.open(GeckoEngine.runtime(context))
        view.setSession(session)

        val tab = Tab(nextId++, session, view).apply {
            isIncognito = incognito
        }
        tabs.add(tab)
        return tab
    }

    fun activate(tab: Tab): Tab? {
        val old = current
        current = tab
        return old
    }

    fun close(tab: Tab): Tab? {
        val idx = tabs.indexOf(tab)
        if (idx < 0) return null
        tabs.removeAt(idx)
        // 先移除再关闭会话，避免残留渲染层
        try {
            (tab.view.parent as? ViewGroup)?.removeView(tab.view)
            tab.session.close()
        } catch (_: Exception) {
        }
        if (current === tab) {
            current = if (tabs.isNotEmpty()) tabs[minOf(idx, tabs.size - 1)] else null
        }
        return current
    }

    fun list(): List<Tab> = tabs.toList()

    fun closeAll() {
        tabs.forEach {
            try {
                (it.view.parent as? ViewGroup)?.removeView(it.view)
                it.session.close()
            } catch (_: Exception) {
            }
        }
        tabs.clear()
        current = null
    }

    fun goBack() = current?.session?.goBack()
    fun goForward() = current?.session?.goForward()
    fun reload() = current?.session?.reload()
    fun stop() = current?.session?.stop()
}
