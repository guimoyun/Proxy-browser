# 影子浏览器 ShadowBrowser

一个轻量的 **Android 浏览器**，界面与交互参考 [Via 浏览器](https://viayoo.com/) 设计，核心差异是：**内嵌 Xray-core + GeckoView 真实浏览器内核**，通过**应用内本地代理**让浏览器流量走代理节点——**不需要系统 VPN、不需要额外开启代理软件、不依赖系统 WebView**。

> ⚠️ **合规与免责声明（必读）**
> 本工程仅供**学习与合法用途**。代理与「指纹保护」技术本身是中性的工具；请遵守你所在国家/地区的法律法规，以及目标网站的服务条款。使用本项目产生的任何法律、安全或商业风险由使用者自行承担，作者不承担任何责任。

---

## 特性

- **真实内核（GeckoView / Firefox 引擎）**：v2.0 起浏览器层基于 Mozilla **GeckoView 153** 重写，内核随 APK 打包（`libxul.so`），**不依赖系统 WebView**，各机型渲染/兼容行为一致，从根上解决 v1 的 WebView 崩溃与代理覆盖兼容问题。
- **内嵌 Xray，无需 VPNService**：Xray 二进制随 APK 打包，以本地子进程常驻运行；Gecko 通过**引擎配置 prefs** 固定走 `127.0.0.1:10809`（HTTP 代理），**完全不占用系统 VPN 通道**，不请求 `BIND_VPN` 权限，状态栏无 VPN 图标。
  - 未连接节点时，Xray 以「直连模式」兜底（浏览器照常上网）；
  - 连接节点后，重启 Xray 进程切到节点出站（浏览器无需重建，切换即时生效）。
- **界面参考 Via**：顶部工具栏（搜索/标题/代理状态/收起）→ 主页（Logo + 搜索框 + 代理入口）→ 底部五键导航（返回 / 前进 / 主页 / 标签计数 / 菜单）；菜单为**底部弹出 3 页 × 10 项**，设置页为分组列表。
- **多标签页**：点击底部标签计数弹出标签面板，可新建 / 切换 / 关闭；支持隐身标签（Gecko 私有模式）。
- **代理节点管理**：
  - 手动添加：`vless://` `vmess://` `trojan://` `ss://` 分享链接一键填充，或逐字段填写。
  - **订阅导入**：填入订阅 URL，自动拉取并批量解析（支持整体 Base64 订阅）。
  - 节点测延迟、连接 / 断开、编辑 / 删除。
- **指纹保护（Gecko 可编程项）**：
  - UA 覆盖：默认 / 随机 / 电脑模式 / 自定义（逐标签生效）；
  - **基线指纹保护（Firefox FPP）**：字体、画布噪声、WebRTC IP 保护等，经引擎 prefs 开启；
  - 可禁用 WebRTC（防真实 IP 泄露）。
  - 诚实说明：完整指纹伪造（Canvas/WebGL/字体逐项精确伪装）需修改引擎源码，本工程不做，也不虚假宣称「全指纹伪装」。
- **功能菜单（类 Via）**：书签、历史、隐身标签、分享、添加书签、电脑模式、设置、翻译、源码查看、全屏、屏幕方向、网络日志等。
  - ⚠️ Gecko 版暂未集成（菜单点击会提示）：页内查找、朗读网页、字体大小、保存/离线页面、有图模式、广告拦截规则、资源嗅探、二维码等——需要 Gecko 的 JS 注入扩展机制，后续版本开放。
- **数据本地化**：书签 / 历史 / 节点 / 订阅均存于应用私有目录（`filesDir`），不上传任何服务器。

---

## 代理原理（关键）

```
浏览器请求（GeckoView 内核）
   │
   ▼
Gecko 引擎 prefs：network.proxy.type=1 → http/ssl = 127.0.0.1:10809   ← 仅本应用进程，非系统级
   │
   ▼
Xray-core（本地子进程，常驻）
   │  inbound: http 127.0.0.1:10809 / socks 127.0.0.1:10808
   ▼
outbound: 你选中的节点（vless/vmess/trojan/ss）或 freedom（未连接时直连）
```

- **不申请** `BIND_VPN_SERVICE`，不使用 `VpnService`，系统状态栏不出现「小钥匙 / VPN」标识，也不建立系统 TUN 虚拟网卡。
- Xray 二进制来自 `app/src/main/assets/xray/xray`，运行时拷贝到应用私有目录并以子进程启动（ABI：`arm64-v8a`）。
- 代理开关 = 切换 Xray 出站（节点 ↔ 直连），**浏览器内核全程不动**，因此连接/断开即时生效、稳定不闪退。

---

## 构建

### 依赖
- JDK 17
- Android SDK：`platforms;android-36` + `build-tools;36.0.0`（AGP 会自动补装 35.0.0）
- Gradle **8.11.1**（仓库含 wrapper，已同步版本）
- AGP **8.9.2** + Kotlin **2.2.0**（构建脚本已声明）

### 本地构建（debug APK）

```bash
# 1. 准备 Xray 二进制（约 30MB，被 .gitignore 忽略，不进 Git）
mkdir -p app/src/main/assets/xray
#   从官方 release 下载 arm64-v8a 包并解压出 xray 可执行文件放到上述路径：
#   https://github.com/XTLS/Xray-core/releases/download/v1.8.24/Xray-android-arm64-v8a.zip
#   也可直接执行 scripts/fetch-xray.sh

# 2. 构建（脚本自动写 local.properties 指向本机 SDK）
bash scripts/build.sh
# 产物：app/build/outputs/apk/debug/app-debug.apk
```

> ⚠️ **体积说明**：GeckoView 内核（libxul.so 约 146MB）+ Xray（30MB），debug APK 约 **212MB**。这是「真内核随 APK 打包」的固有代价；release 签名 + 可选 strip 后可降到约 180MB。安装请确保手机有足够空间。

### CI
`.github/workflows/android.yml`：push 到 `main` 后自动构建 debug APK，构建前自动下载 Xray arm64 二进制，产物上传为 GitHub Actions Artifact。

---

## 常见问题（连接失败排查）

| 现象 | 可能原因 | 处理 |
|---|---|---|
| 点「连接」后一直「连接中」 | 节点不可达 / 端口不通 | 到「代理节点」页点「测延迟」；查看「网络日志」页 xray 报错 |
| 显示「未选择节点」 | 尚未添加或未选中节点 | 先添加节点并点击选中（变绿） |
| 能连上但网页打不开 | 节点本身不稳 / 被墙域名 | 换节点；查看网络日志 |
| 浏览器完全无法上网（灰点） | Xray 进程启动失败（磁盘/权限） | 查看网络日志；确认手机为 arm64 且空间充足 |
| 网页能开但代理状态显示未连接 | 处于直连模式，属正常 | 连接节点后流量才走节点 |

---

## 目录结构

```
app/src/main/java/com/shadowbrowser/app/
├── MainActivity.kt / ShadowApp.kt      # 入口、全局崩溃日志
├── browser/                            # 书签/历史/指纹（Gecko 版）
├── gecko/                              # ★ GeckoView 内核层
│   ├── GeckoEngine.kt                  #   运行时单例 + 代理/指纹 prefs 配置写入
│   ├── GeckoClient.kt                  #   导航/进度/内容委托
│   └── GeckoTabManager.kt              #   标签（GeckoSession + GeckoView）
├── proxy/                              # Xray 进程引擎 + 配置生成 + 网络日志
├── nodes/                              # 节点解析/存储/订阅/测速
└── ui/                                 # Via 风格界面（主页/菜单/设置/节点/书签/历史/日志）
```

---

## 更新记录

### v2.0.0（2026-10）GeckoView 重构
- 浏览器层整体从系统 WebView 迁移到 **GeckoView 153（Firefox 内核）**，随 APK 打包，不再依赖系统 WebView；
- 代理链路改为 **Gecko 引擎 prefs 固定指向本地代理** + **Xray 常驻直连兜底**，切换节点不重建浏览器，解决 v1「假连接 / 闪退 / 代理覆盖兼容」三大问题；
- 指纹升级：新增 **Firefox 基线指纹保护（FPP）** 开关、隐私标签走 Gecko 私有模式；
- 构建链升级：AGP 8.9.2 / Kotlin 2.2.0 / Gradle 8.11.1 / compileSdk 36；
- 已知暂缺（见上「功能菜单」⚠️），页面内会明确提示而非静默失效。

### v1.0.x（已废弃，迁移到 GeckoView）
- v1 为系统 WebView 实现，存在「代理连接假成功、点菜单闪退（ROM 反射 id 差异）、UI 渲染依赖系统 WebView 版本」等问题，自 v2.0 起不再维护该路线。
