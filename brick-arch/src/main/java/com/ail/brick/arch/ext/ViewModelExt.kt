package com.ail.brick.arch.ext

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * 在 Activity 中快捷获取 ViewModel（无参构造）。
 *
 * ```kotlin
 * val vm by lazy { getViewModel<HomeViewModel>() }
 * ```
 */
inline fun <reified VM : ViewModel> FragmentActivity.getViewModel(): VM {
    return ViewModelProvider(this)[VM::class.java]
}

/**
 * 在 Activity 中使用自定义 Factory 获取 ViewModel。
 *
 * ```kotlin
 * val vm by lazy { getViewModel<HomeViewModel>(HomeViewModelFactory(repo)) }
 * ```
 *
 * @param factory 自定义 ViewModelProvider.Factory
 */
inline fun <reified VM : ViewModel> FragmentActivity.getViewModel(
    factory: ViewModelProvider.Factory
): VM {
    return ViewModelProvider(this, factory)[VM::class.java]
}

/**
 * 在 Fragment 中获取 Fragment 级别的 ViewModel。
 */
inline fun <reified VM : ViewModel> Fragment.getViewModel(): VM {
    return ViewModelProvider(this)[VM::class.java]
}

/**
 * 在 Fragment 中使用自定义 Factory 获取 ViewModel。
 *
 * @param factory 自定义 ViewModelProvider.Factory
 */
inline fun <reified VM : ViewModel> Fragment.getViewModel(
    factory: ViewModelProvider.Factory
): VM {
    return ViewModelProvider(this, factory)[VM::class.java]
}

/**
 * 在 Fragment 中获取与宿主 Activity 共享的 ViewModel。
 *
 * ```kotlin
 * val sharedVm by lazy { getActivityViewModel<SharedViewModel>() }
 * ```
 */
inline fun <reified VM : ViewModel> Fragment.getActivityViewModel(): VM {
    return ViewModelProvider(requireActivity())[VM::class.java]
}

/**
 * 在 Fragment 中使用自定义 Factory 获取与宿主 Activity 共享的 ViewModel。
 *
 * @param factory 自定义 ViewModelProvider.Factory
 */
inline fun <reified VM : ViewModel> Fragment.getActivityViewModel(
    factory: ViewModelProvider.Factory
): VM {
    return ViewModelProvider(requireActivity(), factory)[VM::class.java]
}
