<p align="center">
  <img src="docs/assets/banner.jpg" alt="项目横幅" width="100%">
</p>

<p align="center">
  <img src="docs/assets/icon.png" width="88" height="88" alt="应用图标">
</p>

<h1 align="center">通话记录生成</h1>

<p align="center">
  Android 通话记录批量生成工具，使用 Kotlin 与 Jetpack Compose 开发。
</p>

<p align="center">
  <a href="https://github.com/UselessWater/CallLogGeneration/actions/workflows/ci.yml"><img src="https://github.com/UselessWater/CallLogGeneration/actions/workflows/ci.yml/badge.svg" alt="Android CI"></a>
  <a href="https://github.com/UselessWater/CallLogGeneration/releases"><img src="https://img.shields.io/github/v/release/UselessWater/CallLogGeneration" alt="Release"></a>
  <a href="LICENSE"><img src="https://img.shields.io/github/license/UselessWater/CallLogGeneration" alt="License"></a>
</p>

## 简介

本应用用于在 Android 设备上批量写入通话记录，适用于自有设备上的功能测试、演示和数据迁移。应用通过系统 `ContentProvider` 写入 `CallLog`，不修改系统数据库结构；针对不同厂商的字段差异提供运行时检测和降级处理。

本项目仅供合法测试场景使用。请勿用于伪造记录、骚扰、欺诈或其他违法用途。

## 功能

- 批量生成：每行输入一个号码，一次生成多条通话记录。
- 时间设置：指定起始时间、通话时长和记录之间的间隔。
- 多 SIM 卡：选择 SIM 1 或 SIM 2，写入对应的 `PhoneAccount` 与订阅信息。
- 通话类型：呼出、已接、未接、拒接、呼出未接通；vivo 设备使用专用类型值。
- 厂商字段适配：运行时检测 `simid`、`subscription_id`、`subscription_component_name`、`record_duration` 等字段，按设备能力降级。
- 结果反馈：显示生成进度、成功数量和失败原因。
- 应用内更新：从 Gitee 发布页检查新版本。

## 下载

- [GitHub Releases](https://github.com/UselessWater/CallLogGeneration/releases)：由 GitHub Actions 自动构建并发布签名 APK。
- [Gitee Releases](https://gitee.com/uselesswater/CallLogGeneration/releases)：配置 `GITEE_TOKEN` 后，同一工作流会把 APK 自动同步到 Gitee；应用内更新也读取这里。

## 兼容性

| 项目 | 说明 |
| --- | --- |
| 最低版本 | Android 5.1（API 22） |
| 目标版本 | Android 14（API 34） |
| 编译版本 | Android 16（API 36） |
| 已测试设备 | vivo、小米、OPPO、荣耀、华为、三星及 Google SDK 模拟器 |

| 厂商 | 支持情况 | 备注 |
| --- | --- | --- |
| vivo | 基础功能、SIM 标识、呼出未接通 | 使用 vivo 专用通话类型 |
| 小米 | 基础功能 | 兼容性较好 |
| OPPO、华为、荣耀 | 基础功能 | 部分机型无法写入响铃时长，显示为 0 |
| 三星 | 基础功能 | 响铃时长字段不可用 |
| 其他设备 | 标准字段降级 | 以实际设备为准 |

## 构建

### 环境

- JDK 17
- Android SDK Platform 36
- Android SDK Build Tools 35.0.0
- Gradle 8.13（随 Wrapper 提供）

### 本地构建

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

Debug APK 位于 `app/build/outputs/apk/debug/`。

### 自动构建

仓库使用 GitHub Actions 完成构建和发布：

| 工作流 | 触发条件 | 产物 |
| --- | --- | --- |
| Android CI | 推送到任意分支、Pull Request | Debug APK、单元测试结果、Lint 报告 |
| Release | 推送 `vX.Y.Z` tag | 签名 Release APK、GitHub Release、Gitee Release（配置 `GITEE_TOKEN` 后） |

Release 工作流需要以下仓库 Secrets。请使用与现有安装包相同的密钥库，否则已安装用户无法覆盖升级：

| Secret | 说明 |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | 密钥库文件的 Base64 内容 |
| `ANDROID_KEYSTORE_PASSWORD` | 密钥库密码 |
| `ANDROID_KEY_ALIAS` | 密钥别名 |
| `ANDROID_KEY_PASSWORD` | 密钥密码 |
| `GITEE_TOKEN` | Gitee 私人令牌，勾选 `projects` 权限；未配置时跳过 Gitee 发布 |

配置 `GITEE_TOKEN` 后，发布 tag 会同时更新 GitHub Release 和 Gitee Release，应用内更新可以自动检测到新版本。Gitee 私人令牌在「设置 > 私人令牌」中生成。

生成密钥库 Base64（PowerShell）：

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("release.jks"))
```

发布新版本时推送 tag 即可，版本号会自动写入 APK：

```bash
git tag v3.0.2
git push origin v3.0.2
```

如果创建 Release 时提示权限不足，请在仓库 `Settings > Actions > General > Workflow permissions` 中允许工作流读写仓库内容。

### 本地签名

复制 `keystore.properties.example` 为 `keystore.properties`，填写密钥库路径、密码和别名。该文件已被 `.gitignore` 忽略，不会提交到仓库。

## 项目结构

```text
app/src/main/java/com/uselesswater/multicallloggeneration/
├── MainActivity.kt          界面与交互
├── CallLogGenerator.kt      通话记录写入逻辑
├── RuntimeFieldDetector.kt  运行时字段检测
├── DeviceFieldConfig.kt     厂商字段配置
├── UpdateChecker.kt         更新检查
└── DownloadManager.kt       APK 下载与安装
```

## 技术栈

| 组件 | 版本 |
| --- | --- |
| Kotlin | 2.1.0 |
| Jetpack Compose BOM | 2025.01.00 |
| Android Gradle Plugin | 8.12.1 |
| Gradle | 8.13 |
| ThreeTenABP | 1.4.9 |

## 权限

| 权限 | 用途 |
| --- | --- |
| `READ_CALL_LOG`、`WRITE_CALL_LOG` | 读取和写入通话记录 |
| `READ_PHONE_STATE` | 读取通话状态 |
| `READ_PHONE_NUMBERS` | 识别 SIM 卡与订阅信息 |
| `INTERNET`、`ACCESS_NETWORK_STATE` | 检查更新 |
| `REQUEST_INSTALL_PACKAGES` | 安装下载的 APK |

## 已知限制

- 部分厂商 ROM 隐藏了响铃时长字段，写入后可能显示为 0。
- 通话记录字段随系统版本和 ROM 变化，运行时检测只能覆盖常见字段。
- 应用需要通话记录读写权限，权限被拒绝时无法生成记录。
- 不同 ROM 对 `CallLog` 写入的限制不同，少数设备可能无法写入。

## 相关链接

- [GitHub 仓库](https://github.com/UselessWater/CallLogGeneration)
- [Gitee 镜像](https://gitee.com/uselesswater/CallLogGeneration)
- [更新记录](UPDATE_NOTES.md)

## 许可证

[MIT License](LICENSE)

## 免责声明

本工具仅用于在本人拥有或已获授权的设备上进行测试、演示和数据迁移。请勿用于伪造通话记录、骚扰、欺诈或其他违法用途。使用本工具产生的一切后果由使用者自行承担。
