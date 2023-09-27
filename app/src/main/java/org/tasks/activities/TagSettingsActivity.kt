/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.tasks.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import androidx.core.widget.addTextChangedListener
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.todoroo.astrid.activity.MainActivity
import com.todoroo.astrid.activity.TaskListFragment
import com.todoroo.astrid.api.TagFilter
import com.todoroo.astrid.helper.UUIDHelper
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.Strings.isNullOrEmpty
import org.tasks.data.TagDao
import org.tasks.data.TagData
import org.tasks.data.TagDataDao
import org.tasks.databinding.ActivityTagSettingsBinding
import org.tasks.extensions.Context.hideKeyboard
import org.tasks.themes.CustomIcons
import javax.inject.Inject

@AndroidEntryPoint
class TagSettingsActivity : BaseListSettingsActivity() {
    @Inject lateinit var tagDataDao: TagDataDao
    @Inject lateinit var tagDao: TagDao

    private lateinit var name: TextInputEditText
    private lateinit var nameLayout: TextInputLayout

    private var isNewTag = false
    private lateinit var tagData: TagData
    override val defaultIcon: Int = CustomIcons.LABEL

    override fun onCreate(savedInstanceState: Bundle?) {
        tagData = intent.getParcelableExtra(EXTRA_TAG_DATA)
                ?: TagData().apply {
                    isNewTag = true
                    remoteId = UUIDHelper.newUUID()
                }
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            selectedColor = tagData.getColor()!!
            selectedIcon = tagData.getIcon()!!
        }
        name.setText(tagData.name)
        if (isNewTag) {
            name.requestFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(name, InputMethodManager.SHOW_IMPLICIT)
        }
        updateTheme()
    }

    override val isNew: Boolean
        get() = isNewTag

    override val toolbarTitle: String
        get() = if (isNew) getString(R.string.new_tag) else tagData.name!!

    private val newName: String
        get() = name.text.toString().trim { it <= ' ' }

    private suspend fun clashes(newName: String): Boolean {
        return ((isNewTag || !newName.equals(tagData.name, ignoreCase = true))
                && tagDataDao.getTagByName(newName) != null)
    }

    override suspend fun save() {
        val newName = newName
        if (isNullOrEmpty(newName)) {
            nameLayout.error = getString(R.string.name_cannot_be_empty)
            return
        }
        if (clashes(newName)) {
            nameLayout.error = getString(R.string.tag_already_exists)
            return
        }
        if (isNewTag) {
            tagData.name = newName
            tagData.setColor(selectedColor)
            tagData.setIcon(selectedIcon)
            tagDataDao.createNew(tagData)
            setResult(Activity.RESULT_OK, Intent().putExtra(MainActivity.OPEN_FILTER, TagFilter(tagData)))
        } else if (hasChanges()) {
            tagData.name = newName
            tagData.setColor(selectedColor)
            tagData.setIcon(selectedIcon)
            tagDataDao.update(tagData)
            tagDao.rename(tagData.remoteId!!, newName)
            setResult(
                    Activity.RESULT_OK,
                    Intent(TaskListFragment.ACTION_RELOAD)
                            .putExtra(MainActivity.OPEN_FILTER, TagFilter(tagData)))
        }
        finish()
    }

    override fun hasChanges(): Boolean {
        return if (isNewTag) {
            selectedColor >= 0 || selectedIcon >= 0 || !isNullOrEmpty(newName)
        } else {
            selectedColor != tagData.getColor()
                    || selectedIcon != tagData.getIcon()
                    || newName != tagData.name
        }
    }

    override fun finish() {
        hideKeyboard(name)
        super.finish()
    }

    override fun bind() = ActivityTagSettingsBinding.inflate(layoutInflater).let {
        name = it.name.apply {
            addTextChangedListener(
                onTextChanged = { _, _, _, _ -> nameLayout.error = null }
            )
        }
        nameLayout = it.nameLayout
        it.root
    }

    override suspend fun delete() {
        val uuid = tagData.remoteId
        tagDataDao.delete(tagData)
        setResult(
                Activity.RESULT_OK,
                Intent(TaskListFragment.ACTION_DELETED).putExtra(EXTRA_TAG_UUID, uuid))
        finish()
    }

    companion object {
        const val EXTRA_TAG_DATA = "tagData" // $NON-NLS-1$
        private const val EXTRA_TAG_UUID = "uuid" // $NON-NLS-1$
    }
}