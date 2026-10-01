# 影子浏览器 ShadowBrowser

一个轻量的 **Android 浏览器**，界面与交互参考 [Via 浏览器](https://viayoo.com/) 设计，核心差异是：**内嵌 Xray-core**，通过**应用内本地代理**让浏览器流量走代理节点——**不需要系统 VPN、不需要额外开启代理软件**。

> ⚠️ **合规与免责声明（必读）**
> 本工程仅供**学习与合法用途**。代理与「指纹伪装」技术本身是中性的工具；请遵守你所在国家/地区的法律法规，以及目标网站的服务条款。使用本项目产生的任何法律、安全或商业风险由使用者自行承担，作者不承担任何责任。

---

## 特性

- **内嵌 Xray，无需 VPNService**：Xray 二进制随 APK 打包，以本地子进程运行，浏览器流量经 `WebView ProxyController` 指向 `127.0.0.1:10809`（HTTP 代理）转发，**完全不占用系统 VPN 通道**，不请求 `BIND_VPN` 权限。
- **界面参考 Via**：顶部工具栏（搜索/标题/代理状态/收起）→ 主页（Logo + 搜索框 + 代理入口）→ 底部五键导航（返回 / 前进 / 主页 / 标签计数 / 菜单）；菜单为**底部弹出 3 页 × 10 项**，设置页为分组列表。
- **多标签页**：点击底部标签计数弹出标签面板，可新建 / 切换 / 关闭。
- **代理节点管理**：
  - 手动添加：`vless://` `vmess://` `trojan://` `ss://` 分享链接一键填充，或逐字段填写。
  - **订阅导入**：填入订阅 URL，自动拉取并批量解析（支持整体 Base64 订阅）。
  - 节点测延迟、连接 / 断开、编辑 / 删除。
- **指纹伪装（WebView 可改项）**：随机 UA、电脑模式 UA、自定义 UA、伪装时区偏移、禁用 WebRTC。
  > 诚实说明：标准 Android `WebView` 仅能改动上述可观测项；深度指纹（Canvas、WebGL、字体、硬件并发数等）需要自编译 Chromium 内核，本工程不包含，也不虚假宣称「全指纹伪装」。
- **功能菜单（类 Via）**：夜间模式、书签、历史、下载、隐身标签、分享、添加书签、电脑模式、工具箱（页内查找 / 全屏 / 字号 / 分享）、设置、翻译、离线页面、源码查看、有图模式、资源嗅探入口、浏览器标识、网络日志、广告拦截提示等。
- **数据本地化**：书签 / 历史 / 节点 / 订阅均存于应用私有目录（`filesDir`），不上传任何服务器。

---

## 代理原理（关键）

```
浏览器请求
   │
   ▼
WebView  ProxyController.setProxyOverride(127.0.0.1:10809)   ← 仅影响本 WebView，非系统级
   │
   ▼
Xray-core（本地子进程，内置入站）
   │  inbound: http 127.0.0.1:10809 / socks 127.0.0.1:10808
   ▼
outbound: 你选中的节点（vless/vmess/trojan/ss）
```

- **不申请** `BIND_VPN_SERVICE`，不使用 `VpnService`，系统状态栏不出现「小钥匙 / VPN」标识，也不建立系统 TUN 虚拟网卡。
- Xray 二进制来自 `app/src/main/assets/xray/xray`，运行时拷贝到应用私有目录并以子进程启动（ABI：`arm64-v8a`）。
- 未连接节点时，浏览器直连；连接后仅该应用流量走代理，不影响其他 App。

---

## 构建

### 依赖
- JDK 17
- Android SDK（platform 34 + build-tools 34.0.0）
- Gradle 8.7（仓库含 wrapper）

### 本地构建（debug APK）

```bash
# 1. 准备 Xray 二进制（约 15~25MB，被 .gitignore 忽略，不进 Git）
mkdir -p app/src/main/assets/xray
#   从官方 release 下载 arm64-v8a 包并解压出 xray 可执行文件放到上述路径：
#   https://github.com/XTLS/Xray-core/releases/download/v1.8.24/Xray-android-arm64-v8a.zip
#   或直接运行：
#   scripts/fetch-xray.sh

# 2. 配置 SDK 路径（或设置 ANDROID_HOME 环境变量）
#    echo "sdk.dir=$HOME/Android/Sdk" > local.properties

# 3. 编译
./gradlew assembleDebug

# APK 输出
# app/build/outputs/apk/debug/app-debug.apk
```

> 也可一键执行 `scripts/build.sh`（已内置本机 SDK 探测与下载 Xray 的逻辑）。

### CI 构建（GitHub Actions）

仓库已含 `.github/workflows/android.yml`：CI 在构建前**自动下载 Xray 二进制**打进 APK，并把 `app-debug.apk` 作为工件上传。可直接在 Actions 页面下载。

---

## 安装与使用

1. 安装 `app-debug.apk`（允许「未知来源」）。
2. 打开应用 → 点击主页「代理节点」卡片（或菜单 → 代理节点 / 设置 → 代理节点管理）。
3. **添加节点**：点右上角 `+`，粘贴 `vless://` 等分享链接自动填充；或填订阅 URL → 导入。
4. 在列表点「连接」按钮（或长按节点 → 连接）。连接成功后顶部状态条变绿并显示节点名。
5. 正常上网即可；不连接时浏览器直连。

---

## 项目结构

```
app/src/main/java/com/shadowbrowser/app/
├── ShadowApp.kt          Application 初始化
├── MainActivity.kt       宿主（单 Activity，fragment 容器）
├── proxy/                Xray 代理引擎
│   ├── ProxyManager.kt   单例：状态机 + 连接生命周期
│   ├── ProcessXrayEngine.kt  Xray 子进程管理
│   ├── XrayConfigGenerator.kt 生成 xray config.json
│   ├── ProxyLogStore.kt  网络日志（内存缓冲）
├── browser/              WebView 封装
│   ├── TabManager.kt     多标签 + ShadowWebView
│   ├── BrowserClient.kt / ChromeClient.kt  回调（进度→ChromeClient）
│   ├── WebViewProxyHook.kt  ProxyController 代理覆盖
│   ├── FingerprintEngine.kt  UA/时区/WebRTC 伪装
│   ├── BookmarkStore.kt / HistoryStore.kt
├── nodes/                节点解析与存储
│   ├── Node.kt / NodeStore.kt / NodeLinkParser.kt / SubscriptionManager.kt / LatencyTester.kt
├── ui/                   界面
│   ├── BrowserFragment.kt  主界面（主页/工具栏/五键/标签面板/查找栏）
│   ├── MenuSheetController.kt  3 页菜单 → 动作路由
│   ├── NodesFragment / NodeEditFragment / SettingsFragment / IdentityFragment / NetworkLogFragment / BookmarksFragment / HistoryFragment
└── res/                  布局 / 图标 / 主题 / 字符串
```

---

## 文档

- [架构与代理机制](docs/ARCHITECTURE.md)
- [指纹伪装说明](docs/FINGERPRINT.md)

---

## 限制与后续

- 仅支持 `arm64-v8a`（绝大多数现代手机）；如需 32 位设备可自行替换 `xray` 二进制并加 `jniLibs` 或 asset 多 ABI。
- 「指纹伪装」受限于系统 WebView，深度指纹需 Chromium 内核改造（不在本项目范围）。
- 广告拦截为提示/基础过滤，非完整规则引擎。

---

## License

[MIT](LICENSE)
