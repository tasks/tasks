package org.tasks.ui

import android.app.Activity
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import com.google.android.material.composethemeadapter.MdcTheme
import com.todoroo.astrid.api.CaldavFilter
import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.api.GtasksFilter
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.activities.ListPicker
import org.tasks.compose.collectAsStateLifecycleAware
import org.tasks.compose.edit.ListRow
import javax.inject.Inject

@AndroidEntryPoint
class ListFragment : TaskEditControlFragment() {
    @Inject lateinit var chipProvider: ChipProvider

    override fun bind(parent: ViewGroup?): View =
        (parent as ComposeView).apply {
            setContent {
                MdcTheme {
                    ListRow(
                        list = viewModel.selectedList.collectAsStateLifecycleAware().value,
                        colorProvider = { chipProvider.getColor(it) },
                        onClick = {
                            ListPicker.newListPicker(
                                viewModel.selectedList.value,
                                this@ListFragment,
                                REQUEST_CODE_SELECT_LIST
                            )
                                .show(parentFragmentManager, FRAG_TAG_GOOGLE_TASK_LIST_SELECTION)
                        }
                    )
                }
            }
        }


    override fun controlId() = TAG

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
