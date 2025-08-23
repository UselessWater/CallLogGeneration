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
            Constants.FIELD_MISSED_REASON,           // vivo特有字段
            "reject_reason",                         // 其他厂商可能使用的字段
            "call_reject_reason",                    // 通话拒接原因
            Constants.FIELD_IS_REJECTED,             // 拒接标识字段
            "reason"                                 // 通用原因字段（降级）
        )
        setFieldWithFallback(values, missedReasonFields, "rejected", "拒接原因")
        
        setRingDurationField(values, ringDuration, "拒接来电")
        Log.d(TAG, "创建拒接来电记录，响铃时长: ${ringDuration}秒")
    }
    
    /**
     * 创建VoIP通话记录
     */
    fun createVoipCall(values: ContentValues, duration: Int, callType: Int = Constants.CALL_TYPE_OUTGOING,
                      networkType: Int = Constants.DEFAULT_NETWORK_TYPE) {
        values.put(CallLog.Calls.TYPE, callType)
        values.put(CallLog.Calls.DURATION, duration)
        
        // 使用字段级降级机制设置网络类型
        val networkTypeFields = listOf(
            Constants.FIELD_NETWORK_TYPE,      // 标准网络类型字段
            "network",                         // 通用网络字段
            "connection_type",                 // 连接类型
            "data_network_type"                // 数据网络类型
        )
        setFieldWithFallback(values, networkTypeFields, networkType, "网络类型")
        
        // 设置VoIP标识
        val voipFields = listOf(
            Constants.FIELD_IS_VOIP,           // VoIP标识字段
            "is_voip_call",                    // VoIP通话标识
            "call_technology"                  // 通话技术类型
        )
        setFieldWithFallback(values, voipFields, 1, "VoIP标识")
        
        val typeName = when (callType) {
            Constants.CALL_TYPE_OUTGOING -> "呼出"
            Constants.CALL_TYPE_INCOMING -> "来电"
            else -> "未知"
        }
        Log.d(TAG, "创建VoIP${typeName}通话记录，网络类型: ${getNetworkTypeName(networkType)}")
    }
    
    /**
     * 根据通话类型值创建相应的通话记录
     */
    fun createCallByType(values: ContentValues, callTypeValue: Int, duration: Int, 
                        ringDuration: Int = Constants.DEFAULT_RING_DURATION,
                        networkType: Int = Constants.DEFAULT_NETWORK_TYPE) {
        when (callTypeValue) {
            Constants.CALL_TYPE_OUTGOING -> createOutgoingCall(values, duration)
            Constants.CALL_TYPE_INCOMING -> createIncomingCall(values, duration, true)
            Constants.CALL_TYPE_MISSED -> createIncomingCall(values, duration, false, ringDuration)
            Constants.CALL_TYPE_REJECTED -> createRejectedCall(values, ringDuration) // 拒接来电
            -2 -> createVoipCall(values, duration, Constants.CALL_TYPE_OUTGOING, networkType) // VoIP通话
            else -> createOutgoingCall(values, duration) // 默认呼出电话
        }
    }
    
    /**
     * 获取网络类型名称
     */
    private fun getNetworkTypeName(networkType: Int): String {
        return when (networkType) {
            Constants.NETWORK_TYPE_2G -> "2G"
            Constants.NETWORK_TYPE_3G -> "3G"
            Constants.NETWORK_TYPE_4G -> "4G"
            Constants.NETWORK_TYPE_5G -> "5G"
            Constants.NETWORK_TYPE_WIFI -> "WiFi"
            else -> "未知"
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
            -2 -> "VoIP通话"
            else -> "未知类型"
        }
    }
    
    /**
     * 智能设置响铃时长字段，支持多厂商字段级降级
     */
    private fun setRingDurationField(values: ContentValues, ringDuration: Int, callType: String) {
        // 厂商特定的响铃时长字段列表（按优先级排序）
        val vendorSpecificFields = listOf(
            // vivo特有字段
            Constants.FIELD_RECORD_DURATION,      // 录音时长，可能是响铃时长
            Constants.FIELD_MISSED_REASON,        // 未接原因，可能包含响铃信息
            Constants.SIM_ID_FIELD,               // SIM卡ID，某些厂商可能复用
            
            // 其他厂商可能使用的字段
            Constants.FIELD_RING_DURATION,        // 标准响铃时长字段
            Constants.FIELD_RING_TIME,            // 响铃时间
            Constants.FIELD_CALL_RING_DURATION,   // 通话响铃时长
            Constants.FIELD_RING_DURATION_SECONDS, // 响铃时长（秒）
            
            // 通用字段（降级选项）
            "duration"                           // 通话时长字段（最后尝试）
        )
        
        // 使用通用字段降级方法
        setFieldWithFallback(values, vendorSpecificFields, ringDuration, "${callType}响铃时长", "秒")
    }
    
    /**
     * 通用字段级降级方法：优先尝试厂商特定字段，失败后使用降级字段
     * @param values ContentValues对象
     * @param fieldPriorityList 字段优先级列表（从高到低）
     * @param value 要设置的值
     * @param fieldDescription 字段描述（用于日志）
     * @param valueUnit 值单位（用于日志）
     */
    private fun setFieldWithFallback(
        values: ContentValues,
        fieldPriorityList: List<String>,
        value: Any,
        fieldDescription: String,
        valueUnit: String = ""
    ) {
        var success = false
        var fallbackUsed = false
        var usedField = ""
        
        // 按优先级尝试各个字段
        for (field in fieldPriorityList) {
            try {
                when (value) {
                    is Int -> values.put(field, value)
                    is Long -> values.put(field, value)
                    is String -> values.put(field, value)
                    else -> values.put(field, value.toString())
                }
                usedField = field
                success = true
                
                // 检查是否是降级字段（列表中的最后一个）
                if (field == fieldPriorityList.last()) {
                    fallbackUsed = true
                    Log.w(TAG, "使用降级字段设置${fieldDescription}，厂商特定字段均不支持")
                }
                break
            } catch (e: Exception) {
                // 字段不支持，继续尝试下一个
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