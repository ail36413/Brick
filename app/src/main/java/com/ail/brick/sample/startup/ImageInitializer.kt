package com.ail.brick.sample.startup

import android.content.Context
import com.ail.brick.image.BrickImage
import com.ail.brick.startup.AppInitializer
import com.ail.brick.startup.InitPriority

/**
 * 图片加载初始化器 — 正常优先级，依赖日志先初始化。
 */
class ImageInitializer : AppInitializer {
    override val name: String = "BrickImage"
    override val priority: InitPriority = InitPriority.NORMAL
    override val dependencies: List<String> = listOf("BrickLogger")

    override fun onCreate(context: Context) {
        BrickImage.init(context) {
            diskCacheSize(128L * 1024 * 1024)
        }
    }
}
