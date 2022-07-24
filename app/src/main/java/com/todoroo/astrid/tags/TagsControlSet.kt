package com.todoroo.astrid.tags

import android.app.Activity
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import com.google.android.material.composethemeadapter.MdcTheme
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.compose.collectAsStateLifecycleAware
import org.tasks.compose.edit.TagsRow
import org.tasks.tags.TagPickerActivity
import org.tasks.ui.ChipProvider
import org.tasks.ui.TaskEditControlComposeFragment
import javax.inject.Inject

@AndroidEntryPoint
class TagsControlSet : TaskEditControlComposeFragment() {
    @Inject lateinit var chipProvider: ChipProvider
    
    override fun onRowClick() {
        val intent = Intent(context, TagPickerActivity::class.java)
        intent.putParcelableArrayListExtra(TagPickerActivity.EXTRA_SELECTED, viewModel.selectedTags.value)
        startActivityForResult(intent, REQUEST_TAG_PICKER_ACTIVITY)
    }

    override fun bind(parent: ViewGroup?): View =
        (parent as ComposeView).apply {
            setContent {
                MdcTheme {
                    TagsRow(
                        tags = viewModel.selectedTags.collectAsStateLifecycleAware().value,
                        colorProvider = { chipProvider.getColor(it) },
                        onClick = this@TagsControlSet::onRowClick
                    )
                }
            }
        }

    override fun controlId() = TAG

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_TAG_PICKER_ACTIVITY) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                viewModel.selectedTags.value =
                    data.getParcelableArrayListExtra(TagPickerActivity.EXTRA_SELECTED)
                        ?: ArrayList()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    companion object {
        const val TAG = R.string.TEA_ctrl_lists_pref
        private const val REQUEST_TAG_PICKER_ACTIVITY = 10582
    }
}
