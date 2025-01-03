package com.todoroo.astrid.adapter

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.tasks.LocalBroadcastManager
import org.tasks.activities.TagSettingsActivity
import org.tasks.caldav.BaseCaldavCalendarSettingsActivity.Companion.EXTRA_CALDAV_ACCOUNT
import org.tasks.data.dao.CaldavDao
import org.tasks.data.listSettingsClass
import org.tasks.dialogs.NewFilterDialog
import org.tasks.filters.FilterProvider
import org.tasks.filters.FilterProvider.Companion.REQUEST_NEW_LIST
import org.tasks.filters.FilterProvider.Companion.REQUEST_NEW_PLACE
import org.tasks.filters.FilterProvider.Companion.REQUEST_NEW_TAGS
import org.tasks.filters.NavigationDrawerSubheader
import org.tasks.filters.NavigationDrawerSubheader.SubheaderType.CALDAV
import org.tasks.filters.NavigationDrawerSubheader.SubheaderType.PREFERENCE
import org.tasks.filters.NavigationDrawerSubheader.SubheaderType.TASKS
import org.tasks.location.LocationPickerActivity
import org.tasks.preferences.MainPreferences
import org.tasks.preferences.TasksPreferences
import timber.log.Timber
import javax.inject.Inject

class SubheaderClickHandler @Inject constructor(
    private val activity: Activity,
    private val tasksPreferences: TasksPreferences,
    private val caldavDao: CaldavDao,
    private val localBroadcastManager: LocalBroadcastManager,
): SubheaderViewHolder.ClickHandler {
    override fun onClick(subheader: NavigationDrawerSubheader) {
        (activity as AppCompatActivity).lifecycleScope.launch {
            val collapsed = !subheader.isCollapsed
            when (subheader.subheaderType) {
                PREFERENCE -> tasksPreferences.set(booleanPreferencesKey(subheader.id), collapsed)
                CALDAV,
                TASKS -> caldavDao.setCollapsed(subheader.id, collapsed)
            }
            localBroadcastManager.broadcastRefreshList()
        }
    }

    override fun onAdd(subheader: NavigationDrawerSubheader) {
        when (subheader.addIntentRc) {
            FilterProvider.REQUEST_NEW_FILTER ->
                NewFilterDialog.newFilterDialog().show(
                    (activity as AppCompatActivity).supportFragmentManager,
                    FRAG_TAG_NEW_FILTER
                )
            REQUEST_NEW_PLACE ->
                activity.startActivityForResult(
                    Intent(activity, LocationPickerActivity::class.java),
                    REQUEST_NEW_PLACE
                )
            REQUEST_NEW_TAGS ->
                activity.startActivityForResult(
                    Intent(activity, TagSettingsActivity::class.java),
                    REQUEST_NEW_LIST
                )
            REQUEST_NEW_LIST -> {
                (activity as AppCompatActivity).lifecycleScope.launch {
                    val account = caldavDao.getAccount(subheader.id.toLong()) ?: return@launch
                    when (subheader.subheaderType) {
                        NavigationDrawerSubheader.SubheaderType.CALDAV,
                        NavigationDrawerSubheader.SubheaderType.TASKS ->
                            activity.startActivityForResult(
                                Intent(activity, account.listSettingsClass())
                                    .putExtra(EXTRA_CALDAV_ACCOUNT, account),
                                REQUEST_NEW_LIST
                            )

                        else -> {}
                    }
                }
            }
            else -> Timber.e("Unhandled request code: $subheader")
        }

    }

    override fun showError() =
        activity.startActivity(Intent(activity, MainPreferences::class.java))

    companion object {
        const val FRAG_TAG_NEW_FILTER = "frag_tag_new_filter"
    }
}