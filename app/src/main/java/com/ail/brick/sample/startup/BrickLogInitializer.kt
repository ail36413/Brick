package com.ail.brick.sample.startup

import android.content.Context
import com.ail.brick.utils.BrickLog
import com.ail.brick.startup.AppInitializer
import com.ail.brick.startup.InitPriority

/**
 * 基础日志初始化器 — 最高优先级，和 BrickLogger 同级。
 */
class BrickLogInitializer : AppInitializer {
    override val name: String = "BrickLog"
    override val priority: InitPriority = InitPriority.IMMEDIATELY

    override fun onCreate(context: Context) {
        BrickLog.init(isDebug = true, prefix = "Brick")
    }
}
