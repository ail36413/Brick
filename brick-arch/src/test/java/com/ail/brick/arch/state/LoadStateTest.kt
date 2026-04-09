package com.ail.brick.arch.state

import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

/**
 * LoadState 的单元测试，验证状态判断和操作符行为。
 */
class LoadStateTest {

    // ==================== 基本状态 ====================

    @Test
    fun `Loading state properties`() {
        val state: LoadState<String> = LoadState.Loading
        assertTrue(state.isLoading)
        assertFalse(state.isSuccess)
        assertFalse(state.isError)
    }

    @Test
    fun `Success state properties`() {
        val state: LoadState<String> = LoadState.Success("hello")
        assertFalse(state.isLoading)
        assertTrue(state.isSuccess)
        assertFalse(state.isError)
    }

    @Test
    fun `Error state properties`() {
        val state: LoadState<String> = LoadState.Error(RuntimeException("fail"))
        assertFalse(state.isLoading)
        assertFalse(state.isSuccess)
        assertTrue(state.isError)
    }

    @Test
    fun `Error state uses exception message by default`() {
        val state = LoadState.Error(RuntimeException("test error"))
        assertEquals("test error", state.message)
    }

    @Test
    fun `Error state uses custom message when provided`() {
        val state = LoadState.Error(RuntimeException("original"), "custom msg")
        assertEquals("custom msg", state.message)
    }

    // ==================== map ====================

    @Test
    fun `map transforms Success data`() {
        val state: LoadState<Int> = LoadState.Success(42)
        val mapped = state.map { it * 2 }
        assertEquals(LoadState.Success(84), mapped)
    }

    @Test
    fun `map passes through Loading`() {
        val state: LoadState<Int> = LoadState.Loading
        val mapped = state.map { it * 2 }
        assertTrue(mapped is LoadState.Loading)
    }

    @Test
    fun `map passes through Error`() {
        val ex = RuntimeException("err")
        val state: LoadState<Int> = LoadState.Error(ex)
        val mapped = state.map { it * 2 }
        assertTrue(mapped is LoadState.Error)
        assertEquals("err", (mapped as LoadState.Error).message)
    }

    // ==================== getOrNull / getOrDefault ====================

    @Test
    fun `getOrNull returns data on Success`() {
        assertEquals("hello", LoadState.Success("hello").getOrNull())
    }

    @Test
    fun `getOrNull returns null on Loading`() {
        assertNull(LoadState.Loading.getOrNull())
    }

    @Test
    fun `getOrNull returns null on Error`() {
        assertNull(LoadState.Error(RuntimeException()).getOrNull())
    }

    @Test
    fun `getOrDefault returns data on Success`() {
        assertEquals("hello", LoadState.Success("hello").getOrDefault("default"))
    }

    @Test
    fun `getOrDefault returns default on Loading`() {
        assertEquals("default", LoadState.Loading.getOrDefault("default"))
    }

    @Test
    fun `getOrDefault returns default on Error`() {
        assertEquals("default", LoadState.Error(RuntimeException()).getOrDefault("default"))
    }

    // ==================== fold ====================

    @Test
    fun `fold calls correct branch for Loading`() {
        val result = LoadState.Loading.fold(
            onLoading = { "loading" },
            onSuccess = { "success" },
            onError = { "error" }
        )
        assertEquals("loading", result)
    }

    @Test
    fun `fold calls correct branch for Success`() {
        val result = LoadState.Success(42).fold(
            onLoading = { "loading" },
            onSuccess = { "data=$it" },
            onError = { "error" }
        )
        assertEquals("data=42", result)
    }

    @Test
    fun `fold calls correct branch for Error`() {
        val result = LoadState.Error(RuntimeException("oops")).fold(
            onLoading = { "loading" },
            onSuccess = { "success" },
            onError = { "error: ${it.message}" }
        )
        assertEquals("error: oops", result)
    }

    // ==================== onSuccess / onError / onLoading ====================

    @Test
    fun `onSuccess fires on Success`() {
        var called = false
        LoadState.Success("data").onSuccess { called = true }
        assertTrue(called)
    }

    @Test
    fun `onSuccess does not fire on Loading`() {
        var called = false
        LoadState.Loading.onSuccess { called = true }
        assertFalse(called)
    }

    @Test
    fun `onError fires on Error`() {
        var called = false
        LoadState.Error(RuntimeException()).onError { called = true }
        assertTrue(called)
    }

    @Test
    fun `onError does not fire on Success`() {
        var called = false
        LoadState.Success("data").onError { called = true }
        assertFalse(called)
    }

    @Test
    fun `onLoading fires on Loading`() {
        var called = false
        LoadState.Loading.onLoading { called = true }
        assertTrue(called)
    }

    @Test
    fun `onLoading does not fire on Success`() {
        var called = false
        LoadState.Success("data").onLoading { called = true }
        assertFalse(called)
    }

    // ==================== loadStateCatching ====================

    @Test
    fun `loadStateCatching returns Success on normal execution`() = runTest {
        val state = loadStateCatching { 42 }
        assertEquals(LoadState.Success(42), state)
    }

    @Test
    fun `loadStateCatching returns Error on exception`() = runTest {
        val state = loadStateCatching<Int> { throw RuntimeException("boom") }
        assertTrue(state is LoadState.Error)
        assertEquals("boom", (state as LoadState.Error).message)
    }

    // ==================== 链式调用 ====================

    @Test
    fun `chained callbacks work correctly`() {
        var successCalled = false
        var errorCalled = false
        var loadingCalled = false

        LoadState.Success("data")
            .onLoading { loadingCalled = true }
            .onSuccess { successCalled = true }
            .onError { errorCalled = true }

        assertTrue(successCalled)
        assertFalse(errorCalled)
        assertFalse(loadingCalled)
    }

    // ==================== retryLoadState ====================

    @Test
    fun `retryLoadState returns Success on first try`() = runTest {
        val state = retryLoadState(times = 3) { "ok" }
        assertEquals(LoadState.Success("ok"), state)
    }

    @Test
    fun `retryLoadState retries and succeeds`() = runTest {
        var attempt = 0
        val state = retryLoadState(times = 3, initialDelayMillis = 10) {
            attempt++
            if (attempt < 3) throw RuntimeException("fail $attempt")
            "recovered"
        }
        assertEquals(LoadState.Success("recovered"), state)
        assertEquals(3, attempt)
    }

    @Test
    fun `retryLoadState returns Error after all retries exhausted`() = runTest {
        var attempt = 0
        val state = retryLoadState<String>(times = 2, initialDelayMillis = 10) {
            attempt++
            throw RuntimeException("fail $attempt")
        }
        assertTrue(state is LoadState.Error)
        // times=2 retries + 1 final attempt = 3 total
        assertEquals(3, attempt)
    }
}
