package com.io.media.base


import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

abstract class BaseListAdapter<T, VB : ViewDataBinding>(
    diffCallback: DiffUtil.ItemCallback<T>
) : ListAdapter<T, BaseListAdapter.BaseViewHolder<VB>>(diffCallback) {

    class BaseViewHolder<VB : ViewDataBinding>(val binding: VB) : RecyclerView.ViewHolder(binding.root)

    /**
     * Returns the layout resource ID for the given position.
     * Override this method to provide different layouts based on position or item type
     */
    abstract fun getItemLayoutId(position: Int): Int

    /**
     * Binds the data to the ViewDataBinding instance.
     */
    abstract fun bind(binding: VB, item: T, indexOf: Int,payloads: MutableList<Any>?)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<VB> {
        val inflater = LayoutInflater.from(parent.context)
        val binding = DataBindingUtil.inflate<VB>(inflater, viewType, parent, false)
        return BaseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BaseViewHolder<VB>, position: Int) {
//        bind(holder.binding, getItem(position), position)
//        holder.binding.executePendingBindings()
    }

    override fun onBindViewHolder(
        holder: BaseViewHolder<VB>,
        position: Int,
        payloads: MutableList<Any>
    ) {
        super.onBindViewHolder(holder, position, payloads)

        bind(holder.binding, getItem(position), position,payloads)
        holder.binding.executePendingBindings()
    }

    override fun getItemViewType(position: Int): Int {
        return getItemLayoutId(position)
    }
}