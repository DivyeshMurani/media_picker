class ExampleDualLayoutAdapter : BaseListAdapter<YourDataModel, ViewDataBinding>(DiffCallback()) {
    
    companion object {
        private const val VIEW_TYPE_ONE = R.layout.item_layout_one
        private const val VIEW_TYPE_TWO = R.layout.item_layout_two
    }
    
    override fun getItemLayoutId(position: Int): Int {
        // Decide which layout to use based on your condition
        return when {
            getItem(position).someCondition -> VIEW_TYPE_ONE
            else -> VIEW_TYPE_TWO
        }
    }
    
    override fun bind(
        binding: ViewDataBinding,
        item: YourDataModel,
        position: Int,
        payloads: MutableList<Any>?
    ) {
        when (binding) {
            is ItemLayoutOneBinding -> {
                // Bind first layout type
                binding.apply {
                    title.text = item.title
                    // ... other bindings
                }
            }
            is ItemLayoutTwoBinding -> {
                // Bind second layout type
                binding.apply {
                    description.text = item.description
                    // ... other bindings
                }
            }
        }
    }
    
    class DiffCallback : DiffUtil.ItemCallback<YourDataModel>() {
        override fun areItemsTheSame(oldItem: YourDataModel, newItem: YourDataModel): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: YourDataModel, newItem: YourDataModel): Boolean {
            return oldItem == newItem
        }
    }
} 