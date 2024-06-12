package com.todoroo.astrid.adapter

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.tasks.LocalBroadcastManager
import org.tasks.data.dao.CaldavDao
import org.tasks.filters.NavigationDrawerSubheader
import org.tasks.filters.NavigationDrawerSubheader.SubheaderType.CALDAV
import org.tasks.filters.NavigationDrawerSubheader.SubheaderType.ETESYNC
import org.tasks.filters.NavigationDrawerSubheader.SubheaderType.GOOGLE_TASKS
import org.tasks.filters.NavigationDrawerSubheader.SubheaderType.PREFERENCE
import org.tasks.filters.NavigationDrawerSubheader.SubheaderType.TASKS
import org.tasks.preferences.MainPreferences
import org.tasks.preferences.Preferences
import javax.inject.Inject

class SubheaderClickHandler @Inject constructor(
    private val activity: Activity,
    private val preferences: Preferences,
    private val caldavDao: CaldavDao,
    private val localBroadcastManager: LocalBroadcastManager,
): SubheaderViewHolder.ClickHandler {
    override fun onClick(subheader: NavigationDrawerSubheader) {
        (activity as AppCompatActivity).lifecycleScope.launch {
            val collapsed = !subheader.isCollapsed
            when (subheader.subheaderType) {
                PREFERENCE -> preferences.setBoolean(subheader.id.toInt(), collapsed)
                GOOGLE_TASKS,
                CALDAV,
                TASKS,
                ETESYNC -> caldavDao.setCollapsed(subheader.id, collapsed)
            }
            localBroadcastManager.broadcastRefreshList()
        }
    }

    override fun showError() =
        activity.startActivity(Intent(activity, MainPreferences::class.java))

    companion object {
        const val FRAG_TAG_NEW_FILTER = "frag_tag_new_filter"
    }
}