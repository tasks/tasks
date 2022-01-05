package com.todoroo.astrid.adapter

import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.todoroo.astrid.api.FilterListItem
import org.tasks.databinding.FilterAdapterActionBinding
import org.tasks.themes.DrawableUtil

class ActionViewHolder internal constructor(
    private val context: Context,
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
        icon.setImageDrawable(DrawableUtil.getWrapped(context, filter.icon))
        if (onClick != null) {
            row.setOnClickListener {
                onClick.invoke(filter)
            }
        }
    }
}