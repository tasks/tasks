package org.tasks.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ContentAlpha
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.composethemeadapter.MdcTheme
import org.tasks.compose.TaskEditIcon
import org.tasks.compose.TaskEditRow

abstract class TaskEditControlComposeFragment : TaskEditControlFragment() {

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

    override fun bind(parent: ViewGroup?): View =
        (parent as ComposeView).apply {
            setContent {
                MdcTheme {
                    TaskEditRow(
                        icon = { Icon() },
                        content = { Body() },
                        onClick = if (this@TaskEditControlComposeFragment.isClickable)
                            this@TaskEditControlComposeFragment::onRowClick
                        else
                            null
                    )
                }
            }
        }

    @Composable
    protected open fun Icon() {
        TaskEditIcon(
            id = icon,
            modifier = Modifier
                .padding(start = 16.dp, top = 20.dp, end = 32.dp, bottom = 20.dp)
                .alpha(ContentAlpha.medium),
        )
    }

    @Composable
    protected abstract fun Body()
}