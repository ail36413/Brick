package com.ail.brick.sample

import android.app.Application
import com.ail.brick.log.BrickLogger
import com.ail.brick.sample.startup.BrickLogInitializer
import com.ail.brick.sample.startup.ImageInitializer
import com.ail.brick.sample.startup.LogInitializer
import com.ail.brick.sample.startup.StoreInitializer
import com.ail.brick.startup.BrickStartup
import dagger.hilt.android.HiltAndroidApp

/**
 * Brick 示例应用入口
 *
 * 使用 [BrickStartup] 按优先级分级初始化：
 * - **IMMEDIATELY**：[BrickLogInitializer]（基础日志）、[LogInitializer]（增强日志）
 * - **NORMAL**：[StoreInitializer]（MMKV）、[ImageInitializer]（图片加载）
 *
 * @see BrickStartup
 */
@HiltAndroidApp
class SampleApp : Application() {
    override fun onCreate() {
        super.onCreate()

        BrickStartup.init(this) {
            // IMMEDIATELY — 日志必须最先初始化
            add(BrickLogInitializer())
            add(LogInitializer())

            // NORMAL — 常规组件初始化
            add(StoreInitializer())
            add(ImageInitializer())

            // 监听初始化结果
            onResult { result ->
                BrickLogger.d { "Startup: ${result.name} [${result.priority}] ${result.costMillis}ms ${if (result.success) "✓" else "✗"}" }
            }
        }
    }
}
