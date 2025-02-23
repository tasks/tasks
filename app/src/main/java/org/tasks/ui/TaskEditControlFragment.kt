package org.tasks.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.fragment.app.Fragment
import androidx.fragment.compose.content
import androidx.hilt.navigation.compose.hiltViewModel

abstract class TaskEditControlFragment : Fragment() {
    lateinit var viewModel: TaskEditViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = content {
        viewModel = hiltViewModel<TaskEditViewModel>(viewModelStoreOwner = requireParentFragment())
        Content()
    }

    @Composable
    abstract fun Content()
}