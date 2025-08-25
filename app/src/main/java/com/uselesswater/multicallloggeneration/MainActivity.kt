package com.uselesswater.multicallloggeneration

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
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

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
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "onCreate: Activity created")

        setContent {
            CallLogGenerationTheme {
                CallLogGeneratorApp(contentResolver, this::checkAndRequestPermissions)
            }
        }
    }

    private fun checkAndRequestPermissions(): Boolean {
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
            true
        } else {
            Log.i(TAG, "Requesting permissions: ${permissionsNotGranted.joinToString()}")
            requestPermissionLauncher.launch(permissionsNotGranted.toTypedArray())
            false
        }
    }

    companion object {
        private const val TAG = Constants.TAG_MAIN_ACTIVITY
    }
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
                        } catch (e: Exception) {
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
 */
private fun getSubscriptionId(context: Context, simSlot: Int): Int {
    return try {
        val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
        val subscriptionInfos = subscriptionManager.activeSubscriptionInfoList

        if (subscriptionInfos != null && subscriptionInfos.isNotEmpty()) {
            // simSlot是1-based，需要转换为0-based来匹配SlotIndex
            val targetSlotIndex = simSlot - Constants.DEFAULT_SIM_SLOT_INDEX_OFFSET

            // 查找匹配槽位的订阅信息
            val matchingSubscription = subscriptionInfos.find { it.simSlotIndex == targetSlotIndex }

            if (matchingSubscription != null) {
                Log.d("getSubscriptionId", "Found subscription ID ${matchingSubscription.subscriptionId} for SIM slot $simSlot (slot index: $targetSlotIndex)")
                matchingSubscription.subscriptionId
            } else {
                Log.w("getSubscriptionId", "No subscription found for SIM slot $simSlot")
                // 如果找不到指定槽位，返回第一个可用的订阅ID
                subscriptionInfos.firstOrNull()?.subscriptionId ?: -1
            }
        } else {
            Log.w("getSubscriptionId", "No active subscriptions found")
            -1
        }
    } catch (e: SecurityException) {
        Log.e("getSubscriptionId", "SecurityException while accessing subscription info", e)
        -1
    } catch (e: Exception) {
        Log.e("getSubscriptionId", "Error getting subscription ID", e)
        -1
    }
}

// 定义时间范围数据类
data class TimeRange(val name: String, val minSeconds: Int, val maxSeconds: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallLogGeneratorApp(contentResolver: ContentResolver, checkPermission: () -> Boolean) {
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

    // 当前选中的时间范围索引
    var selectedTimeRangeIndex by remember { mutableStateOf(0) }

    // SIM卡选择状态
    var selectedSim by remember { mutableStateOf(1) } // 1 for SIM1, 2 for SIM2
    val context = LocalContext.current
    
    // 更新检查状态
    var showUpdateDialog by remember { mutableStateOf(false) }
    var updateResult by remember { mutableStateOf<UpdateResult?>(null) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var includePreReleases by remember { mutableStateOf(false) }
    var showUpdateOptions by remember { mutableStateOf(false) }
    
    // 下载状态
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableIntStateOf(0) }
    var showDownloadDialog by remember { mutableStateOf(false) }

    // 通话类型选择状态
    var selectedCallTypeIndex by remember { mutableStateOf(0) }
    var ringDuration by remember { mutableIntStateOf(Constants.DEFAULT_RING_DURATION) }
    var selectedNetworkTypeIndex by remember { mutableStateOf(2) } // 默认4G
    var showAdvancedSettings by remember { mutableStateOf(false) }
    
    // 自定义时长状态
    var customMinDuration by remember { mutableIntStateOf(30) }
    var customMaxDuration by remember { mutableIntStateOf(60) }

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
                    androidx.compose.material3.FilledTonalButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(Constants.DATE_BUTTON_TEXT)
                    }
                    androidx.compose.material3.FilledTonalButton(
                        onClick = { showTimePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(Constants.TIME_BUTTON_TEXT)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 通话时长选择 - 下拉框
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
                        value = timeRanges[selectedTimeRangeIndex].name,
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
                                    selectedTimeRangeIndex = index
                                    timeRangeExpanded = false
                                }
                            )
                        }
                    }
                }

                // 自定义时长设置（当选择自定义时显示）
                if (selectedTimeRangeIndex == 3) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(
                            text = "最小时长: ${customMinDuration}秒",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        androidx.compose.material3.Slider(
                            value = customMinDuration.toFloat(),
                            onValueChange = { customMinDuration = it.toInt() },
                            valueRange = Constants.TIME_RANGE_CUSTOM_MIN.toFloat()..Constants.TIME_RANGE_CUSTOM_MAX.toFloat(),
                            steps = 59,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Text(
                            text = "最大时长: ${customMaxDuration}秒",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                        androidx.compose.material3.Slider(
                            value = customMaxDuration.toFloat(),
                            onValueChange = { customMaxDuration = it.toInt() },
                            valueRange = Constants.TIME_RANGE_CUSTOM_MIN.toFloat()..Constants.TIME_RANGE_CUSTOM_MAX.toFloat(),
                            steps = 59,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        // 验证最小最大值
                        if (customMinDuration > customMaxDuration) {
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
                        value = Constants.CALL_TYPE_OPTIONS[selectedCallTypeIndex].first,
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
                        Constants.CALL_TYPE_OPTIONS.forEachIndexed { index, (name, _) ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    selectedCallTypeIndex = index
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
                        // 响铃时长设置（用于未接/拒接来电）
                        val currentCallTypeValue = Constants.CALL_TYPE_OPTIONS[selectedCallTypeIndex].second
                        if (currentCallTypeValue in listOf(Constants.CALL_TYPE_MISSED, -1)) {
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
                if (checkPermission()) {
                    try {
                        val phoneNumbers = phoneNumbersText
                            .split("\n")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }

                        if (phoneNumbers.isEmpty()) {
                            message = Constants.ERROR_NO_PHONE_NUMBERS
                            Log.w("CallLogGeneratorApp", "Validation failed: No phone numbers entered.")
                            return@Button
                        }

                        generatedCount = phoneNumbers.size
                        showDialog = true
                        Log.i("CallLogGeneratorApp", "Validation successful. Showing confirmation dialog for $generatedCount numbers.")
                    } catch (e: Exception) {
                        message = "生成失败: ${e.message}"
                        Log.e("CallLogGeneratorApp", "Error during validation or showing dialog", e)
                    }
                } else {
                    Log.w("CallLogGeneratorApp", "Generate button clicked but permissions are not granted.")
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

        androidx.compose.material3.AlertDialog(
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
        val selectedRange = timeRanges[selectedTimeRangeIndex]
        androidx.compose.material3.AlertDialog(
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
                        text = if (selectedTimeRangeIndex == 3) {
                            "• 通话时长: ${selectedRange.name} (${customMinDuration}-${customMaxDuration}秒)"
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
                        text = "• 通话类型: ${Constants.CALL_TYPE_OPTIONS[selectedCallTypeIndex].first}",
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
                                // 在选定的时间范围内生成随机通话时长
                                val duration = if (selectedTimeRangeIndex == 3) {
                                    // 自定义时长范围
                                    Random.nextInt(customMinDuration, customMaxDuration + 1)
                                } else {
                                    // 预设时长范围
                                    Random.nextInt(selectedRange.minSeconds, selectedRange.maxSeconds + 1)
                                }

                                val values = ContentValues().apply {
                                    put(CallLog.Calls.NUMBER, phoneNumber)
                                    put(CallLog.Calls.DATE, currentTime)
                                    put(CallLog.Calls.NEW, 1)
                                    put(CallLog.Calls.CACHED_NAME, "")
                                    put(CallLog.Calls.CACHED_NUMBER_TYPE, 0)
                                    put(CallLog.Calls.COUNTRY_ISO, Locale.getDefault().country)

                                    // 使用CallLogGenerator创建不同类型的通话记录
                                    val callTypeValue = Constants.CALL_TYPE_OPTIONS[selectedCallTypeIndex].second
                                    
                                    // 对于拒接来电和未接来电，duration应该为0，使用ringDuration作为响铃时长
                                    val finalDuration = if (callTypeValue in listOf(Constants.CALL_TYPE_MISSED, Constants.CALL_TYPE_REJECTED)) 0 else duration
                                    CallLogGenerator.createCallByType(
                                        values = this,
                                        callTypeValue = callTypeValue,
                                        duration = finalDuration,
                                        ringDuration = ringDuration
                                    )

                                    // 使用智能SIM卡适配方案：先尝试vivo逻辑，失败后降级到标准逻辑
                                    try {
                                        putSimCardFieldsWithFallback(this, selectedSim, phoneAccountInfo, context)
                                        Log.d("CallLogInsert", "Using phone account ID: ${phoneAccountInfo.accountId}, component: ${phoneAccountInfo.componentName} for SIM $selectedSim")
                                    } catch (e: Exception) {
                                        Log.e("CallLogInsert", "SIM卡字段设置失败，但继续生成通话记录: ${e.message}")
                                        // 即使SIM卡字段设置失败，仍然继续生成通话记录
                                        try {
                                            // 尝试设置最基本的Android标准字段
                                            this.put(CallLog.Calls.PHONE_ACCOUNT_ID, phoneAccountInfo.accountId)
                                            this.put(CallLog.Calls.PHONE_ACCOUNT_COMPONENT_NAME, phoneAccountInfo.componentName)
                                        } catch (e2: Exception) {
                                            Log.w("CallLogInsert", "无法设置任何SIM卡字段，生成无SIM信息的通话记录")
                                        }
                                    }
                                }

                                contentResolver.insert(Constants.CALL_LOG_URI.toUri(), values)
                                successCount++
                                Log.d("CallLogGeneratorApp", "Successfully inserted log for $phoneNumber ($successCount/${phoneNumbers.size})")

                                // 更新时间：当前通话结束时间 + 随机间隔（40~120秒）
                                val randomInterval = Random.nextInt(Constants.CALL_INTERVAL_MIN, Constants.CALL_INTERVAL_MAX + 1) * Constants.MILLISECONDS_PER_SECOND
                                currentTime += (duration * 1000L) + randomInterval
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
                        isCheckingUpdate = true
                        showUpdateOptions = false
                        checkForUpdate(
                            context = context,
                            includePreReleases = includePreReleases,
                            onStart = { },
                            onResult = { result ->
                                isCheckingUpdate = false
                                updateResult = result
                                showUpdateDialog = true
                            }
                        )
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
                            android.widget.Toast.makeText(context, "下载已取消", android.widget.Toast.LENGTH_SHORT).show()
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
                                                android.widget.Toast.makeText(context, "下载失败", android.widget.Toast.LENGTH_LONG).show()
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
private fun checkForUpdate(
    context: Context,
    includePreReleases: Boolean,
    onStart: () -> Unit,
    onResult: (UpdateResult) -> Unit
) {
    CoroutineScope(Dispatchers.IO).launch {
        onStart()
        
        val updateChecker = UpdateChecker(context)
        UpdateChecker.includePreReleases.value = includePreReleases
        
        updateChecker.checkForUpdate { result ->
            CoroutineScope(Dispatchers.Main).launch {
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
        CallLogGeneratorApp(contentResolver = LocalContext.current.contentResolver) { true }
    }
}


private fun putSimCardFieldsWithFallback(
    values: ContentValues, 
    simSlot: Int, 
    phoneAccountInfo: PhoneAccountInfo,
    context: Context
) {
    // 字段级降级机制：对每个字段优先尝试标准Android逻辑，然后尝试厂商特定字段
    
    // 1. 先设置标准Android字段
    try {
        values.put(CallLog.Calls.PHONE_ACCOUNT_ID, phoneAccountInfo.accountId)
        values.put(CallLog.Calls.PHONE_ACCOUNT_COMPONENT_NAME, phoneAccountInfo.componentName)
        Log.d(Constants.TAG_SIM_ADAPTER, "使用标准Android字段: PHONE_ACCOUNT_ID=${phoneAccountInfo.accountId}, PHONE_ACCOUNT_COMPONENT_NAME=${phoneAccountInfo.componentName}")
    } catch (e: Exception) {
        Log.e(Constants.TAG_SIM_ADAPTER, "设置标准Android字段失败: ${e.message}")
    }
    
    // 2. 然后尝试厂商特定字段 (vivo/OPPO/小米等使用simid字段)
    try {
        values.put(Constants.SIM_ID_FIELD, simSlot)
        Log.d(Constants.TAG_SIM_ADAPTER, "使用厂商特定字段: ${Constants.SIM_ID_FIELD}=$simSlot")
    } catch (e: Exception) {
        Log.w(Constants.TAG_SIM_ADAPTER, "设置厂商字段${Constants.SIM_ID_FIELD}失败: ${e.message}")
    }
    
    // 3. 处理subscription_id字段 (vivo/小米/荣耀等使用)
    try {
        val subscriptionId = getSubscriptionId(context, simSlot)
        if (subscriptionId >= 0) {
            values.put(Constants.SUBSCRIPTION_ID_FIELD, subscriptionId)
            Log.d(Constants.TAG_SIM_ADAPTER, "使用厂商特定字段: ${Constants.SUBSCRIPTION_ID_FIELD}=$subscriptionId")
        }
    } catch (e: Exception) {
        Log.w(Constants.TAG_SIM_ADAPTER, "设置厂商字段${Constants.SUBSCRIPTION_ID_FIELD}失败: ${e.message}")
    }
    
    // 4. 处理subscription_component_name字段 (荣耀等使用)
    try {
        values.put(Constants.SUBSCRIPTION_COMPONENT_NAME_FIELD, phoneAccountInfo.componentName.toString())
        Log.d(Constants.TAG_SIM_ADAPTER, "使用厂商特定字段: ${Constants.SUBSCRIPTION_COMPONENT_NAME_FIELD}=${phoneAccountInfo.componentName}")
    } catch (e: Exception) {
        Log.w(Constants.TAG_SIM_ADAPTER, "设置厂商字段${Constants.SUBSCRIPTION_COMPONENT_NAME_FIELD}失败: ${e.message}")
    }
}