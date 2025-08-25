package com.uselesswater.multicallloggeneration

import android.content.ContentValues
import android.provider.CallLog
import android.util.Log

/**
 * 通话记录生成工具类，支持多种通话类型
 */
object CallLogGenerator {
    
    private const val TAG = "CallLogGenerator"
    
    /**
     * 创建呼出电话记录
     */
    fun createOutgoingCall(values: ContentValues, duration: Int) {
        values.put(CallLog.Calls.TYPE, Constants.CALL_TYPE_OUTGOING)
        values.put(CallLog.Calls.DURATION, duration)
        Log.d(TAG, "创建呼出电话记录，时长: ${duration}秒")
    }
    
    /**
     * 创建来电记录
     */
    fun createIncomingCall(values: ContentValues, duration: Int, isAnswered: Boolean = true, ringDuration: Int = 0) {
        val callType = if (isAnswered) Constants.CALL_TYPE_INCOMING else Constants.CALL_TYPE_MISSED
        values.put(CallLog.Calls.TYPE, callType)
        
        // 对于未接来电，duration表示响铃时长；对于已接来电，duration表示通话时长
        val finalDuration = if (isAnswered) duration else ringDuration
        values.put(CallLog.Calls.DURATION, finalDuration)
        
        if (!isAnswered && ringDuration > 0) {
            setRingDurationField(values, ringDuration, "未接来电")
        }
        
        Log.d(TAG, "创建${if (isAnswered) "已接" else "未接"}来电记录，${if (isAnswered) "通话" else "响铃"}时长: ${finalDuration}秒")
    }
    
    /**
     * 创建拒接来电记录
     */
    fun createRejectedCall(values: ContentValues, ringDuration: Int = Constants.DEFAULT_RING_DURATION) {
        values.put(CallLog.Calls.TYPE, Constants.CALL_TYPE_REJECTED)
        values.put(CallLog.Calls.DURATION, 0)  // 对于拒接来电，duration应该为0
        
        // 使用字段级降级机制设置拒接原因
        val missedReasonFields = listOf(
            Constants.FIELD_MISSED_REASON,           // 通用字段
            "reject_reason",                         // 其他厂商可能使用的字段
            "call_reject_reason",                    // 通话拒接原因
            Constants.FIELD_IS_REJECTED,             // 拒接标识字段
            "reason"                                 // 通用原因字段（降级）
        )
        setFieldWithFallback(values, missedReasonFields, "rejected", "拒接原因", "", FieldType.MISSED_REASON_FIELD)
        
        setRingDurationField(values, ringDuration, "拒接来电")
        Log.d(TAG, "创建拒接来电记录，响铃时长: ${ringDuration}秒")
    }
    
    
    /**
     * 根据通话类型值创建相应的通话记录
     */
    fun createCallByType(values: ContentValues, callTypeValue: Int, duration: Int, 
                        ringDuration: Int = Constants.DEFAULT_RING_DURATION) {
        when (callTypeValue) {
            Constants.CALL_TYPE_OUTGOING -> createOutgoingCall(values, duration)
            Constants.CALL_TYPE_INCOMING -> createIncomingCall(values, duration, true)
            Constants.CALL_TYPE_MISSED -> createIncomingCall(values, duration, false, ringDuration)
            Constants.CALL_TYPE_REJECTED -> createRejectedCall(values, ringDuration) // 拒接来电
            else -> createOutgoingCall(values, duration) // 默认呼出电话
        }
    }
    
    
    /**
     * 获取通话类型名称
     */
    fun getCallTypeName(callTypeValue: Int): String {
        return when (callTypeValue) {
            Constants.CALL_TYPE_OUTGOING -> "呼出电话"
            Constants.CALL_TYPE_INCOMING -> "来电(已接)"
            Constants.CALL_TYPE_MISSED -> "未接来电"
            Constants.CALL_TYPE_REJECTED -> "拒接来电"
            else -> "未知类型"
        }
    }
    
    /**
     * 智能设置响铃时长字段，支持多厂商字段级降级
     */
    private fun setRingDurationField(values: ContentValues, ringDuration: Int, callType: String) {
        // 获取当前设备支持的响铃时长字段
        val deviceConfig = DeviceFieldConfig.getCurrentDeviceConfig()
        val supportedFields = deviceConfig.supportedRingDurationFields
        
        // 使用通用字段降级方法，只使用设备支持的字段
        setFieldWithFallback(values, supportedFields, ringDuration, "${callType}响铃时长", "秒", FieldType.RING_DURATION_FIELD)
    }
    
    /**
     * 通用字段级降级方法：优先尝试厂商特定字段，失败后使用降级字段
     * @param values ContentValues对象
     * @param fieldPriorityList 字段优先级列表（从高到低）
     * @param value 要设置的值
     * @param fieldDescription 字段描述（用于日志）
     * @param valueUnit 值单位（用于日志）
     * @param fieldType 字段类型（用于设备配置验证）
     */
    private fun setFieldWithFallback(
        values: ContentValues,
        fieldPriorityList: List<String>,
        value: Any,
        fieldDescription: String,
        valueUnit: String = "",
        fieldType: FieldType = FieldType.RING_DURATION_FIELD
    ) {
        var success = false
        var fallbackUsed = false
        var usedField = ""
        
        // 先过滤出设备支持的字段
        val deviceConfig = DeviceFieldConfig.getCurrentDeviceConfig()
        val supportedFields = fieldPriorityList.filter { field ->
            DeviceFieldConfig.isFieldSupported(field, fieldType)
        }
        
        // 如果没有支持的字段，直接返回
        if (supportedFields.isEmpty()) {
            Log.w(TAG, "${fieldDescription}: ${value}${valueUnit} (当前设备不支持任何相关字段)")
            return
        }
        
        // 按优先级尝试各个字段（只使用设备支持的字段）
        for (field in supportedFields) {
            try {
                when (value) {
                    is Int -> values.put(field, value)
                    is Long -> values.put(field, value)
                    is String -> values.put(field, value)
                    else -> values.put(field, value.toString())
                }
                
                // 检查字段是否真的有效（ContentValues.put()不会立即抛出异常）
                // 如果字段无效，会在后续操作中抛出异常
                usedField = field
                success = true
                
                // 检查是否是降级字段（支持字段列表中的最后一个）
                if (field == supportedFields.last()) {
                    fallbackUsed = true
                    Log.w(TAG, "使用降级字段设置${fieldDescription}，厂商特定字段均不支持")
                }
                break
            } catch (e: Exception) {
                // 字段不支持，移除可能已设置的无效字段并继续尝试下一个
                if (values.containsKey(field)) {
                    values.remove(field)
                }
                Log.d(TAG, "字段 ${field} 不支持${fieldDescription}设置: ${e.message}")
            }
        }
        
        if (success) {
            Log.d(TAG, "设置${fieldDescription}(${usedField}): ${value}${valueUnit}")
            if (fallbackUsed) {
                Log.i(TAG, "${fieldDescription}设置完成（使用降级方案）")
            }
        } else {
            Log.w(TAG, "${fieldDescription}: ${value}${valueUnit} (所有厂商字段均不支持，使用系统默认机制)")
        }
    }
    
}