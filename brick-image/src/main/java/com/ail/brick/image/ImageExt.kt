package com.ail.brick.image

import android.widget.ImageView
import coil.load
import coil.request.CachePolicy
import coil.request.Disposable
import coil.size.Scale
import coil.transform.CircleCropTransformation
import coil.transform.RoundedCornersTransformation
import coil.transform.Transformation

/**
 * [ImageView] 图片加载扩展函数集。
 *
 * 基于 Coil 提供链式 DSL 配置，自动应用 [BrickImage] 的全局占位图、
 * 错误图和缓存策略。
 *
 * ### 基本加载
 * ```kotlin
 * imageView.loadImage("https://example.com/photo.jpg")
 * ```
 *
 * ### 带配置
 * ```kotlin
 * imageView.loadImage("https://...") {
 *     placeholder(R.drawable.loading)
 *     circle()
 * }
 * ```
 *
 * ### 快捷方法
 * ```kotlin
 * imageView.loadCircle(url)
 * imageView.loadRounded(url, 12f)
 * ```
 */

/**
 * 加载图片，支持 URL / File / @DrawableRes / Uri 等。
 *
 * 返回 [Disposable]，可调用 [Disposable.dispose] 手动取消加载。
 * > Coil 已在 View detach / 新请求时自动取消，仅当需要主动控制时使用。
 *
 * @param data   图片数据源（null 时显示全局错误图）
 * @param config 可选的 DSL 配置块
 * @return [Disposable] 用于手动取消，data 为 null 时返回 `null`
 */
fun ImageView.loadImage(
    data: Any?,
    config: (ImageLoadConfig.() -> Unit)? = null
): Disposable? {
    if (data == null) {
        setImageResource(BrickImage.globalError)
        return null
    }
    val loadConfig = ImageLoadConfig().apply { config?.invoke(this) }

    return load(data) {
        // 占位图
        val ph = loadConfig.placeholderRes.takeIf { it != 0 } ?: BrickImage.globalPlaceholder
        if (ph != 0) placeholder(ph)

        val err = loadConfig.errorRes.takeIf { it != 0 } ?: BrickImage.globalError
        if (err != 0) error(err)

        // 缩放
        scale(loadConfig.scale)

        // 尺寸
        if (loadConfig.overrideWidth > 0 && loadConfig.overrideHeight > 0) {
            size(loadConfig.overrideWidth, loadConfig.overrideHeight)
        }

        // 变换（circle 和 roundedCorners 互斥，circle 优先）
        val transforms = mutableListOf<Transformation>()
        if (loadConfig.isCircle) {
            transforms.add(CircleCropTransformation())
        } else if (loadConfig.cornerRadius > 0f) {
            transforms.add(
                RoundedCornersTransformation(
                    loadConfig.cornerTopLeft.takeIf { it >= 0f } ?: loadConfig.cornerRadius,
                    loadConfig.cornerTopRight.takeIf { it >= 0f } ?: loadConfig.cornerRadius,
                    loadConfig.cornerBottomRight.takeIf { it >= 0f } ?: loadConfig.cornerRadius,
                    loadConfig.cornerBottomLeft.takeIf { it >= 0f } ?: loadConfig.cornerRadius,
                )
            )
        }
        loadConfig.customTransformations?.let { transforms.addAll(it) }
        if (transforms.isNotEmpty()) transformations(transforms)

        // 缓存策略
        if (!loadConfig.cacheEnabled) {
            memoryCachePolicy(CachePolicy.DISABLED)
            diskCachePolicy(CachePolicy.DISABLED)
        }

        // 淡入
        if (loadConfig.crossfadeDuration > 0) {
            crossfade(loadConfig.crossfadeDuration)
        } else {
            crossfade(loadConfig.crossfade)
        }

        // 监听
        val hasStart = loadConfig.onStart != null
        val hasSuccess = loadConfig.onSuccess != null
        val hasError = loadConfig.onError != null
        if (hasStart || hasSuccess || hasError) {
            listener(
                onStart = { if (hasStart) loadConfig.onStart!!.invoke() },
                onSuccess = { _, result -> if (hasSuccess) loadConfig.onSuccess!!.invoke(result) },
                onError = { _, result -> if (hasError) loadConfig.onError!!.invoke(result) },
            )
        }
    }
}

/**
 * 以圆形裁切方式加载图片。
 *
 * @param data 图片数据源
 * @return [Disposable] 用于手动取消
 */
fun ImageView.loadCircle(data: Any?): Disposable? {
    return loadImage(data) { circle() }
}

/**
 * 以指定像素圆角加载图片。
 *
 * @param data     图片数据源
 * @param radiusPx 圆角半径（px）
 * @return [Disposable] 用于手动取消
 */
fun ImageView.loadRounded(data: Any?, radiusPx: Float): Disposable? {
    require(radiusPx >= 0f) { "radiusPx must be >= 0, got $radiusPx" }
    return loadImage(data) { roundedCorners(radiusPx) }
}

/**
 * [loadImage] 的 DSL 配置类。
 *
 * 通过链式调用配置占位图、缩放、圆角、缓存等属性。
 */
class ImageLoadConfig {
    var placeholderRes: Int = 0
    var errorRes: Int = 0
    var scale: Scale = Scale.FIT
    var overrideWidth: Int = 0
    var overrideHeight: Int = 0
    var isCircle: Boolean = false
    var cornerRadius: Float = 0f
    var cornerTopLeft: Float = -1f
    var cornerTopRight: Float = -1f
    var cornerBottomRight: Float = -1f
    var cornerBottomLeft: Float = -1f
    var customTransformations: List<Transformation>? = null
    var cacheEnabled: Boolean = true
    var crossfade: Boolean = true
    var crossfadeDuration: Int = 0
    internal var onStart: (() -> Unit)? = null
    internal var onSuccess: ((coil.request.SuccessResult) -> Unit)? = null
    internal var onError: ((coil.request.ErrorResult) -> Unit)? = null

    /** 设置占位图资源 */
    fun placeholder(res: Int) { placeholderRes = res }
    /** 设置错误图资源 */
    fun error(res: Int) { errorRes = res }
    /** 启用圆形裁切（与 [roundedCorners] 互斥，后者会被忽略） */
    fun circle() { isCircle = true }
    /** 设置统一圆角半径（px），如果已调用 [circle] 则此设置会被忽略 */
    fun roundedCorners(radius: Float) { cornerRadius = radius }
    /** 分别设置四角圆角半径（px） */
    fun roundedCorners(topLeft: Float, topRight: Float, bottomRight: Float, bottomLeft: Float) {
        cornerTopLeft = topLeft; cornerTopRight = topRight
        cornerBottomRight = bottomRight; cornerBottomLeft = bottomLeft
        cornerRadius = maxOf(topLeft, topRight, bottomRight, bottomLeft)
    }
    /** 指定加载尺寸（px） */
    fun override(width: Int, height: Int) { overrideWidth = width; overrideHeight = height }
    /** 禁用内存和磁盘缓存 */
    fun noCache() { cacheEnabled = false }
    /** 添加自定义 [Transformation] */
    fun transform(vararg transformations: Transformation) { customTransformations = transformations.toList() }
    /** 设置渐入动画时长（ms），0 表示关闭 */
    fun crossfade(durationMs: Int) {
        crossfadeDuration = durationMs
        crossfade = durationMs > 0
    }

    /**
     * 设置加载状态监听器。
     *
     * ```kotlin
     * imageView.loadImage(url) {
     *     listener(
     *         onStart = { showProgress() },
     *         onSuccess = { result -> hideProgress() },
     *         onError = { result -> showRetry() }
     *     )
     * }
     * ```
     *
     * @param onStart   加载开始回调
     * @param onSuccess 加载成功回调
     * @param onError   加载失败回调
     */
    fun listener(
        onStart: (() -> Unit)? = null,
        onSuccess: ((coil.request.SuccessResult) -> Unit)? = null,
        onError: ((coil.request.ErrorResult) -> Unit)? = null
    ) {
        this.onStart = onStart
        this.onSuccess = onSuccess
        this.onError = onError
    }
}
