# 指纹保护说明（v2 / GeckoView，诚实版）

v2.0 起浏览器层基于 **GeckoView（Firefox 引擎）**，指纹可改项比系统 WebView 多一档：除 UA 外，还能开启 **Firefox 基线指纹保护（FPP）** 与**禁用 WebRTC（引擎级）**。请务必理解以下边界，避免误解。

## 支持的项目

| 项目 | 实现 | 生效时机 |
| --- | --- | --- |
| 随机 UA | `GeckoSessionSettings.setUserAgentOverride` 随机生成 | 当前及新建标签立即生效 |
| 电脑模式 UA | 切换为桌面端 UA（`Windows NT ... Firefox/156.0`） | 当前及新建标签立即生效 |
| 自定义 UA | 用户输入任意 UA 字符串 | 当前及新建标签立即生效 |
| 基线指纹保护 FPP | 引擎 prefs `privacy.fingerprintingProtection=true` | **重启应用后生效** |
| 禁用 WebRTC | 引擎 prefs `media.peerconnection.enabled=false` | **重启应用后生效** |
| 隐身标签 | `GeckoSessionSettings.Builder().usePrivateMode(true)` | 新建隐身标签即生效 |

入口：**菜单 → 浏览器标识**，或 **设置 → 指纹伪装**。

### Firefox 基线指纹保护（FPP）是什么

FPP 是 Firefox 内置的反指纹机制（GeckoView 通过 `baselineFingerprintingProtection` 系列 API / prefs 暴露），在引擎层统一处理：

- 字体指纹：网页无法通过字体探测差异获知系统字体列表；
- Canvas 噪声 / 读取限制：`toDataURL` 等结果不再暴露 GPU 渲染差异；
- WebRTC IP 保护：阻止网页通过 STUN 探测本机真实 IP（即便未禁用 WebRTC）；
- 减少了 JS 可观察的系统属性差异。

这比 v1（WebView 只能改 UA/时区）在「类指纹浏览器」方向上实质更强：**引擎级统一处理，无需依赖系统 WebView 行为**。

## 诚实的能力边界（重要）

- GeckoView **不提供**逐项伪造（如把 Canvas 输出伪装成固定指纹、伪造 `hardwareConcurrency` 为任意值、伪造屏幕分辨率等）；这类「逐项精确伪装」需要修改引擎源码（自编译 Gecko/Chromium），本工程不包含。
- FPP 与 WebRTC 开关通过引擎 prefs 注入，**改动需重启应用**生效（界面已提示）。
- 真实网络路径：连接节点后出口 IP 变为节点 IP，这是本项目的核心价值，但不属于「指纹伪装」。

## 代理带来的「身份」变化

- 连接节点后，出口 IP 变为节点 IP（`whatismyip` 类站点可见）。
- 配合 UA / FPP / 禁用 WebRTC，可显著降低被同一 IP + UA 关联的概率。
- 请启用「禁用 WebRTC」；本工程未做自定义 DNS 层，如需 DNS 防泄漏建议在节点服务端配置。
