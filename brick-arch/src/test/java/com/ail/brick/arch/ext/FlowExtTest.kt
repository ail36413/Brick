package com.ail.brick.arch.ext

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FlowExtTest {

    // ==================== throttleFirst ====================

    @Test
    fun `throttleFirst emits first element immediately`() = runTest {
        val source = flow {
            emit(1)
            emit(2)
            emit(3)
        }
        val result = source.throttleFirst(500).toList()
        // 同步发射，第一个通过，后续在 500ms 窗口内被丢弃
        assertEquals(listOf(1), result)
    }

    @Test
    fun `throttleFirst emits after window expires`() = runTest {
        // throttleFirst uses System.currentTimeMillis(), so we use Thread.sleep
        // to ensure real wall-clock time passes between emissions
        val source = flow {
            emit("a")
            @Suppress("BlockingMethodInNonBlockingContext")
            Thread.sleep(100)
            emit("b")
            emit("c") // same instant as b, throttled
        }
        val result = source.throttleFirst(50).toList()
        assertEquals(listOf("a", "b"), result)
    }

    @Test
    fun `throttleFirst with zero window emits all`() = runTest {
        val source = flow {
            emit(1)
            emit(2)
            emit(3)
        }
        val result = source.throttleFirst(0).toList()
        assertEquals(listOf(1, 2, 3), result)
    }

    // ==================== debounceAction ====================

    @Test
    fun `debounceAction emits last item after timeout`() = runTest {
        val result = mutableListOf<String>()
        val source = flow {
            emit("a")
            delay(100) // < 300ms, debounced
            emit("b")
            delay(100) // < 300ms, debounced
            emit("c")
            delay(400) // > 300ms, "c" emitted
        }

        val job = launch {
            source.debounceAction(300).collect { result.add(it) }
        }

        advanceTimeBy(1000)
        job.cancel()

        assertEquals(listOf("c"), result)
    }

    // ==================== select ====================

    @Test
    fun `select emits only when selected field changes`() = runTest {
        data class TestState(val count: Int, val name: String)

        val stateFlow = MutableStateFlow(TestState(0, "init"))
        val result = mutableListOf<Int>()

        val job = launch {
            stateFlow.select { it.count }.collect { result.add(it) }
        }

        // 初始值
        advanceTimeBy(10)
        assertEquals(listOf(0), result)

        // 更新 name（count 不变），不应触发
        stateFlow.value = TestState(0, "changed")
        advanceTimeBy(10)
        assertEquals(listOf(0), result)

        // 更新 count，应触发
        stateFlow.value = TestState(1, "changed")
        advanceTimeBy(10)
        assertEquals(listOf(0, 1), result)

        // 更新 count 到相同值，不应触发
        stateFlow.value = TestState(1, "again")
        advanceTimeBy(10)
        assertEquals(listOf(0, 1), result)

        // 更新 count 到新值
        stateFlow.value = TestState(2, "again")
        advanceTimeBy(10)
        assertEquals(listOf(0, 1, 2), result)

        job.cancel()
    }

    @Test
    fun `select works with null values`() = runTest {
        data class TestState(val data: String?)

        val stateFlow = MutableStateFlow(TestState(null))
        val result = mutableListOf<String?>()

        val job = launch {
            stateFlow.select { it.data }.collect { result.add(it) }
        }

        advanceTimeBy(10)
        assertEquals(listOf<String?>(null), result)

        stateFlow.value = TestState("hello")
        advanceTimeBy(10)
        assertEquals(listOf(null, "hello"), result)

        stateFlow.value = TestState("hello") // same, no emit
        advanceTimeBy(10)
        assertEquals(listOf(null, "hello"), result)

        job.cancel()
    }
}
