package com.uselesswater.multicallloggeneration

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.provider.CallLog

import android.telephony.SubscriptionManager
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.mutableIntStateOf
import androidx.core.content.ContextCompat
import com.uselesswater.multicallloggeneration.ui.theme.CallLogGenerationTheme
import kotlin.random.Random
import java.util.*
import androidx.core.net.toUri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// 更新检查相关导入
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope

class MainActivity : ComponentActivity() {

    private var permissionCallback: ((Boolean) -> Unit)? = null
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        Log.i(TAG, "Permissions granted: $allGranted")
        if (allGranted) {
            Toast.makeText(this, Constants.PERMISSION_GRANTED, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, Constants.PERMISSION_PARTIAL, Toast.LENGTH_LONG).show()
        }
        // 执行权限检查后的回调
        permissionCallback?.invoke(allGranted)
        permissionCallback = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "onCreate: Activity created")

        // 启动时清理遗留的测试记录
        try {
            RuntimeFieldDetector.cleanupOnAppStart(this)
        } catch (e: Exception) {
            Log.w(TAG, "启动时清理测试记录失败: ${e.message}")
        }

        setContent {
            CallLogGenerationTheme {
                CallLogGeneratorApp(contentResolver, this::checkAndRequestPermissions)
            }
        }
    }

    private fun checkAndRequestPermissions(callback: ((Boolean) -> Unit)? = null): Boolean {
        val permissionsToRequest = arrayOf(
            Constants.PERMISSION_READ_CALL_LOG,
            Constants.PERMISSION_WRITE_CALL_LOG,
            Constants.PERMISSION_READ_PHONE_STATE,
            Constants.PERMISSION_READ_PHONE_NUMBERS
        )

        val permissionsNotGranted = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        return if (permissionsNotGranted.isEmpty()) {
            Log.d(TAG, "All permissions are already granted.")
            callback?.invoke(true)
            true
        } else {
            Log.i(TAG, "Requesting permissions: ${permissionsNotGranted.joinToString()}")
            // 防止竞态条件：先设置回调再启动权限请求
            synchronized(this) {
                permissionCallback = callback
                requestPermissionLauncher.launch(permissionsNotGranted.toTypedArray())
            }
            false
        }
    }

    companion object {
        private const val TAG = Constants.TAG_MAIN_ACTIVITY
    }
}

/**
 * 验证电话号码格式是否有效
 * 防御性编程：支持多种常见的电话号码格式
 */
private fun isValidPhoneNumber(phoneNumber: String): Boolean {
    if (phoneNumber.isBlank()) return false
    
    // 移除所有空格、连字符、括号等格式字符
    val cleanNumber = phoneNumber.replace("[\\s\\-()+]".toRegex(), "")
    
    // 检查是否只包含数字
    if (!cleanNumber.matches("\\d+".toRegex())) return false
    
    // 检查长度：支持7-15位数字（国际标准）
    return cleanNumber.length in 7..15
}

/**
 * 调试函数：检查现有通话记录的SIM卡相关字段
 */
private fun debugExistingCallLogs(contentResolver: ContentResolver) {
    try {
        val cursor = contentResolver.query(
            Constants.CALL_LOG_URI.toUri(),
            null,
            null,
            null,
            Constants.CALL_LOG_SORT_ORDER
        )

        cursor?.use {
            Log.d(Constants.TAG_DEBUG_CALL_LOG, "Found ${it.count} existing call logs")
            Log.d(Constants.TAG_DEBUG_CALL_LOG, "Available columns: ${it.columnNames.joinToString()}")

            if (it.moveToFirst()) {
                do {
                    val number = it.getString(it.getColumnIndexOrThrow(CallLog.Calls.NUMBER))
                    val phoneAccountId = it.getString(it.getColumnIndexOrThrow(CallLog.Calls.PHONE_ACCOUNT_ID))

                    Log.d(Constants.TAG_DEBUG_CALL_LOG, "Call to $number:")
                    Log.d(Constants.TAG_DEBUG_CALL_LOG, "  PHONE_ACCOUNT_ID: $phoneAccountId")

                    // 检查可能的SIM相关字段（包括vivo特有的字段）

                    Constants.POSSIBLE_SIM_FIELDS.forEach { fieldName ->
                        try {
                            val columnIndex = it.getColumnIndex(fieldName)
                            if (columnIndex >= 0) {
                                val value = it.getString(columnIndex)
                                Log.d(Constants.TAG_DEBUG_CALL_LOG, "  $fieldName: $value")
                            }
                        } catch (_: Exception) {
                            // 字段不存在，忽略
                        }
                    }
                } while (it.moveToNext() && it.position < 2) // 只检查前3条记录
            }
        }
    } catch (e: Exception) {
        Log.e(Constants.TAG_DEBUG_CALL_LOG, "Error debugging call logs", e)
    }
}

/**
 * 获取指定SIM卡槽的SubscriptionId
 * 修正版本：正确处理数据类型和厂商特定逻辑
 * 
 * 注意：此方法从SubscriptionManager获取订阅ID，用于设置数据库字段
 * 不同于分析现有通话记录中的subscription_id字段值
 */
private fun getSubscriptionId(context: Context, simSlot: Int): Int {
    try {
        val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
        val subscriptionInfos = subscriptionManager.activeSubscriptionInfoList

        if (subscriptionInfos != null && subscriptionInfos.isNotEmpty()) {
            Log.d("getSubscriptionId", "Found ${subscriptionInfos.size} active subscriptions for SIM slot $simSlot")
            
            // 记录所有订阅信息以便调试
            subscriptionInfos.forEachIndexed { index, info ->
                Log.d("getSubscriptionId", "Subscription $index: id=${info.subscriptionId}, slot=${info.simSlotIndex}, carrier=${info.carrierName}, state=${info.simSlotIndex}")
            }

            // 根据设备厂商调整映射策略
            val deviceManufacturer = android.os.Build.MANUFACTURER.lowercase()
            val targetSlotIndex = when (deviceManufacturer) {
                "vivo", "oppo", "xiaomi" -> {
                    // 这些厂商通常使用1-based的simSlot直接对应
                    simSlot - 1  // 转换为0-based的slotIndex
                }
                "huawei", "honor" -> {
                    // 华为系列可能有特殊映射
                    simSlot - 1
                }
                else -> {
                    // 标准Android (Google SDK等)
                    simSlot - Constants.DEFAULT_SIM_SLOT_INDEX_OFFSET
                }
            }

            Log.d("getSubscriptionId", "Target slot index: $targetSlotIndex for manufacturer: $deviceManufacturer")

            // 策略1: 精确匹配slotIndex
            var matchingSubscription = subscriptionInfos.find { it.simSlotIndex == targetSlotIndex }
            if (matchingSubscription != null) {
                Log.d("getSubscriptionId", "Found exact match: subscription ID ${matchingSubscription.subscriptionId} for SIM slot $simSlot (slot index: $targetSlotIndex)")
                return matchingSubscription.subscriptionId
            }

            // 策略2: 尝试其他可能的映射
            val alternateMappings = listOf(
                simSlot - 1,    // 0-based
                simSlot,        // 1-based (直接)
                simSlot - 2     // 某些特殊情况
            ).distinct().filter { it >= 0 }

            for (alternateIndex in alternateMappings) {
                matchingSubscription = subscriptionInfos.find { it.simSlotIndex == alternateIndex }
                if (matchingSubscription != null) {
                    Log.w("getSubscriptionId", "Found alternate match: subscription ID ${matchingSubscription.subscriptionId} for slot index $alternateIndex")
                    return matchingSubscription.subscriptionId
                }
            }

            // 策略3: 对于单卡设备或无法匹配的情况，使用第一个有效订阅
            val firstValidSubscription = subscriptionInfos.find { 
                it.subscriptionId > 0  // 确保subscriptionId有效（大于0）
            }
            if (firstValidSubscription != null) {
                Log.w("getSubscriptionId", "Using first valid subscription: ID ${firstValidSubscription.subscriptionId}, slot ${firstValidSubscription.simSlotIndex}")
                return firstValidSubscription.subscriptionId
            }

            // 策略4: 厂商特定的默认值
            val defaultSubscriptionId = when (deviceManufacturer) {
                "vivo" -> 7     // vivo设备常见的subscription_id
                "huawei", "honor" -> 1  // 华为设备常见值
                else -> 1       // 通用默认值
            }
            
            Log.w("getSubscriptionId", "No matching subscription found, using manufacturer default: $defaultSubscriptionId for $deviceManufacturer")
            return defaultSubscriptionId

        } else {
            Log.w("getSubscriptionId", "No active subscriptions found")
            // 返回厂商特定的默认值而不是-1
            val deviceManufacturer = android.os.Build.MANUFACTURER.lowercase()
            val defaultValue = when (deviceManufacturer) {
                "vivo" -> 7
                "huawei", "honor" -> 1
                else -> 1
            }
            Log.w("getSubscriptionId", "Using fallback default value: $defaultValue for $deviceManufacturer")
            return defaultValue
        }
    } catch (e: SecurityException) {
        Log.e("getSubscriptionId", "SecurityException while accessing subscription info: ${e.message}")
        // 返回厂商特定的默认值
        val deviceManufacturer = android.os.Build.MANUFACTURER.lowercase()
        val defaultValue = when (deviceManufacturer) {
            "vivo" -> 7
            "huawei", "honor" -> 1
            else -> 1
        }
        Log.w("getSubscriptionId", "Using security fallback value: $defaultValue")
        return defaultValue
    } catch (e: Exception) {
        Log.e("getSubscriptionId", "Error getting subscription ID: ${e.message}")
        return 1  // 通用安全默认值
    }
}

// 定义时间范围数据类
data class TimeRange(val name: String, val minSeconds: Int, val maxSeconds: Int)

@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallLogGeneratorApp(contentResolver: ContentResolver, checkPermission: (callback: (Boolean) -> Unit) -> Boolean) {
    var phoneNumbersText by remember { mutableStateOf("") }
    var message by remember { mutableStateOf(Constants.DEFAULT_MESSAGE) }
    var showDialog by remember { mutableStateOf(false) }
    var generatedCount by remember { mutableIntStateOf(0) }

    // 起始时间状态
    var startTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    // 定义四个时间范围选项（包括自定义）
    val timeRanges = remember {
        listOf(
            TimeRange(Constants.TIME_RANGE_SHORT_NAME, Constants.TIME_RANGE_SHORT_MIN, Constants.TIME_RANGE_SHORT_MAX),
            TimeRange(Constants.TIME_RANGE_MEDIUM_NAME, Constants.TIME_RANGE_MEDIUM_MIN, Constants.TIME_RANGE_MEDIUM_MAX),
            TimeRange(Constants.TIME_RANGE_LONG_NAME, Constants.TIME_RANGE_LONG_MIN, Constants.TIME_RANGE_LONG_MAX),
            TimeRange(Constants.TIME_RANGE_CUSTOM_NAME, Constants.TIME_RANGE_CUSTOM_MIN, Constants.TIME_RANGE_CUSTOM_MAX)
        )
    }


    // SIM卡选择状态
    var selectedSim by remember { mutableIntStateOf(1) } // 1 for SIM1, 2 for SIM2
    val context = LocalContext.current
    
    // 更新检查状态
    var showUpdateDialog by remember { mutableStateOf(false) }
    var updateResult by remember { mutableStateOf<UpdateResult?>(null) }
    var includePreReleases by remember { mutableStateOf(false) }
    var showUpdateOptions by remember { mutableStateOf(false) }
    
    // 下载状态
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableIntStateOf(0) }
    var showDownloadDialog by remember { mutableStateOf(false) }

    // 通话类型选择状态 - 使用新的类型安全架构
    var callTypeUIState by remember { mutableStateOf(CallTypeUIState()) }
    var ringDuration by remember { mutableIntStateOf(Constants.DEFAULT_RING_DURATION) }
    var showAdvancedSettings by remember { mutableStateOf(false) }
    
    // 获取所有通话类型选项
    remember { CallType.getAllOptions() }

    // 格式化时间显示
    val dateTimeFormatter = remember {
        DateTimeFormatter.ofPattern(Constants.DATE_TIME_PATTERN)
    }

    val displayTime = remember(startTimeMillis) {
        val instant = Instant.ofEpochMilli(startTimeMillis)
        val localDateTime = instant.atZone(ZoneId.systemDefault()).toLocalDateTime()
        localDateTime.format(dateTimeFormatter)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // 应用标题
        Text(
            text = Constants.APP_TITLE,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // 应用说明
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // 电话号码输入区域
        androidx.compose.material3.Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = Constants.PHONE_NUMBER_POOL_TITLE,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                OutlinedTextField(
                    value = phoneNumbersText,
                    onValueChange = { phoneNumbersText = it },
                    label = { Text(Constants.PHONE_NUMBER_LABEL) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    placeholder = { Text(Constants.PHONE_NUMBER_PLACEHOLDER) },
                    shape = MaterialTheme.shapes.medium
                )
            }
        }

        // 时间设置区域
        androidx.compose.material3.Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = Constants.TIME_SETTINGS_TITLE,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // 起始时间显示
                Text(
                    text = "${Constants.START_TIME_LABEL}$displayTime",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilledTonalButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(Constants.DATE_BUTTON_TEXT)
                    }
                    FilledTonalButton(
                        onClick = { showTimePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(Constants.TIME_BUTTON_TEXT)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 通话时长选择 - 下拉框（基于类型属性决定是否显示）
                if (callTypeUIState.shouldShowDurationSettings) {
                    var timeRangeExpanded by remember { mutableStateOf(false) }
                    Text(
                        text = Constants.CALL_DURATION_LABEL,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    ExposedDropdownMenuBox(
                        expanded = timeRangeExpanded,
                        onExpandedChange = { timeRangeExpanded = !timeRangeExpanded }
                    ) {
                        OutlinedTextField(
                            value = timeRanges[callTypeUIState.selectedTimeRangeIndex].name,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = timeRangeExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                        )

                        ExposedDropdownMenu(
                            expanded = timeRangeExpanded,
                            onDismissRequest = { timeRangeExpanded = false }
                        ) {
                            timeRanges.forEachIndexed { index, timeRange ->
                            DropdownMenuItem(
                                text = { Text(timeRange.name) },
                                onClick = {
                                    callTypeUIState = callTypeUIState.updateTimeRangeIndex(index)
                                    timeRangeExpanded = false
                                }
                            )
                        }
                        }
                    }
                }

                // 自定义时长设置（基于UI状态决定是否显示）
                if (callTypeUIState.shouldShowCustomDurationSettings) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        // 最小时长输入框
                        var minDurationText by remember { mutableStateOf(callTypeUIState.customMinDuration.toString()) }
                        var minDurationError by remember { mutableStateOf("") }
                        
                        OutlinedTextField(
                            value = minDurationText,
                            onValueChange = { newValue ->
                                minDurationText = newValue
                                // 实时验证输入
                                when {
                                    newValue.isEmpty() -> {
                                        minDurationError = "请输入最小时长"
                                    }
                                    !newValue.all { it.isDigit() } -> {
                                        minDurationError = "请输入有效数字"
                                    }
                                    else -> {
                                        val value = newValue.toIntOrNull()
                                        when {
                                            value == null -> minDurationError = "请输入有效数字"
                                            value < Constants.TIME_RANGE_CUSTOM_MIN -> minDurationError = "最小时长不能小于${Constants.TIME_RANGE_CUSTOM_MIN}秒"
                                            value > Constants.TIME_RANGE_CUSTOM_MAX -> minDurationError = "最小时长不能大于${Constants.TIME_RANGE_CUSTOM_MAX}秒"
                                            else -> {
                                                minDurationError = ""
                                                callTypeUIState = callTypeUIState.updateCustomDuration(value, callTypeUIState.customMaxDuration)
                                            }
                                        }
                                    }
                                }
                            },
                            label = { Text("最小时长 (秒)") },
                            isError = minDurationError.isNotEmpty(),
                            supportingText = if (minDurationError.isNotEmpty()) {
                                { Text(minDurationError, color = MaterialTheme.colorScheme.error) }
                            } else null,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // 最大时长输入框
                        var maxDurationText by remember { mutableStateOf(callTypeUIState.customMaxDuration.toString()) }
                        var maxDurationError by remember { mutableStateOf("") }
                        
                        OutlinedTextField(
                            value = maxDurationText,
                            onValueChange = { newValue ->
                                maxDurationText = newValue
                                // 实时验证输入
                                when {
                                    newValue.isEmpty() -> {
                                        maxDurationError = "请输入最大时长"
                                    }
                                    !newValue.all { it.isDigit() } -> {
                                        maxDurationError = "请输入有效数字"
                                    }
                                    else -> {
                                        val value = newValue.toIntOrNull()
                                        when {
                                            value == null -> maxDurationError = "请输入有效数字"
                                            value < Constants.TIME_RANGE_CUSTOM_MIN -> maxDurationError = "最大时长不能小于${Constants.TIME_RANGE_CUSTOM_MIN}秒"
                                            value > Constants.TIME_RANGE_CUSTOM_MAX -> maxDurationError = "最大时长不能大于${Constants.TIME_RANGE_CUSTOM_MAX}秒"
                                            else -> {
                                                maxDurationError = ""
                                                callTypeUIState = callTypeUIState.updateCustomDuration(callTypeUIState.customMinDuration, value)
                                            }
                                        }
                                    }
                                }
                            },
                            label = { Text("最大时长 (秒)") },
                            isError = maxDurationError.isNotEmpty(),
                            supportingText = if (maxDurationError.isNotEmpty()) {
                                { Text(maxDurationError, color = MaterialTheme.colorScheme.error) }
                            } else null,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        // 快捷设置按钮
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    callTypeUIState = callTypeUIState.updateCustomDuration(5, 30)
                                    minDurationText = "5"
                                    maxDurationText = "30"
                                    minDurationError = ""
                                    maxDurationError = ""
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("短通话")
                            }
                            FilledTonalButton(
                                onClick = {
                                    callTypeUIState = callTypeUIState.updateCustomDuration(60, 300)
                                    minDurationText = "60"
                                    maxDurationText = "300"
                                    minDurationError = ""
                                    maxDurationError = ""
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("中等通话")
                            }
                            FilledTonalButton(
                                onClick = {
                                    callTypeUIState = callTypeUIState.updateCustomDuration(600, 1800)
                                    minDurationText = "600"
                                    maxDurationText = "1800"
                                    minDurationError = ""
                                    maxDurationError = ""
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("长通话")
                            }
                        }
                        
                        // 验证最小最大值关系
                        if (callTypeUIState.customMinDuration > callTypeUIState.customMaxDuration && minDurationError.isEmpty() && maxDurationError.isEmpty()) {
                            Text(
                                text = "⚠️ 最小时长不能大于最大时长",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // SIM卡选择区域
        androidx.compose.material3.Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = Constants.SIM_SELECTION_TITLE,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // SIM卡选择 - 下拉框
                var simExpanded by remember { mutableStateOf(false) }
                val simOptions = listOf(Constants.SIM_OPTION_SIM1, Constants.SIM_OPTION_SIM2)

                ExposedDropdownMenuBox(
                    expanded = simExpanded,
                    onExpandedChange = { simExpanded = !simExpanded }
                ) {
                    OutlinedTextField(
                        value = simOptions[selectedSim - 1],
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = simExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                    )

                    ExposedDropdownMenu(
                        expanded = simExpanded,
                        onDismissRequest = { simExpanded = false }
                    ) {
                        simOptions.forEachIndexed { index, simOption ->
                            DropdownMenuItem(
                                text = { Text(simOption) },
                                onClick = {
                                    selectedSim = index + 1
                                    simExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // 通话类型选择区域
        androidx.compose.material3.Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "通话类型设置",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // 通话类型选择 - 下拉框
                var callTypeExpanded by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(
                    expanded = callTypeExpanded,
                    onExpandedChange = { callTypeExpanded = !callTypeExpanded }
                ) {
                    OutlinedTextField(
                        value = callTypeUIState.selectedCallType.displayName,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = callTypeExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                    )

                    ExposedDropdownMenu(
                        expanded = callTypeExpanded,
                        onDismissRequest = { callTypeExpanded = false }
                    ) {
                        CallType.entries.forEach { callType ->
                            DropdownMenuItem(
                                text = { Text(callType.displayName) },
                                onClick = {
                                    callTypeUIState = callTypeUIState.updateCallType(callType)
                                    callTypeExpanded = false
                                }
                            )
                        }
                    }
                }

                // 高级设置按钮
                androidx.compose.material3.TextButton(
                    onClick = { showAdvancedSettings = !showAdvancedSettings },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("${if (showAdvancedSettings) "隐藏" else "显示"}高级设置")
                }

                // 高级设置区域
                if (showAdvancedSettings) {
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        // 响铃时长设置（仅用于未接来电）
                        if (callTypeUIState.selectedCallType == CallType.MISSED) {
                            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                Text(
                                    text = "响铃时长: ${ringDuration}秒",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                androidx.compose.material3.Slider(
                                    value = ringDuration.toFloat(),
                                    onValueChange = { ringDuration = it.toInt() },
                                    valueRange = 1f..60f,
                                    steps = 59,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                    }
                }
            }
        }

        // 生成按钮
        androidx.compose.material3.Button(
            onClick = {
                Log.d("CallLogGeneratorApp", "Generate button clicked.")
                
                // 防御性编程：验证UI状态
                val validationResult = callTypeUIState.validateCurrentState()
                if (!validationResult.isValid) {
                    message = validationResult.errorMessage
                    Log.w("CallLogGeneratorApp", "Validation failed: ${validationResult.errorMessage}")
                    return@Button
                }
                
                checkPermission { hasPermission ->
                    if (hasPermission) {
                        try {
                            val phoneNumbers = phoneNumbersText
                                .split("\n")
                                .map { it.trim() }
                                .filter { it.isNotEmpty() }

                            if (phoneNumbers.isEmpty()) {
                                message = Constants.ERROR_NO_PHONE_NUMBERS
                                Log.w("CallLogGeneratorApp", "Validation failed: No phone numbers entered.")
                                return@checkPermission
                            }
                            
                            // 防御性编程：验证电话号码格式
                            val invalidNumbers = phoneNumbers.filter { !isValidPhoneNumber(it) }
                            if (invalidNumbers.isNotEmpty()) {
                                message = "⚠️ 发现无效电话号码格式：${invalidNumbers.take(3).joinToString(", ")}${if (invalidNumbers.size > 3) "等" else ""}"
                                Log.w("CallLogGeneratorApp", "Validation failed: Invalid phone numbers found")
                                return@checkPermission
                            }
                            
                            // 防御性编程：限制生成数量
                            if (phoneNumbers.size > 1000) {
                                message = "⚠️ 一次最多生成1000条通话记录，当前：${phoneNumbers.size}条"
                                Log.w("CallLogGeneratorApp", "Validation failed: Too many phone numbers")
                                return@checkPermission
                            }

                            generatedCount = phoneNumbers.size
                            showDialog = true
                            Log.i("CallLogGeneratorApp", "Validation successful. Showing confirmation dialog for $generatedCount numbers.")
                        } catch (e: Exception) {
                            message = "生成失败: ${e.message}"
                            Log.e("CallLogGeneratorApp", "Error during validation or showing dialog", e)
                        }
                    } else {
                        message = "⚠️ 需要授予通话记录权限才能生成记录"
                        Log.w("CallLogGeneratorApp", "Generate operation aborted: permissions not granted")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Text(
                text = Constants.GENERATE_BUTTON_TEXT,
                style = MaterialTheme.typography.titleMedium
            )
        }

        // 设置按钮
        FilledTonalButton(
            onClick = {
                // 先显示更新选项对话框
                showUpdateOptions = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text(Constants.CHECK_UPDATE_BUTTON_TEXT)
        }

        // 作者信息
        Text(
            text = Constants.AUTHOR_INFO,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 8.dp)
        )
    }

    // 日期选择器对话框
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = startTimeMillis
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            // 只更新日期部分，保留当前时间部分
                            val currentInstant = Instant.ofEpochMilli(startTimeMillis)
                            val selectedInstant = Instant.ofEpochMilli(it)

                            val newInstant = selectedInstant.atZone(ZoneId.systemDefault())
                                .withHour(currentInstant.atZone(ZoneId.systemDefault()).hour)
                                .withMinute(currentInstant.atZone(ZoneId.systemDefault()).minute)
                                .withSecond(currentInstant.atZone(ZoneId.systemDefault()).second)
                                .toInstant()

                            startTimeMillis = newInstant.toEpochMilli()
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("确认")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showDatePicker = false }) {
                    Text("取消")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // 时间选择器对话框
    if (showTimePicker) {
        val currentTime = Instant.ofEpochMilli(startTimeMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalTime()

        val timePickerState = rememberTimePickerState(
            initialHour = currentTime.hour,
            initialMinute = currentTime.minute
        )

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { 
                Text("选择时间", style = MaterialTheme.typography.headlineSmall) 
            },
            text = {
                TimePicker(state = timePickerState)
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        val currentInstant = Instant.ofEpochMilli(startTimeMillis)
                        val localDate = currentInstant.atZone(ZoneId.systemDefault()).toLocalDate()

                        val newInstant = localDate.atTime(timePickerState.hour, timePickerState.minute)
                            .atZone(ZoneId.systemDefault())
                            .toInstant()

                        startTimeMillis = newInstant.toEpochMilli()
                        showTimePicker = false
                    }
                ) {
                    Text("确认")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showTimePicker = false }) {
                    Text("取消")
                }
            }
        )
    }

    // 确认对话框
    if (showDialog) {
        val selectedRange = timeRanges[callTypeUIState.selectedTimeRangeIndex]
        AlertDialog(
            onDismissRequest = { showDialog = false },
            icon = { 
                androidx.compose.material3.Icon(
                    painter = androidx.compose.ui.res.painterResource(android.R.drawable.ic_dialog_info),
                    contentDescription = "确认信息"
                )
            },
            title = { 
                Text("确认生成", style = MaterialTheme.typography.headlineSmall) 
            },
            text = {
                Column {
                    Text(
                        text = "您确定要生成 $generatedCount 条通话记录吗？",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "• 起始时间: $displayTime",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (callTypeUIState.selectedTimeRangeIndex == 3) {
                            "• 通话时长: ${selectedRange.name} (${callTypeUIState.customMinDuration}-${callTypeUIState.customMaxDuration}秒)"
                        } else {
                            "• 通话时长: ${selectedRange.name} (${selectedRange.minSeconds}-${selectedRange.maxSeconds}秒)"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "• SIM 卡: SIM $selectedSim",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "• 通话类型: ${callTypeUIState.selectedCallType.displayName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        Log.i("CallLogGeneratorApp", "Confirmation received. Starting generation.")
                        // 执行实际的生成操作
                        try {
                            val phoneAccountInfo = getPhoneAccountInfo(context, selectedSim)
                            if (phoneAccountInfo == null) {
                                message = String.format(Constants.ERROR_SIM_NOT_FOUND, selectedSim)
                                Log.w("CallLogGeneratorApp", "Could not find phone account for SIM $selectedSim.")
                                showDialog = false
                                return@TextButton
                            }

                            // 调试：打印当前存在的通话记录中的SIM信息
                            debugExistingCallLogs(contentResolver)

                            val phoneNumbers = phoneNumbersText
                                .split("\n")
                                .map { it.trim() }
                                .filter { it.isNotEmpty() }

                            var successCount = 0
                            // 使用起始时间作为第一条记录的时间
                            var currentTime = startTimeMillis

                            Log.d("CallLogGeneratorApp", "Starting loop to generate ${phoneNumbers.size} call logs.")
                            phoneNumbers.forEach { phoneNumber ->
                                // 使用CallTypeUIState计算最终时长
                                val duration = callTypeUIState.calculateFinalDuration(selectedRange)

                                val values = ContentValues().apply {
                                    put(CallLog.Calls.NUMBER, phoneNumber)
                                    put(CallLog.Calls.DATE, currentTime)
                                    put(CallLog.Calls.NEW, 1)
                                    put(CallLog.Calls.CACHED_NAME, "")
                                    put(CallLog.Calls.CACHED_NUMBER_TYPE, 0)
                                    put(CallLog.Calls.COUNTRY_ISO, Locale.getDefault().country)

                                    // 使用CallLogGenerator创建不同类型的通话记录
                                    CallLogGenerator.createCallByType(
                                        values = this,
                                        callType = callTypeUIState.selectedCallType,
                                        duration = duration,
                                        ringDuration = ringDuration,
                                        context = context
                                    )

                                    // 使用智能SIM卡适配方案：先尝试vivo逻辑，失败后降级到标准逻辑
                                    val simFieldsResult = try {
                                        putSimCardFieldsWithFallback(this, selectedSim, phoneAccountInfo, context)
                                        Log.d("CallLogInsert", "SIM卡字段设置成功: accountId=${phoneAccountInfo.accountId}, component=${phoneAccountInfo.componentName}")
                                        true
                                    } catch (e: Exception) {
                                        Log.w("CallLogInsert", "SIM卡字段设置失败: ${e.message}")
                                        false
                                    }
                                    
                                    // 如果SIM卡字段设置失败，尝试设置最基本的标准字段
                                    if (!simFieldsResult) {
                                        val basicFieldsResult = try {
                                            this.put(CallLog.Calls.PHONE_ACCOUNT_ID, phoneAccountInfo.accountId)
                                            this.put(CallLog.Calls.PHONE_ACCOUNT_COMPONENT_NAME, phoneAccountInfo.componentName)
                                            Log.d("CallLogInsert", "使用基本标准字段: ${phoneAccountInfo.accountId}")
                                            true
                                        } catch (e: Exception) {
                                            Log.w("CallLogInsert", "基本字段设置也失败: ${e.message}")
                                            false
                                        }
                                        
                                        if (!basicFieldsResult) {
                                            Log.w("CallLogInsert", "将生成不包含SIM信息的通话记录")
                                        }
                                    }
                                }

                                contentResolver.insert(Constants.CALL_LOG_URI.toUri(), values)
                                successCount++
                                Log.d("CallLogGeneratorApp", "Successfully inserted log for $phoneNumber ($successCount/${phoneNumbers.size})")

                                // 更新时间：当前通话结束时间 + 随机间隔（40~120秒），确保时间不溢出
                                val randomInterval = Random.nextInt(Constants.CALL_INTERVAL_MIN, Constants.CALL_INTERVAL_MAX + 1) * Constants.MILLISECONDS_PER_SECOND
                                val durationMs = duration * 1000L
                                
                                // 检查时间计算是否会溢出
                                val maxSafeTime = Long.MAX_VALUE - randomInterval - durationMs
                                if (currentTime <= maxSafeTime) {
                                    currentTime += durationMs + randomInterval
                                } else {
                                    // 防止溢出，设置一个合理的最大时间（当前时间 + 1年）
                                    val oneYearMs = 365L * 24 * 60 * 60 * 1000
                                    currentTime = minOf(currentTime + durationMs + randomInterval, System.currentTimeMillis() + oneYearMs)
                                    Log.w("CallLogGeneratorApp", "时间计算可能溢出，使用安全时间值: $currentTime")
                                }
                            }

                            message = String.format(Constants.SUCCESS_GENERATION, successCount)
                            Log.i("CallLogGeneratorApp", "Finished generation. Success count: $successCount")
                        } catch (e: SecurityException) {
                            message = "${Constants.ERROR_PERMISSION_DENIED}${e.message}"
                            Log.e("CallLogGeneratorApp", "SecurityException during call log generation", e)
                        } catch (e: Exception) {
                            message = "${Constants.ERROR_GENERATION_FAILED}${e.message}"
                            Log.e("CallLogGeneratorApp", "Generic exception during call log generation", e)
                        }

                        showDialog = false
                    }
                ) {
                    Text("确认生成")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
    
    // 更新选项对话框（先让用户选择是否包含pre-release）
    if (showUpdateOptions) {
        AlertDialog(
            onDismissRequest = { showUpdateOptions = false },
            title = { 
                Text("📦 更新检查选项", style = MaterialTheme.typography.headlineSmall) 
            },
            text = {
                Column {
                    Text(
                        text = "请选择更新策略：",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    // 更新策略选择
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = UpdateChecker.updateToLatest.value,
                            onCheckedChange = { UpdateChecker.updateToLatest.value = it }
                        )
                        Text("总是更新到最新版本", modifier = Modifier.padding(start = 8.dp))
                    }
                    
                    Text(
                        text = "如果启用，将忽略版本新旧直接更新到最新版",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    // 预发布版本选择
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = includePreReleases,
                            onCheckedChange = { includePreReleases = it }
                        )
                        Text("包含预发布版本", modifier = Modifier.padding(start = 8.dp))
                    }
                    
                    Text(
                        text = "预发布版本可能包含新功能但不够稳定",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        // 开始检查更新
                        showUpdateOptions = false
                        
                        // 获取Activity引用并调用扩展函数
                        val activity = context as? MainActivity
                        if (activity != null) {
                            activity.checkForUpdate(
                                includePreReleases = includePreReleases,
                                onStart = { },
                                onResult = { result ->
                                    updateResult = result
                                    showUpdateDialog = true
                                }
                            )
                        } else {
                            // 如果无法获取Activity引用，使用原始方法
                            updateResult = UpdateResult.Error("无法获取Activity上下文")
                            showUpdateDialog = true
                        }
                    }
                ) {
                    Text("开始检查")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { showUpdateOptions = false }
                ) {
                    Text("取消")
                }
            }
        )
    }

    // 下载进度对话框
    if (showDownloadDialog) {
        AlertDialog(
            onDismissRequest = { 
                // 不允许用户取消下载对话框
                if (!isDownloading) {
                    showDownloadDialog = false
                }
            },
            title = {
                Text("📥 正在下载更新", style = MaterialTheme.typography.headlineSmall)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isDownloading) {
                        androidx.compose.material3.LinearProgressIndicator(
                            progress = { downloadProgress / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .padding(vertical = 16.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Text(
                            text = "下载进度: $downloadProgress%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Text(
                            text = "请勿关闭应用，正在下载更新文件...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    } else {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(32.dp)
                                .padding(vertical = 16.dp)
                        )
                        Text(
                            text = "正在准备下载...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                if (isDownloading) {
                    androidx.compose.material3.TextButton(
                        onClick = {
                            // 取消下载
                            val downloadManager = AppDownloadManager(context)
                            downloadManager.cancelDownload()
                            isDownloading = false
                            showDownloadDialog = false
                            Toast.makeText(context, "下载已取消", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("取消下载")
                    }
                } else {
                    null
                }
            },
            dismissButton = {
                if (!isDownloading) {
                    androidx.compose.material3.TextButton(
                        onClick = { showDownloadDialog = false }
                    ) {
                        Text("关闭")
                    }
                } else {
                    null
                }
            }
        )
    }

    // 更新检查结果对话框
    if (showUpdateDialog) {
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            title = {
                Text(
                    when (updateResult) {
                        is UpdateResult.UpdateAvailable -> "📦 发现新版本"
                        is UpdateResult.NoUpdateAvailable -> "✅ 已是最新版本"
                        is UpdateResult.Error -> "❌ 检查更新失败"
                        null -> "检查更新"
                    }
                )
            },
            text = {
                when (val result = updateResult) {
                    is UpdateResult.UpdateAvailable -> {
                        Column {
                            Text("版本: ${result.release.tagName}")
                            Text("发布日期: ${result.release.publishedAt}")
                            if (result.release.prerelease) {
                                Text("⚠️ 预发布版本", color = Color.Yellow)
                            }
                            
                            // 显示更新策略信息
                            if (UpdateChecker.updateToLatest.value) {
                                Text("📋 更新策略: 更新到最新版本", 
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.bodySmall)
                            } else {
                                Text("📋 更新策略: 只更新到新版本", 
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall)
                            }
                            
                            Text("更新内容:")
                            Text(result.release.body, style = MaterialTheme.typography.bodySmall)
                            
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                    is UpdateResult.NoUpdateAvailable -> {
                        if (UpdateChecker.updateToLatest.value) {
                            Text("🎉 恭喜！您已经运行着最新版本！")
                        } else {
                            Text("当前已是最新版本！")
                        }
                    }
                    is UpdateResult.Error -> {
                        Text("检查更新失败: ${result.message}")
                    }
                    null -> {
                        CircularProgressIndicator()
                    }
                }
            },
            confirmButton = {
                when (updateResult) {
                    is UpdateResult.UpdateAvailable -> {
                        androidx.compose.material3.TextButton(
                            onClick = {
                                // 下载更新
                                val release = (updateResult as UpdateResult.UpdateAvailable).release
                                val apkAsset = release.assets.firstOrNull()
                                if (apkAsset != null) {
                                    val downloadManager = AppDownloadManager(context)
                                    // 显示下载进度对话框
                                    isDownloading = true
                                    downloadProgress = 0
                                    showDownloadDialog = true
                                    showUpdateDialog = false
                                    
                                    // 使用前台下载方法
                                    downloadManager.downloadApkSimple(
                                        downloadUrl = apkAsset.downloadUrl,
                                        fileName = apkAsset.name,
                                        onProgress = { progress ->
                                            // 更新下载进度
                                            downloadProgress = progress
                                            Log.d("DownloadProgress", "下载进度: $progress%")
                                        },
                                        onComplete = { file ->
                                            isDownloading = false
                                            showDownloadDialog = false
                                            
                                            if (file != null) {
                                                // 下载完成，立即安装
                                                downloadManager.installApkFile(file)
                                            } else {
                                                // 下载失败
                                                Toast.makeText(context, "下载失败", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    )
                                }
                            }
                        ) {
                            Text("下载更新")
                        }
                    }
                    else -> null
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { showUpdateDialog = false }
                ) {
                    Text("关闭")
                }
            }
        )
    }
}

// 定义PhoneAccountInfo数据类来存储账户信息
data class PhoneAccountInfo(
    val accountId: String,
    val componentName: String
)

private fun getPhoneAccountInfo(context: Context, simSlot: Int): PhoneAccountInfo? {
    Log.d("getPhoneAccountInfo", "Attempting to get phone account for SIM slot: $simSlot")

    return try {
        // 使用SubscriptionManager获取SIM卡信息
        val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
        val subscriptionInfos = subscriptionManager.activeSubscriptionInfoList
        
        if (subscriptionInfos == null || subscriptionInfos.isEmpty()) {
            Log.w("getPhoneAccountInfo", "No active subscriptions found")
            return null
        }

        Log.d("getPhoneAccountInfo", "Found ${subscriptionInfos.size} active subscriptions:")
        
        // 记录所有订阅信息
        subscriptionInfos.forEachIndexed { index, info ->
            Log.d("getPhoneAccountInfo", "Subscription $index: id=${info.subscriptionId}, slot=${info.simSlotIndex}, carrier=${info.carrierName}")
        }

        // 对于多SIM卡设备，尝试根据SIM卡插槽选择
        // SIM卡槽索引可能是0-based或1-based，需要适配不同设备
        val targetSlotIndex = simSlot - Constants.DEFAULT_SIM_SLOT_INDEX_OFFSET
        
        // 首先尝试精确匹配
        var matchingSubscription = subscriptionInfos.find { it.simSlotIndex == targetSlotIndex }
        
        // 如果找不到精确匹配，尝试其他可能的匹配方式
        if (matchingSubscription == null) {
            // 尝试直接使用simSlot作为索引（某些设备可能是1-based）
            matchingSubscription = subscriptionInfos.find { it.simSlotIndex == simSlot }
            
            // 如果还找不到，使用第一个可用的SIM卡
            if (matchingSubscription == null && subscriptionInfos.isNotEmpty()) {
                matchingSubscription = subscriptionInfos[0]
                Log.w("getPhoneAccountInfo", "No exact match found for SIM slot $simSlot, using first available: ${matchingSubscription.simSlotIndex}")
            }
        }
        
        if (matchingSubscription != null) {
            // 使用subscriptionId作为accountId
            val accountInfo = PhoneAccountInfo(
                accountId = matchingSubscription.subscriptionId.toString(),
                componentName = "com.android.phone" // 默认的电话组件
            )
            Log.i("getPhoneAccountInfo", "Selected subscription for SIM $simSlot: ${accountInfo.accountId}")
            return accountInfo
        } else {
            Log.w("getPhoneAccountInfo", "Subscription for SIM slot $simSlot not found. Using first available")
            // 使用第一个可用的订阅
            val firstSubscription = subscriptionInfos[0]
            val accountInfo = PhoneAccountInfo(
                accountId = firstSubscription.subscriptionId.toString(),
                componentName = "com.android.phone"
            )
            Log.i("getPhoneAccountInfo", "Using first subscription: ${accountInfo.accountId}")
            return accountInfo
        }
    } catch (e: SecurityException) {
        Log.e("getPhoneAccountInfo", "SecurityException while accessing subscription info", e)
        null
    } catch (e: Exception) {
        Log.e("getPhoneAccountInfo", "Error getting subscription info", e)
        null
    }
}

@Composable
@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
fun DefaultPreview() {
    CallLogGenerationTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Text(
                text = Constants.APP_TITLE,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "预览界面",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            androidx.compose.material3.Button(
                onClick = { },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Text(
                    text = Constants.GENERATE_BUTTON_TEXT,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

/**
 * 检查更新
 */
private fun MainActivity.checkForUpdate(
    includePreReleases: Boolean,
    onStart: () -> Unit,
    onResult: (UpdateResult) -> Unit
) {
    lifecycleScope.launch(Dispatchers.IO) {
        onStart()
        
        val updateChecker = UpdateChecker(this@checkForUpdate)
        UpdateChecker.includePreReleases.value = includePreReleases
        
        updateChecker.checkForUpdate { result ->
            lifecycleScope.launch(Dispatchers.Main) {
                onResult(result)
            }
        }
    }
}

// 预览函数
@Composable
@Preview(showBackground = true)
fun CallLogGeneratorAppPreview() {
    CallLogGenerationTheme {
        CallLogGeneratorApp(contentResolver = LocalContext.current.contentResolver) { callback -> 
            callback(true)
            true
        }
    }
}


private fun putSimCardFieldsWithFallback(
    values: ContentValues, 
    simSlot: Int, 
    phoneAccountInfo: PhoneAccountInfo,
    context: Context
) {
    // 增强的基于设备配置的字段适配机制，包含空值检查和fallback逻辑
    
    // 1. 先尝试标准Android字段
    try {
        // 验证phoneAccountInfo的有效性
        if (isValidPhoneAccountInfo(phoneAccountInfo)) {
            values.put(CallLog.Calls.PHONE_ACCOUNT_ID, phoneAccountInfo.accountId)
            values.put(CallLog.Calls.PHONE_ACCOUNT_COMPONENT_NAME, phoneAccountInfo.componentName)
            Log.d(Constants.TAG_SIM_ADAPTER, "使用标准Android字段: PHONE_ACCOUNT_ID=${phoneAccountInfo.accountId}, PHONE_ACCOUNT_COMPONENT_NAME=${phoneAccountInfo.componentName}")
        } else {
            Log.w(Constants.TAG_SIM_ADAPTER, "PhoneAccountInfo无效，跳过标准Android字段设置")
        }
    } catch (e: Exception) {
        Log.e(Constants.TAG_SIM_ADAPTER, "设置标准Android字段失败: ${e.message}")
    }
    
    // 2. 根据设备配置尝试厂商特定字段
    val deviceConfig = DeviceFieldConfig.getCurrentDeviceConfig()
    Log.d(Constants.TAG_SIM_ADAPTER, "当前设备配置: ${deviceConfig.description}")
    
    // 尝试simid字段（如果设备支持）
    if (deviceConfig.supportedSimFields.contains("simid")) {
        try {
            // 对于某些设备，simid可能为-1表示无效，需要处理
            val effectiveSimId = if (simSlot > 0) simSlot else 1  // 确保simid至少为1
            values.put("simid", effectiveSimId)
            Log.d(Constants.TAG_SIM_ADAPTER, "使用厂商特定字段: simid=$effectiveSimId (原始: $simSlot)")
        } catch (e: Exception) {
            Log.w(Constants.TAG_SIM_ADAPTER, "设置厂商字段simid失败: ${e.message}")
        }
    }
    
    // 尝试subscription_id字段（如果设备支持）
    if (deviceConfig.supportedSimFields.contains("subscription_id")) {
        try {
            val subscriptionId = getSubscriptionIdSafely(context, simSlot)
            if (subscriptionId != null && subscriptionId >= 0) {
                // 根据设备厂商决定subscription_id的数据类型和值
                val deviceManufacturer = android.os.Build.MANUFACTURER.lowercase()
                when (deviceManufacturer) {
                    "google" -> {
                        // Google SDK设备可能使用字符串值（如"E"）或数字
                        // 基于实际数据，Google设备有时使用特殊字符串
                        if (simSlot == 1) {
                            values.put("subscription_id", subscriptionId)  // 数字
                        } else {
                            values.put("subscription_id", "E")  // 特殊字符串值
                        }
                    }
                    "vivo" -> {
                        // vivo使用数字subscription_id，通常与simid相关
                        values.put("subscription_id", subscriptionId)
                    }
                    "huawei", "honor" -> {
                        // 华为/荣耀使用数字subscription_id
                        values.put("subscription_id", subscriptionId)
                    }
                    else -> {
                        // 其他厂商使用数字
                        values.put("subscription_id", subscriptionId)
                    }
                }
                Log.d(Constants.TAG_SIM_ADAPTER, "使用厂商特定字段: subscription_id=$subscriptionId (厂商: $deviceManufacturer)")
            } else {
                Log.w(Constants.TAG_SIM_ADAPTER, "subscription_id无效($subscriptionId)，尝试fallback方案")
                // Fallback: 使用phoneAccountInfo.accountId作为subscription_id
                if (isValidAccountId(phoneAccountInfo.accountId)) {
                    try {
                        val accountIdAsInt = phoneAccountInfo.accountId.toIntOrNull()
                        if (accountIdAsInt != null && accountIdAsInt >= 0) {
                            values.put("subscription_id", accountIdAsInt)
                            Log.d(Constants.TAG_SIM_ADAPTER, "使用accountId作为subscription_id: $accountIdAsInt")
                        } else {
                            // 如果不能转换为数字，直接使用字符串
                            values.put("subscription_id", phoneAccountInfo.accountId)
                            Log.d(Constants.TAG_SIM_ADAPTER, "使用accountId字符串作为subscription_id: ${phoneAccountInfo.accountId}")
                        }
                    } catch (e: Exception) {
                        Log.d(Constants.TAG_SIM_ADAPTER, "accountId转换为subscription_id失败: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(Constants.TAG_SIM_ADAPTER, "设置厂商字段subscription_id失败: ${e.message}")
        }
    }
    
    // 尝试subscription_component_name字段（如果设备支持）
    if (deviceConfig.supportedSimFields.contains("subscription_component_name")) {
        try {
            val componentName = phoneAccountInfo.componentName
            if (isValidComponentName(componentName)) {
                values.put("subscription_component_name", componentName)
                Log.d(Constants.TAG_SIM_ADAPTER, "使用厂商特定字段: subscription_component_name=$componentName")
            } else {
                Log.w(Constants.TAG_SIM_ADAPTER, "componentName无效，跳过subscription_component_name设置")
            }
        } catch (e: Exception) {
            Log.w(Constants.TAG_SIM_ADAPTER, "设置厂商字段subscription_component_name失败: ${e.message}")
        }
    }
    
    // 尝试华为/荣耀特有的hw_account_id字段
    if (deviceConfig.supportedSimFields.contains("hw_account_id")) {
        try {
            // 对于华为/荣耀设备，尝试设置hw_account_id
            val hwAccountId = getHuaweiAccountId(context, simSlot)
            if (hwAccountId != null) {
                values.put("hw_account_id", hwAccountId)
                Log.d(Constants.TAG_SIM_ADAPTER, "使用华为特定字段: hw_account_id=$hwAccountId")
            }
        } catch (e: Exception) {
            Log.w(Constants.TAG_SIM_ADAPTER, "设置华为字段hw_account_id失败: ${e.message}")
        }
    }
    
    Log.d(Constants.TAG_SIM_ADAPTER, "SIM卡字段设置完成，设备: ${deviceConfig.description}")
}

/**
 * 验证PhoneAccountInfo是否有效
 */
private fun isValidPhoneAccountInfo(phoneAccountInfo: PhoneAccountInfo?): Boolean {
    return phoneAccountInfo != null && 
           isValidAccountId(phoneAccountInfo.accountId) && 
           isValidComponentName(phoneAccountInfo.componentName)
}

/**
 * 验证accountId是否有效
 */
private fun isValidAccountId(accountId: String?): Boolean {
    return !accountId.isNullOrBlank() && 
           accountId.lowercase() != "null" && 
           accountId != "-1"
}

/**
 * 验证componentName是否有效
 */
private fun isValidComponentName(componentName: String?): Boolean {
    return !componentName.isNullOrBlank() && 
           componentName.lowercase() != "null" &&
           componentName.contains("/")  // 有效的ComponentName应该包含"/"
}

/**
 * 安全地获取SubscriptionId，包含错误处理和NULL值检查
 */
private fun getSubscriptionIdSafely(context: Context, simSlot: Int): Int? {
    return try {
        val subscriptionId = getSubscriptionId(context, simSlot)
        if (subscriptionId >= 0) {
            subscriptionId
        } else {
            Log.w(Constants.TAG_SIM_ADAPTER, "getSubscriptionId返回无效值: $subscriptionId")
            null
        }
    } catch (e: Exception) {
        Log.w(Constants.TAG_SIM_ADAPTER, "获取SubscriptionId异常: ${e.message}")
        null
    }
}

/**
 * 获取华为设备的账户ID（如果可用）
 */
private fun getHuaweiAccountId(context: Context, simSlot: Int): String? {
    return try {
        // 对于华为/荣耀设备，hw_account_id通常与subscription_id相关
        val subscriptionId = getSubscriptionIdSafely(context, simSlot)
        if (subscriptionId != null && subscriptionId >= 0) {
            "hw_$subscriptionId"  // 构造华为格式的账户ID
        } else {
            null
        }
    } catch (e: Exception) {
        Log.w(Constants.TAG_SIM_ADAPTER, "获取华为账户ID失败: ${e.message}")
        null
    }
}