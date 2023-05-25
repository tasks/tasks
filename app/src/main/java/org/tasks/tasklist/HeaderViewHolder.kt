package org.tasks.tasklist

import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.todoroo.astrid.api.Filter
import org.tasks.R

class HeaderViewHolder(
        private val context: Context,
        private val headerFormatter: HeaderFormatter,
        view: View,
        callback: (Long) -> Unit
) : RecyclerView.ViewHolder(view) {
    private val title: TextView = view.findViewById(R.id.header)
    private val chevron: ImageView = view.findViewById(R.id.chevron)
    private val row = view.findViewById<View>(R.id.header_row)
    private var sortGroup = -1L
    private var rotation = 0f

    fun bind(filter: Filter, groupMode: Int, section: AdapterSection) {
        sortGroup = section.value
        val header = if (filter.supportsSorting()) {
            headerFormatter.headerStringBlocking(section.value)
        } else {
            null
        }

        if (header == null) {
            row.visibility = View.GONE
        } else {
            row.visibility = View.VISIBLE
            this.title.text = header
            this.title.setTextColor(section.headerColor(context, groupMode))
            rotation = if (section.collapsed) -180f else 0f
            chevron.rotation = rotation
        }
    }

    private fun rotate() {
        rotation = if (rotation == 0f) -180f else 0f
        chevron.animate().rotation(rotation).setDuration(250).start()
    }

    init {
        row.setOnClickListener {
            rotate()
            callback(sortGroup)
        }
    }
}