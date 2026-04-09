package com.ail.brick.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import com.ail.brick.ui.R

/**
 * 圆角裁切容器布局。
 *
 * 通过 [Canvas.clipPath] 实现圆角效果，支持统一圆角或四角独立配置。
 * 支持描边（stroke）效果，可设置描边颜色和宽度。
 *
 * ### XML 用法
 * ```xml
 * <com.ail.brick.ui.widget.RoundLayout
 *     android:layout_width="match_parent"
 *     android:layout_height="wrap_content"
 *     app:round_radius="12dp"
 *     app:round_strokeColor="#FF0000"
 *     app:round_strokeWidth="1dp">
 *     <!-- 子视图将被圆角裁切 -->
 * </com.ail.brick.ui.widget.RoundLayout>
 * ```
 *
 * | XML 属性 | 说明 | 默认值 |
 * |---|---|---|
 * | `round_radius` | 统一圆角半径 | 0 |
 * | `round_topLeftRadius` | 左上角半径 | round_radius |
 * | `round_topRightRadius` | 右上角半径 | round_radius |
 * | `round_bottomLeftRadius` | 左下角半径 | round_radius |
 * | `round_bottomRightRadius` | 右下角半径 | round_radius |
 * | `round_strokeColor` | 描边颜色 | 透明 |
 * | `round_strokeWidth` | 描边宽度 | 0 |
 */
class RoundLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val clipPath = Path()
    private val strokePath = Path()
    private val radii = FloatArray(8)
    private var pathDirty = true

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val strokeRect = RectF()

    /** 描边颜色，默认透明（不绘制） */
    @ColorInt
    var strokeColor: Int = Color.TRANSPARENT
        set(value) {
            field = value
            strokePaint.color = value
            invalidate()
        }

    /** 描边宽度（px），默认 0（不绘制） */
    var strokeWidth: Float = 0f
        set(value) {
            field = value
            strokePaint.strokeWidth = value
            invalidate()
        }

    init {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.RoundLayout)
        val allRadius = ta.getDimension(R.styleable.RoundLayout_round_radius, 0f)
        val tl = ta.getDimension(R.styleable.RoundLayout_round_topLeftRadius, allRadius)
        val tr = ta.getDimension(R.styleable.RoundLayout_round_topRightRadius, allRadius)
        val bl = ta.getDimension(R.styleable.RoundLayout_round_bottomLeftRadius, allRadius)
        val br = ta.getDimension(R.styleable.RoundLayout_round_bottomRightRadius, allRadius)
        strokeColor = ta.getColor(R.styleable.RoundLayout_round_strokeColor, Color.TRANSPARENT)
        strokeWidth = ta.getDimension(R.styleable.RoundLayout_round_strokeWidth, 0f)
        ta.recycle()

        setRadii(tl, tr, br, bl)
    }

    /**
     * 分别设置四个角的圆角半径（px）。
     *
     * @param topLeft     左上角半径
     * @param topRight    右上角半径
     * @param bottomRight 右下角半径
     * @param bottomLeft  左下角半径
     */
    fun setRadii(topLeft: Float, topRight: Float, bottomRight: Float, bottomLeft: Float) {
        radii[0] = topLeft; radii[1] = topLeft
        radii[2] = topRight; radii[3] = topRight
        radii[4] = bottomRight; radii[5] = bottomRight
        radii[6] = bottomLeft; radii[7] = bottomLeft
        pathDirty = true
        invalidate()
    }

    /**
     * 设置统一圆角半径（四角相同）。
     *
     * @param radius 圆角半径（px）
     */
    fun setRadius(radius: Float) {
        setRadii(radius, radius, radius, radius)
    }

    /**
     * 设置描边样式。
     *
     * @param color 描边颜色
     * @param width 描边宽度（px）
     */
    fun setStroke(@ColorInt color: Int, width: Float) {
        strokeColor = color
        strokeWidth = width
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        pathDirty = true
    }

    override fun dispatchDraw(canvas: Canvas) {
        if (pathDirty) {
            clipPath.reset()
            clipPath.addRoundRect(
                RectF(0f, 0f, width.toFloat(), height.toFloat()),
                radii,
                Path.Direction.CW
            )
            pathDirty = false
        }
        canvas.save()
        canvas.clipPath(clipPath)
        super.dispatchDraw(canvas)
        canvas.restore()

        // 描边绘制在子 View 之上，使用 Path 支持四角独立圆角
        if (strokeWidth > 0f && strokeColor != Color.TRANSPARENT) {
            val half = strokeWidth / 2f
            strokeRect.set(half, half, width - half, height - half)
            strokePath.reset()
            strokePath.addRoundRect(strokeRect, radii, Path.Direction.CW)
            canvas.drawPath(strokePath, strokePaint)
        }
    }
}
