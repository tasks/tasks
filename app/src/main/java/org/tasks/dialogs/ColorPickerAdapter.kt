package org.tasks.dialogs

import android.app.Activity
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import org.tasks.R
import org.tasks.billing.Inventory
import org.tasks.dialogs.ColorPalettePicker.Pickable

class ColorPickerAdapter(
    private val activity: Activity,
    private val inventory: Inventory,
    private val onSelected: (Int) -> Unit
) : ListAdapter<Pickable, IconPickerHolder>(DiffCallback()) {

    enum class Palette {
        COLORS,
        ACCENTS,
        LAUNCHERS,
        WIDGET
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IconPickerHolder {
        val view = activity.layoutInflater.inflate(R.layout.dialog_icon_picker_cell, parent, false)
        return IconPickerHolder(activity, view, onSelected)
    }

    override fun onBindViewHolder(holder: IconPickerHolder, position: Int) {
        val pickable = getItem(position)
        val available = inventory.purchasedThemes() || pickable.isFree
        holder.bind(
            position,
            if (available) R.drawable.color_picker else R.drawable.ic_outline_vpn_key_24px,
            pickable.pickerColor,
            1f,
            available
        )
    }

    private class DiffCallback : DiffUtil.ItemCallback<Pickable>() {
        override fun areItemsTheSame(oldItem: Pickable, newItem: Pickable): Boolean =
                oldItem == newItem

        override fun areContentsTheSame(oldItem: Pickable, newItem: Pickable): Boolean = true
    }
}