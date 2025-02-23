package com.todoroo.astrid.tags

import android.app.Activity
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.core.content.IntentCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentSet
import org.tasks.R
import org.tasks.compose.edit.TagsRow
import org.tasks.data.entity.TagData
import org.tasks.tags.TagPickerActivity
import org.tasks.ui.ChipProvider
import org.tasks.ui.TaskEditControlFragment
import javax.inject.Inject

@AndroidEntryPoint
class TagsControlSet : TaskEditControlFragment() {
    @Inject lateinit var chipProvider: ChipProvider
    
    @Composable
    override fun Content() {
        val viewState = viewModel.viewState.collectAsStateWithLifecycle().value
        TagsRow(
            tags = viewState.tags,
            colorProvider = { chipProvider.getColor(it) },
            onClick = {
                val intent = Intent(context, TagPickerActivity::class.java)
                intent.putParcelableArrayListExtra(
                    TagPickerActivity.EXTRA_SELECTED,
                    ArrayList(viewState.tags)
                )
                startActivityForResult(intent, REQUEST_TAG_PICKER_ACTIVITY)
            },
            onClear = { viewModel.setTags(viewState.tags.minus(it)) },
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_TAG_PICKER_ACTIVITY) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                viewModel.setTags(
                    IntentCompat
                        .getParcelableArrayListExtra(
                            data,
                            TagPickerActivity.EXTRA_SELECTED,
                            TagData::class.java
                        )
                        ?.toPersistentSet()
                        ?: persistentSetOf()
                )
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    companion object {
        val TAG = R.string.TEA_ctrl_lists_pref
        private const val REQUEST_TAG_PICKER_ACTIVITY = 10582
    }
}
