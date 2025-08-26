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
     */
    private fun getVivoConfig(model: String): DeviceFieldConfiguration {
        return DeviceFieldConfiguration(
            supportedSimFields = listOf(
                "simid",
                "subscription_id",
                "subscription_component_name"
            ),
            supportedRingDurationFields = listOf(
                "record_duration",  // vivo特有字段
                "missed_reason",
                "ring_duration",
                "ring_time"
            ),
            description = "vivo设备"
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
                "cloud_antispam_type"  // 小米特有字段
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
                "simid"
            ),
            supportedRingDurationFields = listOf(
                "missed_reason",
                "ring_duration",
                "ring_time",
                "oplus_data1",  // OPPO特有字段
                "oplus_data2"   // OPPO特有字段
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
                "subscription_component_name"
            ),
            supportedRingDurationFields = listOf(
                "missed_reason",
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
                "subscription_component_name"
            ),
            supportedRingDurationFields = listOf(
                "missed_reason",
                "ring_duration",
                "ring_time",
                "hw_account_id"  // 荣耀特有字段
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
     */
    fun isFieldSupported(fieldName: String, fieldType: FieldType): Boolean {
        val config = getCurrentDeviceConfig()
        return when (fieldType) {
            FieldType.SIM_FIELD -> config.supportedSimFields.contains(fieldName)
            FieldType.RING_DURATION_FIELD -> config.supportedRingDurationFields.contains(fieldName)
            FieldType.MISSED_REASON_FIELD -> {
                // 拒接原因字段通常与响铃时长字段类似，但也包括一些通用字段
                config.supportedRingDurationFields.contains(fieldName) || 
                fieldName in listOf("missed_reason", "reject_reason", "call_reject_reason", "is_rejected", "reason")
            }
        }
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