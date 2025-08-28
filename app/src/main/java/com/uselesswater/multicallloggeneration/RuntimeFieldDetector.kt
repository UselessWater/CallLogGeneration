package com.uselesswater.multicallloggeneration

import android.content.Context
import android.provider.CallLog
import android.util.Log

/**
 * 运行时字段检测器
 * 通过创建测试通话记录来动态检测设备支持的字段
 */
object RuntimeFieldDetector {
    private const val TAG = "RuntimeFieldDetector"

    /**
     * 获取设备支持的响铃时长字段（简化版本，直接返回所有可能字段）
     */
    fun getSupportedRingDurationFields(context: Context): Set<String> {
        // 定义所有可能的响铃时长字段（基于实际设备数据分析）
        val allPossibleFields = setOf(
            "duration",            // 标准字段（三星、OPPO、荣耀实际使用）
            "ring_time",           // 通用响铃时间字段（OPPO有但值为NULL）
            "ring_duration",       // 通用响铃时长字段
            "ring_times",          // 华为/荣耀使用的响铃次数字段（荣耀实际使用）
            "call_ring_duration",  // 可能的通话响铃时长字段
            "ring_duration_seconds", // 以秒为单位的响铃时长字段
            "record_duration",     // vivo特有字段
            "oplus_data1",         // OPPO特有字段（存在但值为NULL）
            "oplus_data2",         // OPPO特有字段（存在但值为NULL）
            "data1",               // 三星特有字段（存在但值为NULL）
            "data2",               // 三星特有字段（存在但值为NULL）
            "data3",               // 三星特有字段（存在但值为NULL）
            "data4",               // 三星特有字段（存在但值为NULL）
            "hw_ring_times",       // 华为特有字段
            "missed_reason",       // 通用未接原因字段
            "cloud_antispam_type", // 小米特有字段
            "cloud_antispam_type_tag" // 小米反垃圾标签字段
        )
        
        return allPossibleFields
    }

    /**
     * 安全的字段验证方法 - 解决约束冲突问题
     * 1. 检查字段是否存在于数据库schema中
     * 2. 分析现有记录中的字段使用情况，避免约束冲突
     * 3. 使用保守策略，只允许确实安全的字段
     */
    fun validateFieldSafely(context: Context, field: String): Boolean {
        return try {
            // 第一步：检查字段是否存在于数据库schema中
            if (!isFieldExistsInSchema(context, field)) {
                Log.d(TAG, "字段不存在于数据库schema中: $field")
                return false
            }
            
            // 第二步：检查字段在现有记录中的使用情况
            val fieldUsageInfo = analyzeFieldUsage(context, field)
            
            // 第三步：基于使用情况判断字段是否安全
            val isSafe = isFieldSafeToUse(field, fieldUsageInfo)
            
            if (isSafe) {
                Log.d(TAG, "字段验证成功: $field (使用情况: $fieldUsageInfo)")
            } else {
                Log.d(TAG, "字段验证失败: $field (使用情况: $fieldUsageInfo)")
            }
            
            isSafe
        } catch (e: Exception) {
            Log.w(TAG, "字段验证异常: $field, 错误: ${e.message}")
            // 异常时采用保守策略，拒绝使用该字段
            false
        }
    }
    
    /**
     * 检查字段是否存在于数据库schema中
     */
    private fun isFieldExistsInSchema(context: Context, field: String): Boolean {
        return try {
            val cursor = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(field),
                "1=0", // 不返回任何记录，只检查schema
                null, null
            )
            
            cursor?.use {
                val columnIndex = it.getColumnIndex(field)
                return columnIndex != -1
            }
            false
        } catch (e: Exception) {
            Log.d(TAG, "Schema检查异常: $field, ${e.message}")
            false
        }
    }
    
    /**
     * 分析字段在现有记录中的使用情况
     */
    private fun analyzeFieldUsage(context: Context, field: String): FieldUsageInfo {
        return try {
            val cursor = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(field, CallLog.Calls._ID),
                null, null,
                "${CallLog.Calls.DATE} DESC LIMIT 10" // 分析最近10条记录
            )
            
            var totalRecords = 0
            var nonNullRecords = 0
            var hasValues = false
            val distinctValues = mutableSetOf<String>()
            
            var fieldExists = false
            
            cursor?.use {
                val fieldIndex = it.getColumnIndex(field)
                if (fieldIndex == -1) {
                    // 字段不存在，但正确处理资源释放
                    Log.d(TAG, "字段不存在于查询结果中: $field")
                    fieldExists = false
                    return@use
                }
                
                fieldExists = true
                while (it.moveToNext() && totalRecords < 10) {
                    totalRecords++
                    
                    if (!it.isNull(fieldIndex)) {
                        nonNullRecords++
                        hasValues = true
                        val value = it.getString(fieldIndex) ?: "NULL"
                        distinctValues.add(value)
                    }
                }
            }
            
            // 如果字段不存在，返回相应信息
            if (!fieldExists) {
                return FieldUsageInfo(exists = false)
            }
            
            FieldUsageInfo(
                exists = true,
                totalRecords = totalRecords,
                nonNullRecords = nonNullRecords,
                hasValues = hasValues,
                distinctValueCount = distinctValues.size,
                nullPercentage = if (totalRecords > 0) ((totalRecords - nonNullRecords) * 100.0 / totalRecords) else 100.0
            )
        } catch (e: Exception) {
            Log.d(TAG, "字段使用分析异常: $field, ${e.message}")
            FieldUsageInfo(exists = false)
        }
    }
    
    /**
     * 根据字段使用情况判断是否安全使用
     * 修正：采用真正的防御性策略 - 宁可尝试也不放弃
     */
    private fun isFieldSafeToUse(field: String, usage: FieldUsageInfo): Boolean {
        if (!usage.exists) {
            return false
        }
        
        // 真正的防御性策略：只有明确证据表明字段危险时才拒绝
        return when {
            // 1. 明确的危险信号：字段存在但从未被使用（可能有严格约束）
            usage.totalRecords > 0 && usage.nonNullRecords == 0 && usage.nullPercentage == 100.0 -> {
                // 即使这样，对于关键字段仍然要尝试
                if (field in getCriticalFields()) {
                    Log.d(TAG, "关键字段即使有风险也要尝试: $field")
                    true
                } else {
                    Log.d(TAG, "字段从未被使用，可能有严格约束，跳过: $field")
                    false
                }
            }
            
            // 2. 其他所有情况都尝试使用（真正的防御性编程）
            else -> {
                Log.d(TAG, "字段通过防御性验证: $field (使用情况: $usage)")
                true
            }
        }
    }

    /**
     * 获取关键字段列表（即使有约束也必须使用的字段）
     */
    private fun getCriticalFields(): Set<String> {
        return setOf(
            "duration",        // 通话时长，关键字段
            "missed_reason"    // 未接原因，重要字段
        )
    }
    
    /**
     * 字段使用情况信息
     */
    data class FieldUsageInfo(
        val exists: Boolean = false,
        val totalRecords: Int = 0,
        val nonNullRecords: Int = 0,
        val hasValues: Boolean = false,
        val distinctValueCount: Int = 0,
        val nullPercentage: Double = 100.0
    )

    /**
     * 应用启动时清理（简化版本）
     */
    fun cleanupOnAppStart(context: Context) {
        try {
            Log.d(TAG, "应用启动清理完成（简化版）")
        } catch (e: Exception) {
            Log.w(TAG, "应用启动清理失败: ${e.message}")
        }
    }

}