package org.tasks.tags

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.RecyclerView
import org.tasks.R
import org.tasks.billing.Inventory
import org.tasks.data.TagData
import org.tasks.databinding.RowTagPickerBinding
import org.tasks.themes.ColorProvider
import org.tasks.themes.CustomIcons.getIconResId

internal class TagRecyclerAdapter(
        private val context: Context,
        private val viewModel: TagPickerViewModel,
        private val inventory: Inventory,
        private val colorProvider: ColorProvider,
        private val callback: (TagData, TagPickerViewHolder) -> Unit
) : RecyclerView.Adapter<TagPickerViewHolder>() {

    private val differ: AsyncListDiffer<TagData> = AsyncListDiffer(this, TagDiffCallback())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        TagPickerViewHolder(
            context,
            RowTagPickerBinding.inflate(LayoutInflater.from(context), parent, false),
            callback
        )

    override fun onBindViewHolder(holder: TagPickerViewHolder, position: Int) {
        val tagData = differ.currentList[position]
        holder.bind(tagData, getColor(tagData), getIcon(tagData), viewModel.getState(tagData))
    }

    override fun getItemCount(): Int = differ.currentList.size

    private fun getColor(tagData: TagData): Int {
        if (tagData.getColor() != 0) {
            val themeColor = colorProvider.getThemeColor(tagData.getColor()!!, true)
            if (inventory.purchasedThemes() || themeColor.isFree) {
                return themeColor.primaryColor
            }
        }
        return context.getColor(R.color.icon_tint_with_alpha)
    }

    private fun getIcon(tagData: TagData): Int? =
            tagData.getIcon()?.takeIf { it < 1000 || inventory.hasPro }?.let { getIconResId(it) }

    fun submitList(tagData: List<TagData>?) {
        differ.submitList(tagData)
    }
}