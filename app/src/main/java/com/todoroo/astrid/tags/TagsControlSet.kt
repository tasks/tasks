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
import android.widget.TextView
import butterknife.BindView
import com.google.android.material.chip.ChipGroup
import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.astrid.data.Task
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.data.TagDao
import org.tasks.data.TagData
import org.tasks.data.TagDataDao
import org.tasks.tags.TagPickerActivity
import org.tasks.ui.ChipProvider
import org.tasks.ui.TaskEditControlFragment
import java.util.*
import javax.inject.Inject

/**
 * Control set to manage adding and removing tags
 *
 * @author Tim Su <tim></tim>@todoroo.com>
 */
@AndroidEntryPoint
class TagsControlSet : TaskEditControlFragment() {
    @Inject lateinit var tagDao: TagDao
    @Inject lateinit var tagDataDao: TagDataDao
    @Inject lateinit var chipProvider: ChipProvider
    
    @BindView(R.id.no_tags)
    lateinit var tagsDisplay: TextView

    @BindView(R.id.chip_group)
    lateinit var chipGroup: ChipGroup
    
    private lateinit var originalTags: ArrayList<TagData>
    private lateinit var selectedTags: ArrayList<TagData>

    override suspend fun createView(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            originalTags = ArrayList(if (task.isNew) {
                task.tags.mapNotNull { tagDataDao.getTagByName(it) }
            } else {
                tagDataDao.getTagDataForTask(task.id)
            })
            selectedTags = ArrayList(originalTags)
            refreshDisplayView()
        } else {
            selectedTags = savedInstanceState.getParcelableArrayList(EXTRA_SELECTED_TAGS)!!
            originalTags = savedInstanceState.getParcelableArrayList(EXTRA_ORIGINAL_TAGS)!!
            refreshDisplayView()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelableArrayList(EXTRA_SELECTED_TAGS, selectedTags)
        outState.putParcelableArrayList(EXTRA_ORIGINAL_TAGS, originalTags)
    }

    override val layout: Int
        get() = R.layout.control_set_tags

    override suspend fun apply(task: Task) {
        if (tagDao.applyTags(task, tagDataDao, selectedTags)) {
            task.modificationDate = DateUtilities.now()
        }
    }

    override fun onRowClick() {
        val intent = Intent(context, TagPickerActivity::class.java)
        intent.putParcelableArrayListExtra(TagPickerActivity.EXTRA_SELECTED, selectedTags)
        startActivityForResult(intent, REQUEST_TAG_PICKER_ACTIVITY)
    }

    override val isClickable: Boolean
        get() = true

    override val icon: Int
        get() = R.drawable.ic_outline_label_24px

    override fun controlId() = TAG

    override suspend fun hasChanges(original: Task): Boolean {
        return HashSet(originalTags) != HashSet(selectedTags)
    }

    private fun refreshDisplayView() {
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_TAG_PICKER_ACTIVITY) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                selectedTags = data.getParcelableArrayListExtra(TagPickerActivity.EXTRA_SELECTED)!!
                refreshDisplayView()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun requiresId() = true

    companion object {
        const val TAG = R.string.TEA_ctrl_lists_pref
        private const val EXTRA_ORIGINAL_TAGS = "extra_original_tags"
        private const val EXTRA_SELECTED_TAGS = "extra_selected_tags"
        private const val REQUEST_TAG_PICKER_ACTIVITY = 10582
    }
}