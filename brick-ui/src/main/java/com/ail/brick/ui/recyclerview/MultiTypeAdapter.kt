package com.ail.brick.ui.recyclerview

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

/**
 * 多类型 RecyclerView 适配器。
 *
 * 基于 ViewBinding 实现，通过 [register] 注册每种数据类型的创建与绑定逻辑，
 * 调用 [submitList] 更新数据时自动进行 DiffUtil 差量计算（需提供 [itemDiff]）。
 *
 * ### 线程要求
 * [submitList] 和 [register] 必须在**主线程**调用（RecyclerView 适配器约束）。
 * [register] 应在 [submitList] 之前完成，否则可能出现未注册类型异常。
 *
 * ### 基本用法
 * ```kotlin
 * val adapter = MultiTypeAdapter(
 *     itemDiff = { old, new -> old.id == new.id },
 *     contentDiff = { old, new -> old == new }
 * )
 *
 * adapter.register(TextItem::class, { parent ->
 *     ItemTextBinding.inflate(LayoutInflater.from(parent.context), parent, false)
 * }) { binding, item, _ ->
 *     (binding as ItemTextBinding).tvText.text = (item as TextItem).text
 * }
 * ```
 *
 * ### DSL 用法
 * ```kotlin
 * val adapter = multiTypeAdapter {
 *     itemDiff { old, new -> old.id == new.id }
 *     contentDiff { old, new -> old == new }
 *
 *     register<TextItem, ItemTextBinding>(ItemTextBinding::inflate) { binding, item, _ ->
 *         binding.tvText.text = item.text
 *     }
 *     register<ImageItem, ItemImageBinding>(ItemImageBinding::inflate) { binding, item, _ ->
 *         binding.ivImage.load(item.url)
 *     }
 * }
 * ```
 *
 * @param itemDiff    用于判断两个 item 是否为同一条目（如比较 id），
 *                    如未提供则 [submitList] 退化为 [notifyDataSetChanged]
 * @param contentDiff 用于判断两个相同 item 的内容是否发生变化（如比较全部字段），
 *                    如未提供则退化为使用 [itemDiff] 判断
 */
class MultiTypeAdapter(
    private val itemDiff: ((old: Any, new: Any) -> Boolean)? = null,
    private val contentDiff: ((old: Any, new: Any) -> Boolean)? = null
) : RecyclerView.Adapter<MultiTypeAdapter.BindingHolder>() {

    private val items = mutableListOf<Any>()
    private val typeMap = LinkedHashMap<Class<*>, Int>()
    private val creators = mutableListOf<(ViewGroup) -> ViewBinding>()
    private val binders = mutableListOf<(ViewBinding, Any, Int) -> Unit>()

    /** ViewBinding 持有的 ViewHolder */
    class BindingHolder(val binding: ViewBinding) : RecyclerView.ViewHolder(binding.root)

    /**
     * 注册一种数据类型的布局创建器与数据绑定器。
     *
     * @param T      数据类型
     * @param clazz  数据类型的 KClass
     * @param create ViewBinding 工厂，负责 inflate 布局
     * @param bind   数据绑定回调（binding, item, position）
     */
    fun <T : Any> register(
        clazz: kotlin.reflect.KClass<T>,
        create: (ViewGroup) -> ViewBinding,
        bind: (ViewBinding, Any, Int) -> Unit
    ) {
        val viewType = creators.size
        typeMap[clazz.java] = viewType
        creators.add(create)
        binders.add(bind)
    }

    /**
     * 提交新数据列表。
     *
     * 如果构造时提供了 [itemDiff]，则使用 [DiffUtil] 进行差量更新；
     * 否则退化为全量刷新 ([notifyDataSetChanged])。
     *
     * @param newItems 新的数据列表
     */
    @Suppress("NotifyDataSetChanged") // Fallback when no DiffUtil itemDiff is provided
    fun submitList(newItems: List<Any>) {
        if (itemDiff != null) {
            val oldItems = ArrayList(items)
            items.clear()
            items.addAll(newItems)
            DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun getOldListSize() = oldItems.size
                override fun getNewListSize() = newItems.size
                override fun areItemsTheSame(oldPos: Int, newPos: Int) =
                    oldItems[oldPos]::class == newItems[newPos]::class &&
                            itemDiff.invoke(oldItems[oldPos], newItems[newPos])
                override fun areContentsTheSame(oldPos: Int, newPos: Int) =
                    (contentDiff ?: itemDiff)!!.invoke(oldItems[oldPos], newItems[newPos])
            }).dispatchUpdatesTo(this)
        } else {
            items.clear()
            items.addAll(newItems)
            notifyDataSetChanged()
        }
    }

    /** 获取指定位置的数据项 */
    fun getItem(position: Int): Any = items[position]

    override fun getItemViewType(position: Int): Int {
        val item = items[position]
        return typeMap[item::class.java]
            ?: throw IllegalStateException("未注册类型: ${item::class.java.simpleName}")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BindingHolder {
        return BindingHolder(creators[viewType](parent))
    }

    override fun onBindViewHolder(holder: BindingHolder, position: Int) {
        binders[holder.itemViewType](holder.binding, items[position], position)
    }

    override fun getItemCount() = items.size
}

/**
 * MultiTypeAdapter 的 DSL 构建器。
 *
 * ```kotlin
 * val adapter = multiTypeAdapter {
 *     itemDiff { old, new -> (old as? HasId)?.id == (new as? HasId)?.id }
 *     contentDiff { old, new -> old == new }
 *
 *     register<TextItem, ItemTextBinding>(ItemTextBinding::inflate) { binding, item, _ ->
 *         binding.tvText.text = item.text
 *     }
 * }
 * ```
 */
class MultiTypeAdapterBuilder {
    private var itemDiff: ((Any, Any) -> Boolean)? = null
    private var contentDiff: ((Any, Any) -> Boolean)? = null

    @PublishedApi
    internal data class Registration(
        val clazz: kotlin.reflect.KClass<*>,
        val create: (ViewGroup) -> ViewBinding,
        val bind: (ViewBinding, Any, Int) -> Unit
    )

    @PublishedApi
    internal val registrations = mutableListOf<Registration>()

    /** 设置 item 相同性判断 */
    fun itemDiff(block: (old: Any, new: Any) -> Boolean) { itemDiff = block }

    /** 设置内容相同性判断 */
    fun contentDiff(block: (old: Any, new: Any) -> Boolean) { contentDiff = block }

    /**
     * 注册一种数据类型，使用 ViewBinding inflate 方法引用。
     *
     * @param VB   ViewBinding 类型
     * @param T    数据类型
     * @param inflate ViewBinding 的 inflate 函数引用（如 `ItemTextBinding::inflate`）
     * @param bind 数据绑定回调，参数已自动转换为具体类型
     */
    inline fun <reified T : Any, reified VB : ViewBinding> register(
        noinline inflate: (LayoutInflater, ViewGroup, Boolean) -> VB,
        noinline bind: (VB, T, Int) -> Unit
    ) {
        registrations.add(Registration(
            clazz = T::class,
            create = { parent -> inflate(LayoutInflater.from(parent.context), parent, false) },
            bind = { binding, item, pos ->
                @Suppress("UNCHECKED_CAST") // Safe: reified T/VB types guarantee correct cast at registration site
                bind(binding as VB, item as T, pos)
            }
        ))
    }

    fun build(): MultiTypeAdapter {
        val adapter = MultiTypeAdapter(itemDiff, contentDiff)
        registrations.forEach { reg ->
            adapter.register(reg.clazz, reg.create, reg.bind)
        }
        return adapter
    }
}

/**
 * 使用 DSL 构建 [MultiTypeAdapter]。
 *
 * @see MultiTypeAdapterBuilder
 */
fun multiTypeAdapter(block: MultiTypeAdapterBuilder.() -> Unit): MultiTypeAdapter {
    return MultiTypeAdapterBuilder().apply(block).build()
}
