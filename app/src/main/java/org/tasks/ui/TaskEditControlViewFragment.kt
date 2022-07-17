package org.tasks.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.lifecycle.ViewModelProvider
import org.tasks.R

abstract class TaskEditControlViewFragment : TaskEditControlFragment() {
    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.control_set_template, null)
        viewModel = ViewModelProvider(requireParentFragment())[TaskEditViewModel::class.java]
        val content = view.findViewById<ViewGroup>(R.id.content)
        bind(content)
        val icon = view.findViewById<ImageView>(R.id.icon)
        icon.setImageResource(this.icon)
        if (isClickable) {
            content.setOnClickListener { onRowClick() }
        }

        createView(savedInstanceState)

        return view
    }
}