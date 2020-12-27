package com.todoroo.astrid.core

import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import org.tasks.R
import org.tasks.locale.Locale
import org.tasks.preferences.ResourceResolver

class CriterionViewHolder(
        private val context: Context,
        itemView: View,
        private val locale: Locale,
        private val onClick: (String) -> Unit)
    : RecyclerView.ViewHolder(itemView) {

    @BindView(R.id.divider)
    lateinit var divider: View

    @BindView(R.id.icon)
    lateinit var icon: ImageView

    @BindView(R.id.name)
    lateinit var name: TextView

    @BindView(R.id.filter_count)
    lateinit var filterCount: TextView

    @BindView(R.id.row)
    lateinit var row: View

    private lateinit var criterion: CriterionInstance

    init {
        ButterKnife.bind(this, itemView)
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

    @OnClick(R.id.row)
    fun onClick() = onClick(criterion.id)

    fun setMoving(moving: Boolean) {
        if (moving) {
            row.setBackgroundColor(ResourceResolver.getData(context, R.attr.colorControlHighlight))
        } else {
            row.setBackgroundResource(ResourceResolver.getResourceId(context, R.attr.selectableItemBackground))
            row.background.jumpToCurrentState()
        }
    }
}