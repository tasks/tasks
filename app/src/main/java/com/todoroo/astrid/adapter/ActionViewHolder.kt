package com.todoroo.astrid.adapter

import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.tasks.databinding.FilterAdapterActionBinding
import org.tasks.filters.NavigationDrawerAction
import org.tasks.themes.DrawableUtil

class ActionViewHolder internal constructor(
    private val context: Context,
    itemView: View,
    private val onClick: (NavigationDrawerAction) -> Unit
) : RecyclerView.ViewHolder(itemView) {

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

    fun bind(filter: NavigationDrawerAction) {
        text.text = filter.listingTitle
        icon.setImageDrawable(DrawableUtil.getWrapped(context, filter.icon))
        row.setOnClickListener {
            onClick.invoke(filter)
        }
    }
}