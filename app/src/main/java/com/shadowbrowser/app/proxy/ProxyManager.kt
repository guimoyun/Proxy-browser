package com.shadowbrowser.app.proxy

import android.content.Context
import android.util.Log
import com.shadowbrowser.app.browser.WebViewProxyHook
import com.shadowbrowser.app.nodes.Node
import com.shadowbrowser.app.nodes.NodeStore
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 代理总控：负责 启动/停止 代理引擎，并把浏览器流量接入本地代理。
 * 对外暴露可观察的连接状态、当前节点与最近错误。全局唯一（单例）。
 *
 * 崩溃防护：所有协程统一异常处理，任何一步失败都落到 ERROR + lastError，不闪退。
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

    private val exceptionHandler = CoroutineExceptionHandler { _, e ->
        Log.e(TAG, "coroutine error", e)
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + exceptionHandler)
    private val engine: ProxyEngine = ProcessXrayEngine(appContext)

    private val _status = MutableStateFlow(ProxyStatus.DISCONNECTED)
    val status: StateFlow<ProxyStatus> = _status.asStateFlow()
    private val _activeNode = MutableStateFlow<Node?>(null)
    val activeNode: StateFlow<Node?> = _activeNode.asStateFlow()
    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private var _started = false
    private var monitorJob: Job? = null

    fun isConnected(): Boolean = status.value == ProxyStatus.CONNECTED

    fun toggle() {
        if (isConnected()) stop() else startWithSelected()
    }

    /** 用当前选中的节点连接 */
    fun startWithSelected() {
        val node = nodeStore.selectedNode()
        if (node == null) {
            _lastError.value = "未选择节点：请先在“代理节点”页添加并选中节点"
            _status.value = ProxyStatus.ERROR
            return
        }
        start(node)
    }

    fun start(node: Node) {
        scope.launch {
            try {
                _status.value = ProxyStatus.CONNECTING
                _activeNode.value = node
                _lastError.value = null

                if (!WebViewProxyHook.supported()) {
                    _lastError.value = "系统 WebView 不支持应用内代理（需 WebView ≥ 91）。请更新系统 WebView：设置→应用→Android System WebView→更新"
                    _status.value = ProxyStatus.ERROR
                    return@launch
                }

                val config = withContext(Dispatchers.Default) {
                    XrayConfigGenerator.generate(node, engine.localHttpPort, engine.localSocksPort)
                }
                val ok = engine.start(config)
                if (!ok) {
                    _lastError.value = "Xray 启动失败，原因见“网络日志”页"
                    _status.value = ProxyStatus.ERROR
                    return@launch
                }

                withContext(Dispatchers.Main) {
                    try {
                        WebViewProxyHook.apply(engine.localHttpPort) {
                            _status.value = ProxyStatus.CONNECTED
                            _started = true
                            Log.i(TAG, "代理已连接: ${node.displayName()}")
                            monitor()
                        }
                    } catch (e: Exception) {
                        _lastError.value = "应用代理覆盖失败: ${e.message ?: e.javaClass.simpleName}"
                        _status.value = ProxyStatus.ERROR
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "start failed", e)
                _lastError.value = e.message ?: "连接异常"
                _status.value = ProxyStatus.ERROR
            }
        }
    }

    /** 监控引擎：若引擎进程退出（ERROR）但界面仍显示已连接，则自动清理代理覆盖 */
    private fun monitor() {
        monitorJob?.cancel()
        monitorJob = scope.launch {
            while (isActive) {
                delay(1000)
                if (engine.status == ProxyStatus.ERROR && status.value == ProxyStatus.CONNECTED) {
                    _lastError.value = "代理进程已退出，连接中断"
                    withContext(Dispatchers.Main) {
                        try {
                            WebViewProxyHook.clear {
                                _started = false
                                _status.value = ProxyStatus.ERROR
                            }
                        } catch (_: Exception) {
                            _status.value = ProxyStatus.ERROR
                        }
                    }
                    return@launch
                }
            }
        }
    }

    fun stop() {
        scope.launch {
            try {
                monitorJob?.cancel()
                engine.stop()
                withContext(Dispatchers.Main) {
                    try {
                        WebViewProxyHook.clear {
                            _started = false
                            _status.value = ProxyStatus.DISCONNECTED
                            _activeNode.value = null
                            Log.i(TAG, "代理已断开")
                        }
                    } catch (_: Exception) {
                        _status.value = ProxyStatus.DISCONNECTED
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "stop failed", e)
                _status.value = ProxyStatus.DISCONNECTED
            }
        }
    }

    /** 本地 HTTP 端口（外部展示用） */
    val localHttpPort: Int get() = engine.localHttpPort
}
