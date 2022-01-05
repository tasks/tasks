package com.todoroo.astrid.adapter

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.tasks.LocalBroadcastManager
import org.tasks.data.CaldavDao
import org.tasks.data.GoogleTaskDao
import org.tasks.dialogs.NewFilterDialog
import org.tasks.filters.NavigationDrawerSubheader
import org.tasks.filters.NavigationDrawerSubheader.SubheaderType.*
import org.tasks.preferences.MainPreferences
import org.tasks.preferences.Preferences
import org.tasks.ui.NavigationDrawerFragment
import javax.inject.Inject

class SubheaderClickHandler @Inject constructor(
    private val activity: Activity,
    private val preferences: Preferences,
    private val googleTaskDao: GoogleTaskDao,
    private val caldavDao: CaldavDao,
    private val localBroadcastManager: LocalBroadcastManager,
): SubheaderViewHolder.ClickHandler {
    override fun onClick(subheader: NavigationDrawerSubheader) {
        (activity as AppCompatActivity).lifecycleScope.launch {
            val collapsed = !subheader.isCollapsed
            when (subheader.subheaderType) {
                PREFERENCE -> preferences.setBoolean(subheader.id.toInt(), collapsed)
                GOOGLE_TASKS -> googleTaskDao.setCollapsed(subheader.id, collapsed)
                CALDAV, TASKS, ETESYNC -> caldavDao.setCollapsed(subheader.id, collapsed)
            }
            localBroadcastManager.broadcastRefreshList()
        }
    }

    override fun onAdd(subheader: NavigationDrawerSubheader) {
        when (subheader.addIntentRc) {
            NavigationDrawerFragment.REQUEST_NEW_FILTER ->
                NewFilterDialog.newFilterDialog().show(
                    (activity as AppCompatActivity).supportFragmentManager,
                    FRAG_TAG_NEW_FILTER
                )
            else -> activity.startActivityForResult(subheader.addIntent, subheader.addIntentRc)
        }
    }

    override fun showError() =
        activity.startActivity(Intent(activity, MainPreferences::class.java))

    companion object {
        private const val FRAG_TAG_NEW_FILTER = "frag_tag_new_filter"
    }
}