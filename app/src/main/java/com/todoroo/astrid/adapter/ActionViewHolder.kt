package com.todoroo.astrid.adapter

import android.app.Activity
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.todoroo.astrid.api.FilterListItem
import org.tasks.databinding.FilterAdapterActionBinding
import org.tasks.themes.DrawableUtil

class ActionViewHolder internal constructor(
        private val activity: Activity,
        itemView: View,
        private val onClick: ((FilterListItem?) -> Unit)?) : RecyclerView.ViewHolder(itemView) {

    private val row: View
    private val text: TextView
    private val icon: ImageView

    init {
        FilterAdapterActionBinding.bind(itemView).let {
            row = it.row
            text = it.text
            icon = it.icon
        }
    }

    fun bind(filter: FilterListItem) {
        text.text = filter.listingTitle
        icon.setImageDrawable(DrawableUtil.getWrapped(activity, filter.icon))
        if (onClick != null) {
            row.setOnClickListener {
                onClick.invoke(filter)
            }
        }
    }
}