package com.ail.brick.image

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.PorterDuff
import coil.size.Size
import coil.transform.Transformation
import kotlin.math.min

/**
 * 灰度变换——将图片转为黑白灰度效果。
 *
 * ```kotlin
 * imageView.loadImage(url) {
 *     transform(GrayscaleTransformation())
 * }
 * ```
 */
class GrayscaleTransformation : Transformation {
    override val cacheKey = "brick_grayscale"
    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
        }
        val config = input.config ?: Bitmap.Config.ARGB_8888
        val output = Bitmap.createBitmap(input.width, input.height, config)
        Canvas(output).drawBitmap(input, 0f, 0f, paint)
        return output
    }
}

/**
 * 颜色滤镜变换——在图片上叠加指定颜色。
 *
 * 使用 [PorterDuff.Mode.SRC_ATOP] 混合模式。
 *
 * ```kotlin
 * imageView.loadImage(url) {
 *     transform(ColorFilterTransformation(0x80FF0000.toInt()))
 * }
 * ```
 *
 * @param color 滤镜颜色（支持 alpha 通道）
 */
class ColorFilterTransformation(private val color: Int) : Transformation {
    override val cacheKey = "brick_color_filter_${Integer.toHexString(color)}"
    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val config = input.config ?: Bitmap.Config.ARGB_8888
        val output = Bitmap.createBitmap(input.width, input.height, config)
        val canvas = Canvas(output)
        // 先绘制原图，再用 SRC_ATOP 叠加滤镜色（使滤镜在不透明像素上混合）
        canvas.drawBitmap(input, 0f, 0f, null)
        canvas.drawColor(color, PorterDuff.Mode.SRC_ATOP)
        return output
    }
}

/**
 * 边框变换——为图片绘制矩形边框。
 *
 * ```kotlin
 * imageView.loadImage(url) {
 *     transform(BorderTransformation(4f, Color.RED))
 * }
 * ```
 *
 * @param borderWidth 边框宽度（px）
 * @param borderColor 边框颜色
 */
class BorderTransformation(
    private val borderWidth: Float,
    private val borderColor: Int
) : Transformation {

    init {
        require(borderWidth > 0f) { "borderWidth must be > 0, got $borderWidth" }
    }

    override val cacheKey = "brick_border_${borderWidth}_${Integer.toHexString(borderColor)}"
    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val config = input.config ?: Bitmap.Config.ARGB_8888
        val output = Bitmap.createBitmap(input.width, input.height, config)
        val canvas = Canvas(output)
        canvas.drawBitmap(input, 0f, 0f, null)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = borderColor
            style = Paint.Style.STROKE
            strokeWidth = borderWidth
        }
        val half = borderWidth / 2f
        canvas.drawRect(half, half, input.width - half, input.height - half, paint)
        return output
    }
}

/**
 * 模糊变换——对图片施加高斯模糊效果。
 *
 * 采用"缩小→模糊→放大"策略，兼顾性能与效果：
 * 1. 先将图片缩小到 [sampling] 比例（默认 1/4），大幅降低像素处理量
 * 2. 对缩小后的 Bitmap 执行 StackBlur 算法（O(w*h)，与 radius 无关）
 * 3. 再放大回原始尺寸，缩放本身提供额外平滑效果
 *
 * ```kotlin
 * imageView.loadImage(url) {
 *     transform(BlurTransformation())           // 默认 radius=15, sampling=4
 *     transform(BlurTransformation(25, 2))      // 更强模糊，较少采样
 * }
 * ```
 *
 * @param radius   模糊半径（1~25），值越大越模糊
 * @param sampling 采样因子（≥1），值越大处理越快但细节越少
 */
class BlurTransformation(
    private val radius: Int = 15,
    private val sampling: Int = 4
) : Transformation {

    init {
        require(radius in 1..25) { "blur radius must be in 1..25, got $radius" }
        require(sampling >= 1) { "sampling must be >= 1, got $sampling" }
    }

    override val cacheKey = "brick_blur_${radius}_$sampling"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val scaledWidth = (input.width / sampling).coerceAtLeast(1)
        val scaledHeight = (input.height / sampling).coerceAtLeast(1)

        // 1. 缩小（双线性插值已提供初步模糊）
        val scaledBitmap = Bitmap.createScaledBitmap(input, scaledWidth, scaledHeight, true)

        // 2. StackBlur
        val blurred = StackBlur.blur(scaledBitmap, radius)

        // 3. 放大回原始尺寸
        if (blurred.width == input.width && blurred.height == input.height) return blurred
        val output = Bitmap.createScaledBitmap(blurred, input.width, input.height, true)
        if (output !== blurred) blurred.recycle()
        return output
    }
}

/**
 * StackBlur 算法 — 高性能 O(w*h) 模糊，与 radius 无关。
 *
 * 基于 Mario Klingemann 的 StackBlur 算法实现。
 * 使用滑动窗口累加器，水平+垂直两趟扫描完成模糊。
 *
 * @see <a href="http://www.quasimondo.com/StackBlurForCanvas/StackBlurDemo.html">StackBlur</a>
 */
internal object StackBlur {

    fun blur(bitmap: Bitmap, radius: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        val div = 2 * radius + 1
        val divSum = (div + 1) shr 1
        val divSumSq = divSum * divSum

        val ch = Array(4) { IntArray(w * h) } // RGBA channels
        val vMin = IntArray(maxOf(w, h))
        val stack = Array(div) { IntArray(4) }

        // 水平扫描
        for (y in 0 until h) {
            horizontalPass(pixels, ch, stack, vMin, w, h, y, radius, div, divSumSq)
        }

        // 垂直扫描（读中间通道、写回 pixels）
        for (x in 0 until w) {
            verticalPass(pixels, ch, stack, vMin, w, h, x, radius, div, divSumSq)
        }

        val config = bitmap.config ?: Bitmap.Config.ARGB_8888
        return Bitmap.createBitmap(w, h, config).also {
            it.setPixels(pixels, 0, w, 0, 0, w, h)
        }
    }

    private fun horizontalPass(
        pixels: IntArray, ch: Array<IntArray>, stack: Array<IntArray>,
        vMin: IntArray, w: Int, h: Int, y: Int,
        radius: Int, div: Int, divSumSq: Int,
    ) {
        val sum = IntArray(4)
        val inSum = IntArray(4)
        val outSum = IntArray(4)

        for (i in -radius..radius) {
            val p = pixels[y * w + min(w - 1, maxOf(i, 0))]
            val sir = stack[i + radius]
            sir[0] = (p shr 16) and 0xff
            sir[1] = (p shr 8) and 0xff
            sir[2] = p and 0xff
            sir[3] = (p ushr 24) and 0xff
            val weight = radius + 1 - kotlin.math.abs(i)
            for (c in 0..3) sum[c] += sir[c] * weight
            if (i > 0) for (c in 0..3) inSum[c] += sir[c]
            else for (c in 0..3) outSum[c] += sir[c]
        }

        var sp = radius
        for (x in 0 until w) {
            val idx = y * w + x
            for (c in 0..3) ch[c][idx] = (sum[c] / divSumSq).coerceIn(0, 255)
            for (c in 0..3) sum[c] -= outSum[c]

            val si = (sp - radius + div) % div
            val sir = stack[si]
            for (c in 0..3) outSum[c] -= sir[c]

            if (y == 0) vMin[x] = min(x + radius + 1, w - 1)
            val p = pixels[y * w + vMin[x]]
            sir[0] = (p shr 16) and 0xff; sir[1] = (p shr 8) and 0xff
            sir[2] = p and 0xff; sir[3] = (p ushr 24) and 0xff
            for (c in 0..3) inSum[c] += sir[c]
            for (c in 0..3) sum[c] += inSum[c]

            sp = (sp + 1) % div
            val sir2 = stack[sp]
            for (c in 0..3) outSum[c] += sir2[c]
            for (c in 0..3) inSum[c] -= sir2[c]
        }
    }

    private fun verticalPass(
        pixels: IntArray, ch: Array<IntArray>, stack: Array<IntArray>,
        vMin: IntArray, w: Int, h: Int, x: Int,
        radius: Int, div: Int, divSumSq: Int,
    ) {
        val sum = IntArray(4)
        val inSum = IntArray(4)
        val outSum = IntArray(4)

        for (i in -radius..radius) {
            val yp = min(h - 1, maxOf(i, 0))
            val idx = yp * w + x
            val sir = stack[i + radius]
            for (c in 0..3) sir[c] = ch[c][idx]
            val weight = radius + 1 - kotlin.math.abs(i)
            for (c in 0..3) sum[c] += sir[c] * weight
            if (i > 0) for (c in 0..3) inSum[c] += sir[c]
            else for (c in 0..3) outSum[c] += sir[c]
        }

        var sp = radius
        for (y in 0 until h) {
            val idx = y * w + x
            pixels[idx] = (ch[3][idx].coerceIn(0, 255) shl 24) or
                    ((sum[0] / divSumSq).coerceIn(0, 255) shl 16) or
                    ((sum[1] / divSumSq).coerceIn(0, 255) shl 8) or
                    (sum[2] / divSumSq).coerceIn(0, 255)

            for (c in 0..3) sum[c] -= outSum[c]
            val si = (sp - radius + div) % div
            val sir = stack[si]
            for (c in 0..3) outSum[c] -= sir[c]

            if (x == 0) vMin[y] = min(y + radius + 1, h - 1)
            val idx2 = vMin[y] * w + x
            for (c in 0..3) sir[c] = ch[c][idx2]
            for (c in 0..3) inSum[c] += sir[c]
            for (c in 0..3) sum[c] += inSum[c]

            sp = (sp + 1) % div
            val sir2 = stack[sp]
            for (c in 0..3) outSum[c] += sir2[c]
            for (c in 0..3) inSum[c] -= sir2[c]
        }
    }
}
