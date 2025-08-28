package com.uselesswater.multicallloggeneration

/**
 * 通话类型枚举，提供类型安全和更好的架构设计
 * 防御性编程：每个类型都有明确的属性和行为定义
 */
enum class CallType(
    val value: Int,
    val displayName: String,
    val requiresDuration: Boolean,
    val defaultDuration: Int = 0
) {
    /**
     * 呼出电话（已接通）
     */
    OUTGOING(
        value = Constants.CALL_TYPE_OUTGOING,
        displayName = "呼出电话",
        requiresDuration = true
    ),
    
    /**
     * 来电（已接听）
     */
    INCOMING(
        value = Constants.CALL_TYPE_INCOMING,
        displayName = "来电(已接)",
        requiresDuration = true
    ),
    
    /**
     * 未接来电
     */
    MISSED(
        value = Constants.CALL_TYPE_MISSED,
        displayName = "未接来电",
        requiresDuration = false,
        defaultDuration = Constants.DEFAULT_RING_DURATION
    ),
    
    /**
     * 拒接来电
     */
    REJECTED(
        value = Constants.CALL_TYPE_REJECTED,
        displayName = "拒接来电",
        requiresDuration = false,
        defaultDuration = 0
    ),
    
    /**
     * 呼出未接通
     */
    OUTGOING_UNANSWERED(
        value = Constants.CALL_TYPE_OUTGOING_UNANSWERED,
        displayName = "呼出未接通",
        requiresDuration = false,
        defaultDuration = 0
    ),
    
    /**
     * vivo设备呼出未接通（type=2）
     */
    VIVO_OUTGOING_UNANSWERED(
        value = 2,
        displayName = "呼出未接通(vivo)",
        requiresDuration = false,
        defaultDuration = 0
    );
    
    companion object {
        /**
         * 根据值获取通话类型
         * 防御性编程：提供安全的查找机制
         */
        fun fromValue(value: Int): CallType? {
            return entries.find { it.value == value }
        }
        
        /**
         * 根据值获取通话类型，提供默认值
         * 防御性编程：确保总是返回有效的类型
         */
        fun fromValueOrDefault(value: Int, default: CallType = REJECTED): CallType {
            return fromValue(value) ?: default
        }
        
        /**
         * 获取所有通话类型选项
         * 用于UI显示
         */
        fun getAllOptions(): List<Pair<String, Int>> {
            return entries.map { it.displayName to it.value }
        }
        
        /**
         * 检查指定类型是否需要时长设置
         */
        fun requiresDurationSetting(callTypeValue: Int): Boolean {
            return fromValue(callTypeValue)?.requiresDuration ?: false
        }
        
        /**
         * 获取指定类型的默认时长
         */
        fun getDefaultDuration(callTypeValue: Int): Int {
            return fromValue(callTypeValue)?.defaultDuration ?: 0
        }
    }
    
    /**
     * 验证给定的时长是否适用于此通话类型
     * 防御性编程：提供类型特定的验证逻辑
     */
    fun validateDuration(duration: Int): Boolean {
        return when (this) {
            OUTGOING, INCOMING -> duration > 0
            MISSED -> duration >= 0  // 未接来电可以有响铃时长
            REJECTED, OUTGOING_UNANSWERED, VIVO_OUTGOING_UNANSWERED -> duration == 0  // 拒接和呼出未接通必须为0
        }
    }

}