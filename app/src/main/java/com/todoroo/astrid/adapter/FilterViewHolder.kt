package com.todoroo.astrid.adapter

import android.app.Activity
import android.view.View
import android.widget.CheckedTextView
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.ButterKnife
import com.todoroo.astrid.api.*
import org.tasks.R
import org.tasks.billing.Inventory
import org.tasks.filters.PlaceFilter
import org.tasks.locale.Locale
import org.tasks.themes.ColorProvider
import org.tasks.themes.CustomIcons.getIconResId
import org.tasks.themes.DrawableUtil

class FilterViewHolder internal constructor(
        itemView: View,
        private val navigationDrawer: Boolean,
        private val locale: Locale,
        private val activity: Activity,
        private val inventory: Inventory,
        private val colorProvider: ColorProvider,
        private val onClick: ((FilterListItem?) -> Unit)?) : RecyclerView.ViewHolder(itemView) {

    @BindView(R.id.row)
    lateinit var row: View

    @BindView(R.id.text)
    lateinit var text: CheckedTextView

    @BindView(R.id.icon)
    lateinit var icon: ImageView

    @BindView(R.id.size)
    lateinit var size: TextView

    @BindView(R.id.share_indicator)
    lateinit var shareIndicator: ImageView

    lateinit var filter: FilterListItem

    init {
        ButterKnife.bind(this, itemView)
        if (navigationDrawer) {
            text.checkMarkDrawable = null
        }
    }

    fun setMoving(moving: Boolean) {
        itemView.isSelected = moving
    }

    fun bind(filter: FilterListItem, selected: Boolean, count: Int?) {
        this.filter = filter
        if (navigationDrawer) {
            itemView.isSelected = selected
        } else {
            text.isChecked = selected
        }
        val icon = getIcon(filter)
        this.icon.setImageDrawable(DrawableUtil.getWrapped(activity, icon))
        this.icon.drawable.setTint(getColor(filter))
        text.text = filter.listingTitle
        if (count == null || count == 0) {
            size.visibility = View.INVISIBLE
        } else {
            size.text = locale.formatNumber(count)
            size.visibility = View.VISIBLE
        }
        shareIndicator.visibility = if (filter.shared) View.VISIBLE else View.GONE
        if (onClick != null) {
            row.setOnClickListener {
                onClick.invoke(filter)
            }
        }
    }

    private fun getColor(filter: FilterListItem): Int {
        if (filter.tint != 0) {
            val color = colorProvider.getThemeColor(filter.tint, true)
            if (color.isFree || inventory.purchasedThemes()) {
                return color.primaryColor
            }
        }
        return activity.getColor(R.color.text_primary)
    }

    private fun getIcon(filter: FilterListItem): Int {
        if (filter.icon < 1000 || inventory.hasPro) {
            val icon = getIconResId(filter.icon)
            if (icon != null) {
                return icon
            }
        }
        return when (filter) {
            is TagFilter -> R.drawable.ic_outline_label_24px
            is GtasksFilter -> R.drawable.ic_list_24px
            is CaldavFilter -> R.drawable.ic_list_24px
            is CustomFilter -> R.drawable.ic_outline_filter_list_24px
            is PlaceFilter -> R.drawable.ic_outline_place_24px
            else -> filter.icon
        }
    }
}