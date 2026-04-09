package com.ail.brick.sample.startup

import android.content.Context
import com.ail.brick.log.BrickLogger
import com.ail.brick.startup.AppInitializer
import com.ail.brick.startup.InitPriority

/**
 * 日志初始化器 — 最高优先级，确保其他初始化器可以使用日志。
 */
class LogInitializer : AppInitializer {
    override val name: String = "BrickLogger"
    override val priority: InitPriority = InitPriority.IMMEDIATELY

    override fun onCreate(context: Context) {
        BrickLogger.init {
            debug = true
            fileLog = true
            fileDir = "${context.cacheDir.absolutePath}/logs"
            maxFileSize = 5L * 1024 * 1024
            maxFileCount = 10
            crashLog = true
        }
    }
}
