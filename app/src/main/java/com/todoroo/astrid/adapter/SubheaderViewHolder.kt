package com.todoroo.astrid.adapter

import android.content.Intent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.data.CaldavDao
import org.tasks.data.GoogleTaskDao
import org.tasks.databinding.FilterAdapterSubheaderBinding
import org.tasks.filters.NavigationDrawerSubheader
import org.tasks.filters.NavigationDrawerSubheader.SubheaderType
import org.tasks.preferences.MainPreferences
import org.tasks.preferences.Preferences
import org.tasks.themes.DrawableUtil

internal class SubheaderViewHolder(
        itemView: View,
        private val activity: AppCompatActivity,
        private val preferences: Preferences,
        private val googleTaskDao: GoogleTaskDao,
        private val caldavDao: CaldavDao,
        private val localBroadcastManager: LocalBroadcastManager)
    : RecyclerView.ViewHolder(itemView) {

    private val text: TextView
    private val errorIcon: ImageView

    private lateinit var subheader: NavigationDrawerSubheader

    private fun onClick() {
        activity.lifecycleScope.launch {
            val collapsed = !subheader.isCollapsed
            when (subheader.subheaderType) {
                SubheaderType.PREFERENCE -> preferences.setBoolean(subheader.id.toInt(), collapsed)
                SubheaderType.GOOGLE_TASKS -> googleTaskDao.setCollapsed(subheader.id, collapsed)
                SubheaderType.CALDAV, SubheaderType.TASKS, SubheaderType.ETESYNC ->
                    caldavDao.setCollapsed(subheader.id, collapsed)
            }
            localBroadcastManager.broadcastRefreshList()
        }
    }

    fun bind(subheader: NavigationDrawerSubheader) {
        this.subheader = subheader
        text.text = subheader.listingTitle
        when {
            subheader.error -> with(errorIcon) {
                setColorFilter(ContextCompat.getColor(activity, R.color.overdue))
                visibility = View.VISIBLE
            }
            subheader.subheaderType == SubheaderType.ETESYNC -> with(errorIcon) {
                setColorFilter(ContextCompat.getColor(activity, R.color.orange_500))
                visibility = View.VISIBLE
            }
            else -> errorIcon.visibility = View.GONE
        }
        DrawableUtil.setRightDrawable(
                itemView.context,
                text,
                if (subheader.isCollapsed) R.drawable.ic_keyboard_arrow_down_black_18dp else R.drawable.ic_keyboard_arrow_up_black_18dp)
    }

    init {
        FilterAdapterSubheaderBinding.bind(itemView).let {
            text = it.text
            errorIcon = it.iconError
            it.subheaderRow.setOnClickListener { onClick() }
        }
        errorIcon.setOnClickListener {
            activity.startActivity(Intent(activity, MainPreferences::class.java))
        }
    }
}