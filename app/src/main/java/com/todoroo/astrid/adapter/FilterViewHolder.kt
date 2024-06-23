package com.todoroo.astrid.adapter

import android.content.Context
import android.view.View
import android.widget.CheckedTextView
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.core.view.isVisible
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.tasks.R
import org.tasks.billing.Inventory
import org.tasks.compose.components.imageVectorByName
import org.tasks.databinding.FilterAdapterRowBinding
import org.tasks.extensions.formatNumber
import org.tasks.filters.CaldavFilter
import org.tasks.filters.Filter
import org.tasks.filters.getIcon
import org.tasks.themes.ColorProvider
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
    private val size: TextView
    private val shareIndicator: ImageView
    private val icon = MutableStateFlow<String?>(null)
    private val color = MutableStateFlow(0)

    lateinit var filter: Filter

    init {
        FilterAdapterRowBinding.bind(itemView).let {
            row = it.row
            text = it.text
            it.icon.setContent {
                val label = icon.collectAsStateWithLifecycle().value
                val tint = color.collectAsStateWithLifecycle().value
                Icon(
                    imageVector = imageVectorByName(label!!)!!,
                    contentDescription = label,
                    tint = if (tint == 0) MaterialTheme.colorScheme.onSurface else Color(tint),
                )
            }
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
        icon.update { filter.getIcon(inventory) }
        color.update { getColor(filter) }
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
}
