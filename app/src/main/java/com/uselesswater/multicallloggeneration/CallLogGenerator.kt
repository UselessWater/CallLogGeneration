package com.uselesswater.multicallloggeneration

import android.content.ContentValues
import android.content.Context
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
    fun createIncomingCall(values: ContentValues, duration: Int, isAnswered: Boolean = true, ringDuration: Int = 0, context: Context? = null) {
        val callType = if (isAnswered) Constants.CALL_TYPE_INCOMING else Constants.CALL_TYPE_MISSED
        values.put(CallLog.Calls.TYPE, callType)
        
        if (isAnswered) {
            // 已接来电：duration表示通话时长
            values.put(CallLog.Calls.DURATION, duration)
            Log.d(TAG, "创建已接来电记录，通话时长: ${duration}秒")
        } else {
            // 未接来电：设置duration为响铃时长（防御性编程）
            values.put(CallLog.Calls.DURATION, ringDuration)
            // 对于三星设备，设置时间戳字段以支持响铃时长计算
            setSamsungTimestampsForMissedCall(values, ringDuration)
            // 对于华为设备，设置时间戳字段以支持响铃时长计算
            setHuaweiTimestampsForMissedCall(values, ringDuration)
            // 对于荣耀设备，设置时间戳字段以支持响铃时长计算
            setHonorTimestampsForMissedCall(values, ringDuration)
            // 对于OPPO设备，设置时间戳字段以支持响铃时长计算
            setOppoTimestampsForMissedCall(values, ringDuration)
            // 同时使用防御性字段设置机制
            setMultipleRingDurationFields(values, ringDuration, "未接来电", context)
            Log.d(TAG, "创建未接来电记录，响铃时长: ${ringDuration}秒（防御性设置）")
        }
    }
    
    /**
     * 创建呼出未接通记录（根据设备类型适配）
     * vivo设备：type=2, duration=0
     * 其他设备：type=1, duration=0（通过duration=0来区分呼出未接和呼出已接）
     */
    fun createOutgoingUnanswered(values: ContentValues, ringDuration: Int = Constants.DEFAULT_RING_DURATION) {
        if (isVivoDevice()) {
            // vivo设备使用type=2表示呼出未接通
            values.put(CallLog.Calls.TYPE, 2)
            Log.d(TAG, "vivo设备：创建呼出未接通记录，type=2, duration=0")
        } else {
            // 其他设备使用type=1，通过duration=0来区分呼出未接和呼出已接
            values.put(CallLog.Calls.TYPE, Constants.CALL_TYPE_OUTGOING)
            Log.d(TAG, "非vivo设备：创建呼出未接通记录，type=1, duration=0（通过duration=0区分）")
        }
        values.put(CallLog.Calls.DURATION, 0) // 呼出未接通duration为0
        Log.d(TAG, "创建呼出未接通记录，响铃时长: ${ringDuration}秒")
    }
    
    /**
     * 创建拒接来电记录
     */
    fun createRejectedCall(values: ContentValues, ringDuration: Int = Constants.DEFAULT_RING_DURATION, context: Context? = null) {
        values.put(CallLog.Calls.TYPE, Constants.CALL_TYPE_REJECTED)
        // 根据用户要求：拒接电话不需要有响铃时长，duration设置为0
        values.put(CallLog.Calls.DURATION, 0)
        
        // 对于三星设备，设置时间戳字段以支持响铃时长计算
        setSamsungTimestampsForRejectedCall(values, ringDuration)
        // 对于华为设备，设置时间戳字段以支持响铃时长计算
        setHuaweiTimestampsForRejectedCall(values, ringDuration)
        // 对于荣耀设备，设置时间戳字段以支持响铃时长计算
        setHonorTimestampsForRejectedCall(values, ringDuration)
        // 对于OPPO设备，设置时间戳字段以支持响铃时长计算
        setOppoTimestampsForRejectedCall(values, ringDuration)
        
        // 使用字段级降级机制设置拒接原因
        val missedReasonFields = listOf(
            Constants.FIELD_MISSED_REASON,           // 通用字段
            "reject_reason",                         // 其他厂商可能使用的字段
            "call_reject_reason",                    // 通话拒接原因
            Constants.FIELD_IS_REJECTED,             // 拒接标识字段
            "reason"                                 // 通用原因字段（降级）
        )
        setFieldWithFallback(values, missedReasonFields, "rejected", "拒接原因", "", FieldType.MISSED_REASON_FIELD)
        
        // 防御性编程：设置其他响铃时长字段，但不覆盖duration字段
        setMultipleRingDurationFieldsExceptDuration(values, ringDuration, "拒接来电", context)
        Log.d(TAG, "创建拒接来电记录，duration=0（用户要求），其他响铃字段=${ringDuration}秒")
    }
    
    
    /**
     * 根据通话类型枚举创建相应的通话记录
     * 防御性编程：类型安全的入口点
     */
    fun createCallByType(values: ContentValues, callType: CallType, duration: Int, 
                        ringDuration: Int = Constants.DEFAULT_RING_DURATION, context: Context? = null) {
        // 防御性编程：验证输入参数
        if (duration < 0) {
            Log.w(TAG, "通话时长不能为负数: $duration，将使用0")
        }
        
        if (ringDuration < 0) {
            Log.w(TAG, "响铃时长不能为负数: $ringDuration，将使用默认值")
        }
        
        // 验证时长是否符合类型要求
        val finalDuration = if (callType.validateDuration(duration)) {
            duration
        } else {
            Log.w(TAG, "通话类型 ${callType.displayName} 的时长 $duration 不符合要求，将使用默认值 ${callType.defaultDuration}")
            callType.defaultDuration
        }
        
        val finalRingDuration = if (ringDuration >= 0) ringDuration else Constants.DEFAULT_RING_DURATION
        
        when (callType) {
            CallType.OUTGOING -> createOutgoingCall(values, finalDuration)
            CallType.INCOMING -> createIncomingCall(values, finalDuration, true, finalRingDuration, context)
            CallType.MISSED -> createIncomingCall(values, finalDuration, false, finalRingDuration, context)
            CallType.REJECTED -> createRejectedCall(values, finalRingDuration, context)
            CallType.OUTGOING_UNANSWERED -> createOutgoingUnanswered(values, finalRingDuration)
            CallType.VIVO_OUTGOING_UNANSWERED -> {
                // vivo设备专用的呼出未接通类型，直接设置type=2
                values.put(CallLog.Calls.TYPE, Constants.CALL_TYPE_VIVO_OUTGOING_UNANSWERED)
                values.put(CallLog.Calls.DURATION, 0)
                Log.d(TAG, "创建vivo设备呼出未接通记录，type=2, duration=0")
            }
        }
    }
    
    /**
     * 根据通话类型值创建相应的通话记录（向后兼容）
     * 防御性编程：使用类型安全的枚举，统一的入口点
     */
    fun createCallByType(values: ContentValues, callTypeValue: Int, duration: Int, 
                        ringDuration: Int = Constants.DEFAULT_RING_DURATION, context: Context? = null) {
        // 使用类型安全的枚举版本
        val callTypeEnum = CallType.fromValue(callTypeValue)
        if (callTypeEnum == null) {
            Log.w(TAG, "未知通话类型: $callTypeValue，使用拒接来电作为默认")
            createRejectedCall(values, ringDuration, context)
            return
        }
        
        // 委托给类型安全的版本
        createCallByType(values, callTypeEnum, duration, ringDuration, context)
    }
    
    
    /**
     * 获取通话类型名称
     * 防御性编程：使用类型安全的枚举
     */
    fun getCallTypeName(callTypeValue: Int): String {
        val callTypeEnum = CallType.fromValue(callTypeValue)
        return callTypeEnum?.displayName ?: "未知类型($callTypeValue)"
    }
    

    /**
     * 设置多个可能的响铃时长字段，提高不同设备的兼容性
     * 防御性编程：为所有可能的字段都设置相同值，使用数据库验证机制确保安全
     */
    private fun setMultipleRingDurationFields(values: ContentValues, ringDuration: Int, callType: String, context: Context?) {
        // 获取所有可能的响铃时长字段（不依赖设备配置，使用防御性策略）
        val allPossibleFields = if (context != null) {
            RuntimeFieldDetector.getSupportedRingDurationFields(context)
        } else {
            // 没有context时使用默认字段列表
            setOf(
                "duration", "ring_time", "ring_duration", "ring_times", "record_duration",
                "missed_reason", "oplus_data1", "oplus_data2", "data1", "data2", "data3", "data4",
                "hw_ring_times", "cloud_antispam_type", "cloud_antispam_type_tag"
            )
        }
        
        var successCount = 0
        val validatedFields = mutableSetOf<String>()
        val failedFields = mutableSetOf<String>()
        
        Log.d(TAG, "开始为${callType}设置所有可能的响铃时长字段，共${allPossibleFields.size}个候选字段")
        
        // 防御性编程：尝试为每个可能的字段都设置值（不管是否已设置）
        for (field in allPossibleFields) {
            try {
                // 验证字段名称格式
                if (!isValidFieldName(field)) {
                    Log.d(TAG, "跳过无效字段名: $field")
                    failedFields.add(field)
                    continue
                }
                
                // 防御性编程：为所有字段设置统一的响铃时长值
                // 不做特殊处理，让数据库和系统自己决定使用哪个字段
                val fieldValue = when (field) {
                    "missed_reason" -> {
                        // missed_reason字段使用特殊值表示响铃相关
                        if (callType.contains("拒接")) 5 else 0  // 5表示用户拒接，0表示正常未接
                    }
                    "ring_times" -> {
                        // ring_times字段表示响铃次数，使用1作为默认值
                        1
                    }
                    else -> {
                        // 所有其他字段（包括duration）都设置响铃时长
                        // 让系统和数据库决定实际使用哪个字段
                        ringDuration
                    }
                }
                
                // 使用数据库验证机制安全地设置字段
                val success = if (context != null) {
                    setFieldWithValidation(values, field, fieldValue, context)
                } else {
                    setFieldSafely(values, field, fieldValue)
                }
                
                if (success) {
                    validatedFields.add(field)
                    successCount++
                    Log.d(TAG, "成功设置${callType}的响铃时长字段: $field = $fieldValue")
                } else {
                    failedFields.add(field)
                    Log.d(TAG, "设置${callType}的响铃时长字段失败: $field")
                }
            } catch (e: Exception) {
                failedFields.add(field)
                Log.w(TAG, "设置${callType}的响铃时长字段异常: $field, 错误: ${e.message}")
            }
        }
        
        Log.d(TAG, "为${callType}设置响铃时长字段完成，成功设置 $successCount/${allPossibleFields.size} 个字段")
        Log.d(TAG, "成功字段: $validatedFields")
        Log.d(TAG, "失败字段: $failedFields")
        
        // 如果没有任何字段设置成功，记录警告
        if (successCount == 0) {
            Log.w(TAG, "警告：${callType}的响铃时长字段设置全部失败，可能影响兼容性")
        } else {
            Log.i(TAG, "防御性设置完成：${callType}成功设置了${successCount}个响铃时长字段")
        }
    }
    
    /**
     * 设置多个可能的响铃时长字段，但不覆盖duration字段
     * 专门用于拒接来电，确保duration保持为0
     */
    private fun setMultipleRingDurationFieldsExceptDuration(values: ContentValues, ringDuration: Int, callType: String, context: Context?) {
        // 获取所有可能的响铃时长字段（不依赖设备配置，使用防御性策略）
        val allPossibleFields = if (context != null) {
            RuntimeFieldDetector.getSupportedRingDurationFields(context)
        } else {
            // 没有context时使用默认字段列表
            setOf(
                "ring_time", "ring_duration", "ring_times", "record_duration",
                "missed_reason", "oplus_data1", "oplus_data2", "data1", "data2", "data3", "data4",
                "hw_ring_times", "cloud_antispam_type", "cloud_antispam_type_tag"
            )
        }.filter { it != "duration" } // 排除duration字段
        
        var successCount = 0
        val validatedFields = mutableSetOf<String>()
        val failedFields = mutableSetOf<String>()
        
        Log.d(TAG, "开始为${callType}设置响铃时长字段（排除duration），共${allPossibleFields.size}个候选字段")
        
        // 防御性编程：尝试为每个可能的字段都设置值（不包括duration）
        for (field in allPossibleFields) {
            try {
                // 验证字段名称格式
                if (!isValidFieldName(field)) {
                    Log.d(TAG, "跳过无效字段名: $field")
                    failedFields.add(field)
                    continue
                }
                
                // 防御性编程：为所有字段设置统一的响铃时长值
                val fieldValue = when (field) {
                    "missed_reason" -> {
                        // missed_reason字段使用特殊值表示响铃相关
                        if (callType.contains("拒接")) 5 else 0  // 5表示用户拒接，0表示正常未接
                    }
                    "ring_times" -> {
                        // ring_times字段表示响铃次数，使用1作为默认值
                        1
                    }
                    else -> {
                        // 所有其他字段都设置响铃时长
                        ringDuration
                    }
                }
                
                // 使用数据库验证机制安全地设置字段
                val success = if (context != null) {
                    setFieldWithValidation(values, field, fieldValue, context)
                } else {
                    setFieldSafely(values, field, fieldValue)
                }
                
                if (success) {
                    validatedFields.add(field)
                    successCount++
                    Log.d(TAG, "成功设置${callType}的响铃时长字段: $field = $fieldValue")
                } else {
                    failedFields.add(field)
                    Log.d(TAG, "设置${callType}的响铃时长字段失败: $field")
                }
            } catch (e: Exception) {
                failedFields.add(field)
                Log.w(TAG, "设置${callType}的响铃时长字段异常: $field, 错误: ${e.message}")
            }
        }
        
        Log.d(TAG, "为${callType}设置响铃时长字段完成（排除duration），成功设置 $successCount/${allPossibleFields.size} 个字段")
        Log.d(TAG, "成功字段: $validatedFields")
        Log.d(TAG, "失败字段: $failedFields")
        
        // 如果没有任何字段设置成功，记录警告
        if (successCount == 0) {
            Log.w(TAG, "警告：${callType}的响铃时长字段设置全部失败，可能影响兼容性")
        } else {
            Log.i(TAG, "防御性设置完成：${callType}成功设置了${successCount}个响铃时长字段（保持duration=0）")
        }
    }

    /**
     * 通用字段级降级方法：优先尝试厂商特定字段，失败后使用降级字段
     * 增强了空值检查和字段验证机制
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
        
        // 增强的值验证：检查空值和无效值
        if (!isValidValue(value)) {
            Log.w(TAG, "${fieldDescription}: 值无效或为空 (${value})")
            return
        }
        
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
        
        // 注意：此方法保持原有优先级逻辑，主要用于拒接原因等单一字段设置
        // 响铃时长字段应使用setMultipleRingDurationFields进行防御性设置
        for (field in supportedFields) {
            try {
                // 增强的字段设置逻辑：先验证字段名称的有效性
                if (!isValidFieldName(field)) {
                    Log.d(TAG, "跳过无效字段名: $field")
                    continue
                }
                
                // 安全的字段设置
                val fieldSetSuccess = try {
                    setFieldSafely(values, field, value)
                } catch (e: SecurityException) {
                    Log.w(TAG, "字段 ${field} 权限不足: ${e.message}")
                    false
                } catch (e: IllegalArgumentException) {
                    Log.w(TAG, "字段 ${field} 参数无效: ${e.message}")
                    false
                } catch (e: Exception) {
                    Log.w(TAG, "字段 ${field} 设置异常: ${e.message}")
                    false
                }
                
                if (fieldSetSuccess) {
                    usedField = field
                    success = true
                    
                    // 检查是否是降级字段（支持字段列表中的最后一个）
                    if (field == supportedFields.last()) {
                        fallbackUsed = true
                        Log.w(TAG, "使用降级字段设置${fieldDescription}，厂商特定字段均不支持")
                    }
                    break  // 保持原有优先级逻辑，用于非响铃时长字段
                } else {
                    Log.d(TAG, "字段 ${field} 设置失败，尝试下一个字段")
                }
            } catch (e: Exception) {
                // 字段不支持，移除可能已设置的无效字段并继续尝试下一个
                safeRemoveField(values, field)
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
    
    /**
     * 验证值是否有效（修正版：允许合法的NULL值和0值）
     */
    private fun isValidValue(value: Any?): Boolean {
        return when (value) {
            null -> true  // NULL值是合法的，表示字段存在但当前记录无值
            is String -> value.lowercase() != "null"  // 允许空字符串，但拒绝字符串"null"
            is Int -> value >= 0  // 允许0值（如duration=0的未接电话）
            is Long -> value >= 0
            else -> true
        }
    }
    
    /**
     * 验证字段名称是否有效
     */
    private fun isValidFieldName(fieldName: String?): Boolean {
        return !fieldName.isNullOrBlank() && 
               !fieldName.contains(" ") && 
               fieldName.matches(Regex("^[a-zA-Z_][a-zA-Z0-9_]*$"))
    }
    
    /**
     * 安全地设置字段值（修正版：正确处理NULL值和空字符串）
     */
    private fun setFieldSafely(values: ContentValues, field: String, value: Any?): Boolean {
        return try {
            when (value) {
                null -> {
                    // NULL值是合法的，应该设置
                    values.putNull(field)
                    true
                }
                is Int -> {
                    values.put(field, value)
                    true
                }
                is Long -> {
                    values.put(field, value)
                    true
                }
                is String -> {
                    // 只拒绝字符串"null"，允许空字符串
                    if (value.lowercase() != "null") {
                        values.put(field, value)
                        true
                    } else {
                        Log.d(TAG, "跳过字符串'null'值: field: $field")
                        false
                    }
                }
                is Boolean -> {
                    // Boolean类型转换为整数
                    values.put(field, if (value) 1 else 0)
                    true
                }
                is Float -> {
                    // Float类型转换为Long以避免精度问题
                    values.put(field, value.toLong())
                    true
                }
                is Double -> {
                    // Double类型转换为Long以避免精度问题
                    values.put(field, value.toLong())
                    true
                }
                is ByteArray -> {
                    // 字节数组直接设置
                    values.put(field, value)
                    true
                }
                else -> {
                    // 其他类型转换为字符串
                    val stringValue = value.toString()
                    if (stringValue.lowercase() != "null") {
                        values.put(field, stringValue)
                        true
                    } else {
                        Log.d(TAG, "跳过转换后的'null'值: field: $field")
                        false
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "字段设置异常 $field: ${e.message}")
            false
        }
    }
    
    /**
     * 带数据库验证的字段设置方法
     * 真正的防御性编程：先尝试设置，遇到问题再处理
     * 避免过度预测和提前拒绝
     */
    private fun setFieldWithValidation(values: ContentValues, field: String, value: Any?, context: Context): Boolean {
        return try {
            // 第一步：快速检查字段是否存在（只检查存在性，不做复杂判断）
            val fieldExists = RuntimeFieldDetector.validateFieldSafely(context, field)
            
            if (!fieldExists) {
                Log.d(TAG, "字段不存在，跳过: $field")
                return false
            }
            
            // 第二步：值的基本合法性检查（防止明显错误）
            val validatedValue = validateFieldValue(field, value)
            
            // 第三步：直接尝试设置字段（真正的防御性编程）
            val success = setFieldSafely(values, field, validatedValue)
            
            if (success) {
                Log.d(TAG, "字段设置成功: $field = $validatedValue")
            } else {
                Log.d(TAG, "字段设置失败，但这是正常的容错行为: $field")
            }
            
            return success
        } catch (e: Exception) {
            // 异常也是正常的防御性行为，记录但不影响其他字段
            Log.d(TAG, "字段设置异常（容错处理）: $field, ${e.message}")
            false
        }
    }
    
    /**
     * 验证字段值是否合法
     * 防止设置可能导致约束冲突的值
     */
    private fun validateFieldValue(field: String, value: Any?): Any? {
        return when (field) {
            "duration" -> {
                // duration字段必须是非负整数
                when (value) {
                    is Int -> if (value >= 0) value else 0
                    is Long -> if (value >= 0) value.toInt() else 0
                    is String -> {
                        try {
                            val intValue = value.toInt()
                            if (intValue >= 0) intValue else 0
                        } catch (e: NumberFormatException) {
                            0
                        }
                    }
                    else -> 0
                }
            }
            "missed_reason" -> {
                // missed_reason字段通常是小整数
                when (value) {
                    is Int -> if (value in 0..10) value else 0
                    is String -> {
                        if (value.equals("rejected", true)) 5 else 0
                    }
                    else -> 0
                }
            }
            "ring_times" -> {
                // ring_times字段应该是正整数
                when (value) {
                    is Int -> if (value > 0) value else 1
                    is Long -> if (value > 0) value.toInt() else 1
                    else -> 1
                }
            }
            CallLog.Calls.TYPE -> {
                // TYPE字段必须是有效的通话类型
                when (value) {
                    is Int -> if (value in 1..6) value else Constants.CALL_TYPE_MISSED
                    else -> Constants.CALL_TYPE_MISSED
                }
            }
            else -> {
                // 其他字段的通用验证
                when (value) {
                    is String -> {
                        // 拒绝过长的字符串（可能导致数据库错误）
                        if (value.length > 255) value.take(255) else value
                    }
                    is Int -> {
                        // 确保整数在合理范围内
                        if (value < -1000000 || value > 1000000) 0 else value
                    }
                    is Long -> {
                        // 确保长整数在合理范围内，必要时转换为Int
                        when {
                            value < -1000000L -> 0
                            value > 1000000L -> 0
                            else -> value.toInt() // 转换为Int以避免类型不匹配
                        }
                    }
                    is Float -> {
                        // 浮点数转换为整数
                        val intValue = value.toInt()
                        if (intValue < -1000000 || intValue > 1000000) 0 else intValue
                    }
                    is Double -> {
                        // 双精度浮点数转换为整数
                        val intValue = value.toInt()
                        if (intValue < -1000000 || intValue > 1000000) 0 else intValue
                    }
                    null -> null  // 保持null值
                    else -> {
                        // 其他类型尝试转换为字符串，并限制长度
                        val stringValue = value.toString()
                        if (stringValue.length > 255) stringValue.take(255) else stringValue
                    }
                }
            }
        }
    }
    
    /**
     * 安全地移除字段
     */
    private fun safeRemoveField(values: ContentValues, field: String) {
        try {
            if (values.containsKey(field)) {
                values.remove(field)
                Log.d(TAG, "已移除无效字段: $field")
            }
        } catch (e: Exception) {
            Log.d(TAG, "移除字段失败 $field: ${e.message}")
        }
    }
    
    /**
     * 为三星设备的未接电话设置时间戳字段
     * DATE字段设置为当前时间（通话开始时间）
     * LAST_MODIFIED字段设置为通话结束时间（当前时间 + 响铃时长毫秒）
     */
    private fun setSamsungTimestampsForMissedCall(values: ContentValues, ringDuration: Int) {
        // 检查是否为三星设备
        if (!isSamsungDevice()) {
            return
        }
        
        try {
            val currentTime = System.currentTimeMillis()
            val ringDurationMs = ringDuration * 1000L // 转换为毫秒
            
            // DATE字段：当前时间（通话开始时间）
            if (!values.containsKey(CallLog.Calls.DATE)) {
                values.put(CallLog.Calls.DATE, currentTime)
                Log.d(TAG, "三星设备未接电话：设置DATE=${currentTime}（当前时间）")
            } else {
                // 如果DATE已经设置，使用已设置的值作为基准
                val existingDate = values.getAsLong(CallLog.Calls.DATE) ?: currentTime
                Log.d(TAG, "三星设备未接电话：使用已设置的DATE=${existingDate}")
            }
            
            // LAST_MODIFIED字段：通话结束时间（DATE + 响铃时长毫秒）
            val dateValue = values.getAsLong(CallLog.Calls.DATE) ?: currentTime
            val lastModifiedTime = dateValue + ringDurationMs
            values.put(CallLog.Calls.LAST_MODIFIED, lastModifiedTime)
            
            Log.d(TAG, "三星设备未接电话：设置LAST_MODIFIED=${lastModifiedTime}（DATE + ${ringDuration}秒）")
            Log.d(TAG, "三星设备时间戳设置完成：响铃时长可通过LAST_MODIFIED - DATE计算")
        } catch (e: Exception) {
            Log.w(TAG, "三星设备时间戳设置失败: ${e.message}")
        }
    }
    
    /**
     * 为三星设备的拒接电话设置时间戳字段
     * DATE字段设置为当前时间（通话开始时间）
     * LAST_MODIFIED字段设置为通话结束时间（当前时间 + 响铃时长毫秒）
     */
    private fun setSamsungTimestampsForRejectedCall(values: ContentValues, ringDuration: Int) {
        // 检查是否为三星设备
        if (!isSamsungDevice()) {
            return
        }
        
        try {
            val currentTime = System.currentTimeMillis()
            val ringDurationMs = ringDuration * 1000L // 转换为毫秒
            
            // DATE字段：当前时间（通话开始时间）
            if (!values.containsKey(CallLog.Calls.DATE)) {
                values.put(CallLog.Calls.DATE, currentTime)
                Log.d(TAG, "三星设备拒接电话：设置DATE=${currentTime}（当前时间）")
            } else {
                // 如果DATE已经设置，使用已设置的值作为基准
                val existingDate = values.getAsLong(CallLog.Calls.DATE) ?: currentTime
                Log.d(TAG, "三星设备拒接电话：使用已设置的DATE=${existingDate}")
            }
            
            // LAST_MODIFIED字段：通话结束时间（DATE + 响铃时长毫秒）
            val dateValue = values.getAsLong(CallLog.Calls.DATE) ?: currentTime
            val lastModifiedTime = dateValue + ringDurationMs
            values.put(CallLog.Calls.LAST_MODIFIED, lastModifiedTime)
            
            Log.d(TAG, "三星设备拒接电话：设置LAST_MODIFIED=${lastModifiedTime}（DATE + ${ringDuration}秒）")
            Log.d(TAG, "三星设备时间戳设置完成：响铃时长可通过LAST_MODIFIED - DATE计算")
        } catch (e: Exception) {
            Log.w(TAG, "三星设备时间戳设置失败: ${e.message}")
        }
    }
    
    /**
     * 检查当前设备是否为三星设备
     */
    private fun isSamsungDevice(): Boolean {
        return android.os.Build.MANUFACTURER.lowercase() == "samsung"
    }
    
    /**
     * 为华为设备的未接电话设置时间戳字段
     * DATE字段设置为当前时间（通话开始时间）
     * LAST_MODIFIED字段设置为通话结束时间（当前时间 + 响铃时长毫秒）
     */
    private fun setHuaweiTimestampsForMissedCall(values: ContentValues, ringDuration: Int) {
        // 检查是否为华为设备
        if (!isHuaweiDevice()) {
            return
        }
        
        try {
            val currentTime = System.currentTimeMillis()
            val ringDurationMs = ringDuration * 1000L // 转换为毫秒
            
            // DATE字段：当前时间（通话开始时间）
            if (!values.containsKey(CallLog.Calls.DATE)) {
                values.put(CallLog.Calls.DATE, currentTime)
                Log.d(TAG, "华为设备未接电话：设置DATE=${currentTime}（当前时间）")
            } else {
                // 如果DATE已经设置，使用已设置的值作为基准
                val existingDate = values.getAsLong(CallLog.Calls.DATE) ?: currentTime
                Log.d(TAG, "华为设备未接电话：使用已设置的DATE=${existingDate}")
            }
            
            // LAST_MODIFIED字段：通话结束时间（DATE + 响铃时长毫秒）
            val dateValue = values.getAsLong(CallLog.Calls.DATE) ?: currentTime
            val lastModifiedTime = dateValue + ringDurationMs
            values.put(CallLog.Calls.LAST_MODIFIED, lastModifiedTime)
            
            Log.d(TAG, "华为设备未接电话：设置LAST_MODIFIED=${lastModifiedTime}（DATE + ${ringDuration}秒）")
            Log.d(TAG, "华为设备时间戳设置完成：响铃时长可通过LAST_MODIFIED - DATE计算")
        } catch (e: Exception) {
            Log.w(TAG, "华为设备时间戳设置失败: ${e.message}")
        }
    }
    
    /**
     * 为华为设备的拒接电话设置时间戳字段
     * DATE字段设置为当前时间（通话开始时间）
     * LAST_MODIFIED字段设置为通话结束时间（当前时间 + 响铃时长毫秒）
     */
    private fun setHuaweiTimestampsForRejectedCall(values: ContentValues, ringDuration: Int) {
        // 检查是否为华为设备
        if (!isHuaweiDevice()) {
            return
        }
        
        try {
            val currentTime = System.currentTimeMillis()
            val ringDurationMs = ringDuration * 1000L // 转换为毫秒
            
            // DATE字段：当前时间（通话开始时间）
            if (!values.containsKey(CallLog.Calls.DATE)) {
                values.put(CallLog.Calls.DATE, currentTime)
                Log.d(TAG, "华为设备拒接电话：设置DATE=${currentTime}（当前时间）")
            } else {
                // 如果DATE已经设置，使用已设置的值作为基准
                val existingDate = values.getAsLong(CallLog.Calls.DATE) ?: currentTime
                Log.d(TAG, "华为设备拒接电话：使用已设置的DATE=${existingDate}")
            }
            
            // LAST_MODIFIED字段：通话结束时间（DATE + 响铃时长毫秒）
            val dateValue = values.getAsLong(CallLog.Calls.DATE) ?: currentTime
            val lastModifiedTime = dateValue + ringDurationMs
            values.put(CallLog.Calls.LAST_MODIFIED, lastModifiedTime)
            
            Log.d(TAG, "华为设备拒接电话：设置LAST_MODIFIED=${lastModifiedTime}（DATE + ${ringDuration}秒）")
            Log.d(TAG, "华为设备时间戳设置完成：响铃时长可通过LAST_MODIFIED - DATE计算")
        } catch (e: Exception) {
            Log.w(TAG, "华为设备时间戳设置失败: ${e.message}")
        }
    }
    
    /**
     * 为荣耀设备的未接电话设置时间戳字段
     * DATE字段设置为当前时间（通话开始时间）
     * LAST_MODIFIED字段设置为通话结束时间（当前时间 + 响铃时长毫秒）
     */
    private fun setHonorTimestampsForMissedCall(values: ContentValues, ringDuration: Int) {
        // 检查是否为荣耀设备
        if (!isHonorDevice()) {
            return
        }
        
        try {
            val currentTime = System.currentTimeMillis()
            val ringDurationMs = ringDuration * 1000L // 转换为毫秒
            
            // DATE字段：当前时间（通话开始时间）
            if (!values.containsKey(CallLog.Calls.DATE)) {
                values.put(CallLog.Calls.DATE, currentTime)
                Log.d(TAG, "荣耀设备未接电话：设置DATE=${currentTime}（当前时间）")
            } else {
                // 如果DATE已经设置，使用已设置的值作为基准
                val existingDate = values.getAsLong(CallLog.Calls.DATE) ?: currentTime
                Log.d(TAG, "荣耀设备未接电话：使用已设置的DATE=${existingDate}")
            }
            
            // LAST_MODIFIED字段：通话结束时间（DATE + 响铃时长毫秒）
            val dateValue = values.getAsLong(CallLog.Calls.DATE) ?: currentTime
            val lastModifiedTime = dateValue + ringDurationMs
            values.put(CallLog.Calls.LAST_MODIFIED, lastModifiedTime)
            
            Log.d(TAG, "荣耀设备未接电话：设置LAST_MODIFIED=${lastModifiedTime}（DATE + ${ringDuration}秒）")
            Log.d(TAG, "荣耀设备时间戳设置完成：响铃时长可通过LAST_MODIFIED - DATE计算")
        } catch (e: Exception) {
            Log.w(TAG, "荣耀设备时间戳设置失败: ${e.message}")
        }
    }
    
    /**
     * 为荣耀设备的拒接电话设置时间戳字段
     * DATE字段设置为当前时间（通话开始时间）
     * LAST_MODIFIED字段设置为通话结束时间（当前时间 + 响铃时长毫秒）
     */
    private fun setHonorTimestampsForRejectedCall(values: ContentValues, ringDuration: Int) {
        // 检查是否为荣耀设备
        if (!isHonorDevice()) {
            return
        }
        
        try {
            val currentTime = System.currentTimeMillis()
            val ringDurationMs = ringDuration * 1000L // 转换为毫秒
            
            // DATE字段：当前时间（通话开始时间）
            if (!values.containsKey(CallLog.Calls.DATE)) {
                values.put(CallLog.Calls.DATE, currentTime)
                Log.d(TAG, "荣耀设备拒接电话：设置DATE=${currentTime}（当前时间）")
            } else {
                // 如果DATE已经设置，使用已设置的值作为基准
                val existingDate = values.getAsLong(CallLog.Calls.DATE) ?: currentTime
                Log.d(TAG, "荣耀设备拒接电话：使用已设置的DATE=${existingDate}")
            }
            
            // LAST_MODIFIED字段：通话结束时间（DATE + 响铃时长毫秒）
            val dateValue = values.getAsLong(CallLog.Calls.DATE) ?: currentTime
            val lastModifiedTime = dateValue + ringDurationMs
            values.put(CallLog.Calls.LAST_MODIFIED, lastModifiedTime)
            
            Log.d(TAG, "荣耀设备拒接电话：设置LAST_MODIFIED=${lastModifiedTime}（DATE + ${ringDuration}秒）")
            Log.d(TAG, "荣耀设备时间戳设置完成：响铃时长可通过LAST_MODIFIED - DATE计算")
        } catch (e: Exception) {
            Log.w(TAG, "荣耀设备时间戳设置失败: ${e.message}")
        }
    }
    
    /**
     * 为OPPO设备的未接电话设置时间戳字段
     * DATE字段设置为当前时间（通话开始时间）
     * LAST_MODIFIED字段设置为通话结束时间（当前时间 + 响铃时长毫秒）
     */
    private fun setOppoTimestampsForMissedCall(values: ContentValues, ringDuration: Int) {
        // 检查是否为OPPO设备
        if (!isOppoDevice()) {
            return
        }
        
        try {
            val currentTime = System.currentTimeMillis()
            val ringDurationMs = ringDuration * 1000L // 转换为毫秒
            
            // DATE字段：当前时间（通话开始时间）
            if (!values.containsKey(CallLog.Calls.DATE)) {
                values.put(CallLog.Calls.DATE, currentTime)
                Log.d(TAG, "OPPO设备未接电话：设置DATE=${currentTime}（当前时间）")
            } else {
                // 如果DATE已经设置，使用已设置的值作为基准
                val existingDate = values.getAsLong(CallLog.Calls.DATE) ?: currentTime
                Log.d(TAG, "OPPO设备未接电话：使用已设置的DATE=${existingDate}")
            }
            
            // LAST_MODIFIED字段：通话结束时间（DATE + 响铃时长毫秒）
            val dateValue = values.getAsLong(CallLog.Calls.DATE) ?: currentTime
            val lastModifiedTime = dateValue + ringDurationMs
            values.put(CallLog.Calls.LAST_MODIFIED, lastModifiedTime)
            
            Log.d(TAG, "OPPO设备未接电话：设置LAST_MODIFIED=${lastModifiedTime}（DATE + ${ringDuration}秒）")
            Log.d(TAG, "OPPO设备时间戳设置完成：响铃时长可通过LAST_MODIFIED - DATE计算")
        } catch (e: Exception) {
            Log.w(TAG, "OPPO设备时间戳设置失败: ${e.message}")
        }
    }
    
    /**
     * 为OPPO设备的拒接电话设置时间戳字段
     * DATE字段设置为当前时间（通话开始时间）
     * LAST_MODIFIED字段设置为通话结束时间（当前时间 + 响铃时长毫秒）
     */
    private fun setOppoTimestampsForRejectedCall(values: ContentValues, ringDuration: Int) {
        // 检查是否为OPPO设备
        if (!isOppoDevice()) {
            return
        }
        
        try {
            val currentTime = System.currentTimeMillis()
            val ringDurationMs = ringDuration * 1000L // 转换为毫秒
            
            // DATE字段：当前时间（通话开始时间）
            if (!values.containsKey(CallLog.Calls.DATE)) {
                values.put(CallLog.Calls.DATE, currentTime)
                Log.d(TAG, "OPPO设备拒接电话：设置DATE=${currentTime}（当前时间）")
            } else {
                // 如果DATE已经设置，使用已设置的值作为基准
                val existingDate = values.getAsLong(CallLog.Calls.DATE) ?: currentTime
                Log.d(TAG, "OPPO设备拒接电话：使用已设置的DATE=${existingDate}")
            }
            
            // LAST_MODIFIED字段：通话结束时间（DATE + 响铃时长毫秒）
            val dateValue = values.getAsLong(CallLog.Calls.DATE) ?: currentTime
            val lastModifiedTime = dateValue + ringDurationMs
            values.put(CallLog.Calls.LAST_MODIFIED, lastModifiedTime)
            
            Log.d(TAG, "OPPO设备拒接电话：设置LAST_MODIFIED=${lastModifiedTime}（DATE + ${ringDuration}秒）")
            Log.d(TAG, "OPPO设备时间戳设置完成：响铃时长可通过LAST_MODIFIED - DATE计算")
        } catch (e: Exception) {
            Log.w(TAG, "OPPO设备时间戳设置失败: ${e.message}")
        }
    }
    
    /**
     * 检查当前设备是否为华为设备
     */
    private fun isHuaweiDevice(): Boolean {
        return android.os.Build.MANUFACTURER.lowercase() == "huawei"
    }
    
    /**
     * 检查当前设备是否为荣耀设备
     */
    private fun isHonorDevice(): Boolean {
        return android.os.Build.MANUFACTURER.lowercase() == "honor"
    }
    
    /**
     * 检查当前设备是否为OPPO设备
     */
    private fun isOppoDevice(): Boolean {
        return android.os.Build.MANUFACTURER.lowercase() == "oppo"
    }
    
    /**
     * 检查当前设备是否为vivo设备
     */
    fun isVivoDevice(): Boolean {
        return android.os.Build.MANUFACTURER.lowercase() == "vivo"
    }
    
}