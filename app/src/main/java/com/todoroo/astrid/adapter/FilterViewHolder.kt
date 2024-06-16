package com.todoroo.astrid.adapter

import android.content.Context
import android.view.View
import android.widget.CheckedTextView
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import org.tasks.filters.CaldavFilter
import com.todoroo.astrid.api.CustomFilter
import org.tasks.filters.GtasksFilter
import org.tasks.filters.TagFilter
import org.tasks.R
import org.tasks.billing.Inventory
import org.tasks.databinding.FilterAdapterRowBinding
import org.tasks.extensions.formatNumber
import org.tasks.filters.Filter
import org.tasks.filters.PlaceFilter
import org.tasks.themes.ColorProvider
import org.tasks.themes.CustomIcons.getIconResId
import org.tasks.themes.DrawableUtil
import java.util.Locale

class FilterViewHolder internal constructor(
    itemView: View,
    private val navigationDrawer: Boolean,
    private val locale: Locale,
    private val context: Context,
    private val inventory: Inventory,
    private val colorProvider: ColorProvider,
    private val onClick: (Filter) -> Unit,
) : RecyclerView.ViewHolder(itemView) {

    private val row: View
    private val text: CheckedTextView
    private val icon: ImageView
    private val size: TextView
    private val shareIndicator: ImageView

    lateinit var filter: Filter

    init {
        FilterAdapterRowBinding.bind(itemView).let {
            row = it.row
            text = it.text
            icon = it.icon
            size = it.size
            shareIndicator = it.shareIndicator
        }
        if (navigationDrawer) {
            text.checkMarkDrawable = null
        }
    }

    fun setMoving(moving: Boolean) {
        itemView.isSelected = moving
    }

    fun bind(filter: Filter, selected: Boolean, count: Int?) {
        this.filter = filter
        if (navigationDrawer) {
            itemView.isSelected = selected
        } else {
            text.isChecked = selected
        }
        val icon = getIcon(filter)
        this.icon.setImageDrawable(DrawableUtil.getWrapped(context, icon))
        this.icon.drawable.setTint(getColor(filter))
        text.text = filter.title
        if (count == null || count == 0) {
            size.visibility = View.INVISIBLE
        } else {
            size.text = locale.formatNumber(count)
            size.visibility = View.VISIBLE
        }
        shareIndicator.apply {
            isVisible = filter is CaldavFilter && filter.principals > 0
            setImageResource(when {
                filter !is CaldavFilter -> 0
                filter.principals <= 0 -> 0
                filter.principals == 1 -> R.drawable.ic_outline_perm_identity_24px
                else -> R.drawable.ic_outline_people_outline_24
            })
        }
        row.setOnClickListener {
            onClick.invoke(filter)
        }
    }

    private fun getColor(filter: Filter): Int {
        if (filter.tint != 0) {
            val color = colorProvider.getThemeColor(filter.tint, true)
            if (color.isFree || inventory.purchasedThemes()) {
                return color.primaryColor
            }
        }
        return context.getColor(R.color.text_primary)
    }

    private fun getIcon(filter: Filter): Int {
        if (filter.icon < 1000 || inventory.hasPro) {
            val icon = getIconResId(filter.icon)
            if (icon != null) {
                return icon
            }
        }
        return when (filter) {
            is TagFilter -> R.drawable.ic_outline_label_24px
            is GtasksFilter,
            is CaldavFilter -> R.drawable.ic_list_24px
            is CustomFilter -> R.drawable.ic_outline_filter_list_24px
            is PlaceFilter -> R.drawable.ic_outline_place_24px
            else -> filter.icon
        }
    }
}