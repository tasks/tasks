/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.tags

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.chip.ChipGroup
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.data.TagData
import org.tasks.databinding.ControlSetTagsBinding
import org.tasks.tags.TagPickerActivity
import org.tasks.ui.ChipProvider
import org.tasks.ui.TaskEditControlFragment
import javax.inject.Inject

/**
 * Control set to manage adding and removing tags
 *
 * @author Tim Su <tim></tim>@todoroo.com>
 */
@AndroidEntryPoint
class TagsControlSet : TaskEditControlFragment() {
    @Inject lateinit var chipProvider: ChipProvider
    
    private lateinit var tagsDisplay: TextView
    private lateinit var chipGroup: ChipGroup
    
    override fun createView(savedInstanceState: Bundle?) {
        refreshDisplayView()
    }

    override fun onRowClick() {
        val intent = Intent(context, TagPickerActivity::class.java)
        intent.putParcelableArrayListExtra(TagPickerActivity.EXTRA_SELECTED, viewModel.selectedTags)
        startActivityForResult(intent, REQUEST_TAG_PICKER_ACTIVITY)
    }

    override fun bind(parent: ViewGroup?) =
        ControlSetTagsBinding.inflate(layoutInflater, parent, true).let {
            tagsDisplay = it.noTags
            chipGroup = it.chipGroup
            it.root
        }

    override val isClickable = true

    override val icon = R.drawable.ic_outline_label_24px

    override fun controlId() = TAG

    private fun refreshDisplayView() {
        viewModel.selectedTags?.let { selectedTags ->
            if (selectedTags.isEmpty()) {
                chipGroup.visibility = View.GONE
                tagsDisplay.visibility = View.VISIBLE
            } else {
                tagsDisplay.visibility = View.GONE
                chipGroup.visibility = View.VISIBLE
                chipGroup.removeAllViews()
                for (tagData in selectedTags.sortedBy(TagData::name)) {
                    val chip = chipProvider.newClosableChip(tagData)
                    chipProvider.apply(chip, tagData)
                    chip.setOnClickListener { onRowClick() }
                    chip.setOnCloseIconClickListener {
                        selectedTags.remove(tagData)
                        refreshDisplayView()
                    }
                    chipGroup.addView(chip)
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_TAG_PICKER_ACTIVITY) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                viewModel.selectedTags =
                        data.getParcelableArrayListExtra(TagPickerActivity.EXTRA_SELECTED)
                refreshDisplayView()
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