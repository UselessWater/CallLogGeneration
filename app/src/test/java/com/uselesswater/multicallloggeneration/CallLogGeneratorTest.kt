package com.uselesswater.multicallloggeneration

import android.content.ContentValues
import android.provider.CallLog
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

/**
 * CallLogGenerator单元测试
 */
@RunWith(AndroidJUnit4::class)
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
        assertEquals(ringDuration, values.getAsInteger(CallLog.Calls.DURATION))
    }

    @Test
    fun testCreateRejectedCall() {
        val values = ContentValues()
        val ringDuration = 20
        
        CallLogGenerator.createRejectedCall(values, ringDuration)
        
        assertEquals(Constants.CALL_TYPE_REJECTED, values.getAsInteger(CallLog.Calls.TYPE))
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
        assertEquals(ringDuration, values.getAsInteger(CallLog.Calls.DURATION))
    }

    @Test
    fun testGetCallTypeName() {
        assertEquals("呼出电话", CallLogGenerator.getCallTypeName(Constants.CALL_TYPE_OUTGOING))
        assertEquals("来电(已接)", CallLogGenerator.getCallTypeName(Constants.CALL_TYPE_INCOMING))
        assertEquals("未接来电", CallLogGenerator.getCallTypeName(Constants.CALL_TYPE_MISSED))
        assertEquals("拒接来电", CallLogGenerator.getCallTypeName(Constants.CALL_TYPE_REJECTED))
        assertEquals("未知类型", CallLogGenerator.getCallTypeName(999))
    }
}