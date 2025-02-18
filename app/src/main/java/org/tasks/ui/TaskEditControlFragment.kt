package org.tasks.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider

abstract class TaskEditControlFragment : Fragment() {
    lateinit var viewModel: TaskEditViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val composeView = ComposeView(requireActivity())
        viewModel = ViewModelProvider(requireParentFragment())[TaskEditViewModel::class.java]
        bind(composeView)
        createView(savedInstanceState)
        return composeView
    }

    abstract fun bind(parent: ViewGroup?): View

    protected open fun createView(savedInstanceState: Bundle?) {}
}