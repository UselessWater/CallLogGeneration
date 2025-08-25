# 开发初衷
本项目主要为朋友定制开发，如果您有需要欢迎提功能需求，说不定我一空闲，一下午就给你造出来了。
目前通话记录仅仅只有呼出（给朋友定制），其他功能后续有人需要再开发。
目前适配的手机vivo 小米 华为 三星 荣耀等。毕竟是个小工具，适合有一些需要电话截图的上班族忽悠老板，没有什么难的技术，仅仅是要不要耗时间去开发他而已。

# 通话记录生成工具

一个使用Jetpack Compose构建的Android应用，用于批量生成通话记录，支持多SIM卡选择和自定义时间设置。在vivo手机上测试，所有功能通过。

## 📦 版本说明

项目提供多个版本，推荐最新版，一般适配最强。

### 🔄 版本对比（目前已经删除旧版本，1.0之前版本）

| 特性 | 旧UI版本 | 新UI版本 |
|------|----------|----------|
| **界面设计** | 基础Material Design | 现代化Material Design 3 |
| **选择控件** | RadioButton选择 | 下拉框选择 |
| **布局结构** | 简单线性布局 | 卡片式分组布局 |
| **颜色主题** | 默认紫色主题 | 现代蓝色主题 |
| **交互体验** | 基础交互 | 增强交互反馈 |
| **功能特性** | 基础功能 | 完整功能+SIM卡显示优化 |
| **厂商适配** | 标准Android支持 | 支持vivo等厂商特殊字段 |

## 🚀 功能特性

- 📞 **批量生成通话记录** - 支持一次性生成多条通话记录
- ⏰ **自定义时间设置** - 支持设置起始时间和通话时长
- 📱 **多SIM卡支持** - 支持选择SIM1/SIM2拨号
- 🎯 **智能时间分布** - 自动生成合理的通话时间间隔
- 🔒 **权限管理** - 完整的运行时权限请求机制
- 📊 **实时反馈** - 生成进度和结果实时显示
- 🔄 **高级兼容性** - 字段级降级机制，完美适配vivo及其他Android设备
- ⚡ **通话类型支持** - 支持呼出、已接、未接、拒接等多种通话类型

## 🛠️ 技术栈

- **语言**: Kotlin
- **UI框架**: Jetpack Compose
- **构建工具**: Gradle (KTS)
- **目标SDK**: 36 (Android 15)
- **最低SDK**: 26 (Android 8.0)
- **日期时间库**: ThreeTenABP 1.4.8

## 📋 权限要求

应用需要以下权限：

- `READ_CALL_LOG` - 读取通话记录
- `WRITE_CALL_LOG` - 写入通话记录  
- `READ_PHONE_STATE` - 读取手机状态
- `READ_PHONE_NUMBERS` - 读取电话号码信息（用于SIM卡识别）

## 🏗️ 项目结构

```
项目根目录/
├── app/                               # 主应用模块
│   ├── src/main/
│   │   ├── java/com/uselesswater/multicallloggeneration/
│   │   │   ├── MainActivity.kt              # 主活动，包含UI和业务逻辑
│   │   │   ├── Constants.kt                 # 常量定义类
│   │   │   ├── CallLogApplication.kt        # 应用类
│   │   │   ├── CallLogGenerator.kt          # 通话记录生成核心逻辑，包含字段级降级机制
│   │   │   ├── DownloadManager.kt           # APK下载管理（包含AppDownloadManager类）
│   │   │   ├── UpdateChecker.kt             # 更新检查
│   │   │   ├── config/UpdateRepositoryConfig.java  # 更新配置
│   │   │   └── ui/theme/                    # Compose主题文件
│   │   │       ├── Color.kt
│   │   │       ├── Theme.kt
│   │   │       └── Type.kt
│   │   ├── res/                            # 资源文件
│   │   │   ├── drawable/                   # 图片资源
│   │   │   ├── mipmap*/                    # 应用图标
│   │   │   ├── values/                     # 字符串、颜色、主题等
│   │   │   └── xml/                        # XML配置文件
│   │   └── AndroidManifest.xml             # 应用清单文件
│   ├── src/test/                           # 单元测试
│   ├── src/androidTest/                    # 仪器测试
│   ├── build.gradle.kts                    # 模块构建配置
│   └── proguard-rules.pro                  # 代码混淆规则
├── gradle/                                # Gradle配置
│   ├── libs.versions.toml                 # 依赖版本管理
│   └── wrapper/                           # Gradle包装器
├── phone/                                 # 设备测试数据
│   ├── vivo.md                            # vivo设备字段分析
│   └── google_sdk_gphone64_x86_64.md      # Google SDK设备字段分析
├── build.gradle.kts                       # 项目构建配置
├── settings.gradle.kts                    # 项目设置
├── gradle.properties                      # Gradle属性配置
└── gradlew, gradlew.bat                   # Gradle包装器脚本
```

## 🎮 使用说明

### 基本使用

1. **输入电话号码**：在文本框中输入电话号码，每行一个号码
2. **设置起始时间**：点击"选择日期"和"选择时间"按钮设置通话开始时间
3. **选择通话时长**：
   - **新UI版本**: 使用下拉框选择预设的通话时长范围
4. **选择SIM卡**:
   - **新UI版本**: 使用下拉框选择使用SIM1或SIM2拨号
5. **生成记录**：点击"批量生成通话记录"按钮

### 通话时长选项

- 15秒-1分钟
- 30秒-1分钟  
- 1分钟-1分30秒

## ⚙️ 构建和运行

### 构建项目
```bash
./gradlew build
```

### 清理构建
```bash
./gradlew clean
```

### 安装调试版本
```bash
./gradlew installDebug
```

### 运行测试
```bash
# 运行单元测试
./gradlew testDebugUnitTest

# 运行Android测试
./gradlew connectedAndroidTest
```

### 代码质量检查
```bash
# 运行lint检查
./gradlew lintDebug

# 修复lint问题
./gradlew lintFix

# 查看lint报告
./gradlew lintReportDebug
```

## 🔧 开发说明

### 主要功能模块

#### 权限管理 (`MainActivity.kt:85-104`)
- 使用 `ActivityResultContracts.RequestMultiplePermissions` 请求多个权限
- 完整的权限状态检查和请求流程

#### 通话记录生成 (`CallLogGeneratorApp` Composable)
- Jetpack Compose实现的UI界面
- 电话号码池输入处理
- 时间选择器集成
- SIM卡选择逻辑

#### SIM卡处理 (`getPhoneAccountInfo` 函数)
- 通过 `SubscriptionManager` 获取可用SIM卡订阅信息
- 支持多SIM卡设备识别
- 自动映射SIM卡槽到订阅ID
- 智能适配不同设备的SIM卡槽索引（0-based和1-based）

### 技术实现细节

#### 通话记录插入
使用 `ContentResolver` 和 `CallLog.Calls` ContentProvider插入通话记录，采用先进的字段级降级机制：

```kotlin
val values = ContentValues().apply {
    put(CallLog.Calls.NUMBER, phoneNumber)
    put(CallLog.Calls.DATE, currentTime)
    put(CallLog.Calls.DURATION, duration)
    put(CallLog.Calls.TYPE, callType)
    
    // 字段级降级机制：优先标准Android逻辑，失败后尝试厂商特定字段
    // 1. 先设置标准Android字段
    try {
        put(CallLog.Calls.PHONE_ACCOUNT_ID, phoneAccountInfo.accountId)
        put(CallLog.Calls.PHONE_ACCOUNT_COMPONENT_NAME, phoneAccountInfo.componentName)
    } catch (e: Exception) {
        Log.e("CallLogInsert", "设置标准Android字段失败: ${e.message}")
    }
    
    // 2. 然后尝试厂商特定字段 (vivo/OPPO/小米等使用simid字段)
    try {
        put("simid", simSlot)
    } catch (e: Exception) {
        Log.w("CallLogInsert", "设置厂商字段simid失败: ${e.message}")
    }
    
    // 3. 处理subscription_id字段 (vivo/小米/荣耀等使用)
    try {
        put("subscription_id", subscriptionId)
    } catch (e: Exception) {
        Log.w("CallLogInsert", "设置厂商字段subscription_id失败: ${e.message}")
    }
    
    // 4. 处理subscription_component_name字段 (荣耀等使用)
    try {
        put("subscription_component_name", phoneAccountInfo.componentName.toString())
    } catch (e: Exception) {
        Log.w("CallLogInsert", "设置厂商字段subscription_component_name失败: ${e.message}")
    }
}
contentResolver.insert("content://call_log/calls".toUri(), values)
```

#### 时间处理
使用ThreeTenABP库处理日期时间，支持时区正确的日期时间计算。

## 📱 设备兼容性

### 支持的Android版本
- Android 8.0 (API 26) 及以上

### 先进的字段级降级机制
应用采用了创新的字段级降级机制，在所有Android设备上提供最佳兼容性：

#### 兼容性策略
- **优先标准Android逻辑**：在所有设备上都优先尝试标准Android字段
- **字段级降级**：每个字段独立尝试，失败后使用厂商特定字段
- **智能回退**：确保至少设置基本的Android标准字段

#### 字段映射关系
| 厂商特定字段 | Android标准字段 | 支持厂商 | 描述 |
|-------------|----------------|----------|------|
| `simid` | `PHONE_ACCOUNT_ID` | vivo, OPPO, 小米等 | SIM卡标识字段 |
| `subscription_component_name` | `PHONE_ACCOUNT_COMPONENT_NAME` | 荣耀, vivo等 | 组件名称字段 |
| `subscription_id` | - | vivo, 小米, 荣耀等 | 订阅ID字段 |

#### 多厂商设备完美适配
针对vivo、OPPO、小米、荣耀、三星等厂商设备的深度优化：
- ✅ 支持各厂商特有的SIM卡标识字段
- ✅ SIM卡标识正确显示
- ✅ 完整的通话记录功能

#### 其他Android设备兼容性
- ✅ 自动降级到标准Android字段
- ✅ 保持SIM卡功能正常
- ✅ 无厂商检测依赖，纯字段级兼容

## 🐛 问题排查

### 常见问题

1. **SIM卡不显示**
   - 检查是否授予了 `READ_PHONE_NUMBERS` 权限
   - 查看日志确认PhoneAccount信息是否正确获取

2. **权限请求失败**
   - 确保在AndroidManifest.xml中声明了所有必要权限
   - 检查运行时权限请求逻辑

3. **时间显示问题**
   - 确认ThreeTenABP库正确配置
   - 检查时区设置

### 调试信息
应用提供了详细的调试日志，可以通过Logcat查看：
- `CallLogGeneratorApp` - 主界面日志
- `getPhoneAccountInfo` - SIM卡订阅信息获取
- `CallLogInsert` - 通话记录插入详情
- `DebugCallLog` - 现有通话记录调试信息
- `SIMAdapter` - SIM卡适配日志（字段级降级详情）

### 高级调试功能
- **现有通话记录分析**：通过 `debugExistingCallLogs()` 函数分析系统中现有通话记录的字段结构
- **Subscription详细信息**：显示每个SIM卡订阅的ID、槽位、运营商等信息
- **字段级调试**：详细记录每个字段使用的是vivo逻辑还是Android标准逻辑

## ⚠️ 注意事项

*   **关于未接来电的响铃时长**: 对于未接来电 (`type=3`) 和拒接来电 (`type=5`)，`duration` 字段用于设置电话的响铃时长（单位：秒），而非实际通话时长。

## 🌐 GitHub项目

- 项目已开源到GitHub：https://github.com/UselessWater/CallLogGeneration
- 如需下载最新apk，可前往gitee：https://gitee.com/uselesswater/CallLogGeneration

### 📁 分支说明

- **main分支**: 包含最新版本的新UI界面

用户可以根据需要切换分支查看不同版本的代码。

## 📄 许可证

本项目仅供学习和开发测试使用，请勿用于非法用途。许可证：[LICENSE](LICENSE)

## 🤝 贡献

欢迎提交Issue和Pull Request来改进这个项目。

## 📞 支持

如有问题或建议，请通过项目Issue页面提交。

---

**注意**: 本工具由苏兄推出，请遵守相关法律法规，合理使用。作者：UselessWater

*最后更新: 2025年8月25日*
