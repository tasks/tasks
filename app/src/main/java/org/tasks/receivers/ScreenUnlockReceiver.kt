package org.tasks.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.tasks.widget.AppWidgetManager
import timber.log.Timber

class ScreenUnlockReceiver(private val appWidgetManager: AppWidgetManager) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_USER_PRESENT,
            Intent.ACTION_SCREEN_ON -> {
                Timber.d("refreshing widgets: ${intent.action}")
                appWidgetManager.updateWidgets()
            }
        }
    }
}
