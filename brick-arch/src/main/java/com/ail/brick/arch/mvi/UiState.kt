package com.ail.brick.arch.mvi

/**
 * MVI 状态基础接口
 *
 * 所有页面 State 都应实现此接口。
 *
 * ```kotlin
 * data class HomeState(
 *     val isLoading: Boolean = false,
 *     val items: List<String> = emptyList(),
 *     val error: String? = null
 * ) : UiState
 * ```
 */
interface UiState
