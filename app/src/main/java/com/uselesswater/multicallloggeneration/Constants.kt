package com.uselesswater.multicallloggeneration

/**
 * 应用常量类，用于集中管理所有硬编码的常量
 */
object Constants {
    
    // 权限相关
    const val PERMISSION_READ_CALL_LOG = android.Manifest.permission.READ_CALL_LOG
    const val PERMISSION_WRITE_CALL_LOG = android.Manifest.permission.WRITE_CALL_LOG
    const val PERMISSION_READ_PHONE_STATE = android.Manifest.permission.READ_PHONE_STATE
    const val PERMISSION_READ_PHONE_NUMBERS = android.Manifest.permission.READ_PHONE_NUMBERS
    
    // 日志标签
    const val TAG_MAIN_ACTIVITY = "MainActivity"
    const val TAG_DEBUG_CALL_LOG = "DebugCallLog"
    const val TAG_SIM_ADAPTER = "SIMAdapter"

    // 内容提供者URI
    const val CALL_LOG_URI = "content://call_log/calls"
    const val CALL_LOG_SORT_ORDER = "date DESC"

    const val SIM_OPTION_SIM1 = "SIM 1"
    const val SIM_OPTION_SIM2 = "SIM 2"

    // 时间范围配置
    const val TIME_RANGE_SHORT_MIN = 15
    const val TIME_RANGE_SHORT_MAX = 60
    const val TIME_RANGE_MEDIUM_MIN = 30
    const val TIME_RANGE_MEDIUM_MAX = 60
    const val TIME_RANGE_LONG_MIN = 60
    const val TIME_RANGE_LONG_MAX = 90
    const val TIME_RANGE_CUSTOM_MIN = 1
    const val TIME_RANGE_CUSTOM_MAX = 18000
    const val TIME_RANGE_SHORT_NAME = "15秒-1分钟"
    const val TIME_RANGE_MEDIUM_NAME = "30秒-1分钟"
    const val TIME_RANGE_LONG_NAME = "1分钟-1分30秒"
    const val TIME_RANGE_CUSTOM_NAME = "自定义时长"
    
    // 通话间隔时间（毫秒）
    const val CALL_INTERVAL_MIN = 40
    const val CALL_INTERVAL_MAX = 120
    const val MILLISECONDS_PER_SECOND = 1000L
    
    // 界面文本
    const val APP_TITLE = "📞 通话记录生成工具"
    const val DEFAULT_MESSAGE = "本工具由UselessWater推出，请勿用于非法用途！\n请输入电话号码，每行一个号码"
    const val PHONE_NUMBER_POOL_TITLE = "电话号码池"
    const val PHONE_NUMBER_PLACEHOLDER = "例如：\n13800138000\n13900139000\n13700137000"
    const val PHONE_NUMBER_LABEL = "每行一个号码"
    const val TIME_SETTINGS_TITLE = "时间设置"
    const val START_TIME_LABEL = "起始时间："
    const val DATE_BUTTON_TEXT = "选择日期"
    const val TIME_BUTTON_TEXT = "选择时间"
    const val CALL_DURATION_LABEL = "通话时长："
    const val SIM_SELECTION_TITLE = "SIM卡选择"
    const val GENERATE_BUTTON_TEXT = "🚀 批量生成通话记录"
    const val CHECK_UPDATE_BUTTON_TEXT = "🔄 检查更新"
    const val AUTHOR_INFO = "@author UselessWater"
    
    // 错误和成功消息
    const val ERROR_NO_PHONE_NUMBERS = "请至少输入一个电话号码"
    const val ERROR_GENERATION_FAILED = "❌ 生成失败: "
    const val ERROR_PERMISSION_DENIED = "❌ 生成失败: 权限不足。"
    const val ERROR_SIM_NOT_FOUND = "无法找到选择的SIM卡 (SIM %d)。请检查SIM卡状态和权限。"
    const val SUCCESS_GENERATION = "✅ 成功生成 %d 条通话记录！"
    const val PERMISSION_GRANTED = "已获得所有必要权限"
    const val PERMISSION_PARTIAL = "部分权限未授予，功能可能受限"
    
    // 日期时间格式
    const val DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm"
    
    // SIM卡调试字段
    val POSSIBLE_SIM_FIELDS = arrayOf(
        "subscription_id", "sub_id", "sim_id", "simid",
        "slot_id", "sim_slot", "phone_id", "account_id", "sim_name",
        "account_name", "subscription_component_name", "phone_account_id",
        "sim_index", "sim_number", "sim_slot_index"
    )

    const val DEFAULT_SIM_SLOT_INDEX_OFFSET = 1

    // 通话类型常量
    const val CALL_TYPE_INCOMING = 1
    const val CALL_TYPE_OUTGOING = 2
    const val CALL_TYPE_MISSED = 3
    const val CALL_TYPE_REJECTED = 5
    // 呼出未接通（支持该功能的所有设备通用），使用6作为自定义类型值
    const val CALL_TYPE_OUTGOING_UNANSWERED = 6
    // vivo设备呼出未接通，使用2作为类型值
    const val CALL_TYPE_VIVO_OUTGOING_UNANSWERED = 2

    const val FIELD_MISSED_REASON = "missed_reason"
    const val FIELD_IS_REJECTED = "is_rejected"

    // 默认值
    const val DEFAULT_RING_DURATION = 15
}