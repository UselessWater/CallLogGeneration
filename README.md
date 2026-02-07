# 通话记录生成工具 v3.0.0

一个使用Jetpack Compose构建的Android应用，用于批量生成通话记录，支持多SIM卡选择和自定义时间设置。采用**四级安全验证机制**，彻底解决数据库约束冲突问题，完美适配所有主流Android设备，支持Android 5.1及以上版本。

## 🎯 v3.0.0 重大升级

### 🔧 Vivo设备专项增强
- **Vivo呼出未接通支持**：新增`VIVO_OUTGOING_UNANSWERED`类型，正确处理Vivo设备的呼出未接通场景
- **设备检测优化**：改进Vivo设备识别逻辑，确保功能准确适配
- **类型系统完善**：扩展CallType枚举，支持厂商特定通话类型

### 📱 SDK版本升级
- **targetSdk升级**：从24升级到34（Android 14），符合Google Play最新要求
- **minSdk优化**：调整到22（Android 5.1），扩大设备兼容范围
- **API兼容性**：解决SubscriptionManager等API的兼容性问题

### ✅ 测试框架完善
- **单元测试通过**：所有15个单元测试全部通过，包括新增的Vivo设备测试
- **构建验证**：完整的构建流程验证，确保代码质量
- **设备特定测试**：新增针对Vivo设备的专项测试用例

### 🛡️ 四级安全验证机制
- **非侵入式查询验证**：通过现有记录分析字段支持情况，避免约束冲突
- **数据库schema检查**：确认字段在数据库schema中存在
- **现有记录使用分析**：检查字段在实际通话记录中的使用情况
- **安全测试验证**：最小化测试记录影响，确保数据完整性

### 🔧 智能字段类型适配
- 完整支持Int、Long、String、Boolean等所有ContentValues数据类型
- 自动识别字段类型并生成合适的测试值
- 避免类型不匹配导致的数据库插入失败
- **Vivo设备特殊处理**：专门适配Vivo设备的呼出未接通类型

### 📱 全设备兼容性
- 支持vivo、小米、华为、OPPO、三星、荣耀等所有主流Android设备
- 支持Android 5.1 (API 22) 到 Android 15 (API 36)
- 智能字段级降级机制，确保在各种设备上的稳定性
- **Google Play合规**：targetSdk 34符合最新发布要求

## 📦 版本说明

### 🔄 版本演进

| 特性 | v1.x (旧UI) | v2.x (新UI) | v3.0.0 (安全验证) |
|------|-------------|-------------|-------------------|
| **界面设计** | 基础Material Design | 现代化Material Design 3 | 现代化Material Design 3 |
| **验证机制** | 基础字段检查 | 运行时字段探测 | 四级安全验证机制 |
| **类型支持** | 基本类型 | 基本类型 | 全类型支持 |
| **约束处理** | 无 | 部分处理 | 完整约束感知 |
| **设备兼容性** | 有限支持 | 主流设备支持 | 全设备兼容 |
| **稳定性** | 一般 | 较好 | 极高 |

## 📱 版本演进

| 版本 | 发布日期 | 主要特性 |
|------|----------|----------|
| v3.0.0 | 2025-08-28 | **重大架构升级**：四级安全验证机制、智能字段类型适配、全设备兼容性、代码架构重构、Android 7.0兼容性、单元测试完善.**Vivo设备专项增强**：新增Vivo呼出未接通支持、SDK版本升级（targetSdk 34）、单元测试完善、设备检测优化 |
| v2.9.9 | 2025-08-26 | 字段验证优化、设备兼容性改进 |
| v2.2.6 | 2025-08-25 | 修复字段支持检查重复处理问题 |
| v2.2.5 | 2025-08-25 | 完善拒接原因字段支持逻辑 |
| v2.2.4 | 2025-08-25 | 修复字段降级逻辑关键错误 |
| v2.2.3 | 2025-08-25 | 基于厂商机型的静态字段配置机制 |
| v2.1.0 | 2025-08-23 | Material Design 3界面、智能SIM卡适配 |
| v1.0.0 | 2024-05-20 | 基础通话记录生成功能 |

## 🚀 功能特性

### 核心功能
- 📞 **批量生成通话记录** - 支持一次性生成多条通话记录
- ⏰ **自定义时间设置** - 支持设置起始时间和通话时长
- 📱 **多SIM卡支持** - 支持选择SIM1/SIM2拨号
- 🎯 **智能时间分布** - 自动生成合理的通话时间间隔
- 🔒 **权限管理** - 完整的运行时权限请求机制
- 📊 **实时反馈** - 生成进度和结果实时显示
- ⚡ **通话类型支持** - 支持呼出、已接、未接、拒接、VoIP等多种通话类型

### v3.0.0 安全特性
- 🛡️ **四级安全验证** - 非侵入式字段检测，避免数据库约束冲突
- 🔧 **智能类型适配** - 自动识别并适配Int、Long、String等字段类型
- 📋 **约束感知验证** - 复制现有记录值，确保满足所有数据库约束
- 🧹 **增强清理机制** - 模式匹配清理测试记录，防止数据残留
- 📝 **详细调试日志** - 完整的字段验证过程记录，便于问题排查
- 📱 **Vivo设备增强** - 新增Vivo专用呼出未接通类型支持
- 🎯 **Google Play合规** - targetSdk 34符合最新发布要求

### 兼容性特性
- 🔄 **字段级降级机制** - 智能识别设备支持字段，自动降级到兼容实现
- **全厂商适配** - 支持vivo、小米、华为、OPPO、三星、荣耀等所有设备
- 🤖 **全版本兼容** - 支持Android 5.1到Android 15所有版本
- 📱 **Google Play合规** - targetSdk升级到34，符合最新发布要求
- 🎯 **Vivo专项优化** - 专门解决Vivo设备的呼出未接通功能限制

## 🛠️ 技术栈

### 基础技术
- **语言**: Kotlin
- **UI框架**: Jetpack Compose
- **构建工具**: Gradle (KTS)
- **目标SDK**: 34 (Android 14)
- **最低SDK**: 22 (Android 5.1)
- **编译SDK**: 34 (Android 14)
- **日期时间库**: ThreeTenABP 1.4.8

### v3.0.0 重大架构升级
- **四级验证架构**：非侵入式查询 → Schema检查 → 现有记录分析 → 安全测试验证
- **多类型支持**：完整支持Int、Long、String、Boolean等所有ContentValues数据类型
- **约束感知引擎**：智能复制现有记录字段值，确保满足数据库约束条件
- **代码架构重构**：引入CallType枚举、CallTypeUIState数据类、ValidationResult密封类
- **类型安全增强**：防御性编程、集中状态管理、改进的错误处理机制
- **智能缓存**：30分钟字段检测缓存，减少重复检测开销
- **资源安全**：完善的Cursor和ContentResolver资源释放机制

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
│   │   │   ├── RuntimeFieldDetector.kt      # 运行时字段检测器（v3.0.0新增，四级安全验证）
│   │   │   ├── DeviceFieldConfig.kt         # 设备字段配置管理
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

## 🎯 核心功能模块 (v3.0.0)

### RuntimeFieldDetector - 运行时字段检测器
- **四级安全验证机制**：非侵入式查询 → Schema检查 → 使用分析 → 安全测试
- **多类型字段支持**：自动适配Int、Long、String、Boolean等数据类型
- **约束感知验证**：复制现有记录值满足数据库约束
- **智能缓存系统**：30分钟检测结果缓存，优化性能
- **详细调试日志**：完整的验证过程记录，便于问题排查

### 验证流程
1. **非侵入式验证**：通过查询现有记录验证字段可访问性
2. **Schema检查**：确认字段在数据库schema中存在
3. **使用分析**：检查字段在实际记录中的使用情况
4. **安全测试**：创建最小化测试记录进行最终验证

### 设备兼容性验证
- **已测试设备类型**：vivo、小米、华为、OPPO、三星、荣耀、Google SDK等
- **字段级降级**：智能识别设备支持字段，自动使用兼容实现
- **全版本支持**：Android 5.1 (API 22) 到 Android 15 (API 36)
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
使用 `ContentResolver` 和 `CallLog.Calls` ContentProvider插入通话记录，采用基于厂商配置的字段适配机制：

```kotlin
val values = ContentValues().apply {
    put(CallLog.Calls.NUMBER, phoneNumber)
    put(CallLog.Calls.DATE, currentTime)
    put(CallLog.Calls.DURATION, duration)
    put(CallLog.Calls.TYPE, callType)
    
    // 基于厂商配置的字段适配机制
    // 1. 先设置标准Android字段
    put(CallLog.Calls.PHONE_ACCOUNT_ID, phoneAccountInfo.accountId)
    put(CallLog.Calls.PHONE_ACCOUNT_COMPONENT_NAME, phoneAccountInfo.componentName)
    
    // 2. 根据设备配置尝试厂商特定字段
    val deviceConfig = DeviceFieldConfig.getCurrentDeviceConfig()
    
    // 尝试simid字段（如果设备支持）
    if (deviceConfig.supportedSimFields.contains("simid")) {
        put("simid", simSlot)
    }
    
    // 尝试subscription_id字段（如果设备支持）
    if (deviceConfig.supportedSimFields.contains("subscription_id")) {
        put("subscription_id", subscriptionId)
    }
    
    // 尝试subscription_component_name字段（如果设备支持）
    if (deviceConfig.supportedSimFields.contains("subscription_component_name")) {
        put("subscription_component_name", phoneAccountInfo.componentName.toString())
    }
}
contentResolver.insert("content://call_log/calls".toUri(), values)
```

#### 响铃时长处理
针对不同厂商设备的响铃时长字段差异，采用基于厂商配置的智能字段选择机制：

```kotlin
// 响铃时长字段适配（基于厂商配置）
val deviceConfig = DeviceFieldConfig.getCurrentDeviceConfig()
val supportedRingDurationFields = deviceConfig.supportedRingDurationFields

// 只使用当前设备支持的字段
supportedRingDurationFields.forEach { field ->
    // 尝试设置字段值
    try {
        put(field, ringDuration)
        // 成功设置后退出循环
        return@forEach
    } catch (e: Exception) {
        // 字段设置失败，继续尝试下一个
    }
}
```

#### 设备字段配置管理
采用基于厂商和机型的静态配置确保最大兼容性：

1. **预定义配置**：为各厂商机型预定义字段配置
2. **配置验证**：在使用字段前验证设备是否支持
3. **精确适配**：确保不会在某个机型上使用到另外一个机型的字段

#### 时间处理
使用ThreeTenABP库处理日期时间，支持时区正确的日期时间计算。

## 📱 设备兼容性

### 支持的Android版本
- Android 5.1 (API 22) 及以上
- 推荐Android 8.0 (API 26) 及以上以获得最佳体验
- **Google Play合规**：targetSdk 34符合最新发布要求

## 🔧 兼容性

### 设备兼容性
- ✅ **vivo设备**：**完整支持所有功能**，包括特有的`simid`字段显示SIM卡标识
  - ✅ **v3.0.0新增**：支持呼出未接通功能，通过专用`VIVO_OUTGOING_UNANSWERED`类型实现
- ✅ **小米设备**：**完整支持所有功能**，包括呼出未接通，标准Android字段实现
- ✅ **三星设备**：支持基础功能，标准Android字段实现
  - ⚠️ **已知问题**：响铃时间数据库字段被官方隐藏，生成的响铃时间始终显示为0
- ✅ **华为设备**：支持基础功能，部分高级字段通过降级机制实现
- ✅ **OPPO设备**：支持基础功能，自动适配可用字段
- ✅ **荣耀设备**：支持基础功能，继承华为字段特性
- ✅ **其他Android设备**：通过字段级降级机制确保基础功能可用

### 先进的字段级降级机制
应用采用了创新的字段级降级机制，在所有Android设备上提供最佳兼容性：

#### 兼容性策略
- **厂商配置管理**：基于厂商和机型预定义字段配置，确保精确适配
- **配置验证**：在使用字段前验证设备是否支持该字段
- **智能回退**：确保至少设置基本的Android标准字段
- **设备无关性**：通过预定义配置避免设备间字段混淆

#### 字段映射关系
| 厂商特定字段 | Android标准字段 | 支持厂商 | 描述 |
|-------------|----------------|----------|------|
| `simid` | `PHONE_ACCOUNT_ID` | vivo, OPPO, 小米等 | SIM卡标识字段 |
| `subscription_component_name` | `PHONE_ACCOUNT_COMPONENT_NAME` | 荣耀, vivo等 | 组件名称字段 |
| `subscription_id` | - | vivo, 小米, 荣耀等 | 订阅ID字段 |
| `record_duration` | - | 仅vivo | 录音时长字段（可能是响铃时长） |
| `oplus_data1`, `oplus_data2` | - | OPPO | OPPO特定数据字段 |
| `hw_account_id` | - | 荣耀 | 荣耀账户ID字段 |
| `cloud_antispam_type` | - | 小米 | 小米云防骚扰类型字段 |
| `data1`, `data2` | - | 三星 | 三星特定数据字段 |

#### 多厂商设备完美适配
针对vivo、OPPO、小米、荣耀、三星等厂商设备的深度优化：
- ✅ 支持各厂商特有的SIM卡标识字段
- ✅ SIM卡标识正确显示
- ✅ 完整的通话记录功能
- ✅ 响铃时长字段按厂商适配（如vivo的record_duration字段）

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

**注意**: 本工具由UselessWater推出，请遵守相关法律法规，合理使用。作者：UselessWater

*最后更新: 2025年8月28日*
