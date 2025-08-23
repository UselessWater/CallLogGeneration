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
        
        // 尝试设置拒接原因
        try {
            values.put(Constants.FIELD_MISSED_REASON, "rejected")
            Log.d(TAG, "设置拒接原因: rejected")
        } catch (e: Exception) {
            Log.d(TAG, "拒接原因字段不支持: ${e.message}")
        }
        
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
     * 智能设置响铃时长字段，基于vivo系统实际支持的字段
     */
    private fun setRingDurationField(values: ContentValues, ringDuration: Int, callType: String) {
        // 基于vivo系统实际支持的字段进行分析
        val possibleFields = listOf(
            "record_duration",      // 录音时长，可能是响铃时长
            "missed_reason",        // 未接原因，可能包含响铃信息
            "duration"              // 通话时长，但已用于通话总时长
        )
        
        var success = false
        
        for (field in possibleFields) {
            try {
                values.put(field, ringDuration)
                Log.d(TAG, "设置${callType}响铃时长(${field}): ${ringDuration}秒")
                success = true
                break
            } catch (e: Exception) {
                // 字段不支持，继续尝试下一个
                Log.d(TAG, "字段 ${field} 不支持响铃时长设置: ${e.message}")
            }
        }
        
        if (!success) {
            Log.d(TAG, "${callType}响铃时长: ${ringDuration}秒 (vivo系统可能使用其他机制存储响铃时间)")
        }
    }
}