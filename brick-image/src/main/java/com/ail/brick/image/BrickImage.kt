package com.ail.brick.image

import android.content.Context
import android.os.Build
import coil.Coil
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache

/**
 * Brick 图片加载库全局配置入口。
 *
 * 基于 [Coil] 封装，提供统一的缓存配置、GIF 支持和全局占位图。
 *
 * ### 零配置使用
 * 无需调用 [init]，所有加载 API 均可直接使用（采用 Coil 默认 ImageLoader）：
 * ```kotlin
 * imageView.loadImage("https://example.com/photo.jpg")
 * ```
 *
 * ### 自定义配置（可选，推荐在 Application.onCreate）
 * ```kotlin
 * BrickImage.init(this) {
 *     memoryCacheSize(0.25)               // 25% 可用内存
 *     diskCacheSize(100L * 1024 * 1024)   // 100MB 磁盘缓存
 *     enableGif(true)
 *     placeholder(R.drawable.placeholder)
 *     error(R.drawable.error)
 * }
 * ```
 *
 * ### 缓存管理
 * ```kotlin
 * BrickImage.clearMemoryCache(context)
 * BrickImage.clearDiskCache(context)
 * ```
 */
object BrickImage {

    /** 全局占位图资源 ID（0 表示不设置） */
    internal var globalPlaceholder: Int = 0
        private set

    /** 全局错误图资源 ID（0 表示不设置） */
    internal var globalError: Int = 0
        private set

    /** 是否已显式调用 [init] */
    private var initialized = false

    /**
     * 初始化图片加载引擎（可选）。
     *
     * 如果不调用此方法，库将使用 Coil 的默认 ImageLoader，
     * 所有加载 API 仍可正常工作，但不享受自定义缓存大小、GIF、全局占位图等配置。
     *
     * 重复调用会覆盖之前的配置。
     *
     * @param context 任意 Context（内部自动取 applicationContext）
     * @param config  可选的 DSL 配置块
     */
    fun init(context: Context, config: (ImageConfig.() -> Unit)? = null) {
        val appContext = context.applicationContext
        val imageConfig = ImageConfig().apply { config?.invoke(this) }

        val builder = ImageLoader.Builder(appContext)
            .crossfade(imageConfig.crossfadeEnabled)
            .crossfade(imageConfig.crossfadeDuration)

        builder.memoryCache {
            MemoryCache.Builder(appContext)
                .maxSizePercent(imageConfig.memoryCachePercent)
                .build()
        }

        builder.diskCache {
            DiskCache.Builder()
                .directory(appContext.cacheDir.resolve("brick_image_cache"))
                .maxSizeBytes(imageConfig.diskCacheSize)
                .build()
        }

        if (imageConfig.gifEnabled) {
            builder.components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
        }

        globalPlaceholder = imageConfig.placeholderRes
        globalError = imageConfig.errorRes

        Coil.setImageLoader(builder.build())
        initialized = true
    }

    /** 是否已显式初始化 */
    val isInitialized: Boolean get() = initialized

    /** 获取当前全局 [ImageLoader] 实例 */
    fun imageLoader(context: Context): ImageLoader = Coil.imageLoader(context)

    /** 清除内存缓存 */
    fun clearMemoryCache(context: Context) {
        imageLoader(context).memoryCache?.clear()
    }

    /** 清除磁盘缓存 */
    @OptIn(coil.annotation.ExperimentalCoilApi::class)
    fun clearDiskCache(context: Context) {
        imageLoader(context).diskCache?.clear()
    }

    /**
     * 图片加载 DSL 配置。
     *
     * | 属性 | 说明 | 默认值 |
     * |---|---|---|
     * | memoryCachePercent | 内存缓存占可用内存比例 | 0.25 |
     * | diskCacheSize | 磁盘缓存上限（字节） | 100MB |
     * | crossfadeEnabled | 是否开启渐入动画 | true |
     * | crossfadeDuration | 渐入动画时长（ms） | 200 |
     * | gifEnabled | 是否支持 GIF | true |
     * | placeholderRes | 全局占位图资源 | 0（不设置） |
     * | errorRes | 全局错误图资源 | 0（不设置） |
     */
    class ImageConfig {
        var memoryCachePercent: Double = 0.25
        var diskCacheSize: Long = 100L * 1024 * 1024
        var crossfadeEnabled: Boolean = true
        var crossfadeDuration: Int = 200
        var gifEnabled: Boolean = true
        var placeholderRes: Int = 0
        var errorRes: Int = 0

        fun memoryCacheSize(percent: Double) { memoryCachePercent = percent.coerceIn(0.05, 0.5) }
        fun diskCacheSize(bytes: Long) { diskCacheSize = bytes.coerceAtLeast(0) }
        fun enableGif(enabled: Boolean) { gifEnabled = enabled }
        fun placeholder(res: Int) { placeholderRes = res }
        fun error(res: Int) { errorRes = res }
    }
}
