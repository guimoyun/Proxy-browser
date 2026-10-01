package com.shadowbrowser.app.proxy

import android.content.Context
import android.util.Log
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
 * 代理总控（v2 / GeckoView 架构）。
 *
 * 设计（免 VPN 的关键）：浏览器（Gecko）固定走本地代理 127.0.0.1:10809，
 * 因此 xray **常驻运行**：
 *  - 未连接节点 → 「直连模式」（outbound=freedom）兜底，状态显示未连接；
 *  - 连接节点 → 重启 xray 为节点出站，状态显示已连接；
 *  - 断开节点 → 回到直连模式。
 * 全程不使用 VPNService，不弹系统 VPN 图标。
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

    private var monitorJob: Job? = null

    fun isConnected(): Boolean = _status.value == ProxyStatus.CONNECTED

    /** 确保本地代理在运行（浏览器要求 10809 始终可达）。未连接节点时直连模式。 */
    fun ensureRunning() {
        if (_status.value == ProxyStatus.DISCONNECTED) startDirect()
    }

    fun toggle() {
        if (isConnected()) stop() else startWithSelected()
    }

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

                val config = withContext(Dispatchers.Default) {
                    XrayConfigGenerator.generate(node, engine.localHttpPort, engine.localSocksPort)
                }
                if (engine.start(config)) {
                    _status.value = ProxyStatus.CONNECTED
                    Log.i(TAG, "代理已连接: ${node.displayName()}")
                    monitor()
                } else {
                    _lastError.value = "Xray 启动失败，原因见“网络日志”页"
                    _status.value = ProxyStatus.ERROR
                }
            } catch (e: Exception) {
                Log.e(TAG, "start failed", e)
                _lastError.value = e.message ?: "连接异常"
                _status.value = ProxyStatus.ERROR
            }
        }
    }

    /** 直连模式：本地代理在跑，但不经过任何节点 */
    private fun startDirect() {
        scope.launch {
            try {
                _activeNode.value = null
                _status.value = ProxyStatus.CONNECTING
                val config = withContext(Dispatchers.Default) {
                    XrayConfigGenerator.generateDirect(engine.localHttpPort, engine.localSocksPort)
                }
                if (engine.start(config)) {
                    _status.value = ProxyStatus.DISCONNECTED
                } else {
                    _status.value = ProxyStatus.ERROR
                }
            } catch (e: Exception) {
                Log.e(TAG, "startDirect failed", e)
                _status.value = ProxyStatus.ERROR
            }
        }
    }

    fun stop() {
        // 断开节点 = 回到直连模式（浏览器仍可直连上网）
        monitorJob?.cancel()
        startDirect()
    }

    /** 监控引擎：引擎进程退出时同步状态 */
    private fun monitor() {
        monitorJob?.cancel()
        monitorJob = scope.launch {
            while (isActive) {
                delay(1000)
                if (engine.status == ProxyStatus.ERROR && _status.value == ProxyStatus.CONNECTED) {
                    _lastError.value = "代理进程已退出，连接中断"
                    _status.value = ProxyStatus.ERROR
                    return@launch
                }
            }
        }
    }

    val localHttpPort: Int get() = engine.localHttpPort
}
