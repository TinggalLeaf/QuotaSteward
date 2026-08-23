# 配额管家 (QuotaSteward) 📊

> 一个聚合查询多家 AI 服务剩余额度 / 余额 / 配额窗口的 Android 应用,
> 原生 Miuix (澎湃 OS) 风格,支持通用 Lua 风格的脚本 DSL 自定义提取器。

[![Build Status](https://img.shields.io/github/actions/workflow/status/TinggalLeaf/QuotaSteward/build.yml?branch=main&label=build)](https://github.com/TinggalLeaf/QuotaSteward/actions)
[![Release](https://img.shields.io/github/v/release/TinggalLeaf/QuotaSteward)](https://github.com/TinggalLeaf/QuotaSteward/releases)
[![License](https://img.shields.io/github/license/TinggalLeaf/QuotaSteward)](LICENSE)
[![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84)](https://developer.android.com)

---

## ✨ 特性

- **🎨 原生 Miuix 风格** — 澎湃 OS / MIUI 视觉语言(小标题 + 圆角卡片 + 三栏导航)
- **🧩 通用模板** — 每个服务用 JSON DSL 描述请求模板与提取器,无需写代码即可添加新渠道
- **📜 脚本 DSL** — 仿 Lua 的轻量提取器语法:JSON path、算术运算、命名变换(first / sum / min / max / toBool / flip100)
- **📦 内置 12 个预设** — NewAPI、OneAPI、OpenAI、Claude OAuth、Codex OAuth、DeepSeek、Kimi、智谱 GLM、MiniMax、OpenRouter、SiliconFlow 等
- **🖼 100+ 供应商 Logo** — 全部从 [cc-switch](https://github.com/farion1231/cc-switch) 拉到本地 assets
- **⚙️ 完整设置** — 主题(浅/深/系统)+ 6 种色彩预设 + Material You 动态取色 + 刷新间隔 + 网络超时 + 低配额提醒
- **🔄 自动刷新** — 后台协程按用户配置间隔拉取,手动单条刷新,启用 / 停用开关
- **🏗 多分包构建** — GitHub Actions 自动产出 arm64-v8a / armeabi-v7a / x86_64 / x86 + universal APK 与 AAB

## 📸 截图

_截图即将补充_

## 🏗 架构

```
app/src/main/java/com/github/tinggalleaf/ai_quota_dashboard/
├── AIQuotaApp.kt              # Application 入口,初始化 ServiceLocator
├── MainActivity.kt            # Miuix Scaffold + 底部导航 + NavHost
├── ServiceLocator.kt          # 单例容器(无 Hilt,零依赖)
│
├── data/
│   ├── model/                 # ServiceConfig, QuotaResult, AppSettings
│   ├── preset/PresetLoader.kt # 从 assets/presets/*.json 加载内置预设
│   ├── datastore/             # DataStore Preferences(JSON 字符串存服务列表)
│   └── repo/QuotaRepository.kt
│
├── core/
│   ├── template/TemplateEngine.kt   # {{var}} 替换
│   ├── script/
│   │   ├── JsonPathResolver.kt      # $.a.b[0].c 路径解析
│   │   ├── ArithmeticParser.kt      # + - * / 表达式
│   │   ├── ExtractorScript.kt       # JSON DSL 数据类
│   │   └── ExtractorInterpreter.kt  # 解释器
│   └── net/QuotaFetcher.kt          # OkHttp + JSON 解析 + 脚本执行
│
└── ui/
    ├── theme/Theme.kt         # 6 种色彩预设 + Monet 动态取色
    ├── dashboard/             # 主界面:服务卡片 + 配额进度条
    ├── services/              # 服务管理(启用/编辑/删除)
    ├── editor/                # 编辑器 + 预设选择器
    ├── settings/              # 设置页(主题/刷新/网络/关于)
    └── components/            # ProviderIcon 等
```

### 内置预设模板示例 (NewAPI)

```json
{
  "id": "newapi",
  "urlTemplate": "{{baseUrl}}/api/user/self",
  "method": "GET",
  "headers": [
    { "key": "Authorization", "value": "Bearer {{accessToken}}" },
    { "key": "New-Api-User",   "value": "{{userId}}" }
  ],
  "variables": ["baseUrl", "accessToken", "userId"],
  "unit": "USD",
  "iconAsset": "newapi.svg",
  "scriptSource": "{ \"validWhen\":\"response.success == true\", \"remaining\":{\"path\":\"response.data.quota\",\"divide\":500000}, \"used\":{\"path\":\"response.data.used_quota\",\"divide\":500000}, \"total\":{\"expr\":\"remaining + used\"} }"
}
```

### 提取器 DSL

每个 `scriptSource` 是一个 JSON,字段含义:

| 字段 | 作用 |
|------|------|
| `validWhen` | 可选。布尔表达式,失败时该服务标记为无效 |
| `planName` / `remaining` / `used` / `total` | 主字段绑定 |
| `tiers[]` | 多窗口计划(如 5h + 7d) |
| `unit` | `USD` / `CNY` / `TOKENS` / `REQUESTS` |

每个字段绑定 (`FieldBinding`) 支持:

- `path`: JSONPath 表达式,`$.data.limits[0].percentage`
- `expr`: 已绑定字段的算术表达式,`remaining + used`
- `divide` / `multiply`: 数值缩放(用于 NewAPI 的 `/500000` 换算)
- `transform`: 命名变换,`first` / `sum` / `min` / `max` / `toBool` / `flip100`
- `default`: 解析失败时回退

## 🚀 构建

### 前置要求

- JDK 17+
- Android SDK 35+
- Gradle 9.x(随仓库的 `gradlew`)

### 本地构建

```bash
# 调试版 (universal APK)
./gradlew assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk

# Release APK(含 ABI 分包)
./gradlew assembleRelease
# → app/build/outputs/apk/release/app-arm64-v8a-release.apk
# → app/build/outputs/apk/release/app-armeabi-v7a-release.apk
# → app/build/outputs/apk/release/app-x86_64-release.apk
# → app/build/outputs/apk/release/app-x86-release.apk
# → app/build/outputs/apk/release/app-universal-release.apk

# Android App Bundle (Google Play)
./gradlew bundleRelease
# → app/build/outputs/bundle/release/app-release.aab
```

### Release 签名

将 keystore 放在 `app/keystore.jks`,并在仓库根目录创建 `keystore.properties`:

```properties
storeFile=keystore.jks
storePassword=<your-store-pwd>
keyAlias=<your-key-alias>
keyPassword=<your-key-pwd>
```

未提供时 release 会自动用 debug keystore 签名,方便本地测试。

## 🤖 CI / CD

| Workflow | 触发 | 产物 |
|----------|------|------|
| `build.yml` | push / PR 到 main、手动触发 | debug + release + bundle 三个 artifact |
| `release-please.yml` | push 到 main | 自动开 PR → 合入 → 发 GitHub Release |
| `reusable-build.yml` | workflow_call | 复用构建步骤 |

详细配置见 [`.github/workflows/`](.github/workflows/)。

### 发版流程

1. 在 `main` 上提交,commit message 用 [Conventional Commits](https://www.conventionalcommits.org/) 前缀(`feat:` / `fix:` / `chore:` 等)
2. release-please 自动开一个 `chore: release vX.Y.Z` PR
3. 合并该 PR → 自动创建 tag `vX.Y.Z` + GitHub Release + 附加 APK/AAB
4. 手动发布:在 Actions 页面 `workflow_dispatch` 也可触发

## 🤝 致谢

- [Miuix](https://github.com/yiqifanhua/miuix) — 澎湃 OS 风格 Compose 库
- [cc-switch](https://github.com/farion1231/cc-switch) — 额度查询逻辑与 Logo 资源来源
- [OkHttp](https://square.github.io/okhttp/) — 网络层
- [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization) — JSON 解析

## 📄 License

[MIT](LICENSE)
