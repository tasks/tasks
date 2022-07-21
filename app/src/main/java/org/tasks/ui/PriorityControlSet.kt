package org.tasks.ui

import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import com.google.android.material.composethemeadapter.MdcTheme
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.compose.collectAsStateLifecycleAware
import org.tasks.compose.edit.PriorityRow

@AndroidEntryPoint
class PriorityControlSet : TaskEditControlComposeFragment() {

    override fun bind(parent: ViewGroup?): View =
        (parent as ComposeView).apply {
            setContent {
                MdcTheme {
                    PriorityRow(
                        priority = viewModel.priority.collectAsStateLifecycleAware().value,
                        onChangePriority = { viewModel.priority.value = it }
                    )
                }
            }
        }

    override fun controlId() = TAG

    companion object {
        const val TAG = R.string.TEA_ctrl_importance_pref
    }
}
