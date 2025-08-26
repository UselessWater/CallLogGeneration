package com.uselesswater.multicallloggeneration

import android.os.Build

/**
 * 设备字段配置管理器
 * 基于厂商和机型预定义字段配置，确保最大兼容性
 */
object DeviceFieldConfig {
    
    // 厂商标识常量
    private const val MANUFACTURER_VIVO = "vivo"
    private const val MANUFACTURER_XIAOMI = "xiaomi"
    private const val MANUFACTURER_OPPO = "oppo"
    private const val MANUFACTURER_HUAWEI = "huawei"
    private const val MANUFACTURER_HONOR = "honor"
    private const val MANUFACTURER_SAMSUNG = "samsung"
    
    /**
     * 获取当前设备的字段配置
     */
    fun getCurrentDeviceConfig(): DeviceFieldConfiguration {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val model = Build.MODEL.lowercase()
        
        return when (manufacturer) {
            MANUFACTURER_VIVO -> getVivoConfig(model)
            MANUFACTURER_XIAOMI -> getXiaomiConfig(model)
            MANUFACTURER_OPPO -> getOppoConfig(model)
            MANUFACTURER_HUAWEI -> getHuaweiConfig(model)
            MANUFACTURER_HONOR -> getHonorConfig(model)
            MANUFACTURER_SAMSUNG -> getSamsungConfig(model)
            else -> getDefaultConfig()
        }
    }
    
    /**
     * vivo设备字段配置
     * vivo的record_duration字段说明：
     * - 该字段专门用于存储响铃时长（特别是未接电话和拒接电话）
     * - 在已接电话中可能为NULL（因为不需要记录响铃时长）
     * - 在未接电话中记录实际响铃时长
     * - 优先级应该最高，因为这是vivo设备的特色功能
     */
    private fun getVivoConfig(model: String): DeviceFieldConfiguration {
        return DeviceFieldConfiguration(
            supportedSimFields = listOf(
                "simid",
                "subscription_id",
                "subscription_component_name"
            ),
            supportedRingDurationFields = listOf(
                "record_duration",    // vivo特有的响铃时长字段，优先级最高！
                "missed_reason",      
                "ring_duration", 
                "ring_time"
            ),
            description = "vivo设备（支持record_duration响铃时长）"
        )
    }
    
    /**
     * 小米设备字段配置
     */
    private fun getXiaomiConfig(model: String): DeviceFieldConfiguration {
        return DeviceFieldConfiguration(
            supportedSimFields = listOf(
                "simid",
                "subscription_id",
                "subscription_component_name"
            ),
            supportedRingDurationFields = listOf(
                "missed_reason",
                "ring_duration",
                "ring_time",
                "cloud_antispam_type",     // 小米特有字段
                "cloud_antispam_type_tag"  // 小米反垃圾标签字段
            ),
            description = "小米设备"
        )
    }
    
    /**
     * OPPO设备字段配置
     */
    private fun getOppoConfig(model: String): DeviceFieldConfiguration {
        return DeviceFieldConfiguration(
            supportedSimFields = listOf(
                "simid",                    // OPPO主要使用simid，但值可能为-1
                "subscription_id"           // 作为降级选项，但可能为NULL
            ),
            supportedRingDurationFields = listOf(
                "missed_reason",
                "ring_time",               // OPPO特有的响铃时间字段
                "ring_duration",
                "oplus_data1",             // OPPO特有字段
                "oplus_data2"              // OPPO特有字段
            ),
            description = "OPPO设备"
        )
    }
    
    /**
     * 华为设备字段配置
     */
    private fun getHuaweiConfig(model: String): DeviceFieldConfiguration {
        return DeviceFieldConfiguration(
            supportedSimFields = listOf(
                "subscription_id",          // 华为设备支持subscription_id
                "subscription_component_name",
                "hw_account_id"             // 华为特有账户字段
            ),
            supportedRingDurationFields = listOf(
                "missed_reason",
                "ring_times",               // 华为使用ring_times而非ring_time
                "ring_duration",
                "ring_time"
            ),
            description = "华为设备"
        )
    }
    
    /**
     * 荣耀设备字段配置
     */
    private fun getHonorConfig(model: String): DeviceFieldConfiguration {
        return DeviceFieldConfiguration(
            supportedSimFields = listOf(
                "subscription_id",          // 荣耀设备支持subscription_id
                "subscription_component_name",
                "hw_account_id"             // 荣耀特有账户字段
            ),
            supportedRingDurationFields = listOf(
                "missed_reason",
                "ring_times",               // 荣耀使用ring_times字段
                "ring_duration",
                "ring_time"
            ),
            description = "荣耀设备"
        )
    }
    
    /**
     * 三星设备字段配置
     */
    private fun getSamsungConfig(model: String): DeviceFieldConfiguration {
        return DeviceFieldConfiguration(
            supportedSimFields = listOf(),  // 三星基本不使用特殊SIM字段
            supportedRingDurationFields = listOf(
                "missed_reason",
                "ring_duration",
                "ring_time",
                "data1",  // 三星特有字段
                "data2"   // 三星特有字段
            ),
            description = "三星设备"
        )
    }
    
    /**
     * 默认配置（Google原生Android等）
     */
    private fun getDefaultConfig(): DeviceFieldConfiguration {
        return DeviceFieldConfiguration(
            supportedSimFields = listOf(
                "subscription_id",
                "subscription_component_name"
            ),
            supportedRingDurationFields = listOf(
                "missed_reason",
                "ring_duration",
                "ring_time"
            ),
            description = "标准Android设备"
        )
    }
    
    /**
     * 验证字段是否在当前设备配置中支持
     * 优化版本：结合配置和运行时检测
     */
    fun isFieldSupported(fieldName: String, fieldType: FieldType): Boolean {
        val config = getCurrentDeviceConfig()
        val isInConfig = when (fieldType) {
            FieldType.SIM_FIELD -> config.supportedSimFields.contains(fieldName)
            FieldType.RING_DURATION_FIELD -> config.supportedRingDurationFields.contains(fieldName)
            FieldType.MISSED_REASON_FIELD -> {
                // 拒接原因字段通常与响铃时长字段类似，但也包括一些通用字段
                config.supportedRingDurationFields.contains(fieldName) || 
                fieldName in listOf("missed_reason", "reject_reason", "call_reject_reason", "is_rejected", "reason")
            }
        }
        
        // 如果配置中不支持，但字段名看起来合理，仍然尝试（用于未知设备的兼容性）
        if (!isInConfig && isReasonableFieldName(fieldName)) {
            return true  // 允许尝试，让实际插入时决定是否有效
        }
        
        return isInConfig
    }
    
    /**
     * 判断字段名是否看起来合理（用于未知设备的字段支持）
     */
    private fun isReasonableFieldName(fieldName: String): Boolean {
        val commonFieldPatterns = listOf(
            "sim", "subscription", "account", "ring", "duration", "missed", "reason", "reject",
            "data1", "data2", "data3", "data4", "data5", "hw_", "oplus_", "cloud_"
        )
        
        return fieldName.isNotBlank() && 
               fieldName.matches(Regex("^[a-zA-Z_][a-zA-Z0-9_]*$")) &&
               commonFieldPatterns.any { pattern -> fieldName.lowercase().contains(pattern) }
    }
    
    /**
     * 获取运行时字段支持情况（通过查询现有通话记录）
     * 这个方法可以在应用启动时调用以更新设备配置
     */
    fun detectRuntimeFieldSupport(context: android.content.Context): Set<String> {
        val detectedFields = mutableSetOf<String>()
        
        try {
            val contentResolver = context.contentResolver
            val cursor = contentResolver.query(
                android.provider.CallLog.Calls.CONTENT_URI,
                null,
                null,
                null,
                "${android.provider.CallLog.Calls.DATE} DESC LIMIT 1"
            )
            
            cursor?.use {
                val columnNames = it.columnNames
                detectedFields.addAll(columnNames)
            }
        } catch (e: Exception) {
            android.util.Log.w("DeviceFieldConfig", "无法检测运行时字段支持: ${e.message}")
        }
        
        return detectedFields
    }
}

/**
 * 设备字段配置数据类
 */
data class DeviceFieldConfiguration(
    val supportedSimFields: List<String>,
    val supportedRingDurationFields: List<String>,
    val description: String
)

/**
 * 字段类型枚举
 */
enum class FieldType {
    SIM_FIELD,
    RING_DURATION_FIELD,
    MISSED_REASON_FIELD
}