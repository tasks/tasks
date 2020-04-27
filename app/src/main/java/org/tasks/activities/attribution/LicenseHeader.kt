package org.tasks.activities.attribution

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.ButterKnife
import org.tasks.R

internal class LicenseHeader(itemView: View) : RecyclerView.ViewHolder(itemView) {

    @BindView(R.id.license_name)
    lateinit var licenseName: TextView

    init {
        ButterKnife.bind(this, itemView)
    }

    fun bind(license: String?) {
        licenseName.text = license
    }
}