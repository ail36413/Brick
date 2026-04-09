package com.ail.brick.ui.recyclerview

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import androidx.annotation.ColorInt
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * RecyclerView 通用分割线装饰器。
 *
 * 仅在相邻 item 之间绘制分割线，最后一个 item 下方不绘制。
 *
 * ```kotlin
 * recyclerView.addItemDecoration(
 *     DividerDecoration(
 *         height = 1.dp,
 *         color = Color.LTGRAY,
 *         paddingStart = 16.dp,
 *         paddingEnd = 16.dp
 *     )
 * )
 * ```
 *
 * @param height       分割线高度（px）
 * @param color        分割线颜色，默认 #E0E0E0
 * @param paddingStart 左侧缩进（px）
 * @param paddingEnd   右侧缩进（px）
 */
class DividerDecoration(
    private val height: Int = 1,
    @ColorInt private val color: Int = 0xFFE0E0E0.toInt(),
    private val paddingStart: Int = 0,
    private val paddingEnd: Int = 0
) : RecyclerView.ItemDecoration() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = this@DividerDecoration.color
        style = Paint.Style.FILL
    }

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val position = parent.getChildAdapterPosition(view)
        // NO_POSITION 表示 ViewHolder 正在被移除或无效，跳过
        if (position == RecyclerView.NO_POSITION) return
        val itemCount = parent.adapter?.itemCount ?: 0

        when (val lm = parent.layoutManager) {
            is GridLayoutManager -> {
                val spanCount = lm.spanCount
                val column = position % spanCount
                // 水平方向：均分间距
                outRect.left = height * column / spanCount
                outRect.right = height * (spanCount - 1 - column) / spanCount
                // 垂直方向：除最后一行外添加底部间距
                if (position < itemCount - spanCount + (itemCount % spanCount).let { if (it == 0) spanCount else it }) {
                    outRect.bottom = height
                }
            }
            else -> {
                if (position < itemCount - 1) {
                    outRect.bottom = height
                }
            }
        }
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        when (parent.layoutManager) {
            is GridLayoutManager -> drawGrid(c, parent)
            else -> drawLinear(c, parent)
        }
    }

    private fun drawLinear(c: Canvas, parent: RecyclerView) {
        val left = parent.paddingStart + paddingStart
        val right = parent.width - parent.paddingEnd - paddingEnd
        val childCount = parent.childCount
        for (i in 0 until childCount - 1) {
            val child = parent.getChildAt(i)
            val position = parent.getChildAdapterPosition(child)
            if (position == RecyclerView.NO_POSITION) continue
            val params = child.layoutParams as RecyclerView.LayoutParams
            val top = child.bottom + params.bottomMargin
            c.drawRect(left.toFloat(), top.toFloat(), right.toFloat(), (top + height).toFloat(), paint)
        }
    }

    private fun drawGrid(c: Canvas, parent: RecyclerView) {
        val childCount = parent.childCount
        for (i in 0 until childCount) {
            val child = parent.getChildAt(i)
            val position = parent.getChildAdapterPosition(child)
            if (position == RecyclerView.NO_POSITION) continue
            val params = child.layoutParams as RecyclerView.LayoutParams
            // 绘制底部
            val bottom = child.bottom + params.bottomMargin
            c.drawRect(
                child.left.toFloat(), bottom.toFloat(),
                child.right.toFloat(), (bottom + height).toFloat(), paint
            )
            // 绘制右侧
            val right = child.right + params.rightMargin
            c.drawRect(
                right.toFloat(), child.top.toFloat(),
                (right + height).toFloat(), child.bottom.toFloat(), paint
            )
        }
    }
}
