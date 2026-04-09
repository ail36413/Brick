package com.ail.brick.utils

import android.content.Context
import android.content.SharedPreferences
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * SharedPreferences 属性委托工具，以 Kotlin 属性语法读写 SP。
 *
 * ### 定义
 * ```kotlin
 * object AppPrefs : SpDelegate("app_prefs") {
 *     var token by string("token", "")
 *     var userId by int("user_id", 0)
 *     var isFirstLaunch by boolean("first_launch", true)
 * }
 * ```
 *
 * ### 初始化（Application.onCreate 中调用一次）
 * ```kotlin
 * AppPrefs.init(applicationContext)
 * ```
 *
 * ### 读写
 * ```kotlin
 * AppPrefs.token = "abc123"
 * val t = AppPrefs.token   // "abc123"
 * ```
 *
 * > **注意**：未调用 [init] 就访问属性会抛出 [IllegalStateException]。
 *
 * @param name SharedPreferences 文件名
 */
open class SpDelegate(private val name: String) {

    @Volatile
    private lateinit var sp: SharedPreferences

    /** 是否已初始化 */
    val isInitialized: Boolean get() = ::sp.isInitialized

    /**
     * 初始化 SharedPreferences，必须在访问任何属性前调用一次。
     *
     * 推荐在 `Application.onCreate()` 中调用。
     *
     * @param context 任意 Context（内部自动取 applicationContext）
     */
    fun init(context: Context) {
        sp = context.applicationContext.getSharedPreferences(name, Context.MODE_PRIVATE)
    }

    /** 确保已初始化，否则抛出可读性更好的异常 */
    private fun ensureInitialized() {
        check(::sp.isInitialized) {
            "SpDelegate '$name' 尚未初始化，请先调用 init(context)"
        }
    }

    /**
     * 清空当前 SP 文件中的所有键值对。
     */
    fun clear() {
        ensureInitialized()
        sp.edit().clear().apply()
    }

    /**
     * 删除指定 [key] 对应的键值对。
     *
     * @param key 要删除的键名
     */
    fun remove(key: String) {
        ensureInitialized()
        sp.edit().remove(key).apply()
    }

    // ==================== 类型委托工厂 ====================

    /**
     * String 类型属性委托。
     *
     * @param key SP 键名
     * @param default 默认值，默认为空字符串
     */
    fun string(key: String, default: String = "") = object : ReadWriteProperty<Any?, String> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): String {
            ensureInitialized()
            return sp.getString(key, default) ?: default
        }
        override fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
            ensureInitialized()
            sp.edit().putString(key, value).apply()
        }
    }

    /**
     * Int 类型属性委托。
     *
     * @param key SP 键名
     * @param default 默认值，默认为 0
     */
    fun int(key: String, default: Int = 0) = object : ReadWriteProperty<Any?, Int> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): Int {
            ensureInitialized()
            return sp.getInt(key, default)
        }
        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
            ensureInitialized()
            sp.edit().putInt(key, value).apply()
        }
    }

    /**
     * Long 类型属性委托。
     *
     * @param key SP 键名
     * @param default 默认值，默认为 0L
     */
    fun long(key: String, default: Long = 0L) = object : ReadWriteProperty<Any?, Long> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): Long {
            ensureInitialized()
            return sp.getLong(key, default)
        }
        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Long) {
            ensureInitialized()
            sp.edit().putLong(key, value).apply()
        }
    }

    /**
     * Float 类型属性委托。
     *
     * @param key SP 键名
     * @param default 默认值，默认为 0f
     */
    fun float(key: String, default: Float = 0f) = object : ReadWriteProperty<Any?, Float> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): Float {
            ensureInitialized()
            return sp.getFloat(key, default)
        }
        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Float) {
            ensureInitialized()
            sp.edit().putFloat(key, value).apply()
        }
    }

    /**
     * Boolean 类型属性委托。
     *
     * @param key SP 键名
     * @param default 默认值，默认为 false
     */
    fun boolean(key: String, default: Boolean = false) = object : ReadWriteProperty<Any?, Boolean> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): Boolean {
            ensureInitialized()
            return sp.getBoolean(key, default)
        }
        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) {
            ensureInitialized()
            sp.edit().putBoolean(key, value).apply()
        }
    }

    /**
     * Set<String> 类型属性委托。
     *
     * @param key SP 键名
     * @param default 默认值，默认为空集合
     */
    fun stringSet(key: String, default: Set<String> = emptySet()) = object : ReadWriteProperty<Any?, Set<String>> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): Set<String> {
            ensureInitialized()
            return sp.getStringSet(key, default) ?: default
        }
        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Set<String>) {
            ensureInitialized()
            sp.edit().putStringSet(key, value).apply()
        }
    }
}
