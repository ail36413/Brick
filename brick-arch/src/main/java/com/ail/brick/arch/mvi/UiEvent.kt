package com.ail.brick.arch.mvi

/**
 * MVI 一次性事件接口
 *
 * 区别于 [UiState]，Event 只消费一次不会在重建后重放。
 *
 * ```kotlin
 * sealed class HomeEvent : UiEvent {
 *     data class ShowSnackbar(val message: String) : HomeEvent()
 *     data class NavigateTo(val route: String) : HomeEvent()
 * }
 * ```
 */
interface UiEvent
