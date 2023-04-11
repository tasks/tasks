package com.todoroo.astrid.core

import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.tasks.R
import org.tasks.databinding.CustomFilterRowBinding
import org.tasks.extensions.formatNumber
import org.tasks.preferences.ResourceResolver
import java.util.*

class CriterionViewHolder(
    private val context: Context,
    itemView: View,
    private val locale: Locale,
    private val onClick: (String) -> Unit
) : RecyclerView.ViewHolder(itemView) {

    private val divider: View
    private val icon: ImageView
    private val name: TextView
    private val filterCount: TextView
    private val row: View

    private lateinit var criterion: CriterionInstance

    init {
        CustomFilterRowBinding.bind(itemView).let {
            divider = it.divider
            icon = it.icon
            name = it.name
            filterCount = it.filterCount
            row = it.row
        }
        row.setOnClickListener { onClick(criterion.id) }
    }

    fun bind(criterion: CriterionInstance) {
        this.criterion = criterion
        val title: String = criterion.titleFromCriterion

        icon.visibility = if (criterion.type == CriterionInstance.TYPE_UNIVERSE) {
            View.INVISIBLE
        } else {
            View.VISIBLE
        }

        when (criterion.type) {
            CriterionInstance.TYPE_ADD -> {
                icon.setImageResource(R.drawable.ic_call_split_24px)
                divider.visibility = View.VISIBLE
            }
            CriterionInstance.TYPE_SUBTRACT -> {
                icon.setImageResource(R.drawable.ic_outline_not_interested_24px)
                divider.visibility = View.GONE
            }
            CriterionInstance.TYPE_INTERSECT -> {
                icon.setImageResource(R.drawable.ic_outline_add_24px)
                divider.visibility = View.GONE
            }
        }

        name.text = title
        filterCount.text = locale.formatNumber(criterion.end)

        row.isClickable = criterion.type != CriterionInstance.TYPE_UNIVERSE
    }

    fun setMoving(moving: Boolean) {
        if (moving) {
            row.setBackgroundColor(ResourceResolver.getData(context, androidx.appcompat.R.attr.colorControlHighlight))
        } else {
            row.setBackgroundResource(ResourceResolver.getResourceId(context, androidx.appcompat.R.attr.selectableItemBackground))
            row.background.jumpToCurrentState()
        }
    }
}