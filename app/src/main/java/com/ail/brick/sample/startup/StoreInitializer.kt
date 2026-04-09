package com.ail.brick.sample.startup

import android.content.Context
import com.ail.brick.store.BrickStore
import com.ail.brick.startup.AppInitializer
import com.ail.brick.startup.InitPriority

/**
 * MMKV 存储初始化器 — 正常优先级。
 */
class StoreInitializer : AppInitializer {
    override val name: String = "BrickStore"
    override val priority: InitPriority = InitPriority.NORMAL

    override fun onCreate(context: Context) {
        BrickStore.init(context)
    }
}
