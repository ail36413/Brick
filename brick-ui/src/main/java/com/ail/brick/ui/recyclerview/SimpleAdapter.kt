package com.ail.brick.ui.recyclerview

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

/**
 * 通用单类型 RecyclerView 适配器，基于 [ListAdapter] + ViewBinding。
 *
 * 内置 DiffUtil 差量更新，自动处理 item 点击与长按。
 * 支持设置空数据视图——当列表为空时自动显示 emptyView。
 *
 * ### 线程要求
 * [submitList] 必须在**主线程**调用（继承自 [ListAdapter] 约束），
 * DiffUtil 计算在后台线程进行，结果自动分发到主线程。
 *
 * ### 基本用法
 * ```kotlin
 * val adapter = SimpleAdapter<ItemUserBinding, User>(
 *     inflate = ItemUserBinding::inflate,
 *     diffCallback = object : DiffUtil.ItemCallback<User>() {
 *         override fun areItemsTheSame(old: User, new: User) = old.id == new.id
 *         override fun areContentsTheSame(old: User, new: User) = old == new
 *     }
 * ) { binding, item, position ->
 *     binding.tvName.text = item.name
 * }
 *
 * adapter.setOnItemClickListener { user, pos -> openDetail(user) }
 * recyclerView.adapter = adapter
 * adapter.submitList(userList)
 * ```
 *
 * ### 空视图用法
 * ```kotlin
 * adapter.setEmptyView(emptyView)  // 列表为空时自动显示
 * ```
 *
 * @param VB   ViewBinding 类型
 * @param T    数据类型
 * @param inflate       ViewBinding 的 inflate 函数引用
 * @param diffCallback  DiffUtil 比较回调
 * @param bind          数据绑定回调 (binding, item, position)
 */
class SimpleAdapter<VB : ViewBinding, T>(
    private val inflate: (LayoutInflater, ViewGroup, Boolean) -> VB,
    diffCallback: DiffUtil.ItemCallback<T>,
    private val bind: (VB, T, Int) -> Unit
) : ListAdapter<T, SimpleAdapter.BindingViewHolder<VB>>(diffCallback) {

    private var onItemClick: ((T, Int) -> Unit)? = null
    private var onItemLongClick: ((T, Int) -> Boolean)? = null
    private var emptyView: View? = null
    private var recyclerView: RecyclerView? = null

    private val dataObserver = object : RecyclerView.AdapterDataObserver() {
        override fun onChanged() = toggleEmptyView()
        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) = toggleEmptyView()
        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) = toggleEmptyView()
    }

    /** ViewBinding 持有的 ViewHolder */
    class BindingViewHolder<VB : ViewBinding>(val binding: VB) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BindingViewHolder<VB> {
        val binding = inflate(LayoutInflater.from(parent.context), parent, false)
        val holder = BindingViewHolder(binding)
        holder.itemView.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                onItemClick?.invoke(getItem(pos), pos)
            }
        }
        holder.itemView.setOnLongClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                onItemLongClick?.invoke(getItem(pos), pos) ?: false
            } else {
                false
            }
        }
        return holder
    }

    override fun onBindViewHolder(holder: BindingViewHolder<VB>, position: Int) {
        val item = getItem(position)
        bind(holder.binding, item, position)
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
        registerAdapterDataObserver(dataObserver)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        unregisterAdapterDataObserver(dataObserver)
        this.recyclerView = null
    }

    /**
     * 设置空数据视图。列表为空时显示该视图，有数据时自动隐藏。
     *
     * @param view 空数据占位视图，null 表示移除
     */
    fun setEmptyView(view: View?) {
        emptyView = view
        toggleEmptyView()
    }

    private fun toggleEmptyView() {
        val empty = itemCount == 0
        emptyView?.visibility = if (empty) View.VISIBLE else View.GONE
        recyclerView?.visibility = if (empty && emptyView != null) View.GONE else View.VISIBLE
    }

    /**
     * 设置 item 点击监听器。
     *
     * @param listener 回调 (item, position)
     */
    fun setOnItemClickListener(listener: (T, Int) -> Unit) {
        onItemClick = listener
    }

    /**
     * 设置 item 长按监听器。
     *
     * @param listener 回调 (item, position)，返回 true 表示已消费事件
     */
    fun setOnItemLongClickListener(listener: (T, Int) -> Boolean) {
        onItemLongClick = listener
    }
}
