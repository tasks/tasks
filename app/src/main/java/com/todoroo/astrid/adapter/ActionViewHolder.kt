package com.todoroo.astrid.adapter

import android.app.Activity
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.ButterKnife
import com.todoroo.astrid.api.FilterListItem
import org.tasks.R
import org.tasks.themes.DrawableUtil

class ActionViewHolder internal constructor(
        private val activity: Activity,
        itemView: View,
        private val onClick: ((FilterListItem?) -> Unit)?) : RecyclerView.ViewHolder(itemView) {

    @BindView(R.id.row)
    lateinit var row: View

    @BindView(R.id.text)
    lateinit var text: TextView

    @BindView(R.id.icon)
    lateinit var icon: ImageView

    init {
        ButterKnife.bind(this, itemView)
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