package com.uselesswater.multicallloggeneration

import androidx.compose.runtime.Stable

/**
 * 通话类型UI状态管理类
 * 防御性编程：集中管理UI状态逻辑，提供类型安全的状态管理
 */
@Stable
data class CallTypeUIState(
    val selectedCallType: CallType = CallType.OUTGOING,
    val selectedTimeRangeIndex: Int = 0,
    val customMinDuration: Int = Constants.TIME_RANGE_CUSTOM_MIN,
    val customMaxDuration: Int = Constants.TIME_RANGE_CUSTOM_MAX
) {
    
    /**
     * 检查当前选择的通话类型是否需要显示时长设置
     * 防御性编程：基于类型属性而非硬编码判断
     */
    val shouldShowDurationSettings: Boolean
        get() = selectedCallType.requiresDuration
    
    /**
     * 检查是否应该显示自定义时长设置
     * 防御性编程：组合条件判断，确保逻辑清晰
     */
    val shouldShowCustomDurationSettings: Boolean
        get() = shouldShowDurationSettings && selectedTimeRangeIndex == 3
    
    /**
     * 检查是否需要进行时长验证
     * 防御性编程：只有需要时长设置的类型才进行验证
     */
    val shouldValidateDuration: Boolean
        get() = shouldShowDurationSettings && selectedTimeRangeIndex == 3

    /**
     * 验证自定义时长设置
     * 防御性编程：提供完整的验证逻辑和错误信息
     */
    fun validateCustomDuration(): ValidationResult {
        if (!shouldValidateDuration) {
            return ValidationResult.Success
        }
        
        return when {
            customMinDuration < Constants.TIME_RANGE_CUSTOM_MIN -> {
                ValidationResult.Error("最小时长不能小于${Constants.TIME_RANGE_CUSTOM_MIN}秒")
            }
            customMaxDuration > Constants.TIME_RANGE_CUSTOM_MAX -> {
                ValidationResult.Error("最大时长不能大于${Constants.TIME_RANGE_CUSTOM_MAX}秒")
            }
            customMinDuration > customMaxDuration -> {
                ValidationResult.Error("最小时长不能大于最大时长，请检查设置")
            }
            customMaxDuration - customMinDuration < 1 -> {
                ValidationResult.Error("最大时长与最小时长差值至少为1秒")
            }
            else -> ValidationResult.Success
        }
    }
    
    /**
     * 更新选择的通话类型
     * 防御性编程：确保状态更新的一致性
     */
    fun updateCallType(newCallType: CallType): CallTypeUIState {
        return copy(
            selectedCallType = newCallType,
            // 如果新类型不需要时长设置，重置时间范围选择
            selectedTimeRangeIndex = if (!newCallType.requiresDuration) 0 else selectedTimeRangeIndex
        )
    }
    
    /**
     * 更新时间范围选择
     */
    fun updateTimeRangeIndex(newIndex: Int): CallTypeUIState {
        return copy(selectedTimeRangeIndex = newIndex)
    }
    
    /**
     * 更新自定义时长范围
     */
    fun updateCustomDuration(minDuration: Int, maxDuration: Int): CallTypeUIState {
        return copy(
            customMinDuration = minDuration,
            customMaxDuration = maxDuration
        )
    }
    
    /**
     * 验证当前UI状态
     * 防御性编程：统一的状态验证入口
     */
    fun validateCurrentState(): ValidationResult {
        return validateCustomDuration()
    }
    
    /**
     * 计算最终通话时长
     * 防御性编程：根据时间范围和类型计算合适的时长
     */
    fun calculateFinalDuration(selectedRange: TimeRange): Int {
        return if (selectedTimeRangeIndex == 3) {
            // 自定义时长范围，确保边界值安全
            val safeMax = minOf(customMaxDuration, Int.MAX_VALUE - 1)
            val safeMin = maxOf(customMinDuration, 0)
            if (safeMin >= safeMax) safeMin else kotlin.random.Random.nextInt(safeMin, safeMax + 1)
        } else {
            // 预设时长范围，确保边界值安全
            val safeMax = minOf(selectedRange.maxSeconds, Int.MAX_VALUE - 1)
            val safeMin = maxOf(selectedRange.minSeconds, 0)
            if (safeMin >= safeMax) safeMin else kotlin.random.Random.nextInt(safeMin, safeMax + 1)
        }
    }
}

/**
 * 验证结果密封类
 * 防御性编程：类型安全的验证结果表示
 */
sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val message: String) : ValidationResult()

    val isValid: Boolean get() = this is Success
    
    val errorMessage: String get() = when (this) {
        is Success -> ""
        is Error -> message
    }
}