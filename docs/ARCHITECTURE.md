# 架构与代理机制

## 总体架构

```
┌────────────────────────── 影子浏览器 ShadowBrowser ──────────────────────────┐
│                                                                             │
│  BrowserFragment (主界面，参考 Via)                                          │
│   ├─ TabManager ── ShadowWebView × N（多标签）                               │
│   │     ├─ BrowserClient  (WebViewClient)                                   │
│   │     └─ ChromeClient   (WebChromeClient：进度/标题/全屏)                  │
│   ├─ WebViewProxyHook     → ProxyController.setProxyOverride(127.0.0.1:10809)│
│   ├─ FingerprintEngine    → UA / 时区偏移 / WebRTC 注入                      │
│   └─ MenuSheetController  → 3 页 × 10 项菜单 → 动作路由                     │
│                                                                             │
│  ProxyManager (单例)                                                        │
│   ├─ NodeStore              节点 / 订阅持久化 (filesDir/nodes.json)          │
│   ├─ ProcessXrayEngine      启动/停止 xray 子进程 + 端口自检                 │
│   ├─ XrayConfigGenerator    生成 xray config.json                           │
│   └─ ProxyLogStore          网络日志（内存缓冲，UI 读取）                    │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 代理数据通路（不使用 VPNService）

```
WebView 发出的 HTTP(S) 请求
   │
   ▼
ProxyController（androidx.webkit，WebView 91+）
   │  setProxyOverride({ host: 127.0.0.1, port: 10809 }, ProxyController.DIRECT)
   ▼
Xray-core 子进程
   │  inbound:
   │    http  0.0.0.0:10809
   │    socks 0.0.0.0:10808
   ▼
outbound: 用户选中的节点（vless/vmess/trojan/ss）
   ▼
目标站点
```

### 为什么「不用开启 VPN」

- 系统 VPN（`VpnService`）会建立 **TUN 虚拟网卡**，把**整个设备**的流量都路由走，并消耗额外权限、CPU 与电量。
- 本工程的代理只影响**应用内的 WebView**：通过 `ProxyController.setProxyOverride` 把该 WebView 的请求指到本地 Xray 入站端口。
- 因此：不需要 `BIND_VPN_SERVICE` 权限，状态栏不出现 VPN 小图标，其他 App 不受影响。

### 关键实现细节

1. **Xray 二进制**：`app/src/main/assets/xray/xray`（ABI arm64-v8a）。启动时 `context.assets.open("xray/xray")` 拷贝到 `filesDir/xray/xray` 并 `chmod +x`，以 `ProcessBuilder` 启动子进程。
2. **配置生成**：`XrayConfigGenerator` 用 `org.json` 组装 config（含 HTTP+SOCKS 入站、所选 outbound、日志 level）。节点类型映射：
   - `vless` / `vmess` / `trojan` → 对应 outbound `protocol`
   - `shadowsocks` → `shadowsocks` outbound
   - 网络/传输：`network`(tcp/ws/grpc/h2)、`tls`(none/tls/reality)、`security`(auto/aes…)、`sni/host/path/flow/publicKey/shortId/serviceName` 等字段透传。
3. **状态机**：`ProxyStatus` = `DISCONNECTED / CONNECTING / CONNECTED / ERROR`。启动时写 config → 起子进程 → 轮询 `127.0.0.1:10809` 端口连通即视为 `CONNECTED`；`ProxyManager` 以 `StateFlow` 暴露，顶部 `proxyChip` 与主页卡片订阅刷新。
4. **代理覆盖**：连接成功后在 `WebView` 上调用 `WebViewProxyHook.apply(context, webview)`（`ProxyController.setProxyOverride`）；断开时 `clearProxy`。新打开的 WebView 也会在创建时套用。

## 节点与订阅

- **手动**：`NodeLinkParser` 解析 `vless://` `vmess://` `trojan://` `ss://`；也支持逐字段编辑。
- **订阅**：`SubscriptionManager`(OkHttp) 拉取 URL → 若整包为 Base64 则先解码 → 逐行解析出节点，去重后入库，并记住订阅 URL 便于再次导入。

## 数据存储

- 全部位于 `context.filesDir`（应用私有目录）：
  - `nodes.json`：节点列表 + 选中节点 + 订阅 URL
  - `bookmarks.json` / `history.json`：书签与历史
  - `xray/`：Xray 二进制运行副本
  - `config.json`：最新生成的 Xray 配置
- 不上传任何数据。

## 关键 ABI 与系统要求

- minSdk 26（Android 8.0）/ targetSdk 34。
- `ProxyController` 需系统 WebView ≥ 91（Android 10 及以上系统 WebView 均满足；Android 8/9 请保持 WebView 更新）。
- 仅 `arm64-v8a` xray 二进制；如需 32 位可替换资源并增加 ABI。
