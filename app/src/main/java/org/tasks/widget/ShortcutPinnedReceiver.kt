package org.tasks.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.analytics.Firebase
import javax.inject.Inject

@AndroidEntryPoint
class ShortcutPinnedReceiver : BroadcastReceiver() {
    @Inject lateinit var firebase: Firebase

    override fun onReceive(context: Context, intent: Intent) {
        val source = intent.getStringExtra(EXTRA_SOURCE) ?: return
        firebase.logEvent(R.string.event_create_shortcut, R.string.param_type to source)
    }

    companion object {
        const val EXTRA_SOURCE = "extra_source"
    }
}
