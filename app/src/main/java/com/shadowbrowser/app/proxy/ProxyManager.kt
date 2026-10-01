package com.shadowbrowser.app.proxy

import android.content.Context
import android.util.Log
import com.shadowbrowser.app.browser.WebViewProxyHook
import com.shadowbrowser.app.nodes.Node
import com.shadowbrowser.app.nodes.NodeStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 代理总控：负责 启动/停止 代理引擎，并把浏览器流量接入本地代理。
 * 对外暴露可观察的连接状态与当前节点。全局唯一（单例）。
 */
class ProxyManager(context: Context) {

    companion object {
        private const val TAG = "ProxyManager"

        @Volatile var instance: ProxyManager? = null
            private set

        fun init(context: Context): ProxyManager {
            return instance ?: synchronized(this) {
                instance ?: ProxyManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val appContext = context.applicationContext
    val nodeStore = NodeStore(appContext)
    init {
        WebViewProxyHook.init(appContext)
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val engine: ProxyEngine = ProcessXrayEngine(appContext)

    val status: StateFlow<ProxyStatus> = MutableStateFlow(ProxyStatus.DISCONNECTED)
    val activeNode: StateFlow<Node?> = MutableStateFlow(null)

    private var _started = false

    fun isConnected(): Boolean = status.value == ProxyStatus.CONNECTED

    fun toggle() {
        if (isConnected()) stop() else startWithSelected()
    }

    /** 用当前选中的节点连接 */
    fun startWithSelected() {
        val node = nodeStore.selectedNode()
        if (node == null) {
            (status as MutableStateFlow).value = ProxyStatus.ERROR
            return
        }
        start(node)
    }

    fun start(node: Node) {
        scope.launch {
            (status as MutableStateFlow).value = ProxyStatus.CONNECTING
            (activeNode as MutableStateFlow).value = node
            val config = withContext(Dispatchers.Default) {
                XrayConfigGenerator.generate(node, engine.localHttpPort, engine.localSocksPort)
            }
            val ok = engine.start(config)
            if (ok) {
                // 浏览器流量走本地代理
                withContext(Dispatchers.Main) {
                    WebViewProxyHook.setProxy(engine.localHttpPort) {
                        (status as MutableStateFlow).value = ProxyStatus.CONNECTED
                        _started = true
                        Log.i(TAG, "代理已连接: ${node.displayName()}")
                    }
                }
            } else {
                (status as MutableStateFlow).value = ProxyStatus.ERROR
            }
        }
    }

    fun stop() {
        scope.launch {
            engine.stop()
            withContext(Dispatchers.Main) {
                WebViewProxyHook.clearProxy {
                    _started = false
                    (status as MutableStateFlow).value = ProxyStatus.DISCONNECTED
                    (activeNode as MutableStateFlow).value = null
                    Log.i(TAG, "代理已断开")
                }
            }
        }
    }

    /** 本地 HTTP 端口（外部展示用） */
    val localHttpPort: Int get() = engine.localHttpPort
}
