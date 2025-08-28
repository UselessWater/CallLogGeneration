package com.uselesswater.multicallloggeneration

import android.content.ContentValues
import android.provider.CallLog
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * CallLogGenerator单元测试
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24])
class CallLogGeneratorTest {

    @Test
    fun testCreateOutgoingCall() {
        val values = ContentValues()
        val duration = 30
        
        CallLogGenerator.createOutgoingCall(values, duration)
        
        assertEquals(Constants.CALL_TYPE_OUTGOING, values.getAsInteger(CallLog.Calls.TYPE))
        assertEquals(duration, values.getAsInteger(CallLog.Calls.DURATION))
    }

    @Test
    fun testCreateIncomingCallAnswered() {
        val values = ContentValues()
        val duration = 45
        val isAnswered = true
        
        CallLogGenerator.createIncomingCall(values, duration, isAnswered)
        
        assertEquals(Constants.CALL_TYPE_INCOMING, values.getAsInteger(CallLog.Calls.TYPE))
        assertEquals(duration, values.getAsInteger(CallLog.Calls.DURATION))
    }

    @Test
    fun testCreateIncomingCallMissed() {
        val values = ContentValues()
        val ringDuration = 30
        val isAnswered = false
        
        CallLogGenerator.createIncomingCall(values, 0, isAnswered, ringDuration)
        
        assertEquals(Constants.CALL_TYPE_MISSED, values.getAsInteger(CallLog.Calls.TYPE))
        // 防御性编程：未接来电的duration应该设置为响铃时长
        assertEquals(ringDuration, values.getAsInteger(CallLog.Calls.DURATION))
    }

    @Test
    fun testCreateRejectedCall() {
        val values = ContentValues()
        val ringDuration = 20
        
        CallLogGenerator.createRejectedCall(values, ringDuration)
        
        assertEquals(Constants.CALL_TYPE_REJECTED, values.getAsInteger(CallLog.Calls.TYPE))
        // 根据用户要求：拒接来电的duration应该为0
        assertEquals(0, values.getAsInteger(CallLog.Calls.DURATION))
    }

    

    @Test
    fun testCreateCallByTypeOutgoing() {
        val values = ContentValues()
        val duration = 25
        
        CallLogGenerator.createCallByType(values, Constants.CALL_TYPE_OUTGOING, duration)
        
        assertEquals(Constants.CALL_TYPE_OUTGOING, values.getAsInteger(CallLog.Calls.TYPE))
        assertEquals(duration, values.getAsInteger(CallLog.Calls.DURATION))
    }

    @Test
    fun testCreateCallByTypeRejected() {
        val values = ContentValues()
        val ringDuration = 15
        
        CallLogGenerator.createCallByType(values, Constants.CALL_TYPE_REJECTED, 0, ringDuration)
        
        assertEquals(Constants.CALL_TYPE_REJECTED, values.getAsInteger(CallLog.Calls.TYPE))
        // 根据用户要求：拒接来电的duration应该为0
        assertEquals(0, values.getAsInteger(CallLog.Calls.DURATION))
    }

    @Test
    fun testGetCallTypeName() {
        assertEquals("呼出电话", CallLogGenerator.getCallTypeName(Constants.CALL_TYPE_OUTGOING))
        assertEquals("来电(已接)", CallLogGenerator.getCallTypeName(Constants.CALL_TYPE_INCOMING))
        assertEquals("未接来电", CallLogGenerator.getCallTypeName(Constants.CALL_TYPE_MISSED))
        assertEquals("拒接来电", CallLogGenerator.getCallTypeName(Constants.CALL_TYPE_REJECTED))
        assertEquals("未知类型(999)", CallLogGenerator.getCallTypeName(999))
    }
    
    @Test
    fun testSamsungTimestampCalculationForMissedCall() {
        val values = ContentValues()
        val ringDuration = 15 // 15秒响铃时长
        val beforeTime = System.currentTimeMillis()
        
        // 模拟三星设备的未接电话创建
        CallLogGenerator.createIncomingCall(values, 0, false, ringDuration)
        
        val afterTime = System.currentTimeMillis()
        
        // 验证通话类型和duration
        assertEquals(Constants.CALL_TYPE_MISSED, values.getAsInteger(CallLog.Calls.TYPE))
        assertEquals(ringDuration, values.getAsInteger(CallLog.Calls.DURATION))
        
        // 注意：只有在三星设备上才会设置时间戳字段
        // 在测试环境中，Build.MANUFACTURER可能不是"samsung"
        // 所以这里主要验证函数调用不会出错
        // 如果是三星设备，验证时间戳字段的设置
        if (android.os.Build.MANUFACTURER.lowercase() == "samsung") {
            // 验证DATE字段设置（应该接近当前时间）
            val dateValue = values.getAsLong(CallLog.Calls.DATE)
            assertNotNull("DATE字段应该被设置", dateValue)
            assertTrue("DATE应该在测试时间范围内", dateValue!! >= beforeTime && dateValue <= afterTime)
            
            // 验证LAST_MODIFIED字段设置（应该是DATE + 响铃时长毫秒）
            val lastModifiedValue = values.getAsLong(CallLog.Calls.LAST_MODIFIED)
            assertNotNull("LAST_MODIFIED字段应该被设置", lastModifiedValue)
            
            val expectedLastModified = dateValue + (ringDuration * 1000L)
            assertEquals("LAST_MODIFIED应该等于DATE + 响铃时长毫秒", expectedLastModified, lastModifiedValue)
            
            // 验证响铃时长可以通过时间戳相减计算
            val calculatedRingDuration = (lastModifiedValue - dateValue) / 1000L
            assertEquals("通过时间戳计算的响铃时长应该正确", ringDuration.toLong(), calculatedRingDuration)
        }
    }
    
    @Test
    fun testCreateOutgoingUnanswered() {
        val values = ContentValues()
        val ringDuration = 8 // 8秒响铃时长
        
        CallLogGenerator.createOutgoingUnanswered(values, ringDuration)
        
        // 验证通话类型（根据设备类型不同）
        val callType = values.getAsInteger(CallLog.Calls.TYPE)
        if (CallLogGenerator.isVivoDevice()) {
            // vivo设备应该返回type=2
            assertEquals("vivo设备的呼出未接通类型应该是2", 2, callType)
        } else {
            // 其他设备应该返回type=1（呼出电话）
            assertEquals("非vivo设备的呼出未接通类型应该是1", Constants.CALL_TYPE_OUTGOING, callType)
        }
        
        // 验证通话时长为0
        val duration = values.getAsInteger(CallLog.Calls.DURATION)
        assertEquals("呼出未接通的通话时长应该为0", 0, duration)
    }
    
    @Test
    fun testCreateCallByTypeOutgoingUnanswered() {
        val values = ContentValues()
        val ringDuration = 5
        
        CallLogGenerator.createCallByType(values, Constants.CALL_TYPE_OUTGOING_UNANSWERED, 0, ringDuration, null)
        
        // 验证通话类型（根据设备类型不同）
        val callType = values.getAsInteger(CallLog.Calls.TYPE)
        if (CallLogGenerator.isVivoDevice()) {
            // vivo设备应该返回type=2
            assertEquals("vivo设备的呼出未接通类型应该是2", 2, callType)
        } else {
            // 其他设备应该返回type=1（呼出电话）
            assertEquals("非vivo设备的呼出未接通类型应该是1", Constants.CALL_TYPE_OUTGOING, callType)
        }
        
        // 验证通话时长为0
        val duration = values.getAsInteger(CallLog.Calls.DURATION)
        assertEquals("呼出未接通的通话时长应该为0", 0, duration)
    }
    
    @Test
    fun testGetCallTypeNameForOutgoingUnanswered() {
        val typeName = CallLogGenerator.getCallTypeName(Constants.CALL_TYPE_OUTGOING_UNANSWERED)
        assertEquals("呼出未接通类型名称应该正确", "呼出未接通", typeName)
    }
    
    @Test
    fun testCreateCallByTypeVivoOutgoingUnanswered() {
        val values = ContentValues()
        val ringDuration = 3
        
        CallLogGenerator.createCallByType(values, Constants.CALL_TYPE_VIVO_OUTGOING_UNANSWERED, 0, ringDuration, null)
        
        // 验证通话类型应该是2（vivo专用）
        val callType = values.getAsInteger(CallLog.Calls.TYPE)
        assertEquals("vivo专用呼出未接通类型应该是2", Constants.CALL_TYPE_VIVO_OUTGOING_UNANSWERED, callType)
        
        // 验证通话时长为0
        val duration = values.getAsInteger(CallLog.Calls.DURATION)
        assertEquals("vivo呼出未接通的通话时长应该为0", 0, duration)
    }
@Test
    fun testSamsungTimestampCalculationForRejectedCall() {
        val values = ContentValues()
        val ringDuration = 10 // 10秒响铃时长
        val beforeTime = System.currentTimeMillis()
        
        // 模拟三星设备的拒接电话创建
        CallLogGenerator.createRejectedCall(values, ringDuration)
        
        val afterTime = System.currentTimeMillis()
        
        // 验证通话类型和duration
        assertEquals(Constants.CALL_TYPE_REJECTED, values.getAsInteger(CallLog.Calls.TYPE))
        assertEquals(0, values.getAsInteger(CallLog.Calls.DURATION)) // 拒接电话duration为0
        
        // 注意：只有在三星设备上才会设置时间戳字段
        // 在测试环境中，Build.MANUFACTURER可能不是"samsung"
        // 所以这里主要验证函数调用不会出错
        // 如果是三星设备，验证时间戳字段的设置
        if (android.os.Build.MANUFACTURER.lowercase() == "samsung") {
            // 验证DATE字段设置（应该接近当前时间）
            val dateValue = values.getAsLong(CallLog.Calls.DATE)
            assertNotNull("DATE字段应该被设置", dateValue)
            assertTrue("DATE应该在测试时间范围内", dateValue!! >= beforeTime && dateValue <= afterTime)
            
            // 验证LAST_MODIFIED字段设置（应该是DATE + 响铃时长毫秒）
            val lastModifiedValue = values.getAsLong(CallLog.Calls.LAST_MODIFIED)
            assertNotNull("LAST_MODIFIED字段应该被设置", lastModifiedValue)
            
            val expectedLastModified = dateValue + (ringDuration * 1000L)
            assertEquals("LAST_MODIFIED应该等于DATE + 响铃时长毫秒", expectedLastModified, lastModifiedValue)
            
            // 验证响铃时长可以通过时间戳相减计算（即使duration为0）
            val calculatedRingDuration = (lastModifiedValue - dateValue) / 1000L
            assertEquals("通过时间戳计算的响铃时长应该正确", ringDuration.toLong(), calculatedRingDuration)
        }
    }
}