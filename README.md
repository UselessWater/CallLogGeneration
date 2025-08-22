# 通话记录生成工具

一个使用Jetpack Compose构建的Android应用，用于批量生成通话记录，支持多SIM卡选择和自定义时间设置。

## 📦 版本说明

项目提供两个版本供用户选择：

### 🔄 版本对比

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
app/
├── src/main/
│   ├── java/com/uselesswater/multicallloggeneration/
│   │   ├── MainActivity.kt      # 主活动，包含UI逻辑和业务逻辑
│   │   ├── CallLogApplication.kt # 应用类
│   │   └── ui/theme/            # Compose主题文件
│   ├── res/                     # 资源文件
│   └── AndroidManifest.xml      # 应用清单文件
└── build.gradle.kts             # 模块构建配置
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
- 通过 `TelecomManager` 获取可用电话账户
- 支持多SIM卡设备识别
- 自动映射SIM卡槽到PhoneAccount
- 厂商特定字段适配（如vivo的simid字段）

### 技术实现细节

#### 通话记录插入
使用 `ContentResolver` 和 `CallLog.Calls` ContentProvider插入通话记录：

```kotlin
val values = ContentValues().apply {
    put(CallLog.Calls.NUMBER, phoneNumber)
    put(CallLog.Calls.DATE, currentTime)
    put(CallLog.Calls.DURATION, duration)
    put(CallLog.Calls.TYPE, CallLog.Calls.OUTGOING_TYPE)
    put(CallLog.Calls.PHONE_ACCOUNT_ID, phoneAccountInfo.accountId)
    put(CallLog.Calls.PHONE_ACCOUNT_COMPONENT_NAME, phoneAccountInfo.componentName)
    
    // 厂商特定字段适配
    if (Build.MANUFACTURER.equals("vivo", ignoreCase = true)) {
        try {
            put("simid", phoneAccountInfo.slotIndex + 1) // vivo特有字段
        } catch (e: Exception) {
            Log.w("CallLogInsert", "Failed to set vivo specific field", e)
        }
    }
}
contentResolver.insert("content://call_log/calls".toUri(), values)
```

#### 时间处理
使用ThreeTenABP库处理日期时间，支持时区正确的日期时间计算。

## 📱 设备兼容性

### 支持的Android版本
- Android 8.0 (API 26) 及以上

### 多厂商适配
应用已针对以下情况进行优化：

#### 标准Android设备
- 使用标准的 `PHONE_ACCOUNT_ID` 和 `subscription_id` 字段
- 完全遵循Android官方API规范

#### vivo设备特别适配
针对vivo Origin OS 5 (Android 15) 的特殊处理：

**问题**：vivo系统使用特有的 `simid` 字段显示SIM卡标识，标准Android字段在某些vivo设备上可能不生效

**解决方案**：
- 同时设置标准字段和vivo特有字段，双重保障
- 使用try-catch包装厂商特定字段操作，确保兼容性
- 优雅降级机制确保不影响其他设备
- 自动检测设备厂商并应用相应的字段设置

**适配字段**：
- `simid` - SIM卡标识字段（vivo特有，对应SIM卡槽号）
- `subscription_id` - 标准订阅ID字段
- `phone_account_address` - 电话账户地址
- `subscription_component_name` - 订阅组件名称

**实现位置**：
- `MainActivity.kt:527-543` - vivo特有字段设置
- `MainActivity.kt:458-491` - PhoneAccount信息获取
- `MainActivity.kt:494-525` - 通话记录插入逻辑

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
- `getPhoneAccountInfo` - SIM卡账户信息
- `CallLogInsert` - 通话记录插入详情
- `DebugCallLog` - 现有通话记录调试信息
- `VivoAdapter` - vivo设备适配日志

### 新增调试功能
- **现有通话记录分析**：通过 `debugExistingCallLogs()` 函数分析系统中现有通话记录的字段结构
- **PhoneAccount详细信息**：显示每个PhoneAccount的能力、地址、组件名称等详细信息
- **厂商检测**：自动识别设备厂商并应用相应的适配策略

## 🌐 GitHub项目

- 项目已开源到GitHub：https://github.com/UselessWater/CallLogGeneration
- 如需下载最新apk，可前往gitee：https://gitee.com/uselesswater/CallLogGeneration

### 📁 分支说明

- **main分支**: 包含最新版本的新UI界面

用户可以根据需要切换分支查看不同版本的代码。

## 📄 许可证

本项目仅供学习和开发测试使用，请勿用于非法用途。

## 🤝 贡献

欢迎提交Issue和Pull Request来改进这个项目。

## 📞 支持

如有问题或建议，请通过项目Issue页面提交。

---

**注意**: 本工具由苏哥推出，请遵守相关法律法规，合理使用。

*最后更新: 2025年8月22日*
