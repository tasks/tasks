package org.tasks.ui

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

abstract class TaskEditControlFragment : Fragment() {
    lateinit var viewModel: TaskEditViewModel

    protected open fun createView(savedInstanceState: Bundle?) {}

    protected open fun onRowClick() {}

    protected open val isClickable: Boolean
        get() = false

    protected open val icon = 0
    abstract fun controlId(): Int
    protected abstract fun bind(parent: ViewGroup?): View
}