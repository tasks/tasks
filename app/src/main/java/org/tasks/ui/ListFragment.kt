package org.tasks.ui

import android.app.Activity
import android.content.Intent
import android.view.ViewGroup
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import com.google.accompanist.flowlayout.FlowRow
import com.google.android.material.composethemeadapter.MdcTheme
import com.todoroo.astrid.api.CaldavFilter
import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.api.GtasksFilter
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.activities.ListPicker
import javax.inject.Inject

@AndroidEntryPoint
class ListFragment : TaskEditControlFragment() {
    @Inject lateinit var chipProvider: ChipProvider

    override fun bind(parent: ViewGroup?) =
        (parent?.findViewById(R.id.compose_view) as ComposeView).apply {
            setContent {
                MdcTheme {
                    val list = viewModel.selectedList.collectAsState()
                    val selectedList = list.value ?: return@MdcTheme
                    FlowRow(modifier = Modifier.padding(vertical = 20.dp)) {
                        chipProvider.FilterChip(
                            filter = selectedList,
                            defaultIcon = R.drawable.ic_list_24px,
                            showText = true,
                            showIcon = true,
                            onClick = { openPicker() }
                        )
                    }
                }
            }
        }

    override val icon = R.drawable.ic_list_24px

    override fun controlId() = TAG

    override fun onRowClick() = openPicker()

    override val isClickable = true

    override val rootLayout = R.layout.control_set_template_compose

    private fun openPicker() =
            ListPicker.newListPicker(viewModel.selectedList.value!!, this, REQUEST_CODE_SELECT_LIST)
                    .show(parentFragmentManager, FRAG_TAG_GOOGLE_TASK_LIST_SELECTION)

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_SELECT_LIST) {
            if (resultCode == Activity.RESULT_OK) {
                data?.getParcelableExtra<Filter>(ListPicker.EXTRA_SELECTED_FILTER)?.let {
                    if (it is GtasksFilter || it is CaldavFilter) {
                        viewModel.selectedList.value = it
                    } else {
                        throw RuntimeException("Unhandled filter type")
                    }
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    companion object {
        const val TAG = R.string.TEA_ctrl_google_task_list
        private const val FRAG_TAG_GOOGLE_TASK_LIST_SELECTION = "frag_tag_google_task_list_selection"
        private const val REQUEST_CODE_SELECT_LIST = 10101
    }
}