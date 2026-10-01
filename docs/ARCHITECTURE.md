# 架构与代理机制（v2 / GeckoView）

## 总体架构

```
┌────────────────────────── 影子浏览器 ShadowBrowser v2 ──────────────────────────┐
│                                                                                │
│  BrowserFragment (主界面，参考 Via)                                              │
│   ├─ GeckoTabManager ── GeckoView + GeckoSession × N（多标签，真实内核）         │
│   │     └─ GeckoClient  (NavigationDelegate / ContentDelegate / ProgressDelegate)│
│   ├─ GeckoEngine        → 运行时单例 + 写入 geckoview-config.yaml（代理/指纹 prefs）│
│   ├─ FingerprintEngine  → UA 覆盖（GeckoSessionSettings）+ FPP/WebRTC prefs     │
│   └─ MenuSheetController → 3 页 × 10 项菜单 → 动作路由                           │
│                                                                                │
│  ProxyManager (单例)                                                           │
│   ├─ NodeStore              节点 / 订阅持久化 (filesDir/nodes.json)              │
│   ├─ ProcessXrayEngine      启动/停止 xray 子进程 + 端口自检                     │
│   ├─ XrayConfigGenerator    生成 xray config.json（节点/直连两模式）              │
│   └─ ProxyLogStore          网络日志（内存缓冲，UI 读取）                        │
└────────────────────────────────────────────────────────────────────────────────┘
```

## 代理数据通路（不使用 VPNService）

```
GeckoView 发出的 HTTP(S) 请求（Firefox 引擎）
   │
   ▼
Gecko 引擎 prefs（启动时由 geckoview-config.yaml 注入，仅本应用进程生效）
   │  network.proxy.type = 1        （手动代理）
   │  network.proxy.http/ssl = 127.0.0.1:10809
   ▼
Xray-core 子进程（常驻）
   │  inbound:
   │    http  127.0.0.1:10809
   │    socks 127.0.0.1:10808
   ▼
outbound:
   │  · 已连接节点 → vless/vmess/trojan/ss 节点
   │  · 未连接     → freedom（直连，兜底）
   ▼
目标站点
```

### 为什么「不用开启 VPN」

- 系统 VPN（`VpnService`）会建立 **TUN 虚拟网卡**，把**整个设备**的流量都路由走，并消耗额外权限、CPU 与电量。
- 本工程的代理只影响**应用内的 GeckoView**：通过引擎 prefs 把请求指到本地 Xray 入站端口。
- 因此：不需要 `BIND_VPN_SERVICE` 权限，状态栏不出现 VPN 小图标，其他 App 不受影响。

### 关键实现细节

1. **Gecko 内核**：`org.mozilla.geckoview:geckoview-arm64-v8a:153.0.20260810162159`（Mozilla 官方 Maven，稳定渠道）。`libxul.so` 等原生库随 APK 打包，**不依赖系统 WebView**。
2. **代理 prefs 注入**：`GeckoEngine.writeConfig()` 在 `filesDir/gecko/geckoview-config.yaml` 写入
   ```yaml
   prefs:
     network.proxy.type: 1
     network.proxy.http: "127.0.0.1"
     network.proxy.http_port: 10809
     network.proxy.ssl: "127.0.0.1"
     network.proxy.ssl_port: 10809
     network.proxy.share_proxy_settings: true
     network.proxy.no_proxies_on: "localhost, 127.0.0.1"
   ```
   并以 `GeckoRuntimeSettings.Builder().configFilePath(...)` 交给运行时；Gecko 进程启动时读取。
3. **Xray 常驻（直连兜底）**：Gecko 固定指向 10809，因此 `ProxyManager.ensureRunning()` 在浏览器初始化时启动 Xray（未连接节点 → `generateDirect()`，outbound=freedom）。连接节点 = 重启 Xray 出站切到节点；断开 = 回到直连。**切换不重建 Gecko 运行时**，连接/断开即时生效。
4. **状态机**：`ProxyStatus` = `DISCONNECTED / CONNECTING / CONNECTED / ERROR`。`engine.start(config)` 轮询 `127.0.0.1:10809` 端口连通即视为成功；`ProxyManager` 以 `StateFlow` 暴露，顶部 `proxyChip` 与主页卡片订阅刷新；引擎进程意外退出会被监控并置为 `ERROR`。
5. **Xray 二进制**：`app/src/main/assets/xray/xray`（ABI arm64-v8a）。启动时拷贝到 `filesDir/xray/xray` 并 `chmod +x`，以 `ProcessBuilder` 启动子进程。
6. **多标签**：每个标签一个 `GeckoSession + GeckoView`；切换标签 = 移除旧 GeckoView 视图、挂载新视图（会话保留，不重建）。关闭标签先 `removeView` 再 `session.close()`，避免残留渲染层。

## 节点与订阅

- **手动**：`NodeLinkParser` 解析 `vless://` `vmess://` `trojan://` `ss://`；也支持逐字段编辑。
- **订阅**：`SubscriptionManager`(OkHttp) 拉取 URL → 若整包为 Base64 则先解码 → 逐行解析出节点，去重后入库，并记住订阅 URL 便于再次导入。

## 数据存储

- 全部位于 `context.filesDir`（应用私有目录）：
  - `nodes.json`：节点列表 + 选中节点 + 订阅 URL
  - `bookmarks.json` / `history.json`：书签与历史
  - `xray/`：Xray 二进制运行副本
  - `gecko/geckoview-config.yaml`：Gecko 代理/指纹配置
  - `config.json`：最新生成的 Xray 配置
- 不上传任何数据。

## 关键 ABI 与系统要求

- minSdk 26（Android 8.0）/ targetSdk 36 / compileSdk 36。
- 仅 `arm64-v8a`（GeckoView 与 xray 均为 arm64 产物）；32 位设备不支持。
- 构建链：AGP 8.9.2 / Kotlin 2.2.0 / Gradle 8.11.1 / JDK 17。
