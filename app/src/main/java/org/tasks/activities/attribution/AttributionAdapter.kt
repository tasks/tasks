package org.tasks.activities.attribution

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.tasks.R

class AttributionAdapter internal constructor(private val rows: List<AttributionRow>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == 0) {
            LicenseHeader(inflater.inflate(R.layout.row_attribution_header, parent, false))
        } else {
            LicenseRow(inflater.inflate(R.layout.row_attribution, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val row = rows[position]
        if (getItemViewType(position) == 0) {
            (holder as LicenseHeader).bind(row.license)
        } else {
            (holder as LicenseRow).bind(row.copyrightHolder, row.libraries)
        }
    }

    override fun getItemViewType(position: Int) = if (rows[position].isHeader) 0 else 1

    override fun getItemCount() = rows.size
}