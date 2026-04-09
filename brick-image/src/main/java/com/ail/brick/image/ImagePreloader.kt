package com.ail.brick.image

import android.content.Context
import android.graphics.drawable.Drawable
import coil.Coil
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/**
 * 图片预加载工具，在协程中提前将图片缓存到内存/磁盘。
 *
 * ### 单张预加载
 * ```kotlin
 * lifecycleScope.launch {
 *     ImagePreloader.preload(context, "https://example.com/big.jpg")
 * }
 * ```
 *
 * ### 获取 Drawable
 * ```kotlin
 * val drawable = ImagePreloader.get(context, url)
 * imageView.setImageDrawable(drawable)
 * ```
 *
 * ### 批量预加载
 * ```kotlin
 * ImagePreloader.preloadAll(context, imageUrls)
 * ```
 */
object ImagePreloader {

    /**
     * 预加载图片到缓存（不返回 Drawable）。
     *
     * 使用 [execute] 而非 [enqueue] 以确保协程完成前图片已缓存。
     * 内部捕获异常，加载失败时返回 `false` 而非抛出异常。
     *
     * @param context 上下文
     * @param data    图片数据源（URL / File / Uri 等）
     * @return 预加载成功返回 `true`，失败返回 `false`
     */
    suspend fun preload(context: Context, data: Any): Boolean {
        return withContext(Dispatchers.IO) {
            runCatching {
                val request = ImageRequest.Builder(context)
                    .data(data)
                    .build()
                Coil.imageLoader(context).execute(request)
            }.isSuccess
        }
    }

    /**
     * 预加载并返回 [Drawable]。
     *
     * 使用 `allowHardware(false)` 以保证返回的 Bitmap 可被 Canvas 使用。
     * 内部捕获异常，加载失败或发生异常时均返回 `null`。
     *
     * @param context 上下文
     * @param data    图片数据源
     * @return 加载成功返回 Drawable，失败返回 `null`
     */
    suspend fun get(context: Context, data: Any): Drawable? {
        return withContext(Dispatchers.IO) {
            runCatching {
                val request = ImageRequest.Builder(context)
                    .data(data)
                    .allowHardware(false)
                    .build()
                val result = Coil.imageLoader(context).execute(request)
                (result as? SuccessResult)?.drawable
            }.getOrNull()
        }
    }

    /**
     * 批量并行预加载多张图片。
     *
     * 使用 [coroutineScope] + [async] 并发执行，单张失败不影响其他图片。
     *
     * @param context 上下文
     * @param urls    图片数据源列表
     */
    suspend fun preloadAll(context: Context, urls: List<Any>) {
        coroutineScope {
            urls.map { url ->
                async {
                    runCatching { preload(context, url) }
                }
            }.awaitAll()
        }
    }
}
