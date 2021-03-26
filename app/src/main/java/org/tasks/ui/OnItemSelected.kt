package org.tasks.ui

import android.view.View
import android.widget.AdapterView

abstract class OnItemSelected : AdapterView.OnItemSelectedListener {
    final override fun onNothingSelected(parent: AdapterView<*>?) {}
    final override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) =
        onItemSelected(position)

    abstract fun onItemSelected(position: Int)
}