/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.tasks.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import com.todoroo.astrid.activity.MainActivity
import com.todoroo.astrid.activity.TaskListFragment
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.Strings.isNullOrEmpty
import org.tasks.data.dao.TagDao
import org.tasks.data.dao.TagDataDao
import org.tasks.data.entity.TagData
import org.tasks.filters.Filter
import org.tasks.filters.TagFilter
import org.tasks.themes.TasksIcons
import org.tasks.themes.TasksTheme
import javax.inject.Inject

@AndroidEntryPoint
class TagSettingsActivity : BaseListSettingsActivity() {
    @Inject lateinit var tagDataDao: TagDataDao
    @Inject lateinit var tagDao: TagDao
    @Inject lateinit var localBroadcastManager: LocalBroadcastManager

    private lateinit var tagData: TagData
    private val isNewTag: Boolean
        get() = tagData.id == null

    override val defaultIcon = TasksIcons.LABEL

    override fun onCreate(savedInstanceState: Bundle?) {
        tagData = intent.getParcelableExtra(EXTRA_TAG_DATA) ?: TagData()

        if (!isNewTag) baseViewModel.setTitle(tagData.name!!)

        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            baseViewModel.setColor(tagData.color ?: 0)
            baseViewModel.setIcon(tagData.icon ?: defaultIcon)
        }

        setContent {
            TasksTheme {
                BaseSettingsContent()
            }
        }
    }

    override val filter: Filter?
        get() = if (isNewTag) null else TagFilter(tagData)

    override val toolbarTitle: String
        get() = if (isNew) getString(R.string.new_tag) else tagData.name!!

    private val newName: String
        get() = baseViewModel.title.trim { it <= ' ' }

    private suspend fun clashes(newName: String): Boolean {
        return ((isNewTag || !newName.equals(tagData.name, ignoreCase = true))
                && tagDataDao.getTagByName(newName) != null)
    }

    override suspend fun save() {
        val newName = newName
        if (isNullOrEmpty(newName)) {
            baseViewModel.setError(getString(R.string.name_cannot_be_empty))
            return
        }
        if (clashes(newName)) {
            baseViewModel.setError(getString(R.string.tag_already_exists))
            return
        }
        if (isNewTag) {
            tagData
                .copy(
                    name = newName,
                    color = baseViewModel.color,
                    icon = baseViewModel.icon,
                )
                .let { it.copy(id = tagDataDao.insert(it)) }
                .let {
                    localBroadcastManager.broadcastRefresh()
                    setResult(
                        Activity.RESULT_OK,
                        Intent().putExtra(MainActivity.OPEN_FILTER, TagFilter(it))
                    )
                }
        } else if (hasChanges()) {
            tagData
                .copy(
                    name = newName,
                    color = baseViewModel.color,
                    icon = baseViewModel.icon,
                )
                .let {
                    tagDataDao.update(it)
                    tagDao.rename(it.remoteId!!, newName)
                    localBroadcastManager.broadcastRefresh()
                    setResult(
                        Activity.RESULT_OK,
                        Intent(TaskListFragment.ACTION_RELOAD)
                            .putExtra(MainActivity.OPEN_FILTER, TagFilter(it))
                    )
                }
        }
        finish()
    }

    override fun hasChanges(): Boolean {
        return if (isNewTag) {
            baseViewModel.color != 0 || baseViewModel.icon?.isBlank() == false || !isNullOrEmpty(newName)
        } else {
            baseViewModel.color != (tagData.color ?: 0)
                    || baseViewModel.icon != (tagData.icon ?: TasksIcons.LABEL)
                    || newName != tagData.name
        }
    }

    override fun finish() {
        //hideKeyboard(name)
        super.finish()
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

