package com.ail.brick.arch.mvi

/**
 * MVI 用户意图接口
 *
 * 代表用户在 UI 上触发的所有操作。
 *
 * ```kotlin
 * sealed class HomeIntent : UiIntent {
 *     data object Refresh : HomeIntent()
 *     data class LoadMore(val page: Int) : HomeIntent()
 * }
 * ```
 */
interface UiIntent
