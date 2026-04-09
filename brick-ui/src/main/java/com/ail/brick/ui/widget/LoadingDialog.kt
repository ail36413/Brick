package com.ail.brick.ui.widget

import android.app.Dialog
import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.lang.ref.WeakReference

/**
 * 全局单例 Loading 对话框。
 *
 * 同一时刻最多显示一个实例，重复调用 [show] 会先关闭前一个。
 * 内部使用 [WeakReference] 持有 Dialog 以避免 Activity 泄漏。
 *
 * ### 用法
 * ```kotlin
 * LoadingDialog.show(context, "提交中…")
 * // 操作完成
 * LoadingDialog.dismiss()
 * ```
 *
 * ### 带取消回调
 * ```kotlin
 * LoadingDialog.show(context, "加载中…", cancelable = true) {
 *     // 用户按返回键取消时回调
 *     cancelRequest()
 * }
 * ```
 *
 * @see dismiss
 */
object LoadingDialog {

    private var dialogRef: WeakReference<Dialog>? = null
    private var lifecycleObserver: DefaultLifecycleObserver? = null
    private var lifecycleOwnerRef: WeakReference<LifecycleOwner>? = null

    /**
     * 显示 Loading 对话框。
     *
     * @param context    上下文（建议传 Activity Context）
     * @param message    提示文字，默认 "加载中…"
     * @param cancelable 是否可按返回键取消，默认 false
     * @param onCancel   取消时的回调（cancelable = true 时生效），默认 null
     * @return 创建的 [Dialog] 实例
     */
    fun show(
        context: Context,
        message: String = "加载中…",
        cancelable: Boolean = false,
        onCancel: (() -> Unit)? = null
    ): Dialog {
        dismiss()
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val dp16 = (16 * resources.displayMetrics.density).toInt()
            setPadding(dp16 * 2, dp16, dp16 * 2, dp16)

            addView(ProgressBar(context).apply {
                val size = (36 * resources.displayMetrics.density).toInt()
                layoutParams = LinearLayout.LayoutParams(size, size)
            })
            addView(TextView(context).apply {
                text = message
                textSize = 15f
                val dp12 = (12 * resources.displayMetrics.density).toInt()
                setPadding(dp12, 0, 0, 0)
            })
        }

        val dialog = MaterialAlertDialogBuilder(context)
            .setView(layout)
            .setCancelable(cancelable)
            .create()
            .apply {
                setCanceledOnTouchOutside(false)
                if (cancelable && onCancel != null) {
                    setOnCancelListener { onCancel.invoke() }
                }
                window?.apply {
                    setBackgroundDrawableResource(android.R.color.transparent)
                    setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT)
                    setGravity(Gravity.CENTER)
                }
                show()
            }
        dialogRef = WeakReference(dialog)
        bindLifecycle(context)
        return dialog
    }

    /**
     * 关闭当前 Loading 对话框（如果正在显示）。
     */
    fun dismiss() {
        unbindLifecycle()
        dialogRef?.get()?.let { dialog ->
            try {
                if (dialog.isShowing) dialog.dismiss()
            } catch (_: Exception) {
                // Activity 已销毁时 dismiss 可能抛异常，安全忽略
            }
        }
        dialogRef = null
    }

    /**
     * 使用自定义布局显示 Loading 对话框。
     *
     * @param context    上下文
     * @param layoutRes  自定义布局资源 ID
     * @param cancelable 是否可取消
     * @param onCancel   取消回调
     * @return 创建的 [Dialog] 实例
     */
    fun show(
        context: Context,
        @LayoutRes layoutRes: Int,
        cancelable: Boolean = false,
        onCancel: (() -> Unit)? = null
    ): Dialog {
        dismiss()
        val view = LayoutInflater.from(context).inflate(layoutRes, null)
        return showWithView(context, view, cancelable, onCancel)
    }

    /**
     * 使用自定义 View 显示 Loading 对话框。
     *
     * @param context    上下文
     * @param view       自定义视图
     * @param cancelable 是否可取消
     * @param onCancel   取消回调
     * @return 创建的 [Dialog] 实例
     */
    fun showWithView(
        context: Context,
        view: View,
        cancelable: Boolean = false,
        onCancel: (() -> Unit)? = null
    ): Dialog {
        dismiss()
        val dialog = MaterialAlertDialogBuilder(context)
            .setView(view)
            .setCancelable(cancelable)
            .create()
            .apply {
                setCanceledOnTouchOutside(false)
                if (cancelable && onCancel != null) {
                    setOnCancelListener { onCancel.invoke() }
                }
                window?.apply {
                    setBackgroundDrawableResource(android.R.color.transparent)
                    setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT)
                    setGravity(Gravity.CENTER)
                }
                show()
            }
        dialogRef = WeakReference(dialog)
        bindLifecycle(context)
        return dialog
    }

    private fun bindLifecycle(context: Context) {
        val owner = context as? LifecycleOwner ?: return
        unbindLifecycle()
        val observer = object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                dismiss()
            }
        }
        lifecycleObserver = observer
        lifecycleOwnerRef = WeakReference(owner)
        owner.lifecycle.addObserver(observer)
    }

    private fun unbindLifecycle() {
        val owner = lifecycleOwnerRef?.get()
        val observer = lifecycleObserver
        if (owner != null && observer != null) {
            owner.lifecycle.removeObserver(observer)
        }
        lifecycleObserver = null
        lifecycleOwnerRef = null
    }
}
