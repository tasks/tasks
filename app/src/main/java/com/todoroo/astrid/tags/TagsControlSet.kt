package com.todoroo.astrid.tags

import android.app.Activity
import android.content.Intent
import android.view.ViewGroup
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.accompanist.flowlayout.FlowRow
import com.google.android.material.composethemeadapter.MdcTheme
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.data.TagData
import org.tasks.tags.TagPickerActivity
import org.tasks.ui.ChipProvider
import org.tasks.ui.TaskEditControlFragment
import javax.inject.Inject

@AndroidEntryPoint
class TagsControlSet : TaskEditControlFragment() {
    @Inject lateinit var chipProvider: ChipProvider
    
    override fun onRowClick() {
        val intent = Intent(context, TagPickerActivity::class.java)
        intent.putParcelableArrayListExtra(TagPickerActivity.EXTRA_SELECTED, viewModel.selectedTags.value)
        startActivityForResult(intent, REQUEST_TAG_PICKER_ACTIVITY)
    }

    override fun bind(parent: ViewGroup?) =
        (parent?.findViewById(R.id.compose_view) as ComposeView).apply {
            setContent {
                MdcTheme {
                    val tags = viewModel.selectedTags.collectAsState()
                    FlowRow(
                        mainAxisSpacing = 4.dp,
                        crossAxisSpacing = 4.dp,
                        modifier = Modifier.padding(top = 20.dp, bottom = 20.dp, end = 16.dp)
                    ) {
                        if (tags.value.isEmpty()) {
                            Text(
                                text = stringResource(id = R.string.add_tags),
                                style = MaterialTheme.typography.body1,
                                color = colorResource(id = R.color.text_tertiary),
                            )
                        } else {
                            tags.value.sortedBy(TagData::name).forEach { tag ->
                                chipProvider.TagChip(tag, this@TagsControlSet::onRowClick)
                            }
                        }
                    }
                }
            }
        }

    override val isClickable = true

    override val icon = R.drawable.ic_outline_label_24px

    override fun controlId() = TAG

    override val rootLayout = R.layout.control_set_template_compose

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