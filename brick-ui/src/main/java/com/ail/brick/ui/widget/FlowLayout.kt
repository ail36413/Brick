package com.ail.brick.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import com.ail.brick.ui.R
import kotlin.math.max

/**
 * 流式布局（自动换行标签布局）。
 *
 * 子 View 从左到右排列，当前行剩余宽度不够时自动折行。
 * 适用于标签列表、搜索历史等场景。
 *
 * 支持设置最大行数限制（超出行数的子 View 不显示）和行内 gravity 对齐。
 *
 * ### XML 用法
 * ```xml
 * <com.ail.brick.ui.widget.FlowLayout
 *     android:layout_width="match_parent"
 *     android:layout_height="wrap_content"
 *     app:flow_maxLines="3"
 *     app:flow_gravity="center">
 *     <TextView ... />
 *     <TextView ... />
 * </com.ail.brick.ui.widget.FlowLayout>
 * ```
 *
 * | XML 属性 | 说明 | 默认值 |
 * |---|---|---|
 * | `flow_horizontalSpacing` | 子 View 水平间距 | 8dp |
 * | `flow_verticalSpacing` | 子 View 垂直间距（行间距） | 8dp |
 * | `flow_maxLines` | 最大行数（0 = 不限制） | 0 |
 * | `flow_gravity` | 行内对齐方式（start/center/end） | start |
 */
class FlowLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    private val defaultSpacing = (8 * resources.displayMetrics.density).toInt()

    /** 子 View 水平间距（px） */
    var horizontalSpacing: Int = defaultSpacing
        set(value) { field = value; requestLayout() }

    /** 子 View 垂直间距（px） */
    var verticalSpacing: Int = defaultSpacing
        set(value) { field = value; requestLayout() }

    /** 最大行数限制，0 表示不限制 */
    var maxLines: Int = 0
        set(value) { field = value; requestLayout() }

    /** 行内对齐方式，支持 [Gravity.START]、[Gravity.CENTER_HORIZONTAL]、[Gravity.END] */
    var flowGravity: Int = Gravity.START
        set(value) { field = value; requestLayout() }

    /** 记录每行的子 View 索引范围和行宽信息，用于 layout 阶段 gravity 计算 */
    private data class LineInfo(val startIndex: Int, val endIndex: Int, val lineWidth: Int, val lineHeight: Int)
    private val lines = mutableListOf<LineInfo>()

    init {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.FlowLayout)
        horizontalSpacing = ta.getDimensionPixelSize(R.styleable.FlowLayout_flow_horizontalSpacing, defaultSpacing)
        verticalSpacing = ta.getDimensionPixelSize(R.styleable.FlowLayout_flow_verticalSpacing, defaultSpacing)
        maxLines = ta.getInt(R.styleable.FlowLayout_flow_maxLines, 0)
        flowGravity = ta.getInt(R.styleable.FlowLayout_flow_gravity, Gravity.START)
        ta.recycle()
    }

    override fun generateLayoutParams(attrs: AttributeSet?): LayoutParams {
        return MarginLayoutParams(context, attrs)
    }

    override fun generateDefaultLayoutParams(): LayoutParams {
        return MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)

        var lineWidth = 0
        var lineHeight = 0
        var totalHeight = 0
        var maxWidth = 0
        var lineCount = 1
        var lineStartIndex = 0
        val maxLineWidth = widthSize - paddingStart - paddingEnd

        lines.clear()
        var breakDueToMaxLines = false

        // 收集可见子 View 索引
        val visibleIndices = mutableListOf<Int>()
        for (i in 0 until childCount) {
            if (getChildAt(i).visibility != GONE) visibleIndices.add(i)
        }

        for ((seq, i) in visibleIndices.withIndex()) {
            val child = getChildAt(i)
            measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0)
            val lp = child.layoutParams as MarginLayoutParams
            val cw = child.measuredWidth + lp.leftMargin + lp.rightMargin
            val ch = child.measuredHeight + lp.topMargin + lp.bottomMargin

            if (lineWidth + cw > maxLineWidth && lineWidth > 0) {
                // 当前行结束，记录行信息
                lines.add(LineInfo(lineStartIndex, seq - 1, lineWidth - horizontalSpacing, lineHeight))
                totalHeight += lineHeight + verticalSpacing
                maxWidth = max(maxWidth, lineWidth - horizontalSpacing)
                lineCount++

                // 检查最大行数限制
                if (maxLines > 0 && lineCount > maxLines) {
                    // 超出行数的子 View 不参与布局，回退多余的 verticalSpacing
                    totalHeight -= verticalSpacing
                    breakDueToMaxLines = true
                    break
                }

                lineWidth = cw + horizontalSpacing
                lineHeight = ch
                lineStartIndex = seq
            } else {
                lineWidth += cw + horizontalSpacing
                lineHeight = max(lineHeight, ch)
            }

            // 最后一个元素，记录最后一行
            if (seq == visibleIndices.size - 1) {
                lines.add(LineInfo(lineStartIndex, seq, lineWidth - horizontalSpacing, lineHeight))
            }
        }
        if (!breakDueToMaxLines) {
            totalHeight += lineHeight
        }
        maxWidth = max(maxWidth, lineWidth - horizontalSpacing)

        val finalWidth = if (widthMode == MeasureSpec.EXACTLY) widthSize else maxWidth + paddingStart + paddingEnd
        val finalHeight = totalHeight + paddingTop + paddingBottom
        setMeasuredDimension(finalWidth, resolveSize(finalHeight, heightMeasureSpec))
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val parentWidth = r - l - paddingStart - paddingEnd

        // 收集可见子 View
        val visibleChildren = mutableListOf<android.view.View>()
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility != GONE) visibleChildren.add(child)
        }

        var y = paddingTop

        for (line in lines) {
            val startOffset = when (flowGravity) {
                Gravity.END, Gravity.RIGHT -> parentWidth - line.lineWidth
                Gravity.CENTER_HORIZONTAL, Gravity.CENTER -> (parentWidth - line.lineWidth) / 2
                else -> 0
            }
            var x = paddingStart + startOffset

            for (seq in line.startIndex..line.endIndex.coerceAtMost(visibleChildren.size - 1)) {
                val child = visibleChildren[seq]
                val lp = child.layoutParams as MarginLayoutParams

                child.layout(
                    x + lp.leftMargin,
                    y + lp.topMargin,
                    x + lp.leftMargin + child.measuredWidth,
                    y + lp.topMargin + child.measuredHeight
                )
                x += child.measuredWidth + lp.leftMargin + lp.rightMargin + horizontalSpacing
            }

            y += line.lineHeight + verticalSpacing
        }
    }
}
