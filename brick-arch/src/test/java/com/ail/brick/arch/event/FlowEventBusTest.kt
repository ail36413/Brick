package com.ail.brick.arch.event

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * FlowEventBus 的单元测试。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FlowEventBusTest {

    data class TestEvent(val value: String)
    data class OtherEvent(val id: Int)

    @Before
    fun setup() {
        FlowEventBus.clear()
    }

    @After
    fun tearDown() {
        FlowEventBus.clear()
    }

    // ==================== 普通事件 ====================

    @Test
    fun `post and observe normal event`() = runTest {
        var received: Any? = null

        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            FlowEventBus.observe<TestEvent>().first().also { received = it }
        }

        FlowEventBus.post(TestEvent("hello"))
        job.join()

        assertTrue(received is TestEvent)
        assertEquals("hello", (received as TestEvent).value)
    }

    @Test
    fun `events of different types are isolated`() = runTest {
        var testReceived = false
        var otherReceived = false

        val job1 = launch(UnconfinedTestDispatcher(testScheduler)) {
            FlowEventBus.observe<TestEvent>().first()
            testReceived = true
        }

        val job2 = launch(UnconfinedTestDispatcher(testScheduler)) {
            FlowEventBus.observe<OtherEvent>().first()
            otherReceived = true
        }

        FlowEventBus.post(TestEvent("hello"))
        job1.join()

        assertTrue(testReceived)
        assertFalse(otherReceived)

        // cleanup
        job2.cancel()
    }

    // ==================== 粘性事件 ====================

    @Test
    fun `sticky event replays to new subscriber`() = runTest {
        // 先发送粘性事件
        FlowEventBus.postSticky(TestEvent("sticky"))

        // 后订阅也能收到
        var received: TestEvent? = null
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            FlowEventBus.observeSticky<TestEvent>().first().also {
                received = it as? TestEvent
            }
        }
        job.join()

        assertNotNull(received)
        assertEquals("sticky", received?.value)
    }

    @Test
    fun `removeSticky clears sticky event channel`() {
        FlowEventBus.removeSticky(TestEvent::class.java.name)
        // 应该不抛异常，即使通道不存在
    }

    // ==================== clear ====================

    @Test
    fun `clear removes all channels`() = runTest {
        FlowEventBus.post(TestEvent("one"))
        FlowEventBus.postSticky(OtherEvent(1))

        FlowEventBus.clear()

        // 重新获取的 flow 应该是一个新的空 flow
        // 验证方式：发新事件可以被新订阅者接收
        var received = false
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            FlowEventBus.observe<TestEvent>().first()
            received = true
        }

        FlowEventBus.post(TestEvent("two"))
        job.join()

        assertTrue(received)
    }

    // ==================== 自定义 key ====================

    @Test
    fun `custom key isolates same event type`() = runTest {
        var receivedA: Any? = null
        var receivedB: Any? = null

        val jobA = launch(UnconfinedTestDispatcher(testScheduler)) {
            FlowEventBus.observe<TestEvent>("channel_A").first().also { receivedA = it }
        }

        val jobB = launch(UnconfinedTestDispatcher(testScheduler)) {
            FlowEventBus.observe<TestEvent>("channel_B").first().also { receivedB = it }
        }

        FlowEventBus.post(TestEvent("forA"), key = "channel_A")
        jobA.join()

        assertTrue(receivedA is TestEvent)
        assertEquals("forA", (receivedA as TestEvent).value)
        assertNull(receivedB) // channel_B should not have received

        // cleanup
        jobB.cancel()
    }
}
