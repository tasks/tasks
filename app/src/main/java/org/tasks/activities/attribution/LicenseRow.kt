package org.tasks.activities.attribution

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.ButterKnife
import org.tasks.R

internal class LicenseRow(itemView: View) : RecyclerView.ViewHolder(itemView) {
    @BindView(R.id.copyright_holder)
    lateinit var copyrightHolder: TextView

    @BindView(R.id.libraries)
    lateinit var libraries: TextView

    init {
        ButterKnife.bind(this, itemView)
    }

    fun bind(copyrightHolder: String?, libraries: String?) {
        this.copyrightHolder.text = copyrightHolder
        this.libraries.text = libraries
    }
}