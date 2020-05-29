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

class FilterViewHolder : RecyclerView.ViewHolder {
    @BindView(R.id.row)
    lateinit var row: View

    @BindView(R.id.text)
    lateinit var text: CheckedTextView

    @BindView(R.id.icon)
    lateinit var icon: ImageView

    @BindView(R.id.size)
    lateinit var size: TextView

    private var onClick: ((FilterListItem?) -> Unit)? = null
    private var navigationDrawer = false
    private var locale: Locale? = null
    private var activity: Activity? = null
    private var inventory: Inventory? = null
    private var colorProvider: ColorProvider? = null

    internal constructor(
            itemView: View,
            navigationDrawer: Boolean,
            locale: Locale,
            activity: Activity,
            inventory: Inventory,
            colorProvider: ColorProvider,
            onClick: ((FilterListItem?) -> Unit)?) : super(itemView) {
        this.inventory = inventory
        this.colorProvider = colorProvider
        ButterKnife.bind(this, itemView)
        this.navigationDrawer = navigationDrawer
        this.locale = locale
        this.activity = activity
        this.onClick = onClick
        if (navigationDrawer) {
            text.checkMarkDrawable = null
        }
    }

    internal constructor(itemView: View) : super(itemView)

    fun bind(filter: FilterListItem, selected: Boolean, count: Int?) {
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
            size.visibility = View.GONE
        } else {
            size.text = locale!!.formatNumber(count)
            size.visibility = View.VISIBLE
        }
        row.setOnClickListener {
            onClick?.invoke(filter)
        }
    }

    private fun getColor(filter: FilterListItem): Int {
        if (filter.tint != 0) {
            val color = colorProvider!!.getThemeColor(filter.tint, true)
            if (color.isFree || inventory!!.purchasedThemes()) {
                return color.primaryColor
            }
        }
        return activity!!.getColor(R.color.text_primary)
    }

    private fun getIcon(filter: FilterListItem): Int {
        if (filter.icon < 1000 || inventory!!.hasPro()) {
            val icon = getIconResId(filter.icon)
            if (icon != null) {
                return icon
            }
        }
        return when (filter) {
            is TagFilter -> R.drawable.ic_outline_label_24px
            is GtasksFilter -> R.drawable.ic_outline_cloud_24px
            is CaldavFilter -> R.drawable.ic_outline_cloud_24px
            is CustomFilter -> R.drawable.ic_outline_filter_list_24px
            is PlaceFilter -> R.drawable.ic_outline_place_24px
            else -> filter.icon
        }
    }
}