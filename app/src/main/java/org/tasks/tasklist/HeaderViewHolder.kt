package org.tasks.tasklist

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.todoroo.astrid.api.Filter
import org.tasks.R
import java.util.*

class HeaderViewHolder(
        private val context: Context,
        private val locale: Locale,
        view: View,
        callback: (Long) -> Unit) : RecyclerView.ViewHolder(view) {

    private val header: TextView = view.findViewById(R.id.header)
    private var sortGroup = -1L

    fun bind(filter: Filter, sortMode: Int, alwaysDisplayFullDate: Boolean, section: AdapterSection) {
        sortGroup = section.value
        val header = if (filter.supportsSorting()) {
            section.headerString(context, locale, sortMode, alwaysDisplayFullDate)
        } else {
            null
        }

        if (header == null) {
            this.header.visibility = View.GONE
        } else {
            this.header.visibility = View.VISIBLE
            this.header.text = header
            this.header.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, if (section.collapsed) R.drawable.ic_keyboard_arrow_down_black_18dp else R.drawable.ic_keyboard_arrow_up_black_18dp, 0)
            this.header.setTextColor(section.headerColor(context, sortMode))
        }
    }

    init {
        header.setOnClickListener {
            callback(sortGroup)
        }
    }
}